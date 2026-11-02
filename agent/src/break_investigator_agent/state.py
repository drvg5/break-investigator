"""Data passed between graph stages, and the structured outputs the model must produce."""

from enum import StrEnum
from typing import Annotated, Literal, TypedDict

from langchain_core.messages import AnyMessage
from langgraph.graph.message import add_messages
from pydantic import BaseModel, Field


class RootCause(StrEnum):
    """Same labels as the seed manifest, so results can be scored against it. UNDETERMINED is the agent's way out."""

    QUANTITY_MISMATCH = "QUANTITY_MISMATCH"
    PRICE_MISMATCH = "PRICE_MISMATCH"
    SETTLEMENT_DATE_MISMATCH = "SETTLEMENT_DATE_MISMATCH"
    MISSING_CONFIRMATION = "MISSING_CONFIRMATION"
    SSI_MISMATCH = "SSI_MISMATCH"
    MISSING_SSI = "MISSING_SSI"
    STALE_SSI = "STALE_SSI"
    MISSED_CUTOFF = "MISSED_CUTOFF"
    UNDETERMINED = "UNDETERMINED"


class Classification(BaseModel):
    likely_causes: list[RootCause] = Field(description="Causes consistent with the symptom, most likely first")
    checks: list[str] = Field(description="What must be checked to tell these causes apart")


class Evidence(BaseModel):
    tool: str = Field(description="The tool whose result supports this claim")
    field: str = Field(description="The field in that result, e.g. settlementInstruction.accountNumber")
    value: str = Field(description="The value exactly as the tool returned it")
    finding: str = Field(description="What this value shows")


class Recommendation(BaseModel):
    root_cause: RootCause
    confidence: Literal["high", "medium", "low"]
    explanation: str = Field(description="Why the evidence points to this cause and rules out the alternatives")
    proposed_fix: str = Field(description="What the operations analyst should do. Nothing has been changed.")
    evidence: list[Evidence]
    unverified: list[str] = Field(description="Anything that could not be checked; empty if everything was")


class InvestigationState(TypedDict, total=False):
    break_ref: str
    break_record: str
    classification: Classification
    messages: Annotated[list[AnyMessage], add_messages]  # the evidence stage's conversation
    recommendation: Recommendation
