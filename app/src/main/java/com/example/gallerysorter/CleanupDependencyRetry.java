package com.example.gallerysorter;

/** Only an unstable revision is retryable. Broken dependencies and permissions fail closed. */
final class CleanupDependencyRetry {
    static final class RevisionChanged extends Exception {
        RevisionChanged() { super("확인 중 사진이나 기록 상태가 바뀌었어요."); }
    }
    interface Check { void run() throws Exception; }
    interface Wait { void sleep(long millis) throws InterruptedException; }
    static void verify(Check check,java.util.function.BooleanSupplier stopped,Wait wait) throws Exception {
        for(int attempt=0;attempt<6;attempt++) {
            if(stopped.getAsBoolean())throw new InterruptedException();
            try {check.run();return;}
            catch(RevisionChanged changed) {if(attempt==5)throw changed;wait.sleep(500);}
        }
    }
    static boolean retryWorker(int attempts) { return attempts<3; }
}
