"""Private text-only audit. Never opens media or prints matched values."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

TEXT = {".md", ".kt", ".kts", ".toml", ".xml", ".json", ".html", ".css", ".ps1", ".py", ".properties", ".txt", ".pro", ".yaml", ".yml", ".bat", ".sh"}
SECRET = {
    "private-key-header": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "credential-token": re.compile(r"(?:gh[pousr]_[A-Za-z0-9]{30,}|AKIA[A-Z0-9]{16}|sk-[A-Za-z0-9_-]{32,})"),
    "assigned-secret": re.compile(r'''(?i)(?:password|api[_-]?key|access[_-]?token|client[_-]?secret)\s*[:=]\s*["'][^"'\s]{12,}["']'''),
}
CLAIM = re.compile(r"(?i)(?:real|actual|private|nude|naked|explicit|corpus|dataset|benchmark).{0,90}(?:test|validat|imag|photo|video|result|parity)|(?:test|validat|parity).{0,90}(?:nude|naked|corpus|dataset)")

def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args], stderr=subprocess.DEVNULL)

def redacted(path):
    return "[text-path-" + hashlib.sha256(path.encode()).hexdigest()[:12] + "]"

def current(root):
    paths = sorted(set(git(root, "ls-files", "--cached", "--others", "--exclude-standard", "-z").decode().split("\0")))
    findings, descriptions, forbidden = [], [], []
    inspected = 0
    for path in filter(None, paths):
        file = root / path
        if file.suffix.lower() in {".jpg", ".jpeg", ".png", ".mp4", ".webm", ".jks", ".keystore", ".pem"}:
            forbidden.append({"path": redacted(path), "category": "media-or-signing-material-name"})
        if (file.suffix.lower() not in TEXT and file.name not in {"LICENSE", ".gitignore", "gradlew"}) or not file.is_file():
            continue
        inspected += 1
        content = file.read_text(encoding="utf-8-sig", errors="replace")
        for number, line in enumerate(content.splitlines(), 1):
            for category, pattern in SECRET.items():
                if pattern.search(line):
                    findings.append({"path": redacted(path), "line": number, "category": category})
            if CLAIM.search(line):
                descriptions.append({"path": redacted(path), "line": number, "category": "claim-wording-review"})
    return {"text_files": inspected, "secret_candidates": findings, "media_or_signing_names": forbidden,
            "claim_wording_candidates": descriptions}

def history(root):
    # Only prospective-public prose. No historical media, datasets or result files are opened.
    revisions = git(root, "rev-list", "HEAD", "--", "README.md", "docs", "website", "CHANGELOG.md").decode().splitlines()
    seen, hits = set(), []
    for revision in revisions:
        paths = git(root, "ls-tree", "-r", "--name-only", revision, "README.md", "docs", "website", "CHANGELOG.md").decode().splitlines()
        for path in paths:
            if Path(path).suffix.lower() not in {".md", ".html"}:
                continue
            blob = git(root, "rev-parse", f"{revision}:{path}").decode().strip()
            if blob in seen:
                continue
            seen.add(blob)
            for number, line in enumerate(git(root, "show", f"{revision}:{path}").decode(errors="replace").splitlines(), 1):
                if CLAIM.search(line):
                    hits.append({"revision": revision, "path": redacted(path), "line": number, "category": "historical-wording-review"})
    return {"revisions": len(revisions), "unique_prose_blobs": len(seen), "wording_candidates": hits,
            "limit": "Text flags require contextual review; no media inspected and no blanket historical assurance inferred."}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--private-output", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    output = args.private_output.resolve()
    if output.is_relative_to(root):
        raise SystemExit("Historical audit output must stay outside the repository.")
    report = {"schema": 1, "scope": "tracked current text and historical prospective-public prose only",
              "current": current(root), "history": history(root)}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"current_text_files": report["current"]["text_files"],
        "secret_candidate_count": len(report["current"]["secret_candidates"]),
        "media_or_signing_name_count": len(report["current"]["media_or_signing_names"]),
        "current_wording_review_count": len(report["current"]["claim_wording_candidates"]),
        "historical_wording_review_count": len(report["history"]["wording_candidates"])}))
    if report["current"]["secret_candidates"] or report["current"]["media_or_signing_names"]:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
