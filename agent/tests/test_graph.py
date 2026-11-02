"""Workflow behaviour with a scripted model and fake tools: stage order, step limit, errors, evidence (ADR-019, ADR-020)."""

import pytest
from langchain_core.messages import AIMessage
from langchain_core.runnables import RunnableLambda
from langchain_core.tools import ToolException, tool

from break_investigator_agent.graph import InvestigationError, LIMIT_REACHED, MAX_TOOL_CALLS, build_graph
from break_investigator_agent.state import Classification, Recommendation, RootCause


@tool
async def get_break(break_ref: str) -> str:
    """Get a break."""
    return '{"breakRef": "BRK-1", "tradeRef": "TRD-1", "symptom": "FAILED_TO_SETTLE"}'


@tool
async def get_trade(trade_ref: str) -> str:
    """Get a trade."""
    if trade_ref == "NOPE":
        raise ToolException("No trade NOPE")
    return '{"tradeRef": "TRD-1", "accountNumber": "111"}'


def trade_call(ref: str = "TRD-1", call_id: str = "1") -> AIMessage:
    return AIMessage(content="", tool_calls=[{"name": "get_trade", "args": {"trade_ref": ref}, "id": call_id}])


class ScriptedModel:
    """Stands in for the chat model: replays scripted turns for the evidence stage and records every request."""

    def __init__(self, turns):
        self.turns = iter(turns)
        self.requests = {"classify": [], "evidence": [], "analyse": []}
        self.bound_kwargs = None

    def with_structured_output(self, schema, method):
        assert method == "json_schema"  # forced tool use is rejected by the default model

        def respond(messages):
            if schema is Classification:
                self.requests["classify"].append(messages)
                return Classification(likely_causes=[RootCause.SSI_MISMATCH], checks=["compare accounts"])
            self.requests["analyse"].append(messages)
            return Recommendation(root_cause=RootCause.SSI_MISMATCH, confidence="high", explanation="x",
                                  proposed_fix="y", evidence=[], unverified=[])

        return RunnableLambda(respond)

    def bind_tools(self, tools, **kwargs):
        self.bound_kwargs = kwargs

        def respond(messages):
            self.requests["evidence"].append(messages)
            return next(self.turns)

        return RunnableLambda(respond)


async def run(model):
    return await build_graph(model, [get_break, get_trade]).ainvoke({"break_ref": "BRK-1"})


async def test_runs_every_stage_and_returns_a_recommendation():
    model = ScriptedModel([trade_call(), AIMessage(content="Done")])
    result = await run(model)
    assert result["recommendation"].root_cause is RootCause.SSI_MISMATCH
    assert all(len(model.requests[stage]) >= 1 for stage in model.requests)


async def test_break_is_loaded_before_the_model_is_asked_anything():
    model = ScriptedModel([AIMessage(content="Done")])
    await run(model)
    assert "FAILED_TO_SETTLE" in model.requests["classify"][0][1].content


async def test_classifier_checks_reach_the_evidence_stage():
    model = ScriptedModel([AIMessage(content="Done")])
    await run(model)
    assert "compare accounts" in model.requests["evidence"][0][1].content


async def test_tool_results_are_kept_across_evidence_turns():
    model = ScriptedModel([trade_call(), AIMessage(content="Done")])
    await run(model)
    second_turn = model.requests["evidence"][1]
    assert second_turn[1].type == "human" and second_turn[-1].type == "tool"


async def test_analysis_sees_tool_results_but_not_commentary():
    model = ScriptedModel([trade_call(), AIMessage(content="I think it is the price")])
    await run(model)
    request = model.requests["analyse"][0][1].content
    assert '"accountNumber": "111"' in request and "FAILED_TO_SETTLE" in request
    assert "I think it is the price" not in request
    assert LIMIT_REACHED not in request


async def test_step_limit_stops_tool_calls_and_is_reported():
    model = ScriptedModel([trade_call(call_id=str(i)) for i in range(MAX_TOOL_CALLS + 5)])
    result = await run(model)
    assert sum(m.type == "tool" for m in result["messages"]) == MAX_TOOL_CALLS
    assert LIMIT_REACHED in model.requests["analyse"][0][1].content
    assert "recommendation" in result


async def test_tool_error_goes_back_to_the_model():
    model = ScriptedModel([trade_call("NOPE"), AIMessage(content="Done")])
    await run(model)
    assert "No trade NOPE" in model.requests["evidence"][1][-1].content


async def test_failed_break_lookup_stops_before_any_model_call():
    @tool("get_break")
    async def failing_get_break(break_ref: str) -> str:
        """Get a break."""
        raise ToolException("No break BRK-1")

    failing_get_break.handle_tool_error = True  # how the MCP adapter's tools behave
    model = ScriptedModel([])
    with pytest.raises(InvestigationError, match="No break BRK-1"):
        await build_graph(model, [failing_get_break, get_trade]).ainvoke({"break_ref": "BRK-1"})
    assert model.requests["classify"] == []


async def test_tools_are_called_one_at_a_time():
    model = ScriptedModel([AIMessage(content="Done")])
    await run(model)
    assert model.bound_kwargs == {"parallel_tool_calls": False}
