"""Command line search, for trying queries by hand: `uv run kb-search "which SSI applies" --method hybrid -k 5`."""

import argparse
import json
import sys

from dotenv import load_dotenv

from . import store
from .embedding import Embedder
from .ingest import REPO_ROOT
from .retrieval import hybrid_search, keyword_search, vector_search


def main() -> None:
    parser = argparse.ArgumentParser(description="Search the knowledge base.")
    parser.add_argument("query")
    parser.add_argument("--method", choices=["keyword", "vector", "hybrid"], default="hybrid")
    parser.add_argument("-k", type=int, default=5, help="number of results (default 5)")
    parser.add_argument("--json", action="store_true", help="print JSON lines instead of a table")
    args = parser.parse_args()
    load_dotenv(REPO_ROOT / ".env")

    with store.connect() as conn:
        if args.method == "keyword":
            hits = keyword_search(conn, args.query, args.k)
        else:
            search = vector_search if args.method == "vector" else hybrid_search
            hits = search(conn, Embedder(), args.query, args.k)

    if not hits:
        sys.exit("No results.")
    for rank, hit in enumerate(hits, start=1):
        if args.json:
            print(json.dumps({"rank": rank, **hit.__dict__}))
        else:
            print(f"{rank}. {hit.score:.4f}  {hit.chunk_id}  ({hit.doc_title} > {hit.section})")


if __name__ == "__main__":
    main()
