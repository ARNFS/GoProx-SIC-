# GoProx audit delta — 2026-10-27

**State:** IN PROGRESS. This supplements the older 121-criterion matrix; it does not claim a completed 121/121 audit or a release-ready app.

## Evidence recorded today

- Functions TypeScript build: passed using the installed TypeScript compiler directly.
- Functions unit tests: 14 passed, 0 failed (Agora authorization, AI quota enforcement, provider-plan entitlement logic, and verified-review logic).
- Android resource XML: 128 files parsed successfully.
- `firebase.json`, `firestore.indexes.json`, `database.rules.json`: parsed successfully.
- `git diff --check`: passed; Git reported line-ending normalization warnings only.
- Android build and Android unit tests: not re-verified after the latest edits. Earlier build attempts fail before Gradle project configuration because the daemon cannot establish its local loopback connection. This is an environment failure, not evidence that source compilation passes or fails.
- Firebase emulator/rules tests, deployment, Play Billing test purchase, and live-device tests: not run.

## Findings updated from the older checkpoint

| Area | Current source status | Remaining evidence / release gate |
|---|---|---|
| Agora callable authorization (F-01) | Server-side token issuance and participant/channel validation are implemented; six unit tests pass. | Configure secrets and deployed Functions; verify authenticated calls on two real devices and measure call quality. |
| Service image Storage rules (F-02) | Current service-image path is owner-bound with image MIME and size restrictions; source appears aligned with the app upload path. | Emulator allow/deny tests and deployed rules verification. |
| User email privacy (F-03) | Firestore user documents are owner-readable only. Other-user display name/photo are fetched through public Realtime Database child fields; email remains private. | Test actual child-path access and denial in emulator; inspect all profile reads; deploy only after rules review. |
| Review eligibility and rating integrity (F-04/F-05) | Callable requires a completed request for the same service, prevents duplicate customer/service reviews, and writes private evidence and aggregates. Client rules deny review/aggregate writes; legacy scores are identified as unverified and excluded from ranking. | Emulator rules tests, callable integration tests, migration/backfill review, and device flow verification. |
| Profile-photo abuse limits (F-06) | App uses a stable per-user profile path; Storage writes are owner-bound, one named object, image-only, and size-limited. Legacy top-level photos remain read-only for compatibility. | Emulator tests and a check that old uploaded photos still render. |
| Cleartext networking (F-10) | Android network security config denies cleartext HTTP globally; 128 Android resource XML files parse. | Device verification against every required endpoint; confirm there are no intentional HTTP-only dependencies. |
| Android backup (F-11) | Manifest backup is disabled. | Build verification and review of any app-specific backup/data-extraction configuration. |
| AI callable contract and abuse controls (F-08/F-09) | Android reads the `content` field returned by `askAI`; Functions returns that field as validated JSON and checks the selected title against the supplied catalog. The server now atomically limits each authenticated UID to 5 requests/minute and 50/UTC day; three quota tests pass. Debug and release Android source sets select separate App Check providers; `askAI` enforces App Check. | Register the Android app and Play Integrity signing fingerprint in Firebase App Check, register debug tokens for development, and validate on an internal Play track. Test callable denial, quota reset, timeout/provider failure, and service cost under load. |
| Chat attachment handling | Client picker is restricted to common image/document MIME types and rejects known files over 25 MiB; Storage rules independently allowlist image/document/voice MIME types and cap writes at 25 MiB. | Firebase Storage rules compiler/emulator and device upload/download/open tests have not run. Size enforcement at the client is best-effort when a document provider does not report length; server rules remain authoritative. |
| Call UX/performance/reconnection (F-12/F-13/F-14) | Not runtime-verified. | Inspect camera switching/control/reconnect code; measure with two authorized Android devices and network changes. |
| Paid provider plans | Local Play Billing UI, server verification, entitlements, promotion slots, and RTDN reconciliation code exist. Purchases remain gated until external configuration is ready. | Play Console products/base plans, Publisher API service account, RTDN topic/permissions, legal/support URLs, internal-track license tests, cancellation/refund/restore tests, and final package ID confirmation. |

## Coverage accounting

The most recent criterion-by-criterion matrix is the 2026-10-09 initial checkpoint. It recorded 48/121 criteria as partially source-reviewed and 73 as not inspected at that date. This delta adds evidence for several areas, but those changes have not yet been reconciled against every individual row. Therefore the current audit is still **not 121/121**, and no release-readiness percentage is reported. Source-level fixes are not counted as verified runtime passes until the applicable tests and deployed configuration are checked.

## Remaining work to close the audit

1. Reconcile each of the 121 checklist rows against current source and the new evidence, assigning an explicit state and linking each claim to code/configuration or a test.
2. Inspect the remaining Android flows, especially AI, chat/attachments, notifications, account deletion/data lifecycle, offline/error paths, localization/accessibility, and release signing/configuration.
3. Run Android assemble/unit tests in an environment where Gradle can start its local daemon; resolve any source/build failures found there.
4. Add/run Firebase rules and callable integration coverage in a non-production emulator/project.
5. Complete internal Play testing and two-device call/request/chat/reconnection checks before calling the release plan ready.
