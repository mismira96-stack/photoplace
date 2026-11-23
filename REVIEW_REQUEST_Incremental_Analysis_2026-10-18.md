# Antigravity review — incremental analysis / MIXED NEW follow-up

## Scope and prior approval

- Source: `C:\work\projects\PhotoPlace`, current working-tree diff against HEAD `1c8d683`, including new/untracked Java files. Do not review only tracked git diff.
- User relayed approval of the earlier **226-test phase 1** fast path, flicker feedback and legacy Gallery browsing patch. That approval does not cover this follow-up.
- No commit or Play release has been created. Existing unrelated work and legacy device dumps remain untouched.
- Read AGENTS.md / GEMINI.md. MainActivity has only scanner delegation/callback/view binding additions; old image/video scan loops were removed.

## New behavior

### Incremental expensive analysis, full lightweight inventory

- `IncrementalMediaScanner` queries selected image/video rows to reconcile the current inventory, then reconstructs normal PhotoItems for every visible source item. It does not use a generation watermark to query only changed rows.
- `IncrementalAnalysisSession` skips EXIF/video metadata/reverse geocoding for unchanged rows of **both media kinds**, and stages cache changes in memory.
- Base identity uses existing MediaAnalysisSignature (kind/URI/name/modified/added/taken/folder). A new row fingerprint adds MediaStore version, size, duration, generation_modified and exposed coordinates. API29 omits generation; existing signature/size/duration still apply.
- Existing image cache entries are migrated once under their previous base-signature semantics, avoiding a full image reanalysis on upgrade. After migration, fingerprint changes cannot fall back to old entries. The migration intentionally trusts the old cache's validity at first adoption; inspect this compatibility tradeoff.
- Videos receive their own typed fingerprinted cache entries. The disabled legacy NoLocationCache was not re-enabled. Test new GPS invalidation, modified video/size/duration/generation changes and failures carefully.
- Target album matching preserves the previous match scoring in `PreviewItemProjection`. Existing `MediaStoreAlbumLookup` caches target names per path/kind, removing per-photo duplicate provider queries.
- Any provider/null/target lookup/cache save failure propagates to the existing preview error UI, so a partial inventory is not saved as a successful snapshot. Cancellation does not commit staged cache state. Unchanged runs do not rewrite the analysis cache.
- The phase1 preflight remains and now includes `incremental=2` to establish a new baseline once. Snapshot/checkpoint persistence still follows successful completion.
- Deleted/moved source items drop out of the current inventory; source folder and row fingerprints invalidate moved/changed items. Historical Discovery refs still follow existing retention/live-filter policy. Physical media are never altered.

### NEW and result wording

- `MemoryBrowserItem.recentAddedCount` now accepts MIXED as well as discovery-only records with live discovery refs. Test covers an organized existing place receiving a new photo.
- `DiscoveryResultText` describes **places receiving new media**, not newly identified locations. `newPlaceCount` field name remains for compatibility; its meaning is unchanged.
- A controller regression test saves an original photo, adds a photo to the same place, then reanalyzes the exact same URIs: update count is 1 then 0, with original firstSeen preserved.
- User's two earlier reported runs actually processed 3,214 then 3,218 source items; persisted Seongnam refs show 1 then 4 newly first-seen URIs at those scan versions. This supports arrivals in separate batches, not a demonstrated duplicate report of identical input. A true repeated-new issue is not categorically disproven; investigate if it recurs.
- A new photo without GPS is not described as 'no new photos': wording is scoped to additions to place records, and no-location count is preserved.

### Async safety

- `PreviewRefreshDispatch` now checks captured source View attachment before starting a scan or applying preflight feedback. If navigation detached that source screen, callbacks are discarded and the next request remains usable. A regression test covers this.

## Verification (current patch)

- `testDebugUnitTest assembleDebug`: PASS, **237 tests, 0 failures**. `git diff --check`: PASS. Latest debug APK installed retaining app data.
- Device SM-F971N, com.photoplace.app, 1.3.6/code36 debug.
- Initial video-cache fill: 3,218 items, cached 2,936, analyzed 282; inventory 7,619ms, completion 8,272ms. Previous scanner took about 24–29s.
- Deliberately cleared only the newly introduced private optimization checkpoint to force full inventory comparison without touching media: 3,218 cached, **0 analyzed**, inventory 917ms, completion 1,457ms.
- User took a real new photo: 3,219 items, **3,218 cached / 1 analyzed**, inventory 1,626ms, completion 2,376ms. Another arrival: 3,220 items, **3,219 cached / 1 analyzed**, completion 2,487ms.
- The new test photos resolved without location; no new Seongnam NEW badge was expected in those two cases. Real-device GPS-bearing new-photo NEW display remains unverified; MIXED behavior is covered by the regression test.
- Notes, Collections, identity registry, organization links, personalization and album history remain byte-identical to `debug-refresh-20261018/app-data-before-incremental.tar`. Agent did not create/delete/move photos or mutate MediaStore; new photos were taken by the user.

## OPEN — user reports slow Home entry

- User says Home now feels slow. Do not mark instant Home loading complete or claim this patch improves startup.
- Home still rebuilds its view tree, loads album history, validates existing albums and Discovery refs, resolves Home cards and renders them after those queries.
- Home navigation can schedule delayed `restoreMainUiFromState`, which may rebuild preview result rows; inspect retained previewItems and main-thread rendering.
- Legacy resolver fallback may add Gallery reads for Home cards; compare against the phase1 behavior as well as this scanner change.
- Root Activity launch time is not the same as visible Home-card readiness. Separate timings are needed. Avoid hiding the issue under the faster analysis numbers.

## Review gates

1. No new MainActivity business/counting decisions, original trash remains disabled, stable notes/identity intact.
2. Photo/video cache separation; all invalidation and compatibility cases; no-location/GPS behavior; cancelled/failed runs do not establish success.
3. Current inventory/counts/Discovery completeness and target match/duplicate equivalence after extracting old loops.
4. FirstSeen and MIXED NEW behavior, repeat reanalysis and precise updated-place wording.
5. Lifecycle dispatch and Home entry regression. Home caching/instant open remains a separate pending fix.
