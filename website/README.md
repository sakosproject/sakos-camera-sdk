# SakOS Camera static helper site

The dependency-free site contains `/`, `/camera/`, `/docs/camera/`, one CSS
asset and a favicon. It publishes status and bounded verification evidence, with no SDK/model
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
and matched the four uploaded files byte-for-byte.

Latest production deployment: `38ec0f98-dbab-449d-9e2d-1dc834354df7`, source
`80bc3b4`, on October 6, 2026. The stable URL is
[sakosproject.pages.dev](https://sakosproject.pages.dev); the immutable deployment
URL is [38ec0f98.sakosproject.pages.dev](https://38ec0f98.sakosproject.pages.dev).
All three page routes, `/assets/site.css` and `/favicon.svg` returned HTTP 200.
The custom-domain routes and both Pages hostnames serve the public-repository link
without the former private-status text. Cloudflare appends a
`static.cloudflareinsights.com` script to HTML responses. Local navigation checks
passed across 26 files and 47 links.

Custom-domain status: `https://sakosproject.org` is attached and active. On
October 6, 2026, the home page, `/camera/`, `/docs/camera/`, `/assets/site.css`
and `/favicon.svg` each returned HTTP 200 over HTTPS from the apex. Navigation
uses relative links and canonical URLs target this domain. No DNS or domain
attachment steps remain.

## Preview and deployment

From the repository root, preview locally:

```powershell
python -m http.server 4173 --directory website
```

Open `http://localhost:4173/`. Internal navigation is relative; canonical URLs
use `https://sakosproject.org`.

The deployment script stages only the three tracked HTML pages, CSS and favicon
under the ignored `build/site-upload` directory, then uses the existing signed-in Wrangler:

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
replaced by this explicit Pages flow. Before deployment, set `SAKOS_CLOUDFLARE_ACCOUNT_ID` in the local process
environment to the intended 32-character hexadecimal account identifier. The
script validates it, passes it temporarily to Wrangler as
`CLOUDFLARE_ACCOUNT_ID`, and restores the caller's previous value afterward.
Keep the identifier outside tracked files and deployment logs.

If choosing Git integration for a future project, the static build settings are
framework None, blank build command and output directory `website`. Cloudflare
Direct Upload projects cannot later switch to Git integration; keep the upload
command for this project. Pages hosting works independently of GitHub repository
visibility.

## Custom domain verification

The owner attached `sakosproject.org` through Pages Custom domains. The active
apex and all public routes were verified over HTTPS on October 5, 2026. If the
domain is ever detached, re-add it from Workers & Pages > `sakosproject` >
Custom domains, wait for Active/TLS, then repeat the route checks above. Preserve
unrelated DNS records.

Cloudflare references: [custom domains](https://developers.cloudflare.com/pages/configuration/custom-domains/),
[Direct Upload](https://developers.cloudflare.com/pages/get-started/direct-upload/).

## Rollback

Workers & Pages > `sakosproject` > Deployments: choose a prior successful production
deployment and roll back. For the first deployment there is no earlier site version;
re-upload a corrected committed payload. Keep unrelated DNS records intact.
