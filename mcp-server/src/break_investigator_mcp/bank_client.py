"""Thin async HTTP client for the bank core service (Module 1). No business logic (ADR-015)."""

import os
from typing import Any

import httpx

DEFAULT_BASE_URL = "http://localhost:8080"


class BankApiError(Exception):
    """A failure the model should see as a short, actionable message."""


class BankClient:
    def __init__(self, base_url: str, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self.base_url = base_url.rstrip("/")
        self._transport = transport  # injectable for tests

    @classmethod
    def from_env(cls) -> "BankClient":
        return cls(os.environ.get("BANK_API_URL", DEFAULT_BASE_URL))

    async def get(self, path: str, params: dict[str, Any] | None = None) -> Any:
        params = {k: v for k, v in (params or {}).items() if v is not None}
        try:
            async with httpx.AsyncClient(base_url=self.base_url, timeout=10, transport=self._transport) as http:
                response = await http.get(path, params=params)
        except httpx.TransportError as e:
            raise BankApiError(
                f"Bank core service is unreachable at {self.base_url} ({type(e).__name__}). Is it running?"
            ) from e

        if response.status_code in (400, 404):
            # Spring returns RFC 9457 problem details; 'detail' is the human-readable reason.
            raise BankApiError(_problem_detail(response))
        if response.is_error:
            raise BankApiError(f"Bank core service returned HTTP {response.status_code} for {path}.")
        return response.json()


def _problem_detail(response: httpx.Response) -> str:
    try:
        return response.json().get("detail") or response.text
    except ValueError:
        return response.text
