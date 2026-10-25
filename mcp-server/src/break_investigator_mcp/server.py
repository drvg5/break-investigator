"""
MCP server exposing the bank core service as read-only tools.

One tool per endpoint (ADR-012), five read tools (ADR-013). Descriptions explain what the
data means, never how to diagnose a break (ADR-014). Data is passed through as is (ADR-015).

Lists are wrapped in a named object. Returned bare, the SDK emits one content block per item
and none at all for an empty list, so "no confirmation received" would reach the model as a
blank response.
"""

import inspect
from typing import Annotated, Any, Literal

from mcp.server.mcpserver import MCPServer
from mcp.server.mcpserver.exceptions import ToolError
from mcp_types import ToolAnnotations
from pydantic import Field

from .bank_client import BankApiError, BankClient

mcp = MCPServer("bank-core")

# Every v1 tool is read-only (ADR-013); the hint lets MCP clients see that too.
READ_ONLY = ToolAnnotations(read_only_hint=True, destructive_hint=False, idempotent_hint=True)

# Module-level so tests can swap in a client with a mock transport.
bank = BankClient.from_env()



def read_tool(fn):
    """Register fn as a read-only tool, with its docstring dedented into the description the model sees."""
    return mcp.tool(annotations=READ_ONLY, description=inspect.cleandoc(fn.__doc__))(fn)


BreakRef = Annotated[str, Field(description="Break reference, e.g. BRK-0017")]
TradeRef = Annotated[str, Field(description="Our trade reference, e.g. TRD-240048")]


async def _get(path: str, params: dict[str, Any] | None = None) -> Any:
    try:
        return await bank.get(path, params)
    except BankApiError as e:
        raise ToolError(str(e)) from e


@read_tool
async def list_open_breaks() -> dict[str, Any]:
    """List the operations queue of open settlement breaks, oldest first.

    Returns {"breaks": [...]}. Each item has breakRef, tradeRef, symptom, status and detectedAt (UTC).
    The symptom (UNMATCHED, FAILED_TO_SETTLE or PENDING_PAST_CUTOFF) is what the
    monitoring system observed, not the root cause.
    """
    return {"breaks": await _get("/api/breaks", {"status": "OPEN"})}


@read_tool
async def get_break(break_ref: BreakRef) -> dict[str, Any]:
    """Get one settlement break: its trade reference, symptom, status, detection time and event history.

    The symptom describes what was observed, not why it happened. Timestamps are UTC.
    """
    return await _get(f"/api/breaks/{break_ref}")


@read_tool
async def get_trade(trade_ref: TradeRef) -> dict[str, Any]:
    """Get one of our booked trades as recorded in our books.

    Includes counterparty, instrument and market, side (BUY or SELL from our perspective),
    quantity, price, trade and settlement dates, status, and timestamps in UTC:
    bookedAt, and lastAmendedAt (null means the trade was never amended).
    settlementInstruction is the SSI version this trade was enriched with, including its
    account and effective dates; null means no SSI was attached to the trade.
    """
    return await _get(f"/api/trades/{trade_ref}")


@read_tool
async def get_confirmations(trade_ref: TradeRef) -> dict[str, Any]:
    """Get the confirmations the counterparty sent for one of our trades.

    Returns {"confirmations": [...]}, usually with zero or one item. An empty list means the counterparty
    has not sent a confirmation. counterpartySide is the counterparty's own side, so a
    correct confirmation of our BUY says SELL. custodianBic, accountNumber and
    placeOfSettlement are where the counterparty expects to settle. remarks is free text
    written by the counterparty.
    """
    return {"confirmations": await _get(f"/api/trades/{trade_ref}/confirmations")}


@read_tool
async def get_settlement_instructions(
    counterparty_code: Annotated[str, Field(description="Counterparty code, e.g. GRANITEB (from get_trade)")],
    market: Annotated[Literal["US", "CA"] | None, Field(description="Optional market filter")] = None,
) -> dict[str, Any]:
    """Get every standing settlement instruction (SSI) version we hold for a counterparty.

    Returns {"settlementInstructions": [...]}, sorted by market, then by effectiveFrom with the
    latest first. No field marks a version as current. Includes expired versions. Each version is valid from effectiveFrom to effectiveTo,
    both inclusive; effectiveTo null means open-ended. An empty list means we hold no SSI
    for that counterparty (and market, if given).
    """
    ssis = await _get(f"/api/counterparties/{counterparty_code}/settlement-instructions", {"market": market})
    return {"settlementInstructions": ssis}


def main() -> None:
    mcp.run()  # stdio transport (ADR-016)


if __name__ == "__main__":
    main()
