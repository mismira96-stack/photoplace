package com.example.gallerysorter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupExecutionSafetyTest {
    static final byte[] DATA = {1,2,3,4};
    static final class Access implements CleanupExecutionAudit.Access {
        Map<String,byte[]> bytes = new HashMap<>();
        Map<String,String> keys = new HashMap<>();
        Set<String> trashed = new HashSet<>();
        String identity = "identity";
        boolean changeDuringRead;
        public String key(String uri) { return keys.getOrDefault(uri, uri); }
        public boolean trashed(String uri) { return trashed.contains(uri); }
        public InputStream open(String uri) {
            if (changeDuringRead) identity = "changed";
            return new ByteArrayInputStream(bytes.get(uri));
        }
        public CleanupPairVerifier.Metadata metadata(String uri) {
            byte[] b = bytes.get(uri);
            return b == null ? null : new CleanupPairVerifier.Metadata(b.length, identity, "Pictures/Test/", !trashed.contains(uri));
        }
    }
    CleanupExecutionPlan.Proof proof(int n) throws Exception {
        CleanupEvidenceRecord r = new CleanupEvidenceRecord("job", "content://media/external/images/media/"+n,
                "content://media/external/images/media/"+(n+10000), "Pictures/Test/", 1, DATA.length, 1);
        return new CleanupExecutionPlan.Proof(r, CleanupPairVerifier.hex(MessageDigest.getInstance("SHA-256").digest(DATA)),
                "key:"+n, "key:"+(n+10000), "identity", "identity");
    }
    Access access(CleanupExecutionPlan.Proof p, boolean trash) {
        Access a = new Access();
        a.bytes.put(p.record.sourceUri, DATA.clone()); a.bytes.put(p.record.targetUri, DATA.clone());
        a.keys.put(p.record.sourceUri,p.sourceKey); a.keys.put(p.record.targetUri,p.targetKey);
        if (trash) a.trashed.add(p.record.sourceUri);
        return a;
    }
    @Test public void trashSuccessRequiresActualSourceStateAndExpectedTargetBytes() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1); Access a=access(p,true);
        assertEquals(CleanupExecutionAudit.Outcome.TRASHED,CleanupExecutionAudit.audit(Arrays.asList(p),a));
        a.trashed.clear();
        assertEquals(CleanupExecutionAudit.Outcome.NOT_APPLIED,CleanupExecutionAudit.audit(Arrays.asList(p),a));
    }
    @Test public void sameSizedCorruptionIsNotAValidSurvivingCopy() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1); Access a=access(p,true);
        a.bytes.put(p.record.targetUri,new byte[]{4,3,2,1});
        assertEquals(CleanupExecutionAudit.Outcome.NEEDS_RECOVERY,CleanupExecutionAudit.audit(Arrays.asList(p),a));
    }
    @Test public void missingAndTruncatedTargetsRequireRecovery() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1); Access a=access(p,true);
        a.bytes.remove(p.record.targetUri);
        assertEquals(CleanupExecutionAudit.Outcome.NEEDS_RECOVERY,CleanupExecutionAudit.audit(Arrays.asList(p),a));
        a.bytes.put(p.record.targetUri,new byte[]{1});
        assertEquals(CleanupExecutionAudit.Outcome.NEEDS_RECOVERY,CleanupExecutionAudit.audit(Arrays.asList(p),a));
    }
    @Test public void changedIdentityDuringAuditCannotPass() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1); Access a=access(p,true); a.changeDuringRead=true;
        assertEquals(CleanupExecutionAudit.Outcome.NEEDS_RECOVERY,CleanupExecutionAudit.audit(Arrays.asList(p),a));
    }
    @Test public void providerResetCannotReuseAnOldMediaId() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1); Access a=access(p,true); a.keys.put(p.record.sourceUri,"reset:key:1");
        assertEquals(CleanupExecutionAudit.Outcome.NEEDS_RECOVERY,CleanupExecutionAudit.audit(Arrays.asList(p),a));
    }
    @Test public void mixedSourceTrashStateIsNeverReportedAsSuccessOrCancellation() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1), q=proof(2); Access a=access(p,true), b=access(q,false);
        a.bytes.putAll(b.bytes);a.keys.putAll(b.keys);
        assertEquals(CleanupExecutionAudit.Outcome.NEEDS_RECOVERY,CleanupExecutionAudit.audit(Arrays.asList(p,q),a));
    }
    @Test public void emptyAuditFailsClosed() {
        assertEquals(CleanupExecutionAudit.Outcome.NEEDS_RECOVERY,CleanupExecutionAudit.audit(new ArrayList<>(),new Access()));
    }
    @Test public void selectedTargetCannotBeAnotherSourceEvenUnderAnotherUriAlias() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1), q=proof(2);
        q=new CleanupExecutionPlan.Proof(q.record,q.hash,q.sourceKey,p.sourceKey,q.sourceIdentity,q.targetIdentity);
        try {new CleanupExecutionPlan(Arrays.asList(p,q));fail();}catch(IllegalArgumentException expected) { }
    }
    @Test public void threeThousandAndOneSourcesAreRetainedAcrossElevenBatches() throws Exception {
        List<CleanupExecutionPlan.Proof> rows=new ArrayList<>(); for(int n=1;n<=3001;n++)rows.add(proof(n));
        CleanupExecutionPlan plan=new CleanupExecutionPlan(rows,300); Set<String> visited=new HashSet<>();int batches=0;
        for(int offset=0;offset<plan.sources.size();offset+=plan.batchCount(offset)) {
            for(CleanupExecutionPlan.Proof p:plan.batch(offset))assertTrue(visited.add(p.record.sourceUri));batches++;
        }
        assertEquals(3001,visited.size());assertEquals(11,batches);
    }
    @Test public void incompleteOrForgedDigestCannotEnterAnExecutionPlan() throws Exception {
        CleanupExecutionPlan.Proof p=proof(1);
        try {new CleanupExecutionPlan(Arrays.asList(new CleanupExecutionPlan.Proof(p.record,"",p.sourceKey,p.targetKey,
                p.sourceIdentity,p.targetIdentity)));fail();}catch(IllegalArgumentException expected) { }
    }
}
