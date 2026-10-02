"""Inspect only generated SDK artifacts, model bytes and synthetic test summaries."""
import argparse
import hashlib
import io
import json
import re
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

MODULES = ["safety-core", "safety-opennsfw2", "capture-camerax", "capture-video"]
MODEL_SHA = "051a21bf697858c1e2537354a99be09a48d26bbfba0c35216b340f16de7528d7"

def digest(data):
    return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--emulator-tested", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    candidate = root / "build/private-candidate"
    candidate.mkdir(parents=True, exist_ok=True)
    artifacts = []
    dependencies = {}
    for module in MODULES:
        directory = root / f"build/local-maven/org/sakos/camera/{module}/0.0.0-local"
        for suffix in [".aar", "-sources.jar", ".pom", ".module"]:
            file = directory / f"{module}-0.0.0-local{suffix}"
            assert file.is_file(), f"Missing {module} {suffix}"
            data = file.read_bytes()
            notice_prefix = f"META-INF/sakos/{module}/"
            if suffix in {".aar", "-sources.jar"}:
                with zipfile.ZipFile(io.BytesIO(data)) as archive:
                    if suffix == ".aar":
                        with zipfile.ZipFile(io.BytesIO(archive.read("classes.jar"))) as classes:
                            assert notice_prefix + "LICENSE" in classes.namelist()
                            assert notice_prefix + "NOTICE.md" in classes.namelist()
                            assert notice_prefix + "licenses/opennsfw2-MIT.txt" in classes.namelist()
                            assert notice_prefix + "licenses/yahoo-open-nsfw-BSD-2-Clause.txt" in classes.namelist()
                            for name, source in [("LICENSE", "LICENSE"), ("NOTICE.md", "third_party/NOTICE.md"),
                                ("licenses/opennsfw2-MIT.txt", "third_party/licenses/opennsfw2-MIT.txt"),
                                ("licenses/yahoo-open-nsfw-BSD-2-Clause.txt", "third_party/licenses/yahoo-open-nsfw-BSD-2-Clause.txt")]:
                                assert classes.read(notice_prefix + name) == (root / source).read_bytes()
                        models = [name for name in archive.namelist() if name.endswith(".tflite")]
                        assert len(models) == (1 if module == "safety-opennsfw2" else 0)
                        if models:
                            model = archive.read(models[0])
                            assert len(model) == 6_128_536 and digest(model) == MODEL_SHA
                            assert "assets/policy/opennsfw2_still_gate_policy.json" in archive.namelist()
                    else:
                        assert notice_prefix + "LICENSE" in archive.namelist()
                        assert any(name.endswith(".kt") for name in archive.namelist())
                        for name, source in [("LICENSE", "LICENSE"), ("NOTICE.md", "third_party/NOTICE.md"),
                            ("licenses/opennsfw2-MIT.txt", "third_party/licenses/opennsfw2-MIT.txt"),
                            ("licenses/yahoo-open-nsfw-BSD-2-Clause.txt", "third_party/licenses/yahoo-open-nsfw-BSD-2-Clause.txt")]:
                            assert archive.read(notice_prefix + name) == (root / source).read_bytes()
            if suffix == ".pom":
                pom = ET.fromstring(data)
                ns = {"m": "http://maven.apache.org/POM/4.0.0"}
                dependencies[module] = [{"group": d.findtext("m:groupId", namespaces=ns),
                    "artifact": d.findtext("m:artifactId", namespaces=ns),
                    "version": d.findtext("m:version", namespaces=ns),
                    "scope": d.findtext("m:scope", namespaces=ns)} for d in pom.findall("m:dependencies/m:dependency", ns)]
            artifacts.append({"path": file.relative_to(root).as_posix(), "bytes": len(data), "sha256": digest(data)})
    for relative in ["sample-app/build/outputs/apk/debug/sample-app-debug.apk",
        "capture-video/build/outputs/apk/androidTest/debug/capture-video-debug-androidTest.apk",
        "safety-opennsfw2/build/outputs/apk/androidTest/debug/safety-opennsfw2-debug-androidTest.apk",
        "sample-app/build/outputs/apk/androidTest/debug/sample-app-debug-androidTest.apk",
        "integration-tests/consumer/app/build/outputs/apk/androidTest/localRuntime/app-localRuntime-androidTest.apk",
        "integration-tests/consumer/app/build/outputs/apk/release/app-release-unsigned.apk",
        "integration-tests/consumer/app/build/outputs/apk/localRuntime/app-localRuntime.apk",
        "integration-tests/consumer/app/build/outputs/mapping/release/mapping.txt"]:
        file = root / relative
        assert file.is_file(), f"Missing local artifact: {relative}"
        artifacts.append({"path": relative, "bytes": file.stat().st_size, "sha256": digest(file.read_bytes())})
    tests = []
    for module in MODULES + ["sample-app"]:
        suites = list((root / module / "build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
        assert suites, f"No unit test evidence for {module}"
        count = failures = errors = skipped = 0
        for file in suites:
            suite = ET.parse(file).getroot()
            count += int(suite.get("tests", 0)); failures += int(suite.get("failures", 0))
            errors += int(suite.get("errors", 0)); skipped += int(suite.get("skipped", 0))
        assert failures == errors == skipped == 0
        tests.append({"module": module, "tests": count, "failures": failures, "errors": errors, "skipped": skipped})
    logs = candidate / "logs"
    logs.mkdir(exist_ok=True)
    instrumentation = []
    for name in ["candidate-build.log", "local-maven.log", "local-consumer.log", "candidate-emulator.log", "candidate-consumer-runtime.log"]:
        source = root / "build-logs" / name
        if not source.exists():
            continue
        text = source.read_text(encoding="utf-8-sig", errors="replace")
        for path, replacement in [(root, "[workspace]"), (Path.home(), "[user]")]:
            for variant in {str(path), path.as_posix(), path.as_posix().replace(" ", "%20")}:
                text = text.replace(variant, replacement)
        (logs / name).write_text(text, encoding="utf-8")
        if args.emulator_tested and name in {"candidate-emulator.log", "candidate-consumer-runtime.log"}:
            counts = [int(count) for count in re.findall(r"OK \((\d+) tests?\)", text)]
            assert counts and "FAILURES!!!" not in text and "Process crashed" not in text
            instrumentation.append({"log": name, "suite_counts": counts, "tests": sum(counts)})
    if args.emulator_tested:
        assert len(instrumentation) == 2, "Missing current emulator evidence"
    lint = []
    for module in MODULES + ["sample-app", "integration-tests/consumer/app"]:
        files = list((root / module / "build/reports").glob("lint-results-*.xml"))
        assert files, f"No lint evidence for {module}"
        issues = [issue for file in files for issue in ET.parse(file).getroot().findall("issue")]
        errors = sum(issue.get("severity") in {"Error", "Fatal"} for issue in issues)
        assert errors == 0, f"Lint errors for {module}"
        lint.append({"module": module, "errors": errors, "warnings": sum(issue.get("severity") == "Warning" for issue in issues)})
    shutil.copytree(root / "third_party", candidate / "third_party", dirs_exist_ok=True)
    shutil.copyfile(root / "LICENSE", candidate / "LICENSE")
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root).decode().strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=no"], cwd=root).strip())
    build_state_file = root / "build-logs/candidate-source-state.json"
    build_state = json.loads(build_state_file.read_text(encoding="utf-8-sig")) if build_state_file.exists() else {}
    manifest = {"schema": 1, "status": "private local candidate; no external release", "source_commit": revision,
        "tracked_worktree_dirty": dirty, "coordinates": "org.sakos.camera:*:0.0.0-local",
        "artifact_build_commit": build_state.get("source_commit"),
        "build_tracked_worktree_dirty": build_state.get("tracked_worktree_dirty"),
        "interruption_recovery": build_state.get("interruption_recovery"),
        "toolchain": {"gradle": "8.13", "agp": "8.13.2", "kotlin": "2.0.21", "compile_sdk": 36, "min_sdk": 26},
        "model_sha256": MODEL_SHA,
        "configuration": {"model": "opennsfw2_resnet50_v1@051a21bf697858c1",
            "preprocessing": "opennsfw2-bgr-mean-104-117-123@1", "policy": "opennsfw2-still-policy@1",
            "policy_asset_sha256": digest((root / "safety-opennsfw2/src/main/assets/policy/opennsfw2_still_gate_policy.json").read_bytes())},
        "strategies": {"default": "Fixed14", "optional": "Adaptive14",
            "optional_policy": "opennsfw2-still-policy-adaptive14@1", "thresholds": "unchanged source policy"},
        "tooling_source": {"current_code_revision": None,
            "pinned_runtime_calibration_revision": None,
            "code_snapshot_inventory": "docs/TOOLING_PARITY.md"},
        "artifacts": artifacts, "pom_dependencies": dependencies, "unit_tests": tests,
        "lint": lint, "android_instrumentation": instrumentation,
        "local_links": json.loads((root / "build-logs/site-link-check.json").read_text(encoding="utf-8")),
        "emulator_instrumentation_executed": args.emulator_tested,
        "input_scope": "Current suite: generated benign patterns, mocks, simulated scores and isolated emulator scene only.",
        "signing": "release consumer unsigned; debug/test-key APKs only for local runtime verification",
        "remaining_gates": ["owner/legal model and dependency redistribution review", "physical-device and API-range coverage",
            "real-world classifier efficacy and independent validation", "security intake", "all external delivery decisions"]}
    (candidate / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    (candidate / "SHA256SUMS.txt").write_text("".join(f"{a['sha256']}  {a['path']}\n" for a in artifacts), encoding="utf-8")
    print(json.dumps({"artifacts": len(artifacts), "unit_tests": sum(t["tests"] for t in tests), "source_commit": revision,
                      "tracked_worktree_dirty": dirty, "artifact_inspection": "passed"}))

if __name__ == "__main__":
    main()
