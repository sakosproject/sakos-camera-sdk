# Local website preview

This is a dependency-free, local-only static preview for Phase 13. From the repository root, start it with:

```powershell
python -m http.server 4173 --directory website
```

Then open `http://localhost:4173/`. The static routes are `/`, `/camera/`, and `/docs/camera/`. It has no deployment, telemetry, remote assets, forms, or remote SDK installation flow.
