"""Validate local prospective-public documentation/site links without network access."""
import json
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urlsplit

root = Path(__file__).resolve().parents[1]
paths = subprocess.check_output(["git", "ls-files", "--cached", "--others", "--exclude-standard"], cwd=root, text=True).splitlines()
files = sorted({p for p in paths if p == "README.md" or p.startswith(("docs/", "website/")) and Path(p).suffix in {".md", ".html"}})
checked = 0
for relative in files:
    file = root / relative
    text = file.read_text(encoding="utf-8")
    links = re.findall(r"(?:href|src)=[\"']([^\"']+)[\"']", text) if file.suffix == ".html" else re.findall(r"\[[^\]]*\]\(([^)]+)\)", text)
    for link in links:
        target = urlsplit(link.strip("<>"))
        if target.scheme or target.netloc:
            continue
        if not target.path:
            if file.suffix == ".html" and target.fragment:
                assert re.search(r"id=[\"']" + re.escape(target.fragment) + r"[\"']", text), f"Missing local fragment: {relative}"
            continue
        base = root / "website" if target.path.startswith("/") and relative.startswith("website/") else file.parent
        resolved = (base / unquote(target.path).lstrip("/")).resolve()
        assert resolved.is_relative_to(root) and resolved.exists(), f"Missing local link: {relative} -> {target.path}"
        if resolved.is_dir():
            assert (resolved / "index.html").is_file(), f"Missing route index: {relative}"
        checked += 1
result = {"files": len(files), "local_links": checked, "broken_links": 0, "network": "not used"}
output = root / "build-logs/site-link-check.json"
output.parent.mkdir(exist_ok=True)
output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result))
