# SakOS Camera static status site

This dependency-free site publishes project status and bounded verification
evidence. It does not provide the SDK, model, Maven artifacts, a contact form,
or a security-reporting intake. The SDK and model remain local release
candidates.

## Local preview

From the repository root:

```powershell
python -m http.server 4173 --directory website
```

Open `http://localhost:4173/`. The routes are `/`, `/camera/`, and
`/docs/camera/`. Internal navigation is relative; canonical URLs target
`https://sakosproject.org`.

## Cloudflare Pages target

The intended production hostname is the apex `sakosproject.org`. The existing
site is plain HTML and CSS, so its Pages build settings are:

- Framework preset: None
- Build command: blank (or `exit 0` if the dashboard requires a command)
- Build output directory: `website`
- Functions or build dependencies: none
- Custom domain: `sakosproject.org`
- Cloudflare plan target: Free; do not enable paid add-ons for this static site

Cloudflare's current limits list 500 builds per month and 100 custom domains
per project on the Free plan. Static asset requests are free and unlimited on
both Free and paid plans. This site uses no Pages Functions, so its traffic
stays on the static-asset path. It should fit the Free plan without adding a
paid service; account-specific subscriptions and domain-registration renewal
costs cannot be checked from this environment. Cloudflare Pages supports both
private and public Git repositories, so the SDK repository does not need a
visibility change just to connect hosting.

Cloudflare's current documentation describes apex custom domains as requiring
the zone on Cloudflare and the zone to be on the same Cloudflare account as the
Pages project. The Pages project's **Custom domains** flow then associates the
apex and provisions its Pages DNS record. Check the existing project and zone
in the account before making changes; do not create a duplicate project or
replace DNS records blindly.

Read-only DNS lookup on 2026-10-05 found Cloudflare authoritative nameservers
(`anita.ns.cloudflare.com` and `chris.ns.cloudflare.com`) for
`sakosproject.org`, with no apex A record returned. This does not show which
Cloudflare account owns the zone, whether a Pages project already exists, or
whether that project already has a custom-domain binding. Verify those in the
dashboard before deployment.

This repository does not include Cloudflare credentials or a Pages project
identifier. No deployment, DNS mutation, GitHub visibility change, or external
upload is performed by the local preview or verification steps.
