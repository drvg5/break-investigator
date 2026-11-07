"""Command line entry point: load -> chunk -> embed -> store. Safe to rerun; each run rebuilds the table."""

import sys
from pathlib import Path

from dotenv import load_dotenv

from . import store
from .chunking import chunk_documents
from .documents import load_documents
from .embedding import MODEL, Embedder

REPO_ROOT = Path(__file__).resolve().parents[3]


def main() -> None:
    load_dotenv(REPO_ROOT / ".env")
    docs = load_documents()
    chunks = chunk_documents(docs)
    print(f"{len(docs)} documents -> {len(chunks)} chunks", file=sys.stderr)

    print(f"Embedding with {MODEL}...", file=sys.stderr)
    embeddings = Embedder().embed_passages([c.embedding_text for c in chunks])

    try:
        with store.connect() as conn:
            store.create_schema(conn)
            store.replace_all(conn, chunks, embeddings)
    except store.psycopg.OperationalError as e:
        sys.exit(f"Could not reach Postgres at {store.database_url().split('@')[-1]}. Is it running?\n{e}")
    print(f"Stored {len(chunks)} chunks in kb.chunks", file=sys.stderr)


if __name__ == "__main__":
    main()
