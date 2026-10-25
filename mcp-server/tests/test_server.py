"""Tool behaviour against a mocked bank API: registration, pass-through, errors (ADR-012..015)."""

import httpx
import pytest
from mcp.server.mcpserver.exceptions import ToolError

from break_investigator_mcp import server
from break_investigator_mcp.bank_client import BankClient

TRADE = {"tradeRef": "TRD-1", "counterpartyCode": "CPTY", "settlementInstruction": None}
 

def handler(request: httpx.Request) -> httpx.Response:
    path, params = request.url.path, dict(request.url.params)
    if path == "/api/breaks":
        return httpx.Response(200, json=[{"breakRef": "BRK-1", "status": params.get("status")}])
    if path == "/api/trades/TRD-1":
        return httpx.Response(200, json=TRADE)
    if path == "/api/trades/TRD-1/confirmations":
        return httpx.Response(200, json=[])
    if path == "/api/counterparties/CPTY/settlement-instructions":
        return httpx.Response(200, json=[{"market": params.get("market", "ALL")}])
    return httpx.Response(404, json={"title": "Not Found", "detail": f"No resource at {path}"})


@pytest.fixture(autouse=True)
def mock_bank(monkeypatch):
    monkeypatch.setattr(server, "bank", BankClient("http://bank.test", transport=httpx.MockTransport(handler)))


async def test_exposes_exactly_the_five_read_tools():
    names = {t.name for t in await server.mcp.list_tools()}
    assert names == {"list_open_breaks", "get_break", "get_trade", "get_confirmations",
                     "get_settlement_instructions"}


async def test_every_tool_is_marked_read_only():
    for tool in await server.mcp.list_tools():
        assert tool.annotations.read_only_hint is True, tool.name


async def test_descriptions_are_sent_without_source_indentation():
    for tool in await server.mcp.list_tools():
        assert "\n    " not in tool.description, tool.name


async def test_descriptions_explain_meaning_but_not_diagnosis():
    tools = {t.name: " ".join(t.description.split()) for t in await server.mcp.list_tools()}
    assert "empty list means the counterparty has not sent a confirmation" in tools["get_confirmations"]
    assert "Includes expired versions" in tools["get_settlement_instructions"]
    assert "No field marks a version as current" in tools["get_settlement_instructions"]
    # ADR-014: no diagnostic recipes. Root-cause names must never appear in tool descriptions.
    for description in tools.values():
        for leak in ("stale", "mismatch", "root cause is", "missed cut"):
            assert leak not in description.lower()


async def test_list_open_breaks_asks_only_for_open():
    assert await server.list_open_breaks() == {"breaks": [{"breakRef": "BRK-1", "status": "OPEN"}]}


async def test_get_trade_passes_data_through_unchanged():
    assert await server.get_trade("TRD-1") == TRADE


async def test_no_confirmation_is_an_empty_list_not_an_error():
    assert await server.get_confirmations("TRD-1") == {"confirmations": []}


async def test_market_filter_is_forwarded_only_when_given():
    assert await server.get_settlement_instructions("CPTY", "US") == {"settlementInstructions": [{"market": "US"}]}
    assert await server.get_settlement_instructions("CPTY") == {"settlementInstructions": [{"market": "ALL"}]}


async def test_empty_list_still_reaches_the_model_as_one_content_block():
    # Regression: bare list returns produced zero content blocks for [], so the model saw nothing.
    result = await server.mcp.call_tool("get_confirmations", {"trade_ref": "TRD-1"})
    blocks = result[0] if isinstance(result, tuple) else result.content
    assert len(blocks) == 1 and '"confirmations": []' in blocks[0].text


async def test_unknown_ref_becomes_a_readable_tool_error():
    with pytest.raises(ToolError, match="No resource at /api/trades/NOPE"):
        await server.get_trade("NOPE")


async def test_unreachable_backend_says_so(monkeypatch):
    def refuse(request):
        raise httpx.ConnectError("refused")

    monkeypatch.setattr(server, "bank", BankClient("http://bank.test", transport=httpx.MockTransport(refuse)))
    with pytest.raises(ToolError, match="unreachable at http://bank.test"):
        await server.get_break("BRK-1")
