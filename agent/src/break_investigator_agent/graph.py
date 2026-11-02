"""
The investigation workflow: fixed stages, with model-chosen tools only while gathering evidence (ADR-019).

load_break -> classify -> gather_evidence <-> tools -> analyse

The agent ends with a recommendation and has no write tools (ADR-020).
"""

import json
from typing import Any

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_core.tools import BaseTool
from langgraph.graph import END, START, StateGraph
from langgraph.prebuilt import ToolNode

from . import prompts
from .state import Classification, InvestigationState, Recommendation

MAX_TOOL_CALLS = 8
LIMIT_REACHED = "The tool call limit was reached, so the investigation may be incomplete."


class InvestigationError(Exception):
    """The investigation can't start, e.g. an unknown break reference or the bank service is down."""


def _text(content: Any) -> str:
    """Flatten a tool's output, which may be a string or a list of content blocks, into text."""
    if isinstance(content, str):
        return content
    return "\n".join(block.get("text", "") if isinstance(block, dict) else str(block) for block in content)


def _tool_calls_made(state: InvestigationState) -> int:
    return sum(isinstance(m, ToolMessage) for m in state.get("messages", []))


def _evidence_transcript(state: InvestigationState) -> str:
    """Only tool results reach the analysis, so it can't lean on the evidence stage's own commentary."""
    calls = {
        call["id"]: call
        for m in state.get("messages", []) if isinstance(m, AIMessage)
        for call in m.tool_calls
    }
    sections = [f"get_break({{\"break_ref\": \"{state['break_ref']}\"}}) returned:\n{state['break_record']}"]
    for m in state.get("messages", []):
        if isinstance(m, ToolMessage):
            call = calls[m.tool_call_id]
            sections.append(f"{call['name']}({json.dumps(call['args'])}) returned:\n{_text(m.content)}")
    return "\n\n".join(sections)


def build_graph(model: BaseChatModel, tools: list[BaseTool]):
    tools_by_name = {t.name: t for t in tools}
    classifier = model.with_structured_output(Classification, method="json_schema")
    investigator = model.bind_tools(tools, parallel_tool_calls=False)  # one at a time, so the limit is exact
    analyst = model.with_structured_output(Recommendation, method="json_schema")

    async def load_break(state: InvestigationState) -> dict:
        # Invoked as a tool call because MCP tools report failures as content; this exposes the error status.
        call = {"type": "tool_call", "name": "get_break", "args": {"break_ref": state["break_ref"]}, "id": "load_break"}
        result = await tools_by_name["get_break"].ainvoke(call)
        if result.status == "error":
            raise InvestigationError(_text(result.content))  # no break to investigate: stop before spending tokens
        return {"break_record": _text(result.content)}

    async def classify(state: InvestigationState) -> dict:
        result = await classifier.ainvoke([
            SystemMessage(prompts.CLASSIFY),
            HumanMessage(f"Break record:\n{state['break_record']}"),
        ])
        return {"classification": result}

    async def gather_evidence(state: InvestigationState) -> dict:
        if _tool_calls_made(state) >= MAX_TOOL_CALLS:
            return {}
        new_messages = []
        if not state.get("messages"):
            checks = "\n".join(f"- {c}" for c in state["classification"].checks)
            new_messages.append(HumanMessage(f"Break record:\n{state['break_record']}\n\nChecks to make:\n{checks}"))
        system = SystemMessage(prompts.GATHER_EVIDENCE.format(max_tool_calls=MAX_TOOL_CALLS))
        response = await investigator.ainvoke([system, *state.get("messages", []), *new_messages])
        return {"messages": [*new_messages, response]}

    def next_after_evidence(state: InvestigationState) -> str:
        if _tool_calls_made(state) >= MAX_TOOL_CALLS:
            return "analyse"
        last = state["messages"][-1]
        return "tools" if isinstance(last, AIMessage) and last.tool_calls else "analyse"

    async def analyse(state: InvestigationState) -> dict:
        request = f"Tool results:\n\n{_evidence_transcript(state)}"
        if _tool_calls_made(state) >= MAX_TOOL_CALLS:
            request += f"\n\n{LIMIT_REACHED} List what you could not check under unverified."
        result = await analyst.ainvoke([SystemMessage(prompts.ANALYSE), HumanMessage(request)])
        return {"recommendation": result}

    graph = StateGraph(InvestigationState)
    graph.add_node("load_break", load_break)
    graph.add_node("classify", classify)
    graph.add_node("gather_evidence", gather_evidence)
    graph.add_node("tools", ToolNode(tools, handle_tool_errors=True))  # errors go back to the model (ADR-015)
    graph.add_node("analyse", analyse)
    graph.add_edge(START, "load_break")
    graph.add_edge("load_break", "classify")
    graph.add_edge("classify", "gather_evidence")
    graph.add_conditional_edges("gather_evidence", next_after_evidence, ["tools", "analyse"])
    graph.add_edge("tools", "gather_evidence")
    graph.add_edge("analyse", END)
    return graph.compile()
