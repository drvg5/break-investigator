"""Splits documents into one chunk per `##` section, with a size cap (ADR-022)."""

import re
from dataclasses import dataclass

from .documents import Document

MAX_CHARS = 1200
INTRO = "Introduction"


@dataclass(frozen=True)
class Chunk:
    chunk_id: str  # stable and human-readable, e.g. runbook-stale-ssi#resolution-steps; citations use it
    doc_id: str
    doc_title: str
    section: str
    ordinal: int  # position in the document, so a chunk's neighbours can be found
    content: str

    @property
    def embedding_text(self) -> str:
        """What gets embedded: a short header gives short sections the context of their document."""
        return f"{self.doc_title} > {self.section}\n\n{self.content}"


def chunk_document(doc: Document, max_chars: int = MAX_CHARS) -> list[Chunk]:
    chunks: list[Chunk] = []
    for section, body in _sections(doc.text):
        parts = _split(body, max_chars)
        for n, part in enumerate(parts, start=1):
            name = section if len(parts) == 1 else f"{section} (part {n})"
            chunks.append(Chunk(
                chunk_id=f"{doc.doc_id}#{_slug(name)}",
                doc_id=doc.doc_id,
                doc_title=doc.title,
                section=name,
                ordinal=len(chunks),
                content=part,
            ))
    return chunks


def chunk_documents(docs: list[Document], max_chars: int = MAX_CHARS) -> list[Chunk]:
    return [chunk for doc in docs for chunk in chunk_document(doc, max_chars)]


def _sections(text: str) -> list[tuple[str, str]]:
    """(heading, body) pairs. Text before the first `##` heading becomes an Introduction section."""
    sections, heading, lines = [], INTRO, []
    for line in text.splitlines():
        if line.startswith("## "):
            sections.append((heading, "\n".join(lines).strip()))
            heading, lines = line[3:].strip(), []
        else:
            lines.append(line)
    sections.append((heading, "\n".join(lines).strip()))
    return [(h, body) for h, body in sections if body]


def _split(body: str, max_chars: int) -> list[str]:
    """Packs whole paragraphs into parts of at most max_chars. A single longer paragraph stays whole."""
    parts, current = [], ""
    for paragraph in re.split(r"\n\s*\n", body):
        candidate = f"{current}\n\n{paragraph}" if current else paragraph
        if current and len(candidate) > max_chars:
            parts.append(current)
            current = paragraph
        else:
            current = candidate
    return [*parts, current] if current else parts


def _slug(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")
