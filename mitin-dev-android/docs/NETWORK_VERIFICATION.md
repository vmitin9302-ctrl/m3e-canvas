# MITIN DEV — network and full visual verification

Status: actual Android → FastAPI → PostgreSQL validation PASS. Review only; no public-release approval.

## Reviewed sources

- Android base (open PR #2): `ad5844675d470996e3146e01db9066c786eb1dcb`.
- Android tested source: `cf872424565e85904271b230a46a463d960a2d99`; branch `feat/android-api-auth-integration`; dependent draft [PR #3](https://github.com/vmitin9302-ctrl/m3e-canvas/pull/3), base `feat/mitin-dev-android-demo-apk`.
- Backend base: `3b3c8b43b68111c05f7fcc3afceb50484e9e4c80`.
- Backend tested source: `488c2fb47ab81801ca671519be54903201b862c7`; branch `feat/app-mobile-integration-harness`; draft [PR #48](https://github.com/vmitin9302-ctrl/bizflow-business-system/pull/48).
- Alembic head remains `20260906_11`. No migration, production model, router, schema guard or channel changes.

## Completed verification

| Gate | Result | Evidence |
| --- | --- | --- |
| Android build, unit, lint, demo UI | PASS | [Android CI](https://github.com/vmitin9302-ctrl/m3e-canvas/actions/runs/34057705584) |
| Android unit tests | 39 passed, 0 failed/errors/skipped | 12 demo repository + 18 SessionManager + 9 verified HTTPS MockWebServer |
| Demo instrumentation | 3 unique tests; 3 normal + 3 at 160% passed | Real emulator, keyboard/Back/recreation/scrolling, both local roles |
| Backend full pytest | 444 passed, 0 failed/errors/skipped | [Backend CI](https://github.com/vmitin9302-ctrl/bizflow-business-system/actions/runs/34057726408) |
| PostgreSQL subset | 117 passed, 0 failed/errors/skipped | Real constraints, HTTP auth, account/session migrations and concurrency |
| Repeated PostgreSQL concurrency | 23 tests × 3 = 69 executions passed | 25 deselected per targeted run; not added to unique full-suite count |
| Python / JavaScript / whitespace | 97 py_compile, 19 node --check; git diff --check PASS | Backend CI and local feature diff checks |
| pip consistency / full audit | PASS; 0 known vulnerabilities | 45 installed app/test packages + 29 auditor packages, including pip and transitives; no ignores |
| Actual installed Android → TLS → FastAPI → PostgreSQL | PASS; 16 executions, 0 failed/errors/skipped | [Private E2E CI](https://github.com/vmitin9302-ctrl/bizflow-business-system/actions/runs/34057726388) |

APK from this completed E2E: `8311f61e2fb52d485e0820af4f1b7b918efad71bb840aa729db2cc74faf6eb13` (SHA-256). The installed APK and archived APK are byte-identical. Final gates passed: secret scan, backup/permission/source-set boundaries, migration check and real DB aggregates. The disposable DB contained 7 synthetic users, 0 leads, 16 sessions and 23 refresh records (7 used). No identifiers or token values are exported in these aggregates. Android OSV audit: all 141 resolved external runtime/test Maven coordinates, 0 findings.

Environment: Python 3.12.14, PostgreSQL 18.6, pip 25.0.1 → 26.2.1, pip-audit 2.10.1; JDK Temurin 17.0.20.1+1, Gradle 8.13, AGP 8.13.2, Kotlin 2.2.20; emulator 37.1.11.0, Android 15/API 35. Only disposable GitHub-hosted test infrastructure and synthetic fixtures were used.

The E2E gate requires 8 real network tests, 4 independently started OS-process phases, and 2 UI tests repeated at normal/160% font: 14 unique methods, 16 executions. It checks the exact installed APK hash, manifest/backup exclusions, no secrets or DemoRepository in the network APK, genuine migration/check results, and final PostgreSQL/request aggregates. An APK build or an unfinished job does not satisfy this gate.

## Defects found and fixes

- Corrected Compose gradient BorderStroke overload and registered generated public CA resources through the AGP variant sources API. Only a public certificate was committed; no private key.
- Isolated application-owned async failures with a child SupervisorJob, while preserving parent cancellation and single-flight refresh. Added a next-login regression after failed exchange.
- Added encrypted AtomicFile read-back before publishing a token pair.
- Corrected access deadline to start at request initiation with a conservative expiry margin. The earlier 4-second test exposed a server-expired token reaching logout-all; two unit regressions cover transit time and renewal before a mutation. Current real-expiry tests use 15 seconds and actually wait 15.5 seconds. The API POST itself is never blindly retried.
- Synchronized the UI test with actual launcher stop/resume behavior on Android 12+, including Back and reopening after logout. See [Android activity/back-stack documentation](https://developer.android.com/guide/components/activities/tasks-and-back-stack).
- A later login after a long form edit failed before reaching FastAPI. The auth client now retains zero idle TLS sockets; a HTTPS regression checks fresh connections. POST retries remain disabled. This completed E2E passed both normal and 160% UI with that transport setting.
- Added a physical-click/IME synchronization check: closing the keyboard and waiting for insets before tapping the login button. It also verifies the onClick cleared the transient password without logging its value.
- One unchanged demo run had an emulator IME/Back failure. A single rerun on the same SHA passed; both attempts remain visible in [run 34056597226](https://github.com/vmitin9302-ctrl/m3e-canvas/actions/runs/34056597226). The later recorded run above passed on its first attempt. No test was disabled or assertion removed.
- Improved disabled-action contrast, field shapes and welcome hierarchy while retaining the common neon visual style throughout the app.
- Fixed test proxy connection lifetime and disabled forwarded-header trust. Proxy responses still originate from the real service/database.
- Freed only the unused .NET SDK on the disposable runner after Android's minimum userdata image exceeded available disk. No workspace or database volume was removed; no paid runner or budget change.
- Maven inventory audits external resolved modules and all transitives. The local Gradle source project is not an external Maven package.

Warnings were reviewed, not hidden: Android lint has 0 errors, 17 demo/16 internal warnings (newer tool/dependency availability and one existing UseKtx suggestion). Backend pytest reports 1058 deprecation warnings from existing datetime.utcnow, lifecycle/testing APIs and their callers. The prebuilt Android graphics path library is packaged without debug-symbol stripping. No mass dependency upgrade or warning suppression was introduced.

The accompanying final test refinement waits for a rendered frame before taking screenshots and checks the selected session row after revocation. It changes test synchronization, not app behavior. The report above intentionally names an immutable completed source pair; newer PR checks and their artifacts record their own exact Android/backend SHA pair and APK digest.

## Product and release boundary

The full native visual system is updated across all implemented client/owner and network screens: graphite surfaces, violet/cyan outlines, rounded containers, gradient actions, navigation and the MITIN DEV emblem/splash. All 52 target Canvas frames and existing budget/stage values remain preserved. Coverage does not imply all future functions are implemented.

Demo remains offline with no INTERNET permission. Internal has no role switch or mock fallback. Access stays in memory; refresh uses Android Keystore AES-GCM and private no-backup AtomicFile storage. A durable exchange marker, single-flight and epoch/generation checks prevent old-refresh replay after interruption and late-response resurrection after logout. Local data clearing is distinguished from confirmed server revocation.

AI brief, public portfolio and client project APIs are not connected. Future public navigation is present, with honest unavailable states. Owner password-only login stays closed. This auth-only build is not a public-release candidate.

The CI API is destroyed after the run. Phone testing requires your local disposable API, its public test CA and USB adb reverse; no always-on server is provided. Debug signing is temporary. Only API 35 emulator validation is claimed, not a physical-device compatibility matrix. Setup instructions are in the Android README and the private backend mobile integration document.

Production changed: NO
Production DB changed: NO
Production auth routes enabled: NO
Real accounts activated: NO
Real client data used: NO
Real messages sent: NO
Lead data linked or modified: NO
Owner password-only login enabled: NO
Merge/deploy/store publication: NO

Actual Android–FastAPI–PostgreSQL E2E: PASS
