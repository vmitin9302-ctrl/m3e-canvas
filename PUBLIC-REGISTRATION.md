# Registration, account entry and business audit

Clean installations land on registration. Returning installations without a valid session land on login. A server-confirmed profile lands on the main interface. Public Cases and AI brief remain available through navigation; private cabinet access still requires a validated session. Logout clears credentials while retaining only a token-free account-entry preference; clearing app data removes that preference. Existing encrypted refresh credentials mark an upgraded installation as returning. No local boolean grants access.

Refresh tokens remain in the existing Keystore-backed no-backup store; access tokens stay in memory. Passwords and email tokens never enter SavedState or preferences. Non-secret form fields and consents survive Activity recreation. Password fields are deliberately cleared and validated again.

Registration validates a matching 6–128 Unicode-code-point password, email, name and both consents. An empty phone is optional; a supplied phone must contain 7–15 digits and permitted formatting. The six-character policy requires the matching backend password-policy change; do not claim it works against the older 15-character production policy before that change is separately reviewed and deployed.

After registration, the app shows "Проверьте почту". The HTTPS email link performs confirmation in the browser; "Я подтвердил email — войти" returns to login with the user-created password. No emailed-password/code entry is offered. Password recovery also uses the existing HTTPS email page. Verified Android App Links are not introduced.

The cabinet/profile includes Vladimir Mitin's published phone, email, MAX and VK contacts from the site's contact-config.json. Contact buttons open the corresponding handler and never send automatically.

The native business audit reproduces business-audit.js weights, choices, score rounding and three-priority ordering. Answers and result survive Activity recreation. Local scoring works offline. An explicit AI-comment button sends the answers to the same public website-chat endpoint, without auth headers, cookies or automatic retries. The result does not scan the supplied website and never submits a lead. Editing answers cancels and invalidates an outstanding AI comment.

Validation evidence belongs to the final report and CI for the exact commit. Production UI tests do not submit real registration, login, email or AI requests. Registration HTTP, verification, login and cabinet mutations are exercised only with synthetic accounts on a disposable loopback TLS/PostgreSQL harness. No production settings, feature flags, database or deployment are changed by this branch.
