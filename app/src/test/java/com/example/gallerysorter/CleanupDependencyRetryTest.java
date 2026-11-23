package com.example.gallerysorter;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupDependencyRetryTest {
    @Test public void transientRevisionRetriesWithBoundedWait() throws Exception {
        AtomicInteger calls=new AtomicInteger(),waits=new AtomicInteger();
        CleanupDependencyRetry.verify(()->{if(calls.incrementAndGet()<6)throw new CleanupDependencyRetry.RevisionChanged();},()->false,
                millis->{assertEquals(500,millis);waits.incrementAndGet();});
        assertEquals(6,calls.get());assertEquals(5,waits.get());
    }
    @Test public void unstableRevisionNeverBecomesSuccessAfterExhaustion() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        try {CleanupDependencyRetry.verify(()->{calls.incrementAndGet();throw new CleanupDependencyRetry.RevisionChanged();},()->false,millis->{});fail();}
        catch(CleanupDependencyRetry.RevisionChanged expected){assertEquals(6,calls.get());}
    }
    @Test public void permanentFailureIsNotRetried() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        try {CleanupDependencyRetry.verify(()->{calls.incrementAndGet();throw new IllegalStateException("broken link");},()->false,millis->fail());fail();}
        catch(IllegalStateException expected){assertEquals(1,calls.get());}
    }
    @Test public void stoppedWorkDoesNotStartAnotherCheck() throws Exception {
        try {CleanupDependencyRetry.verify(()->fail(),()->true,millis->fail());fail();}
        catch(InterruptedException expected){}
        assertTrue(CleanupDependencyRetry.retryWorker(2));assertFalse(CleanupDependencyRetry.retryWorker(3));
    }
}
