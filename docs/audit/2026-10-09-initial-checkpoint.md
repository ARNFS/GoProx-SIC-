# GoProx audit — initial checkpoint

**Date:** 2026-10-09 (Asia/Yerevan)  
**Audit state:** IN PROGRESS — this is not the full audit and is not a release roadmap.  
**Official restart date:** 2026-11-01 (unchanged)  
**Project root:** `D:\GoProx3`  
**Repository remote:** `https://github.com/ARNFS/GoProx-SIC-.git`

## 1. Scope and evidence rules

This checkpoint records repository reconnaissance, a first source/configuration review, and the Agora performance starting point. It does not claim successful runtime, live Firebase, two-device, security-rules emulator, or production verification. Historical two-device call success is retained as the user-provided reference baseline, not represented as a measurement made on 2026-10-09.

Statuses follow the supplied framework. A source-level concern is distinguished from a reproduced runtime defect. No criterion is marked `PASS — VERIFIED` at this checkpoint because the corresponding acceptance behavior has not been verified end to end.

## 2. Repository and working-tree state

| Item | Observed state |
|---|---|
| Local root | `D:\GoProx3` |
| Git top level | `D:/GoProx3` |
| `origin` | `https://github.com/ARNFS/GoProx-SIC-.git` |
| Local branch | `day1-ai-secret-migration` |
| Local HEAD | `51d79f5` (`verjin versia`) |
| Remote `master` | GitHub API reported `51d79f59feeab4a26e7bbfdc781b0e56524dcb6f`, matching local HEAD |
| Working tree | Four pre-existing modified tracked files; no other changes were reported by the inspected status |
| Android modules | One app module, `:app`; repository also has Firebase Cloud Functions TypeScript code |
| Remote inventory | GitHub tree returned 428 paths |
| Docs/CI | No root `README.md`, `docs` directory, or `.github` CI directory was present before this checkpoint directory was created |

The four pre-existing user-modified files were left untouched:

- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/java/com/example/goprox/AIDialogActivity.java`
- `functions/src/index.ts`

Their current diff is substantial (1368 insertions, 349 deletions in the inspected diff summary) and concerns an AI provider-secret migration. Treat this working state as user work; do not overwrite or reset it.

## 3. Environment and build evidence

- Windows 11; runtime reports Temurin/OpenJDK `17.0.20.1`.
- Gradle wrapper is `9.0-milestone-1`; `.\gradlew.bat --version` completed successfully.
- Android Gradle Plugin is `8.13.2`; `:app` uses compile/target SDK 36, min SDK 26, Java source/target 11.
- Agora dependency is `io.agora.rtc:full-sdk:4.6.4`; Firebase BOM is `34.13.0`.
- Cloud Functions package declares Node 22; local Node was `v24.20.0` and npm `12`.
- `adb` was not found on PATH. No connected-device session or real-device performance capture was available.

Commands/results:

| Command | Result |
|---|---|
| `.\gradlew.bat --version` (repository root, PowerShell) | PASS — wrapper launched and reported Gradle/JDK versions |
| `.\gradlew.bat assembleDebug testDebugUnitTest` | FAILED before project configuration/compilation: `java.io.IOException: Unable to establish loopback connection` |
| `.\gradlew.bat --no-daemon assembleDebug testDebugUnitTest --stacktrace` | Same environment-level daemon/loopback failure (`java.net.SocketException: Invalid argument: connect` / pipe startup); Android compile and unit-test tasks did not run |
| Same `--no-daemon` build/test command in elevated local execution | Same loopback/pipe failure before project configuration; failure persists outside the restricted command sandbox |
| `npm run build --prefix functions` | PASS — TypeScript build exited 0 under local Node 24; declared Functions runtime is Node 22, so runtime compatibility remains unverified |

The Gradle result is **not** evidence of an Android source compilation failure or success. Android tests: NOT RUN. Real-device tests: NOT RUN.

## 4. Firebase configuration and source findings

`firebase.json` maps `firestore.rules`, `database.rules.json`, `storage.rules`, and the `functions` codebase. `.firebaserc` names `myappproject-442cf` as the default project. No deploy was run. Whether these repository rules match the deployed Firebase state is **NOT VERIFIED**.

The Firestore skill’s required database-edition check was attempted with the Firebase CLI, but the read-only database listing did not return within the command window and was interrupted. Database edition and deployed indexes are **NOT VERIFIED**.

### Finding register (initial; source review only unless noted)

| ID / severity candidate | Evidence and impact | Verification still required |
|---|---|---|
| F-01 — Agora channel authorization gap; High candidate / release review required | `CallActivity` joins with `rtcEngine.joinChannel(null, channelName, localUid, opts)`. No client token-fetch path was found in the reviewed call flow. Firestore call-document rules constrain Firestore participants, but do not themselves authorize Agora media-plane channel entry. The current Agora Console App Certificate/token-auth state and actual exploitability are unknown. | Check Agora project token-auth configuration without exposing credentials; test only with authorized test accounts and the intended token flow. Review backend token issuance and channel/UID binding. |
| F-02 — Service image Storage-rule path mismatch; High candidate | `AddPostActivity` uploads service images under `service_images/{userId}/{timestamp}.jpg`. The repository `storage.rules` has profile-photo and chat-attachment matches and a default deny, but no `service_images` match. If these checked-in rules are deployed, service-image uploads are denied. | Verify current deployed Storage Rules and reproduce authorized service image upload in a non-production/test project. Do not weaken default deny as a workaround. |
| F-03 — User-document privacy exposure; High candidate | `firestore.rules` permits any signed-in user to read `/users/{userId}`. The user creation schema includes email. If deployed as written, user documents may expose email to all authenticated users. | Confirm deployed rules and exact user-document fields/use cases; run allow/deny Rules tests. Design field-level/private-profile access before changing rules. |
| F-04 — User-controlled rating aggregates; Medium/High trust risk | Service owner update validation appears to permit owner changes to `rating` and `ratingCount` in `firestore.rules`. A provider could potentially manipulate ranking/trust aggregates if those values are client-writable. | Trace all rating writers and review model; verify deployed rules. Move aggregate writes to a trusted transaction/function if this is confirmed. |
| F-05 — Review eligibility/duplicate controls are client-sided; Medium candidate | Reviewed Firestore rules permit signed-in review creation without an evident completed-request eligibility or one-review-per-customer constraint. UI duplicate checking is not an authorization boundary. | Trace review IDs, request completion, and all writers; add Rules/backend tests before severity is finalized. |
| F-06 — Profile-photo object ownership and upload abuse; Medium candidate | `/profile_photos/{fileName}` appears to allow authenticated users to create arbitrary filenames and read profile images; path does not visibly bind object owner to authenticated UID. A 5 MB image limit exists, but no per-user object-count/rate control was identified in the reviewed rule. | Verify deployed Storage rules, client paths, privacy requirements, and trusted rate controls. Assess cost/abuse limits. |
| F-07 — Attachment MIME validation is broad; Medium candidate | Chat attachment rule accepts any nonempty content type up to 25 MB; matching chat membership gates access, but MIME allowlisting/content inspection was not evident. | Inspect attachment consumer behavior and actual upload paths; test malicious/mismatched media types and size boundaries in a test project. |
| F-08 — AI client/backend response contract mismatch; High functionality candidate | In the current dirty working tree, `functions/src/index.ts` prompts for `TITLE|CONFIDENCE` or `NOT_FOUND`; `AIDialogActivity.java` parses JSON fields including `intent: SERVICE_SEARCH` and `matched_service_title`. The two source contracts do not align. Functions TypeScript builds, but callable execution and client parsing were not tested. | Agree a single versioned response schema, add function/client contract tests, then verify in Firebase emulator/test project. Preserve the user’s in-progress migration changes. |
| F-09 — AI request cost/abuse controls incomplete; Medium candidate | The AI callable checks authentication and bounds input/output-related fields; the client also throttles locally. No server-side per-user rate limit or App Check enforcement was found in the reviewed function. Client throttling can be bypassed. | Review deployed callable settings, App Check adoption, quotas, cost limits, and abuse telemetry; load/failure test in non-production. |
| F-10 — Broad cleartext allowance; Medium candidate | `network_security_config.xml` includes a base config allowing cleartext traffic, with a comment describing a temporary debug need. This expands the app’s HTTP transport allowance. Whether cleartext requests are actually made and whether release manifest selects this config require verification. | Inspect manifest variants and network call sites; confirm release APK policy and required domains. |
| F-11 — Backup configuration needs privacy review; Medium candidate | Manifest has `android:allowBackup="true"`; referenced backup/data-extraction rules appear to be placeholders/TODO. Actual backed-up app data and platform behavior were not validated. | Inspect complete backup rule files and stored sensitive data; validate Android 12+ and older-version behavior. |
| F-12 — Call performance is unmeasured; High investigation priority, not yet a proven defect | Encoder requests 1280×720, 30 FPS, adaptive orientation, quality-priority degradation, and Agora auto mirror. These are requested parameters, not delivered FPS/quality evidence. Reviewed `CallActivity` has no local/remote video stats or network-quality callbacks, and no end-to-end latency instrumentation was found. | Capture baseline on two real devices with Agora video/network stats, CPU/memory/thermal context, network conditions, and timestamped camera/audio control propagation. |
| F-13 — Camera switch control/implementation not evident; Needs verification | A drawable associated with camera switching exists, but no `switchCamera()` call or visible switch control was found in the reviewed `CallActivity`/layout path. | Trace complete call UI and listener wiring; check whether switching is intentionally unavailable or is a reported functional gap. |
| F-14 — Reconnect/offline handling may end calls too early; Medium candidate / needs verification | `CallActivity` handles `onUserOffline` without branching on `reason` and schedules cleanup after 1.2 seconds. It does not implement `onConnectionStateChanged`. The Agora engine is expected to manage reconnects, so impact is not established from source review alone. | On two devices, interrupt/restore one network and capture connection-state, remote-user, and cleanup timestamps; confirm whether a recoverable interruption is treated as call end. |

The Agora token finding is a source-level gap, not a claim that a live channel is currently exploitable. Agora’s official guidance recommends issuing a token from a token server for the channel/user and authenticating clients that join; see [Agora authentication workflow](https://docs.agora.io/en/realtime-media/rtc/build/authenticate-users/authentication-workflow) and [deploy a token server](https://docs.agora.io/en/realtime-media/rtc/build/authenticate-users/deploy-token-server).

## 5. Agora Call Performance Audit — baseline status

### Historical reference (provided in the master prompt)

The previous real-device checkpoint reports successful incoming-call delivery, connection, two-way audio/video, speaker, and microphone on two phones after a Firestore rules correction. This remains the reference behavior to preserve. It is not a fresh test result for this audit date.

### Current source inspection

- `CallActivity.java` uses Agora SDK `4.6.4` and requests 1280×720 at 30 FPS, adaptive orientation, quality-priority degradation, and automatic mirroring.
- The call joins Agora with a null token in the reviewed path.
- No Agora local/remote video FPS stats, network-quality callbacks, or call performance time series were found in the inspected class.
- Read-only `javap` inspection of the cached, exact `full-rtc-basic:4.6.4` artifact confirms that its handler API includes `onLocalVideoStats`, `onRemoteVideoStats`, `onNetworkQuality`, `onRtcStats`, and `onConnectionStateChanged`. Local stats expose capture/encoder/sent FPS, capture/encoded dimensions, bitrate, and uplink packet loss; remote stats expose decoded/rendered FPS, dimensions, bitrate, loss, and end-to-end delay; RTC stats expose CPU/memory usage, RTT/delay, bitrate, and packet-loss fields. This confirms an SDK-supported baseline path without changing the app. The [Android 4.x event-handler reference](https://api-ref.agora.io/en/video-sdk/android/4.x/API/class_irtcengineeventhandler.html) documents the statistics callbacks and reporting cadence; see also [LocalVideoStats](https://api-ref.agora.io/en/video-sdk/android/4.x/API/class_localvideostats.html) and [RemoteVideoStats](https://api-ref.agora.io/en/video-sdk/android/4.x/API/class_remotevideostats.html).
- The event handler is set in `RtcEngineConfig` before `RtcEngine.create`; current handlers include `onUserJoined`, `onUserOffline`, `onRemoteVideoStateChanged`, and `onJoinChannelSuccess`, but not stats or connection-state callbacks. `onUserOffline` schedules cleanup after 1.2 seconds without using its `reason` argument. `cleanupAndFinish` removes the Firestore call-end listener, stops preview, leaves the channel, and destroys the engine. No `onPause`/`onStop` override was found in this activity.
- Camera ON/OFF and audio mute use local Agora mute APIs; speakerphone control is local audio-output routing. Their remote propagation latency was not measured.
- No camera-switch API call was found in the reviewed implementation path.
- No `adb` command was available; no device, thermal, CPU, memory, packet-loss, jitter, or latency measurements were collected.

### Pipeline assessment

| Stage | Current evidence | Status |
|---|---|---|
| Camera capture | Requested encoder dimensions/FPS seen; actual capture format/FPS not measured | PARTIAL |
| Frame processing | No stage metrics or profiler trace collected | NOT INSPECTED |
| Agora encoder | Configuration request inspected; output FPS/bitrate/quality not measured | PARTIAL |
| Network transport | No RTT, loss, jitter, bandwidth, or quality callback measurements | NOT INSPECTED |
| Remote decoder | No remote decode stats/device profile | NOT INSPECTED |
| Remote renderer | No renderer frame/render timing stats | NOT INSPECTED |
| Audio pipeline | High-quality stereo profile and mute/routing calls seen; propagation/audio quality not measured | PARTIAL |
| Lifecycle/reconnect | Source cleanup inspected; no background/foreground/network interruption device test | PARTIAL |

**No performance setting was changed.** Root cause for the reported low FPS/video quality/control delay is **NOT VERIFIED**. The next evidence-gathering task is to make a comparable two-device baseline possible, add/enable minimal diagnostic collection if needed, and separately measure video, audio, and state-control paths before making one focused change.

## 6. Initial 121-criteria matrix

This is a coverage tracker, not the completed audit. `PARTIAL` means relevant source/configuration was reviewed but acceptance behavior is not verified. `FAIL` means the checked-in source/configuration itself fails an identified contract or intended path; live deployment/reproduction may still be pending. `NOT INSPECTED` means substantive criterion review has not started. No `NOT APPLICABLE` decisions have been made.

**Current coverage:** 48/121 criteria received some source/configuration review. 73/121 remain `NOT INSPECTED`. 0 criteria are `PASS — VERIFIED`. Verified readiness is therefore reported as **0 verified passes at this checkpoint; no percentage calculated**. This is not a statement that all features are broken; it reflects that acceptance tests have not been run. Critical/High release blockers are **not yet adjudicated**; F-01 through F-03 are high-priority candidates requiring deployment/runtime verification.

| ID | Criterion | Status | Evidence / reference |
|---:|---|---|---|
| 1 | Core functionality | NOT INSPECTED | |
| 2 | User registration | NOT INSPECTED | |
| 3 | Login / logout | NOT INSPECTED | |
| 4 | Authentication integrity | NOT INSPECTED | |
| 5 | Profile creation | NOT INSPECTED | |
| 6 | Profile editing | NOT INSPECTED | |
| 7 | Profile data integrity | NOT INSPECTED | |
| 8 | Service creation | NOT INSPECTED | |
| 9 | Service editing | NOT INSPECTED | |
| 10 | Service deletion | NOT INSPECTED | |
| 11 | Service ownership | NOT INSPECTED | |
| 12 | Service search | NOT INSPECTED | |
| 13 | Service filtering | NOT INSPECTED | |
| 14 | Service details | NOT INSPECTED | |
| 15 | Service requests | NOT INSPECTED | |
| 16 | Request lifecycle | NOT INSPECTED | |
| 17 | Request ownership / authorization | NOT INSPECTED | |
| 18 | Duplicate request protection | NOT INSPECTED | |
| 19 | Chat creation | NOT INSPECTED | |
| 20 | Chat access control | NOT INSPECTED | |
| 21 | Message sending | NOT INSPECTED | |
| 22 | Message editing / deletion rules | NOT INSPECTED | |
| 23 | Read/unread state | NOT INSPECTED | |
| 24 | Chat metadata integrity | NOT INSPECTED | |
| 25 | File attachments | PARTIAL | Storage rules and client paths; F-07 |
| 26 | Image validation | PARTIAL | Client validation inspected; server/rule enforcement incomplete; F-02/F-06 |
| 27 | File type validation | PARTIAL | Client allowlist vs broad Storage MIME rule; F-07 |
| 28 | File size validation | PARTIAL | Client service-image limit and Storage limits seen; boundary tests absent |
| 29 | File upload reliability | PARTIAL | Path/rule mismatch candidate F-02; failure/retry behavior not tested |
| 30 | Stale / orphaned file handling | NOT INSPECTED | |
| 31 | Voice/media functionality | NOT INSPECTED | |
| 32 | Calls / real-time communication | PARTIAL | Call source inspected; historical baseline only; F-12 |
| 33 | Call authorization | PARTIAL | Null Agora token in reviewed join path; token enforcement/App Certificate state and actual channel access not verified; F-01 |
| 34 | Call notification delivery | PARTIAL | Call/notification source only partially reviewed; delivery not tested |
| 35 | Notifications | PARTIAL | Firebase configuration and call flow sampled; end-to-end delivery not tested |
| 36 | FCM token privacy | PARTIAL | Token storage/access not fully traced or tested |
| 37 | AI functionality | FAIL | Current working-tree client/backend response contract mismatch; F-08 |
| 38 | AI input validation | PARTIAL | Callable auth/input bounds inspected; callable tests absent |
| 39 | AI failure handling | PARTIAL | Timeout/error paths sampled; live failure behavior not tested |
| 40 | Security rules | PARTIAL | Checked-in Storage path mismatch and broad user read; deployed state and intended field visibility unknown; F-02/F-03 |
| 41 | Authentication / authorization boundaries | PARTIAL | Firestore participant rules sampled; Agora separate authorization gap F-01 |
| 42 | Sensitive-data protection | NOT INSPECTED | |
| 43 | Anti-abuse / business-logic protection | PARTIAL | AI/server quota and upload abuse candidates F-06/F-09 |
| 44 | Data validation | PARTIAL | Service, review, and AI validation sampled; test evidence absent |
| 45 | Data consistency / no accidental overwrites | NOT INSPECTED | |
| 46 | Server timestamps | NOT INSPECTED | |
| 47 | Error / exception handling | NOT INSPECTED | |
| 48 | Loading / lifecycle / callback handling | PARTIAL | CallActivity cleanup and callback wiring sampled; no background/foreground device test; F-14 |
| 49 | Offline / network-failure handling | PARTIAL | Agora user-offline cleanup sampled; reconnect behavior untested; F-14 |
| 50 | Crash resistance | NOT INSPECTED | |
| 51 | Performance | PARTIAL | Agora configuration only; no measured baseline; F-12 |
| 52 | Low-end device performance | NOT INSPECTED | |
| 53 | Database/query efficiency | PARTIAL | One request query pattern sampled; indexes and workload not verified |
| 54 | N+1 query prevention | NOT INSPECTED | |
| 55 | Scalability | NOT INSPECTED | |
| 56 | Testing / QA | PARTIAL | Only boilerplate Android tests and no Firestore rules test suite found in repository inventory; tests did not run |
| 57 | Regression testing | PARTIAL | Historical call baseline recorded; no current regression run |
| 58 | UI/UX quality | NOT INSPECTED | |
| 59 | Accessibility / usability | NOT INSPECTED | |
| 60 | Monetization | NOT INSPECTED | |
| 61 | Growth / retention mechanisms | NOT INSPECTED | |
| 62 | Observability / logging | PARTIAL | Call performance metrics absent in reviewed class; wider telemetry not audited |
| 63 | Backup / recovery | PARTIAL | Android backup configuration sampled; Firebase backup/recovery not audited; F-11 |
| 64 | Deployment / rollback | NOT INSPECTED | |
| 65 | Dependencies / configuration / secrets | PARTIAL | Gradle/Firebase/Functions configuration sampled; AI migration uncommitted; F-08 |
| 66 | Maintainability / code quality | NOT INSPECTED | |
| 67 | Documentation | NOT INSPECTED | |
| 68 | Release readiness | NOT INSPECTED | No release build/configuration verification |
| 69 | Product / Business Logic | NOT INSPECTED | |
| 70 | Software / Product Design | NOT INSPECTED | |
| 71 | Software Architecture | NOT INSPECTED | |
| 72 | Product design / management / engineering | NOT INSPECTED | |
| 73 | Business Rules | NOT INSPECTED | |
| 74 | Business Thinking / Product Thinking | NOT INSPECTED | |
| 75 | Anti-Abuse | PARTIAL | AI and media abuse controls sampled; F-06/F-09 |
| 76 | Fraud Prevention | PARTIAL | Client-writable review/rating concerns sampled; F-04/F-05 |
| 77 | Reliability / Resilience | PARTIAL | Call interruption/reconnect path sampled; real-network failure test pending; F-14 |
| 78 | Security | PARTIAL | Static rules/client review surfaced candidates; no emulator/security tests |
| 79 | Identity & Access Control | PARTIAL | Firestore and Agora boundary sampled; F-01/F-03 |
| 80 | Privacy | PARTIAL | Checked-in rule allows signed-in reads of user documents that include email at creation; deployed state and product visibility policy need confirmation; F-03 |
| 81 | Data Integrity | PARTIAL | Rating/review write paths sampled; F-04/F-05 |
| 82 | State Management / State Machine | NOT INSPECTED | |
| 83 | Scalability | NOT INSPECTED | |
| 84 | Performance | NOT INSPECTED | Covered for initial call config under criterion 51 only; broader performance not inspected |
| 85 | Network Reliability | PARTIAL | Call reconnection callbacks absent in reviewed class; network interruption not tested; F-14 |
| 86 | Architecture | NOT INSPECTED | |
| 87 | Maintainability | NOT INSPECTED | |
| 88 | Modularity | NOT INSPECTED | |
| 89 | Testing | NOT INSPECTED | Detailed testing strategy/coverage not audited beyond criterion 56 |
| 90 | Observability | PARTIAL | Agora stats/quality callbacks absent from reviewed call class; F-12 |
| 91 | Logging & Monitoring | PARTIAL | Call metrics not found in reviewed class; global logs/alerts not fully audited |
| 92 | Battery Efficiency | NOT INSPECTED | |
| 93 | Resource Efficiency | NOT INSPECTED | |
| 94 | Compatibility | NOT INSPECTED | |
| 95 | UX / UI | NOT INSPECTED | |
| 96 | Accessibility | NOT INSPECTED | |
| 97 | User Lifecycle | NOT INSPECTED | |
| 98 | Notifications | PARTIAL | Call notification path sampled; no delivery/retry tests |
| 99 | File / Media Security | PARTIAL | Rules/path/type constraints sampled; F-02/F-06/F-07 |
| 100 | Search / Filtering / Ranking | NOT INSPECTED | |
| 101 | Social / Communication Abuse | PARTIAL | Call/chat authorization sampled; moderation/rate controls not audited |
| 102 | Economics / Cost Efficiency | PARTIAL | AI and media cost/abuse vectors identified; no usage/billing data |
| 103 | API / Backend Design | PARTIAL | Callable AI and Functions setup sampled; deployment/runtime not verified |
| 104 | Database Design | PARTIAL | User, service, review, and call rule shapes sampled; no full model audit |
| 105 | Caching | NOT INSPECTED | |
| 106 | Offline Support | NOT INSPECTED | |
| 107 | Synchronization | NOT INSPECTED | |
| 108 | Error Handling | PARTIAL | AI callable timeout/error structure sampled; runtime paths not tested |
| 109 | Edge Cases | NOT INSPECTED | |
| 110 | Chaos / Failure Testing | PARTIAL | No failure/chaos test evidence found in inspected test inventory; no execution |
| 111 | Disaster Recovery | NOT INSPECTED | |
| 112 | Backup & Restore | PARTIAL | Android backup policy sampled; database backup/restore unverified; F-11 |
| 113 | Deployment / Release Management | PARTIAL | Firebase config and Functions scripts sampled; no CI/release pipeline found in initial inventory |
| 114 | Versioning / Backward Compatibility | NOT INSPECTED | |
| 115 | Configuration Management | PARTIAL | Gradle, Firebase project alias, and cleartext/backup config sampled; F-10/F-11 |
| 116 | Legal / Compliance | NOT INSPECTED | |
| 117 | Localization / Internationalization | NOT INSPECTED | |
| 118 | Documentation | NOT INSPECTED | |
| 119 | Dependency Management | PARTIAL | Gradle/Firebase/Agora versions sampled; compatibility/vulnerability review not run |
| 120 | Code Quality | NOT INSPECTED | |
| 121 | Technical Debt Management | NOT INSPECTED | |

## 7. Readiness and release blockers

- **Audit coverage:** 48/121 criteria received partial source/configuration review; 73 remain `NOT INSPECTED`.
- **Verified readiness:** 0 criteria currently qualify as `PASS — VERIFIED`; no percentage is calculated because acceptance evidence is incomplete and no weighting model has been defined.
- **Critical/High blocker status:** no final blocker classification yet. F-01 (Agora authorization), F-02 (service image rules path), and F-03 (user data reads) are High candidates. Their deployed state or exploitability is not established. F-08 (AI contract mismatch) appears deterministic at source level and blocks that AI response flow until fixed and verified.
- **Roadmap:** not produced; full audit and release-readiness assessment remain pending.

## 8. Missing evidence register

1. Current deployed Firestore, Realtime Database, and Storage rules, indexes, and Firebase project configuration.
2. Firestore database edition; CLI list attempt stalled and was interrupted.
3. Agora Console authentication mode/App Certificate status and any server-side token implementation/configuration.
4. Two real Android devices with current app build, `adb`/Logcat access, and controlled Wi-Fi/mobile-network conditions.
5. Agora local/remote stats and timestamps for camera/mute state propagation; CPU, memory, temperature/throttling, jitter, loss, RTT, and bandwidth.
6. Android Gradle build and unit-test output from an environment where Gradle can establish its loopback/daemon connection.
7. Functions Node 22 build/runtime and callable integration test in emulator or non-production Firebase project.
8. Full review of authentication, profiles, services, requests, chat, notifications, attachments, AI, lifecycle, privacy, backups, release signing, and product flows.
9. Rules allow/deny suites and verification of server-side review/rating/request invariants.

## 9. Follow-up diagnostic — 2026-10-09

- Found Android SDK at `%LOCALAPPDATA%\Android\Sdk`; its `adb.exe` reports version `37.0.0-14910828`. `adb devices -l` returned an empty device list: no handset is currently connected/authorized.
- PowerShell/.NET TCP loopback test passed.
- Temurin 17 JShell `Pipe.open()` reproduced `Unable to establish loopback connection` / `SocketException: Invalid argument: connect`.
- An installed Oracle JDK 25 JShell `Pipe.open()` passed, but the Gradle wrapper still failed with the same daemon/pipe error when run under JDK 25.
- A cached Gradle 9.4.1 distribution also failed at daemon communication with the same error. This rules out the failure being specific only to the pinned Gradle 9.0 milestone, but the actual Windows/JVM/Gradle interaction root cause remains **NOT VERIFIED**.
- Android Studio's `jbr` directory is incomplete for Java execution: `lib\jvm.cfg` is absent. No project source/configuration was changed by these experiments.
- Read-only `javap` inspection of the cached exact Agora 4.6.4 RTC JAR confirmed the stats callbacks and fields listed above. No API instrumentation was added because a build and device session are still unavailable.

## 10. Next exact task

The Codex command runner cannot currently execute any tested Gradle distribution past daemon communication. Ask for a build from a normal Android Studio/PowerShell session outside this runner, or otherwise restore a Java/Gradle daemon path that supports Gradle's local channel. From repository root `D:\GoProx3`, run `.\gradlew.bat assembleDebug testDebugUnitTest` and preserve the first error output if it fails. Once a build is available, connect and authorize two real Android devices with `adb`, then capture a comparable Agora baseline: local/remote FPS, resolution/bitrate, network quality, connection state, timestamped camera/audio-control actions, plus device/network context. Run call regressions before proposing any performance-setting change. If no devices are available, mark device measurements `BLOCKED` and continue independent source audit work without claiming performance results.

No call settings, Firebase rules, deployment state, or existing user changes were modified in this checkpoint.
