# Review Request: Memory Collection UI Slice

Review the current uncommitted Memory Collection UI slice in the PhotoPlace working tree. Inspect the actual code and focused diff; do not rely only on the earlier approval in `REVIEW_Memory_Collection_UI_Slice_2026-09-13.md`.

## Changed scope

- `app/src/main/java/com/example/gallerysorter/MainActivity.java`
- `app/src/main/java/com/example/gallerysorter/MemoryCollectionMembership.java`
- `app/src/test/java/com/example/gallerysorter/MemoryCollectionMembershipTest.java`
- `TODO.md`
- `WORKLOG_2026-09-13.md`

The working tree contains other pre-existing local documents/device dumps and an unrelated modified worklog. Do not treat or modify those as part of this review.

## Implemented behavior

- `기억 모으기`: select at least two currently available Discovery Memory places, enter a title, resolve stable `mem_<UUID>` IDs, and persist through `MemoryCollectionStore`.
- One active Collection per stable Memory is enforced in selection UI and by the Store.
- `내 기억 모음` cards appear in the Discovery browser. Collection members are folded out of the unfiltered place grid but remain available through search.
- Collection detail displays `date -> place -> existing date note -> photos`, with incremental 48-thumbnail expansion, rename, and dissolve.
- Dissolve only removes Collection metadata. No Gallery album creation, file movement, or original trash action is connected.
- Follow-up fix after the first review: layout refresh restores Collection detail and selection screens; selection keys/check states survive refresh, and newly unavailable selections are pruned.
- Final polish after follow-up review: same-day aggregate periods render once, the Collection representative cover has a TalkBack description, and a unit test verifies members from multiple Collections are folded together.

## Verification and device state

- `testDebugUnitTest`: PASS.
- `assembleDebug`: PASS.
- `git diff --check`: PASS.
- Rebuilt debug APK installed with `adb install -r` on `R5KL503VHQR` and app launched. User interaction smoke tests and restart persistence test are still pending.
- After the follow-up polish, the newest debug APK was installed with `adb install -r` on `R5KL503VHQR` and launched. User interaction smoke tests and restart persistence are still pending.
- No Gallery/MediaStore mutation or personal photo operation was performed.

## Known limitation / release gate

Selection and Collection detail currently use currently available Discovery refs. A Memory whose usable media exists only in its organized Gallery output is not yet resolved through its exact `OrganizationLink`. Review whether this limitation is clearly contained and documented; do not mark the full Memory Collection lifecycle or release ready until the shared Memory media resolver preserves Gallery-only media, date grouping, and notes.

## Review questions

1. Find concrete correctness, lifecycle, data-loss, accessibility, or navigation issues in the changed code. Order findings by severity and cite file/line.
2. Verify that Collection member identity remains stable and that grouped places remain searchable and individually openable.
3. Check the layout-refresh fix, including selection state and behavior when selected records become unavailable.
4. Check that rename/dissolve do not mutate source Memory, date notes, or Gallery media.
5. Identify missing focused tests or device smoke cases that would block release.
6. Give separate verdicts for this UI checkpoint and for full Play release readiness.

Do not edit files, create albums, move/trash media, or change user data while reviewing.
