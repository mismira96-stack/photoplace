# Review request — PhotoPlace refresh performance / legacy Gallery browsing

## Review target

- Repository: `C:\work\projects\PhotoPlace`, HEAD `1c8d683` plus the current uncommitted working-tree patch. Review the working tree, not HEAD alone.
- Read repository AGENTS.md / GEMINI.md first. Other pre-existing untracked review files/device dumps and `WORKLOG_2026-08-15.md` changes are outside this patch.
- This is NOT completed incremental scanning or instant Home loading. It is a no-change fast path, a legacy Gallery browsing fix, and non-blocking refresh feedback.

## Changes

1. `PreviewRefreshGate` / `PreviewRefreshController` compare full-access permissions, selected paths, analysis policy, external volume version/generation, source count and saved snapshot revision. Only a successful full result with matching before/after state establishes a persistent checkpoint. Changed/unknown/API29 states use the original full scan.
2. `PreviewRefreshDispatch` leaves current views/results in place during preflight, coalesces pending taps and starts progress/clears preview state only when a full scan is needed. Existing preparation statements were moved into a callback in MainActivity; new decisions are outside the Activity.
3. `PreviewRefreshNoticeRenderer` reuses its inline TextView and reserves two text lines. Tap feedback uses a small scale animation and a brief checking state (minimum 180ms for presenting the result). No modal popup or transient full progress screen on unchanged checks.
4. `PreviewCompletionPresenter` routes zero newly updated places to inline feedback instead of a “0 places” modal. Non-zero results retain the original dialog.
5. `MemoryMediaResolver` reads an already recorded organizedAlbum path for legacy/bulk albums lacking OrganizationLink; exact links still have priority. No new alias/link backfill or filename-based album search was added. `MemoryBrowserDetail.canOpenPhotos` follows actual resolved refs.

## User-reported issue still OPEN — new photos / Seongnam

- User added a few photos taken at home, ran Place Finder, and observed another long full analysis. This is the expected limitation of phase 1, not proof of incremental scanning.
- User reports the Seongnam card does not show NEW, and a repeated run reports Seongnam as a new place again. The repeated-new condition has NOT been independently reproduced or fixed in this patch. Do not mark it resolved.
- Inspect `MemoryBrowserItem.recentAddedCount`: current legacy code suppresses recent count for sourceType other than DISCOVERED_ONLY, including MIXED records. This is a code-confirmed candidate for missing NEW, not a verified complete explanation of the user's case.
- `DiscoverySnapshotController.updateFor` counts incoming groups containing a previously unseen URI; it does not count strictly new place identities. The wording “new place” can therefore include new media in an existing place. Inspect snapshot persistence/URI identity/firstSeen version and generation changes between runs before attributing repeated results to a cause.
- `DiscoverySnapshotMapper` excludes duplicateInTarget media; merger preserves refs omitted from incoming records. Review these interactions with already organized Seongnam media and the live filter.

## Required review focus

- P0: no additional business decisions/counting in MainActivity, no file/MediaStore mutations, original trash disabled, stable date notes/Collection identity preserved.
- False unchanged risks: partial access, null/provider errors, cancellation, partial scans, failed snapshot/checkpoint writes, concurrent source updates, version reset, multiple volumes, modified snapshot. Snapshot revision currently uses mtime + length; assess whether stronger validation is needed.
- Async dispatch: ordering of preflight/UI preparation/worker execution, pending taps and screen/lifecycle changes while preflight waits. Check that stale UI callbacks cannot start scans on unrelated screens.
- Legacy album fallback: exact-path precedence, missing/failed lookup behavior and no unsafe identity backfill.
- Clarify that any library generation change currently causes a full scan, even if outside selected source folders. Incremental processing and immediate cached Home rendering remain TODO.

## Verification

- 226 unit tests PASS; `testDebugUnitTest assembleDebug` PASS. Tests cover gate invalidation/failure cases, dispatch ordering/tap coalescing, completion routing, legacy Gallery detail, and exact link priority.
- Device SM-F971N / package com.photoplace.app debug installed with app data retained.
- Before the user's newly added photos: 3,213 items, full scan 23,715ms; repeat unchanged preflight 16–31ms, including process restart. These are worker timings, not full UI latency.
- Found the original `하남에서 · 105장` card in Discovery History. Date sections and internal viewer (`8월 8일 · 하남에서`, `1/2`) were verified after the resolver fix.
- User confirmed flicker disappeared after preflight was moved before UI preparation; then requested clearer tap feedback. Latest build adds that feedback.
- Notes/Collections/identity registry/organization links/personalization/album history matched pre-test backup bytes after the initial patch. No photos moved/deleted/copied and no MediaStore writes were performed by this work. Actual new-photo scenario was supplied by the user.

See `TODO.md`, `WORKLOG_2026-10-18.md` and `AGENT_SHARED_TODO.md` for scope and remaining work. No release/Play submission was performed.
