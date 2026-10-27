# GoProx marketplace and monetization design

**Status:** Local implementation is in progress. Google Play products, service-account secret, RTDN topic, and live purchases are not configured or for sale.  
**Market assumption:** Armenia-first launch; verify target markets and tax treatment before Play Console setup.

## Current product evidence

- GoProx is a two-sided local-services marketplace. People can browse service listings, open provider details, send a service request, chat, and manage their own listings from the Android app.
- The provider-plan flow is now implemented locally with Play Billing, server-verified entitlements, promotion-slot enforcement, labelled promoted listings, and privacy-minimal plan funnel events. The Play Console products and live service-account/RTDN setup are still absent.
- `HomeActivity` loads services for local search/filter/sort. The app now records privacy-minimal home opens, service-detail opens, new provider listings and successful service requests; it still has no search exposure, subscription or revenue funnel.
- Service requests record a request lifecycle; there is no in-app payment/escrow flow for the real-world service itself.

## Product and business logic recommendation

Keep the marketplace's core loop free for both sides: browse, publish one standard service listing, send/receive requests, and chat. Monetize providers who want recurring business tools and extra, clearly labelled visibility. Do not charge customers to contact providers, gate ordinary service discovery, or imply that paying means a provider is verified or will receive work.

The first paid value should be transparent promotion with measurable delivery. Sponsored cards must remain relevant to the user's category/location, be visibly labelled **Sponsored**, and never displace safety or quality checks. Never sell a guaranteed position, lead count, booking, income, or trust badge. Subscription value should recur every billing period; do not sell one-off boosts disguised as subscriptions.

## Proposed plan ladder

The four requested names are ordered from entry to highest service level. Exact quotas below are launch hypotheses, not existing features. Implement and measure each benefit before charging for it. The current local build delivers only concurrent sponsored-listing slots; it does not yet report views, clicks, leads, or revenue per provider. That makes the benefit operationally real but its return hard for a provider to judge, so treat early plans as a small cohort experiment and add trustworthy aggregate performance reporting before broad rollout.

| Plan | Best fit | Proposed recurring value | Launch price hypothesis (Armenia) |
|---|---|---|---:|
| **GoProx Lux** | New solo provider | 1 concurrently promoted listing | ֏990/month |
| **GoProx Pro** | Active solo provider | 3 concurrently promoted listings | ֏2,490/month |
| **GoProx VIP** | High-volume provider | 8 concurrently promoted listings | ֏6,990/month |
| **GoProx Premium** | Established provider/business | 20 concurrently promoted listings | ֏14,990/month |

The tier slot counts are implemented, but must be tested with providers before treating them as a final value/price ladder. A promoted listing remains sponsored until its verified subscription period ends or the provider stops the promotion. Renewals extend the active promotion window; cancellations retain access through paid expiry. The listed Armenia amounts remain research hypotheses only and are not shown in the app; the app uses Google Play's localized monthly base-plan price. Local public examples range from Pages.am high-priority placement starting at ֏15,000/year to Jobs.am one-off job-post packages starting at ֏13,000; those products are not directly comparable to GoProx provider promotion. Test willingness to pay with real providers before setting Play prices.

Keep a free provider tier permanently: a basic profile, standard listings, organic search, incoming requests and chat. The current app does not enforce a one-listing limit, so the free-plan copy follows the actual behavior. More listings should not be the first paywall; early supply and coverage are more valuable than extracting a listing fee before GoProx can demonstrate demand.

## Pricing-screen UX

1. Open from the provider's profile and from a non-blocking “Grow your reach” entry after they have created a listing. Never interrupt customer search, request or chat.
2. Lead with the outcome and limits: “Promote your services and understand interest”; “Promotion can increase exposure but cannot guarantee requests or income.”
3. Show all four tiers in a short comparison, with a monthly/annual selector only when both offers exist. Show the exact localized Play price, billing period, auto-renewal and cancellation information before the Play purchase sheet.
4. Mark a tier “Recommended for solo professionals” only as a product-fit suggestion; do not use “Most popular” until analytics supports it. Leave the free tier easy to keep.
5. Include restore/check purchases, manage subscription, support, terms and privacy links. Never hide cancellation or make users contact GoProx to stop renewal.
6. After purchase, confirm the exact benefit and its expiration/reset date. For pending Play purchases, show pending and grant no benefit.

## Implementation gates before any plan can be sold

1. **Value and policy QA:** exercise concurrent slots, filters, sponsor labels, renewal extension, downgrade capacity, remove-promotion, pending/refund/revocation and service deletion with test accounts. No outcome or trust guarantee is sold.
2. **Play products:** create the four `goprox_*_monthly` subscriptions and one monthly base plan per product in a single subscription group. Set country prices and test with license testers. The Android app fetches `ProductDetails` and shows only Play's localized price.
3. **Secure entitlement:** code verifies tokens through Google Play Developer API, binds the purchase to the SHA-256 obfuscated signed-in account ID, hashes token identifiers at rest, blocks cross-account/replaced-token reuse, handles active/grace/canceled-unexpired states, and server-acknowledges. RTDN reconciliation code exists; the topic and service-account permissions still need setup and live tests.
4. **Policy and support:** make recurring terms, renewal frequency, price and cancellation clear; provide working privacy, subscription terms, refund/help contact and subscription management. Do not launch a price-only screen or claim features that are not delivered.
5. **Measurement:** plan-screen, billing-start, activation, promotion activation/removal events are coded without user IDs or purchase tokens. They are not yet sufficient to show providers promotion performance. Add validated renewal, cancellation, refund, exposure and net-proceeds reporting after RTDN and financial reconciliation are configured; give providers aggregate views and service-detail opens without exposing customer identity.
6. **Experiment:** begin with provider interviews and a small Armenia cohort. Test the proposed monthly prices and plan mix against activation, paid conversion, repeat value, renewal, refund and net revenue per active provider. Keep the free baseline and change prices only from recorded evidence.

## Google Play constraints checked

Provider subscriptions that unlock in-app digital promotion/tools must use Google Play Billing unless an enrolled, applicable exception/program permits otherwise. Real-world cleaning, repairs or other physical service payments are a separate category and should not be routed through Play Billing. The Play policy requires accurate in-app terms/pricing; the subscription policy requires truthful, recurring value and forbids deceptive purchase flows. Google recommends backend token verification, granting only valid purchased entitlements, and timely acknowledgement.

References (checked 2026-10-27):

- Google Play [Payments policy](https://support.google.com/googleplay/android-developer/answer/9858738?hl=en)
- Google Play [Subscriptions policy](https://support.google.com/googleplay/android-developer/answer/9900533?hl=en-GB)
- Android Developers [Play Billing integration](https://developer.android.com/google/play/billing/integrate)
- Android Developers [Purchase security](https://developer.android.com/google/play/billing/security)
- Armenia market reference only: [Pages.am services](https://www.pages.am/en/services/) and [Jobs.am pricing](https://www.jobs.am/pricing/)

## Work explicitly not done

The repository has local BillingClient and plan-screen code, the four product IDs, server-side Google Play Developer API verification/acknowledgement, private entitlements, promotion callables, RTDN handler, Firestore rules, and an index. **No Play Console products, subscription group/base plans/country prices, Publisher API access/service-account secret, RTDN Pub/Sub topic/permissions, production Functions/rules/index deployment, privacy/terms/refund URLs, or live purchase tests were configured.** A server-side `GOOGLE_PLAY_BILLING_ENABLED` parameter defaults to false; the app also requires validated HTTPS terms/privacy/support URLs and the Publisher API service account before purchase buttons can activate. The app shows no made-up price and disables purchase when Google Play does not return a monthly base-plan offer. Do not sell these plans until the remaining setup and test gates pass.

### External launch setup still required

1. Confirm the final Android application ID before setup. The current Gradle application ID and backend verification constant are both `com.example.goprox`; changing it requires aligning Firebase registration and Play Console. Then create one Play Console subscription group containing `goprox_lux_monthly`, `goprox_pro_monthly`, `goprox_vip_monthly`, and `goprox_premium_monthly`; configure one auto-renewing monthly base plan for each, country prices, subscription descriptions, and testing access.
2. Set `GOOGLE_MAPS_API_KEY` in local build configuration and restrict it to the final Android package and signing certificate. The manifest now injects this value instead of shipping a literal placeholder; map tiles will not load until configured.
3. Enable Google Play Android Developer API access for a least-privilege service account, grant it access to the app in Play Console, then set the raw service-account JSON as the `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` Functions secret. Do not commit that key.
4. Create the Pub/Sub topic `goprox-play-rtdn`, allow Google Play Developer Notifications to publish to it, and configure RTDN for this app/package. The Functions handler requires that topic and Publisher API access to reconcile renewals, cancellations, grace periods and expirations.
5. Deploy and review the Firestore rules/index and Cloud Functions. Then exercise purchases, pending state, restore, cancellation, grace/account hold, replacement, expiry, and service deletion in an internal test track with license testers.
6. Supply and verify live privacy, subscription terms, refund/support contact links and Play listing disclosures before any public launch.

## Product UX changes made during this review

- Home now shows separate loading, first-listing, filtered-no-match and network-error states, with an action to offer a service, clear search/filters, or retry.
- Search now evaluates every query against the full service source list; changing a query can broaden results again.
- Search, AI and filter controls have explicit accessibility names and at least 48dp touch targets.
- Core marketplace funnel events contain no user ID, service ID, email, search text or payment data; analytics failures do not block creating a listing or requesting a service.
- Reviews are now submitted by an authenticated callable tied to the caller's completed request; the server enforces one review per service/customer, writes protected evidence and aggregate records, and blocks client edits to listing rating totals. Scores from old listings are shown as unverified and excluded from sorting/rating filters until verified review data replaces the aggregate. Legacy review comments remain visible but cannot prove a completed request; they should not be represented as verified social proof. The service detail screen explains review eligibility and does not offer the form before a completed request is verified.
- App backup is disabled to avoid restoring local authentication and device data. The Home map API key is now injected from local build configuration; the key still needs configuration and app-signing restrictions.
- Firestore user documents now allow reads only by their owner, preventing other signed-in users from reading stored email addresses. Public display names and profile photos are read from the matching Realtime Database child fields; those rules expose only those public fields. Existing profile photos remain readable, but profile photo writes now use an owner-bound stable path and are limited to one 5 MB JPEG/PNG/WebP image per user. Firestore/Realtime Database/Storage rules still need emulator and deployed-project verification.
- Network security configuration now denies cleartext HTTP connections globally. XML parsing confirms the resource is well formed; endpoint behavior still needs device verification.
- Verified review submission is tied to a completed request and one review per customer/service. Private server-owned evidence drives rating totals; legacy public review comments remain visible but their scores are marked unverified and excluded from aggregate ranking/filtering. This source-level fix has unit coverage, but the rules/callable have not been emulator- or device-tested.
- The AI callable now requires App Check and enforces atomic per-account limits of 5 requests per minute and 50 per UTC day. Android uses the App Check debug provider only in debug builds and Play Integrity in release builds. Firebase App Check app registration, Play Integrity signing-certificate linkage, and debug-token registration are external setup gates; no live callable test has run.
- Chat attachments now use a MIME allowlist for common images and documents, with an early client size check and an independent 25 MiB Storage rule. Voice uploads are limited to common audio MIME types. Unknown-length file providers rely on the Storage rule as the authoritative size bound; rules emulator and device checks remain outstanding.
- Removed broad media-library permissions: listing/chat attachments use the system content picker, while camera/microphone access remains limited to calling/voice features. This avoids requesting photo/video/audio access the app does not need.
- Earlier in this local work, Android debug assembly and unit tests passed (20 tests, 0 failures). The project wrapper is pinned to Gradle 8.13 to match AGP 8.13.2. A fresh build after the latest billing/review/profile changes still could not start because the Gradle daemon cannot establish its local loopback connection in this environment; the code therefore still needs a fresh Android build before merge/release. On 2026-10-27, the Functions TypeScript build and Node tests were rerun directly through the installed Node/TypeScript binaries because the `npm` wrapper points to a missing npm installation; after adding AI quota enforcement, TypeScript passed and all 14 tests passed. All 128 Android resource XML files parsed; `firebase.json`, `firestore.indexes.json`, and `database.rules.json` parsed; `git diff --check` passed (line-ending warnings only). No Firebase emulator, deployment, Play test purchase, or live device test has been run.
