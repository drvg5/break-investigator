"""Connects to the bank-core MCP server as a client (ADR-016): it runs as a stdio subprocess."""

import os
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from pathlib import Path

from langchain_core.tools import BaseTool
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_mcp_adapters.tools import load_mcp_tools

REPO_ROOT = Path(__file__).resolve().parents[3]
SERVER = "bank-core"


@asynccontextmanager
async def bank_core_tools() -> AsyncIterator[list[BaseTool]]:
    """Yield the server's tools. One session is held open, so the investigation shares one subprocess."""
    client = MultiServerMCPClient({
        SERVER: {
            "transport": "stdio",
            "command": "uv",
            "args": ["run", "--quiet", "--project", str(REPO_ROOT / "mcp-server"), "bank-core-mcp"],
            "env": dict(os.environ),
        }
    })
    async with client.session(SERVER) as session:
        yield await load_mcp_tools(session)
