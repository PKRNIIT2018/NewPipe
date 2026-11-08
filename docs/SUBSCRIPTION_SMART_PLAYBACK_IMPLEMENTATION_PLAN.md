# NewPipe smart playback roadmap

Repository: PKRNIIT2018/NewPipe  
Implementation branch: `feature/subscription-autoplay`  
Protected base: `dev` (do not modify, merge, or rebase without approval)

## Current baseline and build gate

- The initial implementation adds a feed-menu action that builds a finite queue from cached subscription uploads via `FeedDatabaseManager.getStreams(groupId, false, true, false)`, deduplicates by service ID + URL, and starts a `SinglePlayQueue`.
- This is an explicit feed action, **not** global automatic queue replenishment or an opt-in autoplay setting.
- GitHub Actions run 42535564697 failed at the Gradle build/test step; precise error is not yet available from the connector. Do not assume a root cause.
- The `continuous` Gradle build type already adds a separate application-ID suffix and uses debug signing. Preserve this; verify the resolved ID in the built manifest and verify that an existing official NewPipe installation is not replaced.
- The workflow was adjusted to match upstream CI tasks (`assembleContinuous lintContinuous testDebugUnitTest`) and upload diagnostic reports on failure. Inspect the next run and full log before applying a source-level fix.

**Release gate 0:** CI green, unit tests green, APK artifact present, application ID verified, smoke test on a phone, no unintended changes to `dev`. If failing, inspect Gradle output, fix one root cause at a time, and rerun.

## Architecture

Use a small, local-first feature package (`local/smartqueue` or equivalent) with interfaces:
- `SubscriptionQueueRepository`: retrieve cached feed streams for ALL or a selected feed group, preserving membership and upload metadata. Use existing Room DAO / `FeedDatabaseManager`; do not fetch on main thread.
- `SmartQueueFilter`: pure, deterministic policy for completed/partial streams, type (Shorts/live), age, duration, blocked channels/keywords, duplicate IDs, unavailable streams and future uploads.
- `SmartQueueCoordinator`: holds session identity, cursor/seen IDs, user-manual queue precedence, refresh/backoff and exhaustion state. Do not replace an explicit playlist or manually enqueued item.
- `SmartQueueStore`: persist small session metadata and selected filters; prefer existing preferences/Room patterns. Never serialize an entire large media object graph into preferences.
- `SleepTimerController`: monotonic deadline, stop-after-current option, cancellation and foreground/background lifecycle integration.

Player integration: inspect `Player.java` playback-end and auto-queue handling, `PlayerHelper.autoQueueOf`, `PlayQueue` lifecycle, `NavigationHelper`, and queue serialization before wiring. Smart mode must bypass recommendation-based auto-queue while enabled and restore existing behavior when disabled. Avoid main-thread DB queries and race conditions with skip/repeat/shuffle.

## Phase 1 — Persistent smart queue (P0)

- Add opt-in `Subscription-only autoplay` preference, default OFF.
- Persist mode, group ID, sort order, last stream key, playback position, seen stream keys and bounded queue metadata.
- Restore only after app/process restart when the user chooses to resume; never silently start media on app launch.
- At end of queue, fetch the next eligible batch from the cached feed, excluding seen IDs; trigger refresh only through existing feed refresh infrastructure with cooldown/backoff.
- Stop gracefully on empty/offline/error; no infinite retries. Do not interrupt a user-created playlist or manual queue.
- Respect repeat/shuffle and media session controls; define explicit precedence: manual actions > explicit playlists > smart queue > recommendation auto-queue.
- Handle partial progress and stream failures; limit consecutive failures.

Acceptance: playback resumes after process death without duplicating the last finished item; 20-video queue advances without unrelated recommendations; manual enqueue wins; empty feed stops; no repeated fetch loop.

## Phase 2 — Smart feed filters (P1)

- Settings: completed videos OFF by default, partial videos ON, Shorts OFF optional, livestreams OFF optional, max age (7/30/90/all), min/max duration, keyword exclusion, sort newest/oldest.
- Define Shorts by available stream metadata and duration fallback; never assert exact Shorts classification from duration alone.
- Reuse feed DAO completion logic; treat watch-history disabled as unknown rather than falsely marking watched.
- Keep feed browsing and queue filter policies separately configurable.
- Filtering happens before pagination; bounded over-fetch prevents an empty first batch from being mistaken for exhaustion.

Acceptance: each filter works independently and combined; group filtering never leaks other channels; missing duration/date handled deterministically; preferences survive restart.

## Phase 3 — Channel-group autoplay (P1)

- Select All Subscriptions or a saved group using existing `FeedGroupEntity` and `FeedDatabaseManager.getStreams(groupId,...)`.
- Support switching groups with clear confirmation if active playback would be replaced.
- Keep selection and session state across restart; handle deleted/renamed/empty groups and removed subscriptions.
- Optionally shuffle within selected group without leaking to unrelated channels.

Acceptance: group A plays only its member channels; switching to group B excludes A-only channels; deleting selected group falls back to All or stops with clear message; empty group never invokes related recommendations.

## Phase 4 — Sleep timer (P1)

- Presets: 15, 30, 45, 60, 90 minutes, custom duration, and stop after current video.
- Countdown uses elapsed realtime/monotonic clock, not wall clock; display remaining time in player and allow cancel/extend.
- Stop and pause playback without changing user queue or watch history incorrectly.
- Work with screen off/background player; if the process dies, restore only if the deadline is still valid and user expectations are clear.
- Timer expiry takes precedence over queue replenishment, repeat and autoplay.

Acceptance: timer stops playback within 2 seconds under normal conditions; no next item starts after expiry; pause/seek/screen-off handled; cancellation works; no battery-intensive polling.

## Test matrix

Unit tests (pure Kotlin/Java where feasible):
- completed vs partial vs unknown history, missing metadata, duplicate service ID + URL, upload order, Shorts/live filtering, age boundary, group membership, stale/removed subscriptions;
- cursor and seen-set persistence, process recreation, queue replenishment, empty/offline/error/backoff, explicit queue precedence;
- timer deadlines, cancellation, expiry and stop-after-current.

Integration/instrumentation:
- Room queries with ALL and group IDs; subscriptions removed while queue active; preference migration and default OFF;
- ExoPlayer end-of-item, skip, shuffle, repeat, background service, notification/media controls and restoration;
- manual playlist and enqueue remain intact; no related auto-queue in smart mode.

CI:
- Run upstream lint/JVM tests and build continuous APK; add new targeted tests.
- Upload JUnit reports and diagnostics on failure; upload APK only on success.
- Validate Gradle wrapper/dependency security, permissions, APK application ID, signing certificate, min SDK, and license notices.
- Smoke test on Samsung Android: fresh install, existing official app coexistence, background playback, battery optimization, offline mode, screen lock and database import compatibility.

## Delivery and rollback

Deliver only when all gates pass:
1. green workflow URL and commit SHA;
2. signed debug/continuous APK artifact with SHA-256 checksum and version identifier;
3. verified package name distinct from official NewPipe and installation instructions;
4. documented test results, known limitations, backup/export instructions and rollback path;
5. release notes and manual QA checklist.

Debug/continuous signing is for testing only. Do not publish it as a production release without managing a stable signing key and update strategy. Back up official NewPipe before import; do not overwrite its data.

## Implementation order

1. Read failing run logs, diagnose and fix; verify workflow green.
2. Add isolated filtering tests and opt-in preference.
3. Add repository + coordinator, persistent session and queue replenishment.
4. Add channel-group selector and filter UI.
5. Add sleep timer with lifecycle tests.
6. Run full CI and on-device QA, publish test APK artifact.
