"""Command line entry point. Progress goes to stderr and the recommendation JSON to stdout, so it can be piped to jq."""

import argparse
import asyncio
import os
import sys

from dotenv import load_dotenv
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import AIMessage

from .graph import InvestigationError, build_graph
from .state import Recommendation
from .tools import REPO_ROOT, bank_core_tools

DEFAULT_MODEL = "claude-opus-5-5"


def build_model() -> ChatAnthropic:
    return ChatAnthropic(
        model=os.environ.get("AGENT_MODEL", DEFAULT_MODEL),
        max_tokens=16000,
        reasoning_effort="high",
        # Retry on another model if safety classifiers decline a request.
        betas=["server-side-fallback-2026-07-01"],
        model_kwargs={"fallbacks": "default"},
    )


def _log(message: str) -> None:
    print(message, file=sys.stderr)


async def investigate(break_ref: str) -> Recommendation:
    async with bank_core_tools() as tools:
        graph = build_graph(build_model(), tools)
        result = None
        try:
            async for update in graph.astream({"break_ref": break_ref}, stream_mode="updates", config={"recursion_limit": 40}):
                for stage, output in update.items():
                    _log(f"[{stage}]")
                    for message in (output or {}).get("messages", []):
                        if isinstance(message, AIMessage):
                            for call in message.tool_calls:
                                _log(f"  {call['name']}({call['args']})")
                    if output and "recommendation" in output:
                        result = output["recommendation"]
            return result
        except InvestigationError as e:
            failure = e  # raised after the session closes, or its task group wraps it in an ExceptionGroup
    raise failure


def main() -> None:
    parser = argparse.ArgumentParser(description="Investigate a settlement break and recommend a fix.")
    parser.add_argument("break_ref", help="Break reference, e.g. BRK-0017")
    args = parser.parse_args()
    load_dotenv(REPO_ROOT / ".env")
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("ANTHROPIC_API_KEY is not set. Add it to .env at the repo root (see .env.example).")
    try:
        recommendation = asyncio.run(investigate(args.break_ref))
    except InvestigationError as e:
        sys.exit(str(e))
    print(recommendation.model_dump_json(indent=2))


if __name__ == "__main__":
    main()
