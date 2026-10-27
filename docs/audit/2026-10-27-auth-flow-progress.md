# GoProx Login/Register work log

**Date:** 2026-10-27 (Asia/Yerevan)  
**Scope:** Android email/password and Google sign-in, profile initialization, and auth form UX.

## Findings addressed

- Registration queried sign-in methods before creating an account, adding a request and exposing account-existence details. The flow now calls Firebase Auth directly and maps account conflicts to a generic message.
- Password input was trimmed during login and registration, so a password containing intentional leading or trailing spaces could not be entered reliably. Password strings are now preserved exactly; only email is trimmed and normalized.
- Login and registration handled Firebase failures inconsistently and allowed repeated taps while requests were pending. Both screens now validate inputs inline, disable duplicate submissions during requests, handle Google cancellation, and show user-facing messages by Firebase error code.
- If auth succeeds but profile synchronization fails, the app signs the user out and explains how to retry through sign-in. This avoids leaving a half-initialized auth session that the splash screen would route around.
- Google registration used the legacy activity result API and account-method preflight. Both auth screens now use the Activity Result API and let Firebase Auth resolve the credential.
- Profile initialization wrote whole maps and could overwrite existing profile fields. `AuthProfileSync` reads existing profile data, keeps existing values where present, validates field types, then merges the four supported identity fields into Firestore and updates only those fields in Realtime Database.
- Auth form inputs now expose email/password autofill hints, keyboard actions, and higher-contrast action/link colors.

## Verification

- `:app:assembleDebug` completed successfully in the original auth pass.
- `:app:testDebugUnitTest` completed successfully in the broader app pass: 18 tests passed across the current unit-test suites, including auth input validation.
- Firebase emulator/live Auth, Google OAuth on device, email delivery, and Firestore/Realtime Database profile writes were not exercised in this local JVM/build run.

## Auth screen UX follow-up (2026-10-27)

- Registration now shows all required email/password fields immediately instead of hiding them behind a second email step. This makes the available choices and expected effort clear before submission.
- Login and registration now have short task-focused introductions, visible labels, clearer section separators, and a verification note on email registration.
- Added Material password visibility toggles and corrected low-contrast hint and password-reset text colors.
- The email form controls stay disabled while requests are pending. Keyboard submit now only handles the configured Go/Done actions, and email/password autofill hints remain enabled.
- Verification after this UI follow-up: `:app:assembleDebug` succeeded. Unit tests were not rerun for this presentation-only pass. Live Firebase/device auth remains unverified.

## Remaining follow-up

- Verify email/password registration, verification link/resend, login, password reset, duplicate-account behavior, and Google sign-in on Android devices against the configured Firebase project.
- Exercise profile initialization for new users and repair of a profile present in only one database; confirm current Firebase rules accept each operation.
- Consider replacing the two profile databases with a single authoritative profile source in a separately planned migration. This change preserves the current dual-store architecture.

No Firebase rules, production configuration, or deployed resources were changed. No commit or push was made.
