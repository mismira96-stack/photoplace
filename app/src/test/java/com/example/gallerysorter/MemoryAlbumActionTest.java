package com.example.gallerysorter;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class MemoryAlbumActionTest {
    @Test public void additionPinsKnownPathAndSkipsExistingPhotoWithoutChangingRefs() throws Exception {
        MemoryRecord record = mixed("Pictures/성남에서/", Arrays.asList(
                ref("1", "old.jpg", MediaKind.PHOTO, false),
                ref("2", "new.jpg", MediaKind.PHOTO, false),
                ref("3", "video.mp4", MediaKind.VIDEO, false)));
        int[] nameResolutions = {0};
        DiscoveryAlbumOrganizer.Preparation result = MemoryAlbumAction.prepare(record,
                new DiscoveryAlbumOrganizer.AlbumLookup() {
                    public String resolveTargetRelativePath(String place, String proposed) {
                        nameResolutions[0]++;
                        return "Pictures/wrong-folder/";
                    }
                    public DiscoveryAlbumOrganizer.Match find(String path, String name, boolean video) {
                        assertEquals("Pictures/성남에서/", path);
                        return new DiscoveryAlbumOrganizer.Match(!video, name.equals("old.jpg"));
                    }
                });
        assertEquals(0, nameResolutions[0]);
        assertEquals(1, result.actionableCount(false));
        assertEquals(1, result.duplicateCount);
        assertEquals(1, result.excludedVideoCount(false));
        assertEquals(3, record.discoveryGroup.photoRefs.size());
        assertEquals("앨범에 추가", MemoryAlbumAction.label(record));
        assertEquals("사진 추가하기", MemoryAlbumAction.confirmationLabel(result));
        assertEquals("기존 위치 앨범에 사진을 추가할까요?", MemoryAlbumAction.confirmationTitle(result));
    }

    @Test public void allExistingPhotosProduceNoCopyCandidates() throws Exception {
        DiscoveryAlbumOrganizer.Preparation result = MemoryAlbumAction.prepare(
                mixed("Pictures/성남에서/", Collections.singletonList(ref("1", "old.jpg", MediaKind.PHOTO, false))),
                lookup(true, true));
        assertEquals(0, result.actionableCount(false));
        assertEquals(1, result.duplicateCount);
    }

    @Test public void resolvedCopiedPhotosDoNotKeepAdditionButtonVisible() {
        DiscoveryPhotoRef original = ref("1", "old.jpg", MediaKind.PHOTO, false);
        MemoryRecord record = mixed("Pictures/성남에서/", Collections.singletonList(original));
        assertTrue(MemoryAlbumAction.canPrepare(record)); // Preparation remains independently callable.
        List<DiscoveryPhotoRef> output = Collections.singletonList(
                ref("actual-copy", "old.jpg", MediaKind.PHOTO, false));
        assertFalse(MemoryAlbumAction.canPrepare(record, output));
        assertFalse(MemoryBrowserDetail.from(MemoryBrowserItem.from(record), record, output).canOrganize);
        assertEquals(1, record.discoveryGroup.photoRefs.size());
    }

    @Test public void remainingNewOriginalEnablesAdditionAndNewVideosDoNot() {
        DiscoveryPhotoRef old = ref("1", "old.jpg", MediaKind.PHOTO, false);
        DiscoveryPhotoRef fresh = ref("2", "new.jpg", MediaKind.PHOTO, false);
        DiscoveryPhotoRef video = ref("3", "new.mp4", MediaKind.VIDEO, false);
        MemoryRecord record = mixed("Pictures/성남에서/", Arrays.asList(old, fresh, video));
        DiscoveryPhotoRef output = ref("actual-copy", "old.jpg", MediaKind.PHOTO, false);
        assertTrue(MemoryAlbumAction.canPrepare(record, Arrays.asList(output, fresh, video)));
        assertFalse(MemoryAlbumAction.canPrepare(record, Arrays.asList(output, video)));
        assertFalse(MemoryAlbumAction.canPrepare(record, Collections.emptyList()));
    }

    @Test public void originalAlreadyInTargetFolderIsNotAnAddition() {
        DiscoveryPhotoRef alreadyThere = new DiscoveryPhotoRef("content://media/1", 1,
                MediaKind.PHOTO, "image/jpeg", "old.jpg", 1L, "성남", "성남", "KR", "대한민국",
                "", "", "Pictures/성남에서", 1L, 1L, false);
        MemoryRecord record = mixed("Pictures/성남에서/", Collections.singletonList(alreadyThere));
        assertFalse(MemoryAlbumAction.canPrepare(record, Collections.singletonList(alreadyThere)));
    }

    @Test public void discoveryOnlyNewAlbumStillHasActionAndAlbumOnlyDoesNot() {
        DiscoveryPhotoRef original = ref("1", "new.jpg", MediaKind.PHOTO, false);
        MemoryRecord discovered = MemoryRepository.fromDiscoveryGroup(group(Collections.singletonList(original)));
        assertTrue(MemoryAlbumAction.canPrepare(discovered, Collections.singletonList(original)));
        MemoryRecord albumOnly = new MemoryRecord("path:album", "성남", "성남", "성남", "", "KR",
                "대한민국", "", "", 121, 0, 0, 1, 2, "", MemorySourceType.ORGANIZED_ALBUM, null,
                new OrganizedAlbumRef("Pictures/성남에서/", "성남에서", 121, "", "KR", "대한민국", 1, 2),
                0, 121, false, true, false, false);
        assertFalse(MemoryAlbumAction.canPrepare(albumOnly, Collections.singletonList(original)));
    }

    @Test public void missingAlbumDoesNotFallBackToCreatingAnotherFolder() {
        assertThrows(IllegalStateException.class, () -> MemoryAlbumAction.prepare(
                mixed("Pictures/성남에서/", Collections.singletonList(ref("1", "new.jpg", MediaKind.PHOTO, false))),
                lookup(false, false)));
    }

    @Test public void unknownAlbumReadPropagatesWithoutProducingAPlan() {
        assertThrows(IllegalStateException.class, () -> MemoryAlbumAction.prepare(
                mixed("Pictures/성남에서/", Collections.singletonList(ref("1", "new.jpg", MediaKind.PHOTO, false))),
                new DiscoveryAlbumOrganizer.AlbumLookup() {
                    public String resolveTargetRelativePath(String place, String proposed) { return proposed; }
                    public DiscoveryAlbumOrganizer.Match find(String path, String name, boolean video) {
                        throw new IllegalStateException("permission unavailable");
                    }
                }));
    }

    @Test public void staleEmptyAndVideoOnlyDiscoveryDoNotOfferPhotoAddition() {
        for (List<DiscoveryPhotoRef> refs : Arrays.asList(
                Collections.singletonList(ref("1", "stale.jpg", MediaKind.PHOTO, true)),
                Collections.singletonList(ref("", "empty.jpg", MediaKind.PHOTO, false)),
                Collections.singletonList(ref("3", "video.mp4", MediaKind.VIDEO, false)),
                Collections.<DiscoveryPhotoRef>emptyList())) {
            assertFalse(MemoryAlbumAction.canPrepare(mixed("Pictures/성남에서/", refs)));
        }
        assertFalse(MemoryAlbumAction.canPrepare(null));
    }

    @Test public void unsafeOrAbsentKnownPathCannotEnableMixedAddition() {
        for (String path : Arrays.asList("", "Pictures/", "DCIM/Camera/", "Pictures/../Camera/",
                "Pictures//성남에서/", "Pictures\\성남에서\\")) {
            assertFalse(MemoryAlbumAction.canPrepare(mixed(path,
                    Collections.singletonList(ref("1", "new.jpg", MediaKind.PHOTO, false)))));
        }
    }

    @Test public void discoveryOnlyCreationStillUsesExistingLookup() throws Exception {
        MemoryRecord record = MemoryRepository.fromDiscoveryGroup(group(
                Collections.singletonList(ref("1", "new.jpg", MediaKind.PHOTO, false))));
        assertTrue(MemoryAlbumAction.canPrepare(record));
        assertEquals("앨범 만들기", MemoryAlbumAction.label(record));
        DiscoveryAlbumOrganizer.Preparation result = MemoryAlbumAction.prepare(record, lookup(false, false));
        assertEquals(1, result.actionableCount(false));
        assertEquals("이 장소만 만들기", MemoryAlbumAction.confirmationLabel(result));
    }

    private static DiscoveryAlbumOrganizer.AlbumLookup lookup(boolean exists, boolean duplicate) {
        return new DiscoveryAlbumOrganizer.AlbumLookup() {
            public String resolveTargetRelativePath(String place, String proposed) { return proposed; }
            public DiscoveryAlbumOrganizer.Match find(String path, String name, boolean video) {
                return new DiscoveryAlbumOrganizer.Match(exists, duplicate && !name.isEmpty());
            }
        };
    }

    private static MemoryRecord mixed(String path, List<DiscoveryPhotoRef> refs) {
        DiscoveryMemoryGroup group = group(refs);
        return new MemoryRecord("path:" + path, "성남", "성남", "성남", "", "KR", "대한민국", "", "",
                refs.size(), refs.size(), 0, 1L, 2L, "", MemorySourceType.MIXED, group,
                new OrganizedAlbumRef(path, "성남에서", 373, "", "KR", "대한민국", 1L, 2L),
                0, refs.size(), true, true, false, true);
    }

    private static DiscoveryMemoryGroup group(List<DiscoveryPhotoRef> refs) {
        return new DiscoveryMemoryGroup("discovery:성남", "성남", "성남", "KR", "대한민국", "", "",
                refs.size(), refs.size(), 0, 1L, 2L, "", refs, 0, 1L);
    }

    private static DiscoveryPhotoRef ref(String id, String name, MediaKind kind, boolean stale) {
        return new DiscoveryPhotoRef(id.isEmpty() ? "" : "content://media/" + id, 1L, kind,
                kind == MediaKind.VIDEO ? "video/mp4" : "image/jpeg", name, 1L,
                "성남", "성남", "KR", "대한민국", "", "", "", 1L, 1L, stale);
    }
}
