# Production AI validation

Production base URL: https://24promtbot.ru/. Android baseline cf0771d8925d24f539bd4e97e0a2ba6b018afdc3. Backend reference main 6d20d94aa160956473e3812b6534803520d9cb35 (read only; deployed meta does not expose a Git SHA).

Capabilities are read before main screens. Failure is closed with a branded retry state. AI availability follows meta. Client login is hidden in production. Submission false hides contact collection; attempting it yields an honest unavailable state. For this validation edition prepare/confirm are also blocked in the production HTTP transport. This guard must be separately reviewed for a future submission release; changing server env alone does not remove it.

The launch screen uses native Compose canvas and the existing vector emblem, with subtle animated glow, a dot pattern and an actual connection indicator. Service selection adapts through FlowRow. Chat distinguishes user and assistant, retains full Russian text, and preserves the existing adaptive navigation. Cards, typography, final brief and cabinet use the same visual system. No raster AI assets or new libraries are required.

Production validation is opt-in through a PR label and never runs on every push. Only three new brief sessions (under_10k / 10_20k / unknown), two messages and one final generation each. Portfolio context is asserted against echoed source_portfolio_slug. Network interruption uses the existing persisted operation and recovery; no blind retry of the full scenario. The six size/font combinations reuse the last final brief without model generation. Presentation/error tests use an injected repository; these are not labelled real AI.

Normal AI requests necessarily persist technical brief sessions and may record existing AI audit events on the server. No direct database edits, migrations, account creation, Lead creation, notification calls or backend deployment are authorized or performed. CRM count verification requires existing authorized read access and must be reported BLOCKED TO VERIFY if unavailable. APK scan excludes the separate instrumentation APK; synthetic inputs live only there. Evidence excludes session secrets, proof tokens, contact details and complete final briefs.

The APK uses debug signing only and system TLS. No signing key, provider key or production credential is committed. The backend repository is unchanged.
