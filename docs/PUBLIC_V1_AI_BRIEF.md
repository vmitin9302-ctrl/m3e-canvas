# Public v1 AI brief candidate

The native internal and production flavors share portfolio, BriefViewModel/Repository, SessionManager and Compose screens. Demo remains independent. Navigation labels are Кейсы / AI-бриф / Кабинет. Portfolio passes the selected slug into the setup screen without overwriting an existing session.

Flow: service + one of six budgets → actual backend AI conversation → full final brief → contact normalization/preparation → unchecked consent → explicit Отправить заявку → server-confirmed reference. No local or fake Lead is displayed. Existing profile/session functionality is retained; projects show a truthful unavailable state.

`BriefStore` persists the anonymous bearer and any unacknowledged operation using Android Keystore AES-GCM and AtomicFile in noBackupFilesDir. Pending commands are saved before transmission. Rotation retains ViewModel state; process recreation reloads the secret and obtains authoritative backend state. A lost confirm response is resolved through GET before any replay; a replay retains the same request UUID. OkHttp automatic retries, redirects, cookies and cache remain disabled. Exceptions and record diagnostics do not print secrets.

Typed responses expose revision, messages, final text/sections, prepared contact, proof, consent digest, pending state and submitted reference. Errors distinguish offline, timeout, rate limit, invalid contact, expired session/proof and unavailable features. Request length/history bounds are enforced by the server. No external AI key is in the application.

`productionRelease` is unsigned, targets only https://24promtbot.ru/, uses system TLS trust, has no debug CA, cleartext or localhost fallback. It is a build candidate, not a store-ready artifact. `internalDebug` remains explicitly configured for the isolated verified localhost TLS harness; an unconfigured build does not connect. `demoDebug` remains offline.

Backend flags default disabled. The production variant must not be used to send real test leads. CI tests the internal APK against a disposable FastAPI/PostgreSQL environment, with a fake only at the AI provider boundary and no real notification delivery. Backend pins the exact Android SHA in tests/mobile_android_ref.txt. Evidence must include both exact SHAs; YAML presence is not a passed E2E.

Release blockers: Legal / RKN mobile channel review required; owner feature-enabling approval; release signing key; real client activation/email verification; owner MFA; real projects/stages API; store requirements. No merge, deployment, signing-key creation or publication is authorized by this candidate work.
