# Media Analysis Cache Integration Plan - 2026-08-26

## Goal

Avoid repeated EXIF and reverse-Geocoder work for unchanged media without hiding any
photo from preview counts, Discovery History, or Memory browsing.

This is an incremental-analysis optimization, not a filtering feature.

## Scope Of The First Patch

- Connect `MediaAnalysisStore` to **image scanning only** in `MainActivity.loadSourceImages()`.
- Keep video scanning on the existing path.
- Do not change album creation, `SortWorker`, Discovery merging, or UI behavior.
- Do not re-enable the old `NoLocationCache`.

## Read Path

1. Build `MediaAnalysisSignature` from the current MediaStore row.
2. If MediaStore exposes latitude and longitude, ignore any cached `NO_LOCATION` result and reanalyze.
3. Otherwise read the matching `MediaAnalysisEntry` from an in-memory index loaded from `MediaAnalysisStore` once per analysis run.
4. On a cache hit, reconstruct the normal `LocationResult` and continue through the existing `buildPhotoItem()` path.
5. On a cache miss, call the existing `readLocation()` and stage the normalized result for storage.

Invariant: a cache hit skips only EXIF/Geocoder work. It must still produce a normal
`PhotoItem`, remain in Preview/Discovery inventory, and contribute to the visible
`위치 없음` count when applicable.

## Write Path

- Maintain a run-local map of existing and newly analyzed entries.
- Save the complete updated map only after the image scan completes normally.
- On cancellation, thrown error, or incomplete scan, do not persist newly staged entries.
- `MediaAnalysisStore.saveAll()` remains the only writer and keeps its temp/backup atomic-write behavior.

## Invalidation

The signature already separates changed/copy/move media through URI and row metadata.
Treat the following as cache misses:

- new URI or MediaStore id;
- changed name, modified/added/taken time, size/duration, type, source folder, or policy version;
- a current MediaStore GPS coordinate when the cache has `NO_LOCATION`;
- an unsupported/corrupt cache entry.

Do not infer a country or location from an old place name when a cache entry is missing.

## Required Tests Before Device Verification

1. Cache hit reconstructs an analyzed `PhotoItem` equivalent to the miss path.
2. Cache hit for `NO_LOCATION` still emits a `PhotoItem` counted as no-location and excluded from Discovery groups.
3. New image alongside cached images: all images remain in output; only the new image calls `readLocation()`.
4. Current MediaStore GPS invalidates a cached `NO_LOCATION` entry.
5. Cancelled/failed scan does not commit staged cache entries.

## Device Smoke Test

1. Analyze a small source folder once.
2. Analyze the same folder again: Discovery History remains complete and the run is visibly faster.
3. Add one new GPS photo: the existing place remains and the new photo appears as `NEW` for that place.
4. Include a no-location photo: it remains visible in the no-location count on both runs.
5. Move/copy or edit a test image: it is reanalyzed rather than receiving an unrelated cached result.

## Explicitly Deferred

- video scan integration;
- cache trimming/TTL and large-cache performance policy;
- MediaStore reconciliation after external deletion;
- checkpoint/resume for interrupted full analysis.
