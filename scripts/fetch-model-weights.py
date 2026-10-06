"""Fetch only the model weight file pinned by SOURCE_MANIFEST.json."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import os
import tempfile
from urllib.parse import urlparse
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = ROOT / "docs/model-conversion/SOURCE_MANIFEST.json"
WEIGHTS_PATH = ROOT / "build/model-conversion/inputs/open_nsfw_weights.h5"
HDF5_SIGNATURE = bytes.fromhex("894844460d0a1a0a")
MAX_BYTES = 25_000_000
ALLOWED_FINAL_HOSTS = {"github.com", "release-assets.githubusercontent.com"}


def load_manifest() -> dict:
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    source = manifest["source"]
    parsed = urlparse(source["asset_url"])
    if parsed.scheme != "https" or parsed.hostname != "github.com":
        raise SystemExit("The manifest must name the pinned HTTPS GitHub release URL.")
    if Path(source["asset_name"]).name != source["asset_name"]:
        raise SystemExit("Unsafe asset name in source manifest.")
    if int(source["reported_bytes"]) > MAX_BYTES:
        raise SystemExit("Manifest asset size exceeds the configured download bound.")
    return manifest


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_file(path: Path, expected_size: int, expected_digest: str | None) -> str:
    actual_size = path.stat().st_size
    if actual_size != expected_size:
        raise SystemExit(f"Weight size mismatch: expected {expected_size}, got {actual_size}.")
    with path.open("rb") as stream:
        signature = stream.read(len(HDF5_SIGNATURE))
    if signature != HDF5_SIGNATURE:
        raise SystemExit("Downloaded input does not have the expected HDF5 signature.")
    digest = sha256_file(path)
    if expected_digest and digest.lower() != expected_digest.lower():
        raise SystemExit(f"Pinned SHA-256 mismatch: expected {expected_digest}, got {digest}.")
    return digest


def atomic_write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    encoded = (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8")
    fd, temporary = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(encoded)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def download(source: dict) -> str:
    WEIGHTS_PATH.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix="open_nsfw_weights.", suffix=".part", dir=WEIGHTS_PATH.parent)
    received = 0
    digest = hashlib.sha256()
    try:
        request = Request(source["asset_url"], headers={"User-Agent": "sakos-model-conversion/1"})
        with urlopen(request, timeout=90) as response, os.fdopen(fd, "wb") as output:
            final = urlparse(response.geturl())
            if final.scheme != "https" or final.hostname not in ALLOWED_FINAL_HOSTS:
                raise SystemExit(f"Unexpected release download redirect host: {final.hostname!r}.")
            declared = response.headers.get("Content-Length")
            if declared and int(declared) != int(source["reported_bytes"]):
                raise SystemExit(f"Unexpected Content-Length: {declared}.")
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                received += len(chunk)
                if received > int(source["reported_bytes"]) or received > MAX_BYTES:
                    raise SystemExit("Release download exceeded the pinned size bound.")
                if received <= len(HDF5_SIGNATURE) and HDF5_SIGNATURE[:received] != chunk[:received]:
                    raise SystemExit("Release response does not begin with the HDF5 signature.")
                digest.update(chunk)
                output.write(chunk)
            output.flush()
            os.fsync(output.fileno())
            final_url = response.geturl()
        if received != int(source["reported_bytes"]):
            raise SystemExit(f"Unexpected downloaded size: expected {source['reported_bytes']}, got {received}.")
        os.replace(temporary, WEIGHTS_PATH)
        return digest.hexdigest(), final_url
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--establish-pin", action="store_true",
        help="On first acquisition only, record the measured digest and retrieval facts in the source manifest.",
    )
    args = parser.parse_args()
    manifest = load_manifest()
    source = manifest["source"]
    expected = source.get("sha256")

    if args.establish_pin:
        if expected:
            raise SystemExit("The source is already pinned; use the normal verified fetch instead.")
        measured, final_url = download(source)
        validate_file(WEIGHTS_PATH, int(source["reported_bytes"]), measured)
        source["sha256"] = measured
        source["retrieved_utc"] = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
        source["final_https_url"] = final_url
        atomic_write_json(MANIFEST_PATH, manifest)
        print(json.dumps({"asset": source["asset_name"], "bytes": WEIGHTS_PATH.stat().st_size,
                          "sha256": measured, "pin_recorded": True,
                          "publisher_sha256": source.get("publisher_sha256"),
                          "retrieved_utc": source["retrieved_utc"]}, indent=2))
        return

    if not expected:
        raise SystemExit("No SHA-256 is pinned. Review the source, then run once with --establish-pin.")
    if WEIGHTS_PATH.exists():
        measured = validate_file(WEIGHTS_PATH, int(source["reported_bytes"]), expected)
        print(json.dumps({"asset": source["asset_name"], "bytes": WEIGHTS_PATH.stat().st_size,
                          "sha256": measured, "verified_existing_file": True}, indent=2))
        return
    measured, final_url = download(source)
    if measured.lower() != expected.lower():
        WEIGHTS_PATH.unlink(missing_ok=True)
        raise SystemExit(f"Pinned SHA-256 mismatch: expected {expected}, got {measured}.")
    validate_file(WEIGHTS_PATH, int(source["reported_bytes"]), expected)
    print(json.dumps({"asset": source["asset_name"], "bytes": WEIGHTS_PATH.stat().st_size,
                      "sha256": measured, "verified_download": True,
                      "final_https_url": final_url}, indent=2))


if __name__ == "__main__":
    main()
