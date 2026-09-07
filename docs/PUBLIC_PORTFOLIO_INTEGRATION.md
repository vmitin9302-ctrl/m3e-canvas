# Public portfolio integration

## Data and contract
The sole source is backend `app/portfolio_catalog.py:PROJECTS`, also used by `/projects` and `/projects/{slug}`. No HTML scraping, DB table, migration or Android fixture catalog is introduced.

GET `/api/public/portfolio` returns an array; GET `/api/public/portfolio/{slug}` returns one object or 404. Fields: `slug`, `title`, `short_description`, `description`, nullable `category`, `tags`, nullable `cover_image_url`, nullable `project_url`. Current catalog has one description; both description fields use it honestly. Gallery and features are absent in the source and are not fabricated. Unknown fields are ignored by Android for forward compatibility; required fields and bounds are validated.

Explicit `published=false` or `internal=true` entries are excluded. DTO uses an allowlist; case filenames, admin metadata and CRM data cannot be returned. Maximum 100 records, 200-character title, 4000-character descriptions, 12 tags (100 characters each). Image paths are projected only from `/portfolio/<filename>` against an explicit trusted HTTPS origin, never the incoming Host header. No URL is fetched server-side.

## Release boundary
API registration in app.main is OFF by default (`PUBLIC_PORTFOLIO_API_ENABLED=false`). Future enablement also requires explicit HTTPS `PUBLIC_BASE_URL` and a separately authorized deployment. No production settings are changed here. No production auth route is enabled. Migration required: NO.

## Android
The network `internalDebug` variant replaces the portfolio placeholder with native Compose list/detail, Loading/Success/Empty/Error, retry and explicit Refresh. `demoDebug` remains the existing offline demonstrator without INTERNET permission. The portfolio repository reuses HttpAuthApi's verified HTTPS client policy; it never receives SessionManager or tokens. Auth code is unchanged. ViewModel retains the list and selected project across rotation, survives recomposition, and keeps a 60-second memory freshness window. Explicit Refresh always performs a GET with no-cache; there is no disk catalog or infinite cache. Navigation cancellation does not turn cancellation into a fake error. No HTTP calls are made in the card UI; image transport is in the repository.

Images use existing OkHttp (no new dependency), bounded 4 MiB responses and sampled bitmap decoding at max 1440 pixels, with 8192-pixel source bounds. Composition cancellation cancels the underlying call; no Activity/Context is retained by the loader. Missing/error images have an honest placeholder. This integration intentionally permits images from the configured isolated backend `/portfolio/` only; CDN support requires a separately reviewed transport-origin configuration.

External links accept HTTPS only, reject credentials/unsafe schemes, and launch ACTION_VIEW in the system browser. No WebView. Missing links hide the CTA. Discuss similar project opens the existing honest AI-next-stage screen and does not send requests or create leads.

## Isolated verification
Both repos are built at immutable commit SHAs recorded in `android-sha.txt` and `backend-sha.txt`. Backend private CI checks out Android using `tests/mobile_android_ref.txt`. Test-only harness helpers are reused from backend PR #48 head `c1d4b1b41454af7054163ff215d489c03c417669`, without merging that branch; only catalog routes/static assets and test assertions were added. The harness is never imported by production app.main. Loopback HTTPS uses an ephemeral CA with hostname verification; only the public CA is embedded in the APK. Existing synthetic PostgreSQL auth/session regression remains; catalog endpoints have no DB dependency. Existing migrations run only to prepare disposable auth test databases, not as a new project migration.

CI runs full backend regression, syntax, pip check and existing dependency audits; Android demo regression, internal unit/lint/build, auth/session network tests and real portfolio UI E2E. Portfolio UI is checked at 320/360/412 dp and 100/160% fonts. Separate fake-repository UI tests exercise loading/empty/error/retry and long/missing-image content; they are not presented as the real HTTPS E2E.

APK is an internal debug build and requires the temporary HTTPS harness on localhost:8443 via adb reverse and its matching CA. The CI harness ends after the job; this is not a permanently connected public release. No production deploy, AI call, lead creation or real client account is used.

## Add a project without releasing an APK
Add a public entry to the existing server PROJECTS catalog with slug/name/description/category/live_url/cover (optional tags). Publish its image through the backend static route. Test and deploy the backend/catalog through a separately approved release. In an app configured for that environment, Refresh fetches the new record automatically; no Android source change or APK release is needed. Current production enablement remains outside this stage.
