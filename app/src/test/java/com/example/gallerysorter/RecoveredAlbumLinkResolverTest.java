package com.example.gallerysorter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecoveredAlbumLinkResolverTest {
    static final long TAKEN=1765152000123L;
    static DiscoveryMemoryGroup group(String key,String title,String country) {
        DiscoveryPhotoRef photo=new DiscoveryPhotoRef("source:"+key,10,MediaKind.PHOTO,"image/jpeg","fresh.jpg",TAKEN,
                title,title,country,"","","","DCIM/Camera/",1,1,false);
        return new DiscoveryMemoryGroup(key,title,title,country,"","","",1,1,0,TAKEN,TAKEN,photo.sourceUri,Collections.singletonList(photo),0,1);
    }
    static DiscoveryMemoryGroup videoOnlyGroup(String key, String title, String country) {
        DiscoveryPhotoRef video = new DiscoveryPhotoRef("source:" + key, 10, MediaKind.VIDEO, "video/mp4", "movie.mp4", TAKEN,
                title, title, country, "", "", "", "DCIM/Camera/", 1, 1, false);
        return new DiscoveryMemoryGroup(key, title, title, country, "", "", "", 1, 0, 1, TAKEN, TAKEN, video.sourceUri, Collections.singletonList(video), 0, 1);
    }
    static StoredAlbumSummary album(String title,String country) {
        return new StoredAlbumSummary(title+"에서","Pictures/"+title+"에서/",127,"2025-12-08","2025-12-08",
                "album:"+title,"",0,country,"","","",127,0,true);
    }
    static DiscoverySnapshot snapshot(DiscoveryMemoryGroup... groups) {
        return new DiscoverySnapshot(1,1,1,"test",groups.length,Arrays.asList(groups),"test","test");
    }
    static RecoveredAlbumLinkResolver.GeoReader nearby() {
        return uri->new RecoveredAlbumLinkResolver.Point(uri.startsWith("album:")?37.6:37.6001,128.4);
    }
    static MemoryRepository repository(Map<String,String> ids,RecoveredAlbumLinkResolver.GeoReader reader) {
        return new MemoryRepository(snapshot(group("discovery:평창","평창","KR")),Collections.singletonList(album("평창","")),ids,Collections.emptyList(),null,reader);
    }
    @Test public void reinstallRestoredCountrylessAlbumIsConnectedOnlyWithGpsEvidence() {
        MemoryRepository oldPath=repository(Collections.emptyMap(),null);
        assertEquals(2,oldPath.memories().size());
        MemoryRepository fixed=repository(Collections.emptyMap(),nearby());
        List<MemoryRecord> result=fixed.memories();assertEquals(1,result.size());
        MemoryRecord record=result.get(0);assertEquals(MemorySourceType.MIXED,record.sourceType);
        assertEquals("discovery:평창",record.memoryKey);assertEquals("Pictures/평창에서/",record.organizedAlbum.relativePath);
        assertEquals(TAKEN,record.discoveryGroup.photoRefs.get(0).takenAtMillis);
        assertEquals("discovery:평창",fixed.presentationKey("path:Pictures/평창에서/"));
        assertEquals(MemorySourceType.MIXED,fixed.memory("path:Pictures/평창에서/").sourceType);
        assertEquals(MemorySourceType.MIXED,fixed.memory("discovery:평창").sourceType);
        assertNull(fixed.usableMemoryLink(record)); // UI recovery never grants original cleanup rights.
        assertTrue(record.canOpenGalleryAlbum);assertTrue(record.canAddNewItems);
    }
    @Test public void videoOnlyDiscoveryGroupIsConnectedToRestoredAlbumWithGpsEvidence() {
        MemoryRepository repo = new MemoryRepository(snapshot(videoOnlyGroup("discovery:안동", "안동", "KR")),
                Collections.singletonList(album("안동", "")), Collections.emptyMap(), Collections.emptyList(), null, nearby());
        List<MemoryRecord> result = repo.memories();
        assertEquals(1, result.size());
        MemoryRecord record = result.get(0);
        assertEquals(MemorySourceType.MIXED, record.sourceType);
        assertEquals("안동", record.title);
        assertEquals("Pictures/안동에서/", record.organizedAlbum.relativePath);
        assertEquals(128, record.itemCount);
        assertEquals(127, record.photoCount);
        assertEquals(1, record.videoCount);
        assertEquals("사진 127장 · 동영상 1개", MemoryMediaCounts.forRecord(record));
    }
    @Test public void missingOrUnavailableGpsNeverFallsBackToNameAlone() {
        assertEquals(2,repository(Collections.emptyMap(),uri->null).memories().size());
        assertEquals(2,repository(Collections.emptyMap(),uri->{throw new SecurityException();}).memories().size());
        assertEquals(2,repository(Collections.emptyMap(),uri->new RecoveredAlbumLinkResolver.Point(0,0)).memories().size());
    }
    @Test public void sameNameAtFarCoordinatesStaysSeparate() {
        assertEquals(2,repository(Collections.emptyMap(),uri->new RecoveredAlbumLinkResolver.Point(uri.startsWith("album:")?35:37.6,128.4)).memories().size());
    }
    @Test public void conflictingStableIdsStaySeparateAndNeverReassignNotes() {
        Map<String,String> aliases=new LinkedHashMap<>();aliases.put("path:Pictures/평창에서/","mem_album_notes");aliases.put("discovery:평창","mem_discovery_notes");
        Map<String,String> before=new LinkedHashMap<>(aliases);
        assertEquals(2,repository(aliases,nearby()).memories().size());assertEquals(before,aliases);
    }
    @Test public void existingAlbumIdentityRemainsPrimaryForItsNotesAndCollections() {
        Map<String,String> ids=Collections.singletonMap("path:Pictures/평창에서/","mem_album_notes");
        MemoryRepository repo=repository(ids,nearby());MemoryRecord result=repo.memories().get(0);
        assertEquals("path:Pictures/평창에서/",result.memoryKey);assertEquals("mem_album_notes",repo.stableId(result.memoryKey));
        assertEquals(result.memoryKey,repo.presentationKey("discovery:평창"));
    }
    @Test public void existingDiscoveryIdentityRemainsPrimaryForItsNotesAndCollections() {
        MemoryRepository repo=repository(Collections.singletonMap("discovery:평창","mem_source_notes"),nearby());
        MemoryRecord record=repo.memories().get(0);assertEquals("discovery:평창",record.memoryKey);
        assertEquals("mem_source_notes",repo.stableId(record.memoryKey));
        assertEquals(TAKEN,record.discoveryGroup.startDateMillis);
    }
    @Test public void unknownCountryOverseasAndKnownCountryConflictStaySeparate() {
        assertEquals(2,new MemoryRepository(snapshot(group("d","평창","JP")),Collections.singletonList(album("평창","")),Collections.emptyMap(),Collections.emptyList(),null,nearby()).memories().size());
        assertEquals(2,new MemoryRepository(snapshot(group("d","평창","KR")),Collections.singletonList(album("평창","US")),Collections.emptyMap(),Collections.emptyList(),null,nearby()).memories().size());
    }
    @Test public void homonymousDiscoveryRecordsAreAmbiguousEvenWithNearbyGps() {
        MemoryRepository repo=new MemoryRepository(snapshot(group("kr","평창","KR"),group("us","평창","US")),Collections.singletonList(album("평창","")),Collections.emptyMap(),Collections.emptyList(),null,nearby());
        assertEquals(3,repo.memories().size());
    }
    @Test public void existingOutputOwnershipAndManualSourcesNeverGetGuessed() {
        MemoryRecord target=MemoryRepository.fromOrganizedAlbum(album("평창",""));
        MemoryRecord source=MemoryRepository.fromDiscoveryGroup(group("d","평창","KR"));
        assertTrue(RecoveredAlbumLinkResolver.resolve(Collections.singletonList(target),Collections.singletonList(source),Collections.emptyMap(),Collections.singleton("Pictures/평창에서"),nearby()).isEmpty());
        MemoryRecord manual=MemoryRepository.fromDiscoveryGroup(group("manual:abc","평창","KR"));
        assertTrue(RecoveredAlbumLinkResolver.resolve(Collections.singletonList(target),Collections.singletonList(manual),Collections.emptyMap(),Collections.emptySet(),nearby()).isEmpty());
    }
    @Test public void representativeReadsAreCachedAcrossBrowserAndDetailRequests() {
        int[] calls={0};MemoryRepository repo=repository(Collections.emptyMap(),uri->{calls[0]++;return nearby().read(uri);});
        repo.memories();repo.memories();repo.presentationKey("path:Pictures/평창에서/");repo.memory("discovery:평창");
        assertEquals(2,calls[0]);
    }
}
