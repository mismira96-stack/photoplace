package com.example.gallerysorter;

import org.junit.Test;
import org.json.JSONObject;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryManagementPolicyTest {
    @Test public void selectingEitherSideExpandsConfirmedPlacementPair() {
        UnresolvedMediaRecord source=row(1,"Download/"),copy=row(2,"Pictures/p/");
        List<ManualMediaAssignment> pair=Arrays.asList(new ManualMediaAssignment(source,"p",copy.assetKey(),false),new ManualMediaAssignment(copy,"p","",true));
        assertEquals(2,ManualAssignmentSelection.expand(Collections.singletonList(source),pair).size());
        assertEquals(2,ManualAssignmentSelection.expand(Collections.singletonList(copy),pair).size());
    }
    @Test public void unrelatedOrAlreadySplitPlacementsAreNotChangedByPairSelection() {
        UnresolvedMediaRecord source=row(1,"Download/"),copy=row(2,"Pictures/p/");
        List<ManualMediaAssignment> split=Arrays.asList(new ManualMediaAssignment(source,"p",copy.assetKey(),false),new ManualMediaAssignment(copy,"q","",true));
        assertEquals(1,ManualAssignmentSelection.expand(Collections.singletonList(copy),split).size());
    }
    @Test public void pairedSelectionDoesNotDuplicateAnAlreadySelectedFile() {
        UnresolvedMediaRecord source=row(1,"Download/"),copy=row(2,"Pictures/p/");
        List<ManualMediaAssignment> pair=Arrays.asList(new ManualMediaAssignment(source,"p",copy.assetKey(),false),new ManualMediaAssignment(copy,"p","",true));
        assertEquals(2,ManualAssignmentSelection.expand(Arrays.asList(copy,source),pair).size());
    }
    @Test public void manualVideoConfirmationAndExecutionUseSamePreference() {
        DiscoveryPhotoRef video=new DiscoveryPhotoRef("content://media/external/video/media/1",1,MediaKind.VIDEO,"video/mp4","v.mp4",1000,"p","p","","","","","Download/",0,1,false,true);
        DiscoveryAlbumOrganizer.PreparedItem item=new DiscoveryAlbumOrganizer.PreparedItem(video,"p","Pictures/p/",new DiscoveryAlbumOrganizer.Match(false,false));
        DiscoveryAlbumOrganizer.Preparation prep=new DiscoveryAlbumOrganizer.Preparation(Collections.singletonList(item),1,1,0,0);
        assertEquals(1,prep.actionableCount(OrganizationMediaPolicy.shouldMoveDiscoveryVideos(true,prep)));
        assertEquals(0,prep.excludedVideoCount(OrganizationMediaPolicy.shouldMoveDiscoveryVideos(true,prep)));
        assertEquals(0,prep.actionableCount(OrganizationMediaPolicy.shouldMoveDiscoveryVideos(false,prep)));
    }
    @Test public void manualCopyKeepsUnknownDateDespiteProviderGeneratedToday()throws Exception {
        UnresolvedMediaRecord source=new UnresolvedMediaRecord("content://media/external/images/media/1","external_primary","v1",1,"PHOTO","a.png","image/png","Download/",10,20,0);
        UnresolvedMediaRecord recorded=new UnresolvedMediaRecord("content://media/external/images/media/2","external_primary","v1",2,"PHOTO","a.png","image/png","Pictures/우리집에서/",10,20,0);
        UnresolvedMediaRecord current=new UnresolvedMediaRecord(recorded.uri,recorded.volume,recorded.mediaVersion,2,"PHOTO","a.png","image/png",recorded.relativePath,10,20,1800000000000L);
        ManualPlaceSnapshot s=manual(Arrays.asList(new ManualMediaAssignment(source,"p",recorded.assetKey(),false),new ManualMediaAssignment(recorded,"p","",true)));
        DiscoverySnapshot projection=EffectiveMediaPlaceResolver.project(empty(),s,live(source,current),false);
        assertEquals(0,projection.groups.get(0).photoRefs.get(0).takenAtMillis);
        ManualGalleryPolicy policy=new ManualGalleryPolicy(s,live(source,current),s.mergedAliases(Collections.emptyMap()));
        MemoryRecord record=MemoryRepository.fromDiscoveryGroup(group("manual:p"));
        MemoryMediaStoreReader.Result filtered=policy.filter(MemoryMediaStoreReader.Result.found(Collections.singletonList(ref(current,false))),record);
        assertEquals(0,filtered.refs.get(0).takenAtMillis);assertTrue(filtered.refs.get(0).manualAssigned);
    }
    @Test public void emptyAlbumWithAuthoredNotesIsResolvedWithoutRequiringMissingPhotos() {
        DiscoveryMemoryGroup g=new DiscoveryMemoryGroup("manual:p","manual-place:p","집","","","","",0,0,0,0,0,"",Collections.emptyList(),0,1,true);
        MemoryRecord record=MemoryRepository.fromDiscoveryGroup(g);
        MemoryMediaResolution result=MemoryMediaResolver.resolve(record,link("mem_p","Pictures/집에서/"),(path,memory)->MemoryMediaStoreReader.Result.missing());
        assertEquals(MemoryMediaSourcePolicy.Source.EMPTY,result.source);assertTrue(result.refs.isEmpty());
    }
    private UnresolvedMediaRecord row(long id,String path){return new UnresolvedMediaRecord("content://media/external/images/media/"+id,
            "external_primary","v1",id,"PHOTO","IMG_0001.jpg","image/jpeg",path,10,20,1700000000000L);}
    private ManualPlaceSnapshot manual(List<ManualMediaAssignment> assigned)throws Exception {
        JSONObject root=ManualPlaceSnapshot.empty().writableCopy();
        root.getJSONObject("places").put("p",new JSONObject().put("memoryKey","manual:p").put("stableMemoryId","mem_p").put("name","우리집"));
        root.getJSONObject("places").put("q",new JSONObject().put("memoryKey","manual:q").put("stableMemoryId","mem_q").put("name","회사"));
        return new ManualPlaceSnapshot(1,root.toString()).withAssignments(assigned,assigned.size());
    }
    private DiscoverySnapshot empty(){return new DiscoverySnapshot(1,1,1,"",0,Collections.emptyList(),"","");}
    private Map<String,UnresolvedMediaRecord> live(UnresolvedMediaRecord... rows){Map<String,UnresolvedMediaRecord> map=new LinkedHashMap<>();for(UnresolvedMediaRecord r:rows)map.put(r.assetKey(),r);return map;}
    private DiscoveryMemoryGroup group(String key,DiscoveryPhotoRef... refs){return new DiscoveryMemoryGroup(key,key,key,"","","","",refs.length,refs.length,0,0,0,"",Arrays.asList(refs),0,1);}
    private DiscoveryPhotoRef ref(UnresolvedMediaRecord row,boolean manual){return new DiscoveryPhotoRef(row.uri,row.mediaId,MediaKind.PHOTO,row.mimeType,row.name,row.takenAt,"place","place","","","","",row.relativePath,0,1,false,manual);}
    private OrganizationLink link(String id,String path){return new OrganizationLink("link_1",OrganizationLink.SubjectType.MEMORY,id,"job","album",path,1,OrganizationLink.Status.SUCCESS,1,0,0);}

    @Test public void noExistingRecordsCanCreateManualMemoryWithoutGps()throws Exception {
        UnresolvedMediaRecord r=row(1,"Download/");ManualPlaceSnapshot s=manual(Collections.singletonList(new ManualMediaAssignment(r,"p","",false)));
        DiscoverySnapshot projected=EffectiveMediaPlaceResolver.project(empty(),s,live(r),false);
        MemoryRecord record=new MemoryRepository(projected,Collections.emptyList(),s.mergedAliases(Collections.emptyMap()),Collections.emptyList()).memory("manual:p");
        assertNotNull(record);assertEquals(1,record.itemCount);assertTrue(record.discoveryGroup.photoRefs.get(0).manualAssigned);
        assertTrue(MemoryAlbumAction.canPrepare(record));assertEquals("",record.countryCode);
    }
    @Test public void confirmedCopyPreferredOnlyWhenItExistsAndBelongsToSamePlace()throws Exception {
        UnresolvedMediaRecord source=row(1,"Download/"),copy=row(2,"Pictures/우리집에서/");
        ManualPlaceSnapshot s=manual(Arrays.asList(new ManualMediaAssignment(source,"p",copy.assetKey(),false),new ManualMediaAssignment(copy,"p","",true)));
        DiscoverySnapshot shown=EffectiveMediaPlaceResolver.project(empty(),s,live(source,copy),false);
        DiscoveryMemoryGroup g=shown.groups.get(0);assertEquals(1,g.itemCount);assertEquals(copy.uri,g.photoRefs.get(0).sourceUri);
        DiscoverySnapshot missing=EffectiveMediaPlaceResolver.project(empty(),s,live(source),false);
        assertEquals(source.uri,missing.groups.get(0).photoRefs.get(0).sourceUri);
        DiscoverySnapshot guard=EffectiveMediaPlaceResolver.project(empty(),s,live(source,copy),true);
        assertEquals(2,guard.groups.get(0).itemCount);
    }
    @Test public void copyReassignedToAnotherRecordDoesNotHideSource()throws Exception {
        UnresolvedMediaRecord source=row(1,"Download/"),copy=row(2,"Pictures/우리집에서/");
        ManualPlaceSnapshot s=manual(Arrays.asList(new ManualMediaAssignment(source,"p",copy.assetKey(),false),new ManualMediaAssignment(copy,"q","",true)));
        DiscoverySnapshot shown=EffectiveMediaPlaceResolver.project(empty(),s,live(source,copy),false);
        assertEquals(1,shown.groups.get(0).itemCount);assertEquals(source.uri,shown.groups.get(0).photoRefs.get(0).sourceUri);
    }
    @Test public void unresolvedExcludesAssignmentsAndKnownAlbumsButNotUnrelatedSameNames()throws Exception {
        UnresolvedMediaRecord source=row(1,"Download/"),organized=row(2,"Pictures/우리집에서/"),other=row(3,"Other/");
        ManualPlaceSnapshot s=manual(Collections.singletonList(new ManualMediaAssignment(source,"p","",false)))
                .withInventory(1,Arrays.asList(source,organized,other));
        List<UnresolvedMediaRecord> result=EffectiveMediaPlaceResolver.unresolved(s,live(source,organized,other),Collections.singleton("Pictures/우리집에서/"));
        assertEquals(1,result.size());assertEquals(other.uri,result.get(0).uri);
    }
    @Test public void manualSameNameFileIsNotSuppressedByAlbumFilenameSignature()throws Exception {
        UnresolvedMediaRecord source=row(1,"Download/"),other=row(2,"Pictures/우리집에서/");
        MemoryRecord r=MemoryRepository.fromDiscoveryGroup(group("manual:p",ref(source,true)));
        MemoryMediaResolution result=MemoryMediaResolver.resolve(r,link("mem_p","Pictures/우리집에서/"),
                (path,memory)->MemoryMediaStoreReader.Result.found(Collections.singletonList(ref(other,false))));
        assertEquals(2,result.refs.size());
    }
    @Test public void manualGalleryOwnerWinsOverOldPhysicalAlbumMembership()throws Exception {
        UnresolvedMediaRecord copy=row(2,"Pictures/우리집에서/");ManualPlaceSnapshot s=manual(Collections.singletonList(new ManualMediaAssignment(copy,"q","",true)));
        Map<String,String> aliases=new HashMap<>(s.mergedAliases(Collections.emptyMap()));aliases.put("old","mem_p");
        ManualGalleryPolicy policy=new ManualGalleryPolicy(s,live(copy),aliases);
        MemoryRecord old=MemoryRepository.fromDiscoveryGroup(group("old")).withGalleryPolicy(policy);
        MemoryMediaStoreReader.Result raw=MemoryMediaStoreReader.Result.found(Collections.singletonList(ref(copy,false)));
        assertTrue(policy.filter(raw,old).refs.isEmpty());
        MemoryRecord target=MemoryRepository.fromDiscoveryGroup(group("manual:q")).withGalleryPolicy(policy);
        assertEquals(1,policy.filter(raw,target).refs.size());
    }
    @Test public void authoredNotesKeepTheirOriginalIdAndEmptyDateSection() {
        UnresolvedMediaRecord r=row(1,"Download/");DiscoveryMemoryGroup original=group("old",ref(r,false));
        DiscoverySnapshot raw=new DiscoverySnapshot(1,1,1,"",1,Collections.singletonList(original),"","");
        MemoryDateNote note=new MemoryDateNote("mem_old","20231114","중요한 메모",1,1);
        Map<String,String> aliases=new HashMap<>();aliases.put("old","mem_old");
        DiscoverySnapshot projected=MemoryNoteProjection.retain(raw,empty(),aliases,Collections.singletonList(note));
        MemoryRecord record=MemoryRepository.fromDiscoveryGroup(projected.groups.get(0));
        assertEquals("old",record.memoryKey);assertEquals(0,record.itemCount);assertTrue(record.canOpenPhotos);
        List<MemoryPhotoSection> sections=MemoryPhotoSection.withNotes(Collections.emptyList(),Collections.singletonList(note));
        assertEquals("20231114",sections.get(0).dateKey);assertTrue(sections.get(0).photos.isEmpty());assertEquals("mem_old",note.stableMemoryId);
    }
    @Test public void legacyCountOnlyGroupSurvivesUnrelatedManualPlaces()throws Exception {
        DiscoveryMemoryGroup legacy=new DiscoveryMemoryGroup("legacy","legacy","legacy","","","","",121,0,0,0,0,"",Collections.emptyList(),0,1);
        DiscoverySnapshot raw=new DiscoverySnapshot(1,1,1,"",121,Collections.singletonList(legacy),"","");
        DiscoverySnapshot result=EffectiveMediaPlaceResolver.project(raw,manual(Collections.emptyList()),Collections.emptyMap(),false);
        assertEquals(121,result.groups.get(0).itemCount);
    }
    @Test public void manualSameNameAlbumGetsANewPathInsteadOfAdoptingOldFolder()throws Exception {
        MemoryRecord record=MemoryRepository.fromDiscoveryGroup(group("manual:p",ref(row(1,"Download/"),true)));
        String proposed=OrganizePlaceService.planFor(record).targetRelativePath;
        DiscoveryAlbumOrganizer.Preparation prep=DiscoveryAlbumOrganizer.prepare(Collections.singletonList(record),new DiscoveryAlbumOrganizer.AlbumLookup(){
            public String resolveTargetRelativePath(String name,String path){throw new AssertionError("Must not guess manual association by name");}
            public DiscoveryAlbumOrganizer.Match find(String path,String name,boolean video){return new DiscoveryAlbumOrganizer.Match(path.equals(proposed),true);}
        });
        assertEquals(1,prep.items.size());assertNotEquals(proposed,prep.items.get(0).targetRelativePath);assertFalse(prep.items.get(0).duplicateInTarget);
    }
    @Test public void manualVideoOnlyRecordCanPrepareAnAlbum() {
        UnresolvedMediaRecord media=new UnresolvedMediaRecord("content://media/external/video/media/1","external_primary","v1",1,"VIDEO","v.mp4","video/mp4","Download/",10,20,1000);
        DiscoveryPhotoRef video=new DiscoveryPhotoRef(media.uri,1,MediaKind.VIDEO,media.mimeType,media.name,1000,"place","place","","","","","Download/",0,1,false,true);
        assertTrue(MemoryAlbumAction.canPrepare(MemoryRepository.fromDiscoveryGroup(group("manual:p",video))));
    }
    @Test public void changedSelectedFileIsNotSilentlyAcceptedByIntentionalTrash() {
        UnresolvedMediaRecord before=row(1,"Download/"),changed=row(1,"Other/");
        try{MemoryTrashExecution.verifyObservations(Collections.singletonList(before),Collections.singletonList(changed));fail();}
        catch(IllegalStateException expected){ }
    }
    private MemoryTrashJournal.Session plan(int count){List<UnresolvedMediaRecord> rows=new ArrayList<>();for(int i=1;i<=count;i++)rows.add(row(i,"Download/"));MemoryTrashJournal.Session s=new MemoryTrashJournal.Session(rows);s.state="AUDITING";return s;}
    @Test public void intentionalTrash1001BoundaryCountsEachBatchOnce() {
        MemoryTrashJournal.Session s=plan(1001);MemoryTrashProgress.record(s,1000,300,700,0);
        assertEquals("PAUSED",s.state);assertEquals(1000,s.offset);assertEquals(300,s.moved);assertEquals(700,s.skipped);
        s.state="AUDITING";MemoryTrashProgress.record(s,1,1,0,0);assertEquals("DONE",s.state);assertEquals(301,s.moved);assertEquals(1001,s.offset);
    }
    @Test public void canceledSystemRequestDoesNotSkipItsUnmovedBatch() {
        MemoryTrashJournal.Session s=plan(3);MemoryTrashProgress.record(s,3,0,3,0);
        assertEquals("PAUSED",s.state);assertEquals(0,s.offset);assertEquals(0,s.skipped);assertEquals(0,s.audited);
    }
    @Test public void unknownMediaStateStopsFurtherBatchesWithoutAdvancing() {
        MemoryTrashJournal.Session s=plan(1001);MemoryTrashProgress.record(s,1000,990,9,1);
        assertEquals("NEEDS_ATTENTION",s.state);assertEquals(0,s.offset);assertEquals(990,s.batchMoved);assertEquals(0,s.moved);
    }
    @Test public void repeatedOrInvalidCheckpointsCannotIncrementTwice() {
        MemoryTrashJournal.Session s=plan(100);MemoryTrashProgress.record(s,50,50,0,0);
        try{MemoryTrashProgress.record(s,50,50,0,0);fail();}catch(IllegalArgumentException expected){ }
        assertEquals(50,s.batchMoved);assertEquals(50,s.audited);
    }
}
