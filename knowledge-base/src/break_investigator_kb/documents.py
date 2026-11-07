"""Loads the synthetic knowledge base: one Markdown file per document, titled by its first `# ` heading."""

from dataclasses import dataclass
from pathlib import Path

DOCS_DIR = Path(__file__).resolve().parents[2] / "docs"


@dataclass(frozen=True)
class Document:
    doc_id: str  # the file name without .md, e.g. runbook-stale-ssi
    title: str
    text: str  # everything after the title line


def load_documents(docs_dir: Path = DOCS_DIR) -> list[Document]:
    """Every .md file except README.md, sorted by name so ingestion is repeatable."""
    return [_parse(path) for path in sorted(docs_dir.glob("*.md")) if path.name != "README.md"]


def _parse(path: Path) -> Document:
    first_line, _, rest = path.read_text(encoding="utf-8").partition("\n")
    if not first_line.startswith("# "):
        raise ValueError(f"{path.name} must start with a '# ' title line")
    return Document(doc_id=path.stem, title=first_line[2:].strip(), text=rest.strip())
