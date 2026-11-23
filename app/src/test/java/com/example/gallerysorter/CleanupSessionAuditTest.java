package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tests actual persisted-cursor transitions, including interrupted batches and copy loss. */
public class CleanupSessionAuditTest {
    private final CleanupExecutionSafetyTest fixtures = new CleanupExecutionSafetyTest();
    private CleanupExecutionJournal.Session session(int count) throws Exception {
        List<CleanupExecutionPlan.Proof> proofs = new ArrayList<>();
        for (int n=1;n<=count;n++) proofs.add(fixtures.proof(n));
        CleanupExecutionJournal.Session s = new CleanupExecutionJournal.Session(new CleanupExecutionPlan(proofs,300));
        s.state="AUDIT_PENDING"; return s;
    }
    private CleanupExecutionSafetyTest.Access access(CleanupExecutionJournal.Session s,int trashed) {
        CleanupExecutionSafetyTest.Access a = new CleanupExecutionSafetyTest.Access();
        for (int i=0;i<s.plan.proofs.size();i++) {
            CleanupExecutionSafetyTest.Access b=fixtures.access(s.plan.proofs.get(i),i<trashed);
            a.bytes.putAll(b.bytes);a.keys.putAll(b.keys);a.trashed.addAll(b.trashed);
        }
        return a;
    }
    @Test public void repeatedAuditOfFirstBatchCannotAdvanceAnotherThreeHundred() throws Exception {
        CleanupExecutionJournal.Session s=session(301);
        CleanupExecutionSafetyTest.Access a=access(s,300);
        CleanupSessionAudit.inspect(s,a,proofs->{});
        assertEquals(300,s.offset);assertEquals("PAUSED",s.state);
        CleanupSessionAudit.inspect(s,a,proofs->{fail("Paused work must not inspect another batch");});
        assertEquals(300,s.offset);assertEquals("PAUSED",s.state);
        s.state="AUDIT_PENDING";a.trashed.add(s.plan.proofs.get(300).record.sourceUri);
        CleanupSessionAudit.inspect(s,a,proofs->{});
        assertEquals(301,s.offset);assertEquals("DONE",s.state);
        CleanupSessionAudit.inspect(s,a,proofs->{fail("Completed work is idempotent");});
        assertEquals(301,s.offset);
    }
    @Test public void canceledNextBatchRetainsPreviouslyCompletedCount() throws Exception {
        CleanupExecutionJournal.Session s=session(301);s.offset=300;
        CleanupSessionAudit.inspect(s,access(s,300),proofs->{});
        assertEquals("PAUSED",s.state);assertEquals(300,s.offset);
        CleanupSessionAudit.inspect(s,access(s,300),proofs->{fail("Paused batch must await explicit continuation");});
        assertEquals(300,s.offset);
    }
    @Test public void partialOsMoveNeverAdvancesCursor() throws Exception {
        CleanupExecutionJournal.Session s=session(301);
        CleanupSessionAudit.inspect(s,access(s,130),proofs->{fail();});
        assertEquals("NEEDS_RECOVERY",s.state);assertEquals(0,s.offset);assertEquals("PARTIAL_TRASH_STATE",s.reason);
    }
    @Test public void lostPreviouslyCompletedCopyStopsRemainingBatch() throws Exception {
        CleanupExecutionJournal.Session s=session(301);s.offset=300;
        CleanupExecutionSafetyTest.Access a=access(s,301);
        a.bytes.remove(s.plan.proofs.get(0).record.targetUri);
        CleanupSessionAudit.inspect(s,a,proofs->{fail();});
        assertEquals("NEEDS_RECOVERY",s.state);assertEquals(300,s.offset);
    }
    @Test public void dependencyFailureCannotBecomeCompleted() throws Exception {
        CleanupExecutionJournal.Session s=session(1);
        CleanupSessionAudit.inspect(s,access(s,1),proofs->{throw new IllegalStateException("changed note");});
        assertEquals("NEEDS_RECOVERY",s.state);assertEquals(0,s.offset);assertEquals("MEMORY_PROJECTION_CHANGED",s.reason);
    }
    @Test public void restorationAdvancesOnlyVerifiedBatchAndCanResume() throws Exception {
        CleanupExecutionJournal.Session s=session(301);s.offset=300;s.restoring=true;
        CleanupExecutionSafetyTest.Access a=access(s,301);
        for(int i=0;i<300;i++)a.trashed.remove(s.plan.proofs.get(i).record.sourceUri);
        CleanupSessionAudit.inspect(s,a,proofs->{fail();});
        assertEquals(300,s.restoreOffset);assertEquals("NEEDS_RECOVERY",s.state);
        s.state="AUDIT_PENDING";a.trashed.clear();
        CleanupSessionAudit.inspect(s,a,proofs->{fail();});
        assertEquals(301,s.restoreOffset);assertEquals("CANCELED",s.state);
    }
    @Test public void externallyRestoredOriginalsResolveRecoveryWithoutNeedingCopies() throws Exception {
        CleanupExecutionJournal.Session s=session(1);s.state="NEEDS_RECOVERY";
        CleanupExecutionSafetyTest.Access a=access(s,0);a.bytes.remove(s.plan.proofs.get(0).record.targetUri);
        CleanupSessionAudit.inspect(s,a,proofs->{fail();});
        assertEquals("CANCELED",s.state);
    }
    @Test public void lookingUpARestorationBatchCannotMutateSavedCursor() throws Exception {
        CleanupExecutionJournal.Session s=session(301);s.offset=300;s.restoreOffset=301;s.restoring=true;
        assertEquals(300,CleanupSessionAudit.restorationBatch(s).size());
        assertEquals(301,s.restoreOffset);
        CleanupSessionAudit.inspect(s,access(s,301),proofs->{fail();});
        assertEquals(0,s.restoreOffset);assertEquals("NEEDS_RECOVERY",s.state);
    }
    @Test public void explicitContinuationUpgradesOnlyRemainingBatchAndRotatesIdentity() throws Exception {
        CleanupExecutionJournal.Session old=session(2608);old.offset=600;old.state="PAUSED";
        CleanupExecutionJournal.Session next=old.forContinuation();
        assertEquals(300,old.plan.batchSize);assertEquals(1000,next.plan.batchSize);
        assertEquals(600,next.offset);assertEquals(1000,next.plan.batchCount(next.offset));
        assertNotEquals(old.id,next.id);assertEquals(old.plan.sources,next.plan.sources);
        CleanupSessionAudit.inspect(next,access(next,1600),proofs->{});
        assertEquals(600,next.offset); // PAUSED never applies work automatically.
        next.state="AUDIT_PENDING";CleanupSessionAudit.inspect(next,access(next,1600),proofs->{});
        assertEquals(1600,next.offset);assertEquals("PAUSED",next.state);
    }
    @Test public void pendingAndRecoveryBatchesCannotBeRepartitioned() throws Exception {
        for(String state:new String[]{"REQUESTED","AUDIT_PENDING","NEEDS_RECOVERY","DONE","CANCELED"}) {
            CleanupExecutionJournal.Session s=session(301);s.state=state;
            try{s.forContinuation();fail(state);}catch(IllegalStateException expected){}
            assertEquals(300,s.plan.batchSize);
        }
    }
    @Test public void newPlansUseThousandAndKeepFinalRemainder() throws Exception {
        CleanupExecutionPlan plan=new CleanupExecutionPlan(session(2008).plan.proofs);
        assertEquals(1000,plan.batchCount(0));assertEquals(1000,plan.batchCount(1000));assertEquals(8,plan.batchCount(2000));
        CleanupExecutionJournal.Session s=new CleanupExecutionJournal.Session(plan);s.offset=1000;s.state="AUDIT_PENDING";
        CleanupSessionAudit.inspect(s,access(s,2000),proofs->{});
        assertEquals(2000,s.offset);assertEquals("PAUSED",s.state);
        s.state="AUDIT_PENDING";CleanupSessionAudit.inspect(s,access(s,2008),proofs->{});
        assertEquals(2008,s.offset);assertEquals("DONE",s.state);
    }
    @Test public void legacyJournalBatchSizeIsNeverInferredFromNewDefault() throws Exception {
        assertEquals(300,CleanupExecutionJournal.storedBatchSize(new org.json.JSONObject()));
        assertEquals(1000,CleanupExecutionJournal.storedBatchSize(new org.json.JSONObject().put("batchSize",1000)));
        for(Object invalid:new Object[]{"1000",0,2001,4294968296L,1000.5}) {
            try{CleanupExecutionJournal.storedBatchSize(new org.json.JSONObject().put("batchSize",invalid));fail();}
            catch(IllegalStateException expected){}
        }
    }
}
