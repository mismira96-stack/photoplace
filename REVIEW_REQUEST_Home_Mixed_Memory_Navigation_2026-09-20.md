# Review Request: Home Discovery Navigation - 2026-09-20

## Scope and current gate

Update after review: the user supplied Antigravity's final approval on 2026-09-20 (zero release blockers) and authorized rebuilding/uploading 1.3.6. Back latency and header-count differences remain non-blocking follow-ups. The sections below preserve the review input and its verification limits.

Review the working-tree changes against commit `676c7bb` on `codex/photoplace-v2-bg-wip`.
The user requested review BEFORE further release preparation. No new release AAB has been built or uploaded for this revision. The earlier code36 AAB is stale and must not be uploaded.

## Final product decision

- Home "최근 발견한 장소" shows only Discovery-backed Memories, at most four.
- Mixed Memories are eligible, but separate location-album cards are not added.
- Album-only records do not fill empty slots. An empty Discovery set hides this section.
- Order is latest media date first, with memoryKey as a deterministic tie-breaker.
- Display place names such as 성남 and 송파구, not the album labels 성남에서 / 송파구에서.
- Card tap opens the unified Memory detail directly.
- Home -> detail -> Back returns Home. Browser-origin detail keeps the Browser return path.
- "전체 보기" opens the Memory browser.
- The earlier album-first and Discovery-3/album-1 experiments are superseded.

## Verified causes

1. The Home patch passed a merged `path:...` key, while detail lookup depended on `memoryBrowserShowsOrganizedSources`. Its initial value is false, but visiting the Browser can change it. This made Home navigation depend on prior screen history.
2. `MemoryBrowserItem.from` preferred the merged record's album title over its Discovery place name.
3. Independent album and Discovery loops could render the same mixed place twice.
4. Detail Back always returned to the Browser/country screen, even for Home entry.

## Implementation to review

- `HomeRecentPlacesResolver.java` (new):
  - Selects Discovery-backed records from `repository.memories()`, preserving the canonical key used by unified detail.
  - Sorts and limits to four; no separate album list or album navigation target.
  - Uses the existing MemoryMediaResolver only for the selected cards, on homeProjectionWorker.
  - Counts resolved photos/videos and selects the latest available photo cover (video fallback if no photo).
  - Gallery lookup unavailable is not displayed as a successful zero-photo count.
- `HomeRecentPlacesRenderer.java` (new):
  - Owns the section, horizontal row, cards and click binding.
  - Delegates theme helpers, image loading and navigation through Host.
- `MainActivity.java`:
  - Removes legacy Home card selection loops, unused entry renderer, album card renderer and helpers.
  - Calls the resolver on the worker and renderer on the UI thread.
  - Explicitly passes `includeOrganizedSources=true` from Home cards.
  - Preserves Home return origin through detail/viewer refresh; resets it on Home, Browser, country and Collection navigation.
  - Header Back, system Back and bottom return action share one routing method.
  - Diff against 676c7bb: 46 added / 213 removed lines (net -167 at review preparation).
- `MemoryBrowserState.java`:
  - Title precedence: explicit displayName -> Discovery placeName -> existing title/canonical fallback.
  - Applies to Browser/detail too, keeping the destination's title consistent with Home. Organized-only labels remain unchanged.

## Automated verification

`testDebugUnitTest`: PASS, 210 tests total.
`assembleDebug`: PASS.
`git diff --check`: PASS.

Five new tests in `HomeRecentPlacesResolverTest`:
1. Mixed 성남/송파구 each produce one place-named card; default-false lookup reproduces the old miss, explicit Home lookup opens detail from a fresh controller.
2. Album-only/null repository produces no Discovery row.
3. Actual media recency controls ordering; maximum four cards; unrelated album excluded.
4. One Discovery card is not backfilled with albums.
5. Exact Gallery output + new Discovery uses deduplicated count/latest cover and resolves the same detail media.

## Device evidence and remaining checks

- Installed latest debug APK with `adb install -r` on R5KL503VHQR; app data preserved.
- Home UI hierarchy: 성남 -> 하남 -> 송파구 -> 서초구. No standalone album cards.
- Home 성남 tap opened Memory detail titled 성남, with September 20 and September 19 date sections.
- System Back from that detail returned to Home (PhotoPlace header and recent cards observed).
- User reported no major issue after the fix.
- Latest header-Back, Home -> viewer -> detail -> Home, rotation, and Browser-origin Back need reviewer/device regression confirmation; do not claim all were individually re-tested here.

## Review focus / known limitations

1. Verify canonical key + explicit source flag are wired end-to-end, not only in the unit test.
2. Check origin state is not carried into other tabs/country/Collection routes. Check viewer and layout refresh preserve Home return.
3. Check place title precedence does not overwrite explicit user displayName.
4. Back still feels slow to the user. `returnToMainScreen -> buildUi` reconstructs the Home views and schedules repository/live-filter/card media reads again. The selected-card Gallery queries are on the worker, but may delay cards appearing; no timing measurements or latency fix is claimed.
5. Home now uses resolved photo/video totals; existing detail summary still uses repository aggregate metadata. On device, Home 성남 showed 2168 photos + 22 videos while the detail header showed 사진 2178장. The date grid uses resolved refs. Review this existing header-count discrepancy separately from navigation; do not claim count parity.
6. Existing filename-based media dedupe limitations remain. This patch does not change that policy.
7. No Gallery writes, new album creation, original trash, snapshot rewrite or date-note ownership changes.

## Requested review response

List blockers first with code references; distinguish confirmed failures from untested paths.
Provide separate verdicts for this Home fix checkpoint and release readiness.
Specifically assess Back latency and the metadata/resolved-count discrepancy instead of marking them verified without measurements.
