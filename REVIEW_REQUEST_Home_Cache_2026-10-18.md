# Antigravity review — cached Home summaries / unchanged-query skip

## Scope

- Repository `C:\work\projects\PhotoPlace`, working tree against HEAD `1c8d683`, including new Java files.
- Prior approval covers the 226-test phase1 patch only. Incremental analysis has its separate 237-test review request. This Home patch is a third scope; current total is **247 tests PASS**.
- Implemented planned steps 1/2: saved summary first, live-query skip when unchanged. Root Home view reuse is intentionally separate and NOT implemented.

## Changes outside MainActivity

- `HomeDashboardSnapshot`: at most four recent-place cards plus country summary cards; canonical destination key, title, cover URI, count text, country code and source counts. No photo lists, notes, identities or OrganizationLinks are copied.
- `HomeDashboardStore`: app-private `home_dashboard_cache.json`, compact JSON, schema/bounds validation, <=64KB, temporary file plus atomic replacement. Corrupt/missing/unsupported cache is disposable and rebuilt. Current device payload ~1.4KB.
- `HomeRevisionReader`: requires full media/location read access; Android 11+ volume version/generation plus metadata (mtime/size) for discovery snapshot, album history, identity registry, links, personalization and Collections. No media row/folder queries in this check. API29/unknown state never validates a query-skip baseline.
- `HomeDashboardPolicy`: exact valid revision allows skip; unknown/concurrently changed state does not. An unavailable recent-card resolution cannot validate even a matching revision.
- `HomeDashboardController`: reads the small cache once per Activity; binds cached content immediately or on initial view attachment; checks revision on the Home worker. Only a changed revision triggers the existing live projection. Identical summary content is not rebound, avoiding another pop-in/flicker.
- Existing short-lived Activity album caches are invalidated on their UI thread before a required refresh. Data load and projection remain on the worker. Before/after tokens validate a refreshed baseline. Known unavailable cards keep revision empty so a later request retries.
- `HomeRefreshRequests` coalesces requests while retaining one follow-up when navigation/resume requests another check during an in-flight refresh. Latest attached Home target receives a completed projection; closed/detached views receive none.
- `HomeCountryCardsRenderer`: renders cached country summaries. Country clicks re-read current records on the worker before entering the existing country detail. Recent-place clicks still use the existing fresh detail resolver and canonical key / includeOrganizedSources=true.
- MainActivity only initializes/binds/lifecycle-connects the controller and renderers. Its old asynchronous Home projection block and country summary-card builder were removed. Overall current MainActivity diff: +71/-362 (net -291 vs HEAD, includes previous scanner extraction).

## Invalidation / correctness focus

- Cached Home is a display summary, not authority for opening photos, copying, deleting, stable identity or notes. Detail/organization continue through existing current-data paths.
- Full permission checks gate cache display. Permission loss during refresh suppresses rendered cached data; permission/query uncertainty cannot establish a skip baseline.
- Library edits, external volume/version changes, snapshot analysis updates and app album/link/identity/Collection changes invalidate the baseline independently.
- File revisions intentionally use mtime+length, consistent with phase1; they are not content hashes or comprehensive corruption detection. Summary cache validation is separate.
- Existing legacy album/repository adapters internally retain some fail-safe catch behavior. Review whether they need explicit freshness/error status in a future strict source contract; this patch does not redefine their semantics. Explicit controller exceptions retain previous summary, and unavailable card resolutions cannot validate.
- Source changes during a refresh produce an empty revision and therefore force a subsequent validation rather than an unchanged baseline.

## Measurements / device checks

SM-F971N, package com.photoplace.app debug, app data retained:

- No Home cache: source loading 982ms, card projection/revision/cache work 143ms; cards bound at 1,259ms, pre-draw at 1,263ms from Home build entry.
- Place detail → Back: cached bind 21ms / pre-draw 26ms; unchanged check 29ms. No new source/card queries.
- Country Japan → Back: cached bind 30ms / pre-draw 36ms; unchanged check 39ms. Country detail showed Japan, 818 photos / 13 records, confirming summary code routes to real current detail.
- Cold process restart with existing cache: cache read 1ms, cached pre-draw 122ms; Android Activity launch 345ms. After APK reinstall another cold launch was slower (cached pre-draw 319ms, Activity launch 976ms). Report both; device/class-loading state varies. The key result is cached cards in the initial Home frame, not a guaranteed 0.3s total launch.
- An intervening changed-state refresh ran in the background (~911ms source / 245ms remaining projection/cache work) with no refreshed bind when display content was identical.
- Source file/store bytes for notes, Collections, identity registry, links, personalization and album history remained identical to the pre-incremental backup. No photo/file moves/deletes/copies or MediaStore mutations.
- `testDebugUnitTest assembleDebug` PASS, **247 tests / 0 failures**; `git diff --check` PASS. New tests cover round-trip destinations, corrupt/unsupported/oversized cache, empty valid result, identical display suppression, revision stability, unknown/unavailable results, queued follow-up request and summary-only storage.

## Remaining scope

- Root Home view tree is still rebuilt. This patch removes its live data dependency on repeat entry, not all first-frame/Activity startup cost.
- Home/detail aggregate count mismatch is an older tracked item and not fixed by caching.
- GPS-bearing new-photo NEW device verification remains separate from this patch; existing tests cover MIXED NEW.
- No release bump, commit or Play submission performed.
