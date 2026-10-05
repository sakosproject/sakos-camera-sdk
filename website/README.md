# SakOS Camera static helper site

The dependency-free site contains `/`, `/camera/`, `/docs/camera/` and one CSS
asset. It publishes status and bounded verification evidence, with no SDK/model
or binary downloads, contact form, uploads or private security intake.

## Hosting target and launch status

Owner authorization: 2026-10-05. Wrangler 4.147.0 is signed in to the account that
owns the active `sakosproject.org` zone. There was no existing SakOS Pages project.
The Direct Upload project is `sakosproject`, production branch `main`,
with `https://sakosproject.pages.dev` as its stable Pages hostname and
`https://sakosproject.org` as the intended apex. The site is live on its stable Pages
hostname. Static Pages hosting uses no Functions or paid add-ons.

Initial deployment on October 5, 2026: `03e28380-f1d9-4368-bb2a-d01cf3ae9c5e`,
source `e691942`. Home, camera, project status and CSS returned HTTP 200 over HTTPS
and matched the four uploaded files byte-for-byte. The final relative footer-link
update is deployed separately and recorded below.

Latest production deployment: `85674b0d-8103-475c-b4d3-916cbd58b489`, source
`6b4441a94c46676fa4dd34d8d9b02f4681c649d8`, on October 5, 2026. The stable URL is
[sakosproject.pages.dev](https://sakosproject.pages.dev); the immutable deployment
URL is [85674b0d.sakosproject.pages.dev](https://85674b0d.sakosproject.pages.dev).
All three page routes and `/assets/site.css` returned HTTP 200 and matched the
upload byte-for-byte after deployment. Local navigation checks passed across
26 files and 44 links. This execution-record update changes no deployed HTML/CSS.

Apex status: not attached to the Pages project; no apex A/AAAA record returned at
verification. Complete the manual steps below. Navigation uses relative links and
works on either hostname; canonical URLs already target the intended apex.

## Preview and deployment

From the repository root, preview locally:

```powershell
python -m http.server 4173 --directory website
```

Open `http://localhost:4173/`. Internal navigation is relative; canonical URLs
use `https://sakosproject.org`.

The deployment script stages only the three tracked HTML pages and CSS under the
ignored `build/site-upload` directory, then uses the existing signed-in Wrangler:

```powershell
.\scripts\deploy-helper-site.ps1
```

This is Direct Upload, so a Git push alone does not redeploy the site. After a
website change, commit/push it and rerun that command. No Android build is required.
Project creation (once, only if absent):

```powershell
wrangler pages project create sakosproject --production-branch main --force
```

Wrangler 4.147.0 delegates new Pages commands to Workers by default. The installed
CLI's `--force` option selects actual Pages for initial project creation. Once
the Pages project exists, uploads use it directly without that option. The first
delegated creation failed before creating a project or deployment, and was
replaced by this explicit Pages flow. The script selects the verified account
using a temporary `CLOUDFLARE_ACCOUNT_ID` environment value because Pages config
does not support an `account_id` field; it restores the caller's value afterward.

If choosing Git integration for a future project, the static build settings are
framework None, blank build command and output directory `website`. Cloudflare
Direct Upload projects cannot later switch to Git integration; keep the upload
command for this project. Private repositories can remain private.

## Attach the real domain manually

If the apex is not yet active:

1. In the same Cloudflare account, open Workers & Pages > `sakosproject`.
2. Open Custom domains > Set up a domain.
3. Enter `sakosproject.org` and continue.
4. Confirm the DNS record Cloudflare proposes for the apex, targeting
   `sakosproject.pages.dev`. Use this flow to associate the domain and provision
   its DNS record; do not merely add a CNAME before associating the domain.
5. Wait for the domain to show Active and TLS to finish provisioning. Verify
   `https://sakosproject.org/`, `/camera/`, `/docs/camera/` and `/assets/site.css`.

The apex zone must be in the Pages project's Cloudflare account; that match has
been verified. Wrangler's current OAuth session has Pages write and Zone read,
but does not advertise DNS write. Dashboard confirmation can complete DNS setup
without changing credentials or requesting broader CLI token scopes.

Cloudflare references: [custom domains](https://developers.cloudflare.com/pages/configuration/custom-domains/),
[Direct Upload](https://developers.cloudflare.com/pages/get-started/direct-upload/).

## Rollback

Workers & Pages > `sakosproject` > Deployments: choose a prior successful production
deployment and roll back. For the first deployment there is no earlier site version;
re-upload a corrected committed payload. Keep unrelated DNS records intact.
