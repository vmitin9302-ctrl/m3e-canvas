# Public registration review

Reuses cabinet auth endpoints and the existing owner MFA flow. Adds matching password
confirmation, separate PD/terms controls, bounded local validation, generic mail
notices, invalid-link feedback and an auth mode retained across recreation. Passwords
and email tokens are not persisted. No automatic mutation retry is added.

The backend emails use HTTPS /app/verify-email and /app/reset-password with a token
fragment and a standalone web confirmation fallback. Verified Android App Links are
deferred until a reviewed release signing certificate/assetlinks configuration exists.
Manual token entry remains supported. Email/password reset actions remain gated by
the registration capability; production registration stays false.

Release blockers outside this code: actual SMTP recipient roundtrip, approved
privacy/consent updates and user agreement at https://24promtbot.ru/terms.html. The
agreement is not present in current backend source and must be published before
public registration is enabled. No production legal documents were changed.

Validation: unit, lint/build and six-case cabinet UI matrix including registration,
verification across recreation, password reset and owner MFA. Exact SHA pair is
pinned by the backend Draft PR; see final CI evidence rather than inferring PASS.
