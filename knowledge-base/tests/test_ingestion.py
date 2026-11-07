"""Documents and chunking: structure, size cap, stable citation ids, and the synthetic-data rule. No database needed."""

import re

import pytest

from break_investigator_kb.chunking import INTRO, MAX_CHARS, chunk_document, chunk_documents
from break_investigator_kb.documents import Document, load_documents

DOCS = load_documents()
CHUNKS = chunk_documents(DOCS)


def doc(text: str) -> Document:
    return Document(doc_id="test-doc", title="Test Doc", text=text)


def test_every_document_loads_with_a_title():
    assert len(DOCS) >= 12
    assert all(d.title and d.text for d in DOCS)


def test_one_chunk_per_section_named_after_its_heading():
    chunks = chunk_document(doc("## First\nAlpha.\n\n## Second\nBeta."))
    assert [(c.section, c.content) for c in chunks] == [("First", "Alpha."), ("Second", "Beta.")]
    assert [c.chunk_id for c in chunks] == ["test-doc#first", "test-doc#second"]


def test_text_before_the_first_heading_becomes_an_introduction():
    chunks = chunk_document(doc("Opening words.\n\n## Body\nText."))
    assert chunks[0].section == INTRO and chunks[0].content == "Opening words."


def test_long_sections_split_at_paragraphs_into_numbered_parts():
    paragraphs = [f"Paragraph {i} " + "x" * 80 for i in range(10)]
    chunks = chunk_document(doc("## Long\n" + "\n\n".join(paragraphs)), max_chars=300)
    assert len(chunks) > 1
    assert chunks[0].section == "Long (part 1)" and chunks[0].chunk_id == "test-doc#long-part-1"
    assert all(len(c.content) <= 300 for c in chunks)
    assert "\n\n".join(c.content for c in chunks) == "\n\n".join(paragraphs)  # nothing lost or reordered


def test_embedding_text_carries_the_document_and_section():
    chunk = chunk_document(doc("## Resolution steps\nDo the thing."))[0]
    assert chunk.embedding_text.startswith("Test Doc > Resolution steps\n\n")


def test_real_chunks_have_unique_ids_and_respect_the_cap():
    ids = [c.chunk_id for c in CHUNKS]
    assert len(ids) == len(set(ids))
    assert all(len(c.content) <= MAX_CHARS for c in CHUNKS)


def test_chunk_ids_are_stable_across_runs():
    # Retrieval labels point at chunk ids, so re-ingesting must not change them.
    assert [c.chunk_id for c in chunk_documents(load_documents())] == [c.chunk_id for c in CHUNKS]


@pytest.mark.parametrize("name", ["RBC", "Royal Bank", "Morgan Stanley"])
def test_no_employer_or_client_names(name):
    # CLAUDE.md hard rule: synthetic data only.
    for d in DOCS:
        assert not re.search(rf"\b{name}\b", d.title + d.text, re.IGNORECASE), d.doc_id
