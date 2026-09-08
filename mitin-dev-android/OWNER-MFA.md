# Owner MFA activation

Base: `b11f88af4a67ccd1acbb11d3e2790ded7bf4e781`.

The existing production login and cabinet screens now use the same session
manager as the internal build. Availability still comes from server capabilities;
registration=false displays “Регистрация временно недоступна”. Portfolio and AI
brief behavior is preserved.

Owner password verification receives a short-lived challenge, not tokens. The
authenticator screen sends a six-digit code to `/api/v1/auth/mfa/verify`. Only the
verified token pair is persisted using the existing encrypted refresh store.
Passwords, codes and challenges are absent from SavedState and diagnostic state.
An Activity recreation retains the application-owned challenge but clears the
typed code. Process restart, Back/cancel, expiry or an ambiguous network result
requires a fresh password login. A late response cannot restore a cancelled
session. Concurrent submissions share no repeated mutation; a second submission
is rejected locally. Wrong-code retries are explicit and server-rate-limited.

Unit tests cover strict challenge decoding, successful and failed verification,
expiry, recreation boundary, cancelled/late responses and double submission. The
disposable cross-repository cabinet matrix exercises real password/TOTP login,
keyboard, Back and Activity recreation at 320/360/412 dp and 100/160% fonts, plus
client A/B isolation. Its synthetic TOTP helper is test-harness-only and waits for
an unused actual time step instead of disabling replay protection.

Input assertions use Compose `InputText`, not the visually masked `EditableText`,
and never export the compared password/code. See the [semantics reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsProperties).

Review readiness is determined by CI on the exact Android/backend pair. No merge,
deployment, production accounts or production configuration changes are part of
this draft.
