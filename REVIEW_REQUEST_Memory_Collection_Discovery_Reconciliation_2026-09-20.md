# Review Request — Memory Collection / Discovery Reconciliation

## Context

After PhotoPlace 1.3.5, places that already had a Memory Collection and a Gallery output did not show newly discovered media consistently.

Observed on device `R5KL503VHQR`:

- The discovery snapshot contained newer Songpa-gu refs through 2026-09-19.
- The existing Gallery output ended around 2026-09-12.
- Memory/Collection views showed the Gallery date range and hid the newer Discovery refs.
- Collection dissolve and Discovery tab projection could make the place appear missing even though data remained on device.

This request covers a narrow read-only projection hotfix. It does not create albums, move/copy files, trash originals, or mutate MediaStore.

## Changes to review

### 1. Discovery browser source projection

`MainActivity.showMemoryBrowserScreen()` now defaults to the merged `MemoryRepository.memories()` projection so organized-only places remain reachable from the Discovery tab and search.

Review that:

- search still exposes original Memory records;
- collection member folding does not hide search results;
- existing overseas/home navigation is not regressed.

### 2. Collection detail and card source resolution

`MemoryCollectionResolver` and `MemoryCollectionCardRenderer` now use `MemoryMediaResolver` with the exact usable Memory `OrganizationLink` and Gallery reader.

Review that:

- each member is resolved independently;
- Gallery output is used when available;
- Discovery fallback still works when no usable Gallery output exists;
- `lastKnownAlias` fallback does not bind a member to the wrong Memory;
- date notes remain keyed by `stableMemoryId + dateKey`;
- no Gallery/MediaStore mutation occurs.

### 3. Newly discovered media after organization

`MemoryMediaResolver` keeps Gallery output as the primary source and appends only live Discovery refs whose normalized media filename is not already present in the Gallery output.

Review carefully:

- duplicate suppression uses `MediaStoreAlbumLookup.fileSignature()` and media kind;
- stale/empty refs are excluded;
- merged refs are timestamp sorted newest-first;
- a filename-only match can cause false positives or false negatives and must remain documented as a known limitation;
- the implementation does not accidentally append all Discovery refs or duplicate moved originals.

### 4. Representative thumbnail

`MemoryRepository.merge()` now chooses the incoming Discovery cover when its end date is newer than the organized record, otherwise preserving the existing cover.

Review that:

- a newer Discovery photo updates the thumbnail;
- organized-only records keep their existing thumbnail;
- empty incoming covers do not erase a valid cover.

### 5. Home ordering

The Home recent-place row now gives completed location albums priority, while reserving a Discovery slot so newly found places are not completely pushed out when four or more albums exist.

Review that:

- albums appear before Discovery cards;
- new Discovery places such as Songpa-gu and Seongnam remain visible;
- the row remains bounded and does not change the Discovery tab ordering.

## Tests and verification

- `./gradlew.bat testDebugUnitTest`: PASS, 205 tests
- `./gradlew.bat assembleDebug`: PASS
- `git diff --check`: PASS
- Debug APK installed on `R5KL503VHQR` with app data preserved.
- No Gallery album creation, file copy/move, trash, or MediaStore mutation was performed.

## Device smoke-test targets

1. Songpa-gu Memory shows the existing Gallery photos and newer Discovery photos.
2. The date list reaches the newer Discovery date.
3. The representative thumbnail changes to the newer photo.
4. Existing moved originals are not duplicated.
5. Collection detail shows the same reconciled media projection.
6. Collection dissolve leaves the place searchable in Discovery.
7. An organized-only place remains visible and openable.

## Review questions

1. Is the controlled Gallery-plus-new-Discovery projection safe under the current filename-based identity policy?
2. Should duplicate matching be strengthened with size, timestamp, media kind, or another stable signal before release?
3. Does changing the default Discovery browser projection to `repository.memories()` introduce any navigation, count, or performance regression?
4. Are Collection card counts, cover selection, date grouping, and notes consistent with detail view?
5. Is this ready for a 1.3.6 hotfix, or should any item remain a release blocker?
