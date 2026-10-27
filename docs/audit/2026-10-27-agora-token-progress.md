# GoProx release-readiness work log — Agora call authorization

**Date:** 2026-10-27 (Asia/Yerevan)  
**Repository:** `D:\GoProx3`  
**Branch / base revision observed:** `day1-ai-secret-migration` / `7077376e07daaf50cf02cf065435225d65d853fe`

## Completed in this session

- Added a callable Cloud Function, `getAgoraCallToken`, in `europe-west1`. It requires Firebase Authentication, checks that the caller is a participant in their own `/calls/{uid}` document, and only issues a publisher token when that call is accepted and the requested channel, `callId`, caller, and callee agree.
- Moved the Agora app ID in `CallActivity` to the existing `BuildConfig.AGORA_APP_ID` configuration path. The app now obtains its channel-bound token from the callable before joining and renews it when Agora reports expiration.
- Added `agora-token@2.0.7`, a Functions build-and-test script, and six Node tests covering accepted-call authorization, participant checks, call/channel mismatches, UID mapping, and token generation.
- Preserved the pre-existing local edit in `ServiceRequestsActivity.java`.

## Verification evidence

- Functions TypeScript build completed successfully.
- Functions tests: 6 passed, 0 failed.
- Android debug assembly and `testDebugUnitTest` completed successfully.
- Android unit tests reported: 15 passed, 0 failed, 0 errors, 0 skipped (1 `ExampleUnitTest`, 9 `ServicePriceParserTest`, 5 `ServiceTest`).
- `git diff --check` reported no whitespace errors; only Git's LF-to-CRLF conversion notices for the npm manifests.
- Source review of `firestore.rules` confirmed that call updates are limited to the participant-specific transitions, including callee acceptance of the incoming call and acceptance of the caller-side state. This supports the server's `status == "accepted"` precondition but is not a substitute for an emulator or deployed-rules test.

## Still required before counting this as end-to-end release verified

- Configure matching Agora app ID values for the Android build and Firebase Secret Manager, and provision `AGORA_APP_CERTIFICATE` in Firebase Secret Manager. The certificate was not present in the inspected local properties; no credential values were read or recorded.
- Deploy the callable to the intended Firebase project and verify callable auth/secret configuration there.
- Run an accepted-call flow on two real devices and verify join, token renewal, rejection for unauthorized callers, and call termination.
- Review rate limiting/abuse controls and operational monitoring for token issuance.

## Accounting

The project-wide 40% target is not claimed as reached. The available 121-criterion framework requires evidence and a fixed progress calculation; this session did not produce an agreed weighted roadmap or the production and end-to-end evidence needed to convert these code changes into a project-wide percentage. The Agora call authorization item remains **PARTIAL** pending credentials, deployment, and live verification.

No Firebase deploy, secret write, commit, or push was performed. This work log does not claim a project-wide release-readiness percentage: implementation and local builds alone do not satisfy production configuration and end-to-end acceptance.
