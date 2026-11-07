"""Local embeddings with bge-small-en-v1.5 through fastembed (ADR-023). Nothing leaves the machine."""

from pathlib import Path

from fastembed import TextEmbedding

MODEL = "BAAI/bge-small-en-v1.5"
DIMENSIONS = 384
CACHE_DIR = Path.home() / ".cache" / "fastembed"  # the default is a temp dir that macOS clears

# bge v1.5 retrieves better when short queries carry this instruction; passages are embedded without it.
QUERY_INSTRUCTION = "Represent this sentence for searching relevant passages: "


class Embedder:
    def __init__(self) -> None:
        self._model = TextEmbedding(MODEL, cache_dir=str(CACHE_DIR))  # downloads about 70 MB on first use

    def embed_passages(self, texts: list[str]) -> list[list[float]]:
        return [vector.tolist() for vector in self._model.passage_embed(texts)]

    def embed_query(self, query: str) -> list[float]:
        return next(iter(self._model.query_embed(QUERY_INSTRUCTION + query))).tolist()
