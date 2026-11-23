package com.example.gallerysorter;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Owns execution and process-death recovery. Activity only forwards lifecycle and results. */
final class CleanupExecutionController {
    private static final CleanupExecutionController INSTANCE = new CleanupExecutionController();
    static CleanupExecutionController get() { return INSTANCE; }
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile java.lang.ref.WeakReference<Activity> foreground = new java.lang.ref.WeakReference<>(null);
    private CleanupExecutionScreen currentScreen;
    private boolean auditing;
    private int watchGeneration;
    private boolean screenHidden;
    private String currentMessage = "선택한 사진을 다시 확인하고 있어요";
    void pause(Activity a) {
        if (foreground.get() != a) return;
        foreground.clear(); watchGeneration++;
        if (currentScreen != null) { currentMessage = currentScreen.message(); dismiss(currentScreen); currentScreen = null; }
        if (auditing) busy.set(false);
    }
    private boolean visible(Activity a) { return usable(a) && foreground.get() == a && !screenHidden; }

    static boolean usable(Activity a) { return a != null && !a.isFinishing() && !a.isDestroyed(); }
    static boolean active(Context c) throws Exception {
        CleanupExecutionJournal.Session s = new CleanupExecutionJournal(c).read();
        return s != null && s.active();
    }
    void start(Activity a, List<CleanupCandidate> selected, List<CleanupCandidate> all, Consumer<String> notice) {
        if (!CleanupExecutionPolicy.isEnabled() || !usable(a) || !busy.compareAndSet(false, true)) return;
        foreground = new java.lang.ref.WeakReference<>(a);
        screenHidden = false;
        Context c = a.getApplicationContext();
        CleanupExecutionScreen progress = progress(a, "선택한 사진을 다시 확인하고 있어요");
        worker.execute(() -> {
            try (MediaMutationCoordinator.Reservation reservation=MediaMutationCoordinator.reserve(c,"cleanup")) {
                if (active(c)) throw new IllegalStateException("이전 휴지통 작업 확인을 먼저 마쳐 주세요.");
                try (CleanupEvidenceStore ledger = new CleanupEvidenceStore(c)) {
                    CleanupEvidenceStore.JournalEntry legacy = ledger.readLatestJournal();
                    if (legacy != null && !"CANCELED".equals(legacy.status))
                        throw new IllegalStateException("이전 버전의 휴지통 요청이 있어 자동 실행을 중단했어요. 갤러리 휴지통을 확인해 주세요.");
                }
                CleanupExecutionPlan plan = CleanupExecutionPreflight.prepare(c, selected, all, () -> Thread.currentThread().isInterrupted(), (done,total) -> {
                    if (done % 5 == 0 || done == total) main.post(() -> {
                        currentMessage = done==total ? "사진 내용 확인 완료 · 기록 연결을 확인하고 있어요" : "선택한 사진 재확인 " + done + " / " + total;
                        if (visible(a) && currentScreen != null && currentScreen.isShowing()) currentScreen.setMessage(currentMessage);
                    });
                });
                CleanupExecutionJournal.Session s = new CleanupExecutionJournal.Session(plan);
                new CleanupExecutionJournal(c).save(s);
                prepare(a, s, progress, notice);
            } catch (Exception failed) { fail(a, progress, failed, notice); }
        });
    }
    private void prepare(Activity a, CleanupExecutionJournal.Session s, CleanupExecutionScreen progress, Consumer<String> notice) throws Exception {
        Context c = a.getApplicationContext();
        if (s.offset > 0) {
            publishProgress(a,"이미 정리한 위치 앨범의 복사본을 확인하고 있어요");
            List<CleanupExecutionPlan.Proof> completed = completed(s);
            if (CleanupExecutionAudit.audit(completed, new CleanupMediaAccess(c)) != CleanupExecutionAudit.Outcome.TRASHED) {
                s.state = "NEEDS_RECOVERY"; new CleanupExecutionJournal(c).save(s);
                throw new IllegalStateException("이전 묶음의 복사본 확인이 필요해 작업을 중단했어요. 원본을 복구해 주세요.");
            }
        }
        publishProgress(a,"시스템 승인 전 이번 묶음을 최종 확인하고 있어요");
        String revision = CleanupExecutionPreflight.verifyBatch(c,s.plan,s.offset,(done,total)->{
            if(done%5==0||done==total)publishProgress(a,done==total?"기록 연결 최종 확인 · 곧 시스템 확인창이 열려요":"시스템 승인 전 최종 확인 "+done+" / "+total);
        });
        main.post(() -> {
            dismiss(progress);
            try {
                if (!visible(a)) { busy.set(false); return; }
                if (revision == null || !revision.equals(CleanupExecutionPreflight.revision(c)))
                    throw new IllegalStateException("사진이나 화면이 바뀌었어요. 작업을 다시 열어 주세요.");
                synchronized(CleanupExecutionJournal.LOCK){
                    CleanupExecutionJournal.Session current = new CleanupExecutionJournal(c).read();
                    if (!same(s,current) || !("READY".equals(current.state) || "PAUSED".equals(current.state)))
                        throw new IllegalStateException("작업 상태가 바뀌었어요. 다시 확인해 주세요.");
                    s.state="REQUESTED";s.restoring=false;new CleanupExecutionJournal(c).save(s);
                }
                launch(a, s, false);
            } catch (Exception failed) { busy.set(false); if (notice != null) notice.accept(failed.getMessage()); }
        });
    }
    private void launch(Activity a, CleanupExecutionJournal.Session s, boolean restore) throws Exception {
        if (!usable(a) || foreground.get() != a) throw new IllegalStateException("원본 정리 화면을 다시 열어 주세요.");
        if (!CleanupExecutionPolicy.isEnabled()) throw new IllegalStateException("휴지통 이동이 꺼져 있어요.");
        if (!"REQUESTED".equals(s.state) || s.restoring != restore) throw new IllegalStateException("잘못된 휴지통 요청 상태");
        LinkedHashSet<Uri> uris = new LinkedHashSet<>();
        List<CleanupExecutionPlan.Proof> records = restore ? restorationBatch(s) : s.plan.batch(s.offset);
        for (CleanupExecutionPlan.Proof p : records) uris.add(Uri.parse(p.record.sourceUri));
        if (uris.isEmpty() || uris.size() > s.plan.batchSize)
            throw new IllegalStateException("잘못된 휴지통 요청 수량");
        // Restoration is also bounded; recovery offers each affected batch separately.
        if (uris.size() > 2000) throw new IllegalStateException("갤러리 휴지통에서 원본을 복구해 주세요.");
        a.startIntentSenderForResult(MediaStore.createTrashRequest(a.getContentResolver(), uris, !restore)
                .getIntentSender(), 21, null, 0, 0, 0, null);
    }
    void result(Activity a, int resultCode, Consumer<String> notice) {
        // Freeze immediately on the main thread, before any subsequent lifecycle callback.
        busy.set(true);
        audit(a, notice, usable(a) ? progress(a, "휴지통 상태와 복사본을 확인하고 있어요") : null);
    }
    void resume(Activity a, Consumer<String> notice) {
        if (usable(a)) foreground = new java.lang.ref.WeakReference<>(a);
        screenHidden = false;
        if (!usable(a)) return;
        if (!busy.compareAndSet(false, true)) {
            if (!auditing) progress(a, currentMessage);
            return;
        }
        audit(a, notice, null);
    }
    private void audit(Activity a, Consumer<String> notice, CleanupExecutionScreen auditProgress) {
        auditing = true;
        int generation = ++watchGeneration;
        Context c = a.getApplicationContext();
        worker.execute(() -> {
            try {
                CleanupExecutionJournal.Session s;
                synchronized (CleanupExecutionJournal.LOCK) {
                    CleanupExecutionJournal journal = new CleanupExecutionJournal(c);
                    s = journal.read();
                    if (s != null && "REQUESTED".equals(s.state)) { s.state="AUDIT_PENDING";journal.save(s); }
                }
                if (s == null || !s.active()) {
                    main.post(() -> { if(generation==watchGeneration){auditing=false;busy.set(false);dismiss(auditProgress);} });return;
                }
                if ("READY".equals(s.state) || "PAUSED".equals(s.state)) {
                    main.post(() -> { if(generation==watchGeneration){auditing=false;busy.set(false);dismiss(auditProgress);offerContinue(a,s,notice);} });return;
                }
                String name = CleanupAuditWorker.enqueue(c,s);
                main.post(() -> {
                    if(generation!=watchGeneration || !visible(a)) { busy.set(false);return; }
                    dismiss(auditProgress);
                    progress(a,s.offset + " / " + s.plan.sources.size() + "장 이동 확인 완료"
                            + "\n복사본 보존 확인 중 · 앱을 닫아도 검증을 이어가요.");
                    watchAudit(a,notice,name,generation);
                });
            } catch (Exception failed) { fail(a,auditProgress,failed,notice); }
        });
    }
    private void watchAudit(Activity a, Consumer<String> notice, String name, int generation) {
        if(generation!=watchGeneration || !visible(a)) return;
        worker.execute(() -> {
            try {
                List<androidx.work.WorkInfo> rows=androidx.work.WorkManager.getInstance(a.getApplicationContext())
                        .getWorkInfosForUniqueWork(name).get();
                boolean pending=rows.isEmpty();
                for(androidx.work.WorkInfo row:rows) if(!row.getState().isFinished()) pending=true;
                if(pending){
                    androidx.work.Data data=rows.isEmpty()?androidx.work.Data.EMPTY:rows.get(rows.size()-1).getProgress();
                    int checked=data.getInt("checked",0),total=data.getInt("total",0);
                    if(total>0)main.post(()->{if(generation==watchGeneration && visible(a) && currentScreen!=null)
                        currentScreen.setMessage("현재 묶음 복사본 확인 " + checked + " / " + total + "건 · 앱을 닫아도 이어가요.");});
                    main.postDelayed(()->watchAudit(a,notice,name,generation),750);return;
                }
                CleanupExecutionJournal.Session s;
                synchronized(CleanupExecutionJournal.LOCK){s=new CleanupExecutionJournal(a).read();}
                main.post(()->{
                    if(generation!=watchGeneration || !visible(a)) return;
                    auditing=false;busy.set(false);dismiss(currentScreen);currentScreen=null;
                    if(s==null)return;
                    if("READY".equals(s.state)||"PAUSED".equals(s.state))offerContinue(a,s,notice);
                    else if("NEEDS_RECOVERY".equals(s.state))recovery(a,s,notice);
                    else if("DONE".equals(s.state)||"CANCELED".equals(s.state)) {
                        currentScreen=new CleanupExecutionScreen(a,()->{});
                        currentScreen.render("DONE".equals(s.state)?"원본 정리가 완료됐어요":"이번 작업을 마쳤어요",
                            "DONE".equals(s.state)?"위치 앨범의 복사본 보존도 확인했어요.":"이동하지 않은 원본은 그대로 있어요. 이동한 원본은 갤러리 휴지통에서 확인할 수 있어요.",
                            s.offset,s.plan.sources.size(),"확인",()->{},null,null);currentScreen.show();
                    } else if(notice!=null)notice.accept("상태 확인을 마치지 못했어요. 원본 정리를 다시 열어 주세요.");
                });
            }catch(Exception e){fail(a,null,e,notice);}
        });
    }
    private void offerContinue(Activity a, CleanupExecutionJournal.Session s, Consumer<String> notice) {
        busy.set(false);
        if (!visible(a)) return;
        dismiss(currentScreen);
        currentScreen = new CleanupExecutionScreen.Builder(a).setTitle("원본 정리 진행")
                .setMessage(s.offset + " / " + s.plan.sources.size() + "장 완료\n다음 " + Math.min(CleanupExecutionPlan.BATCH_SIZE,s.plan.sources.size()-s.offset)
                        + "장을 다시 확인한 후 시스템 확인창을 열어요.")
                .setPositiveButton("남은 사진 계속 정리", (d,w) -> {
                    if (!busy.compareAndSet(false, true)) return;
                    CleanupExecutionScreen p = progress(a, "다음 묶음을 확인하고 있어요");
                    worker.execute(() -> { try {
                        CleanupExecutionJournal.Session current = new CleanupExecutionJournal(a).read();
                        if (!same(s,current) || !("READY".equals(current.state) || "PAUSED".equals(current.state)))
                            throw new IllegalStateException("작업 상태가 바뀌었어요. 다시 확인해 주세요.");
                        CleanupExecutionJournal.Session next=current.forContinuation();
                        new CleanupExecutionJournal(a).save(next);
                        prepare(a,next,p,notice);
                    } catch (Exception e) { fail(a, p, e, notice); } });
                }).setNegativeButton("나중에 계속", null).show();
    }
    private void recovery(Activity a, CleanupExecutionJournal.Session s, Consumer<String> notice) {
        busy.set(false);
        CleanupRecoveryNotifier.show(a.getApplicationContext());
        if (!visible(a)) return;
        dismiss(currentScreen);
        currentScreen = new CleanupExecutionScreen.Builder(a).setTitle("원본 복구가 필요해요")
                .setMessage(CleanupExecutionAudit.explanation(s.reason) + "\n남은 정리는 중단했어요. 원본 복구를 요청하거나 갤러리 휴지통에서 직접 복구해 주세요.")
                .setPositiveButton("원본 복구 요청", (d,w) -> {
                    if (!busy.compareAndSet(false, true)) return;
                    worker.execute(() -> { try {
                        CleanupMediaAccess access = new CleanupMediaAccess(a);
                        for (CleanupExecutionPlan.Proof p : restorationBatch(s))
                            if (!p.sourceKey.equals(access.key(p.record.sourceUri)))
                                throw new IllegalStateException("원본 식별자가 바뀌었어요. 갤러리 휴지통에서 직접 확인해 주세요.");
                        main.post(() -> { try {
                            if (!visible(a)) { busy.set(false); return; }
                            synchronized(CleanupExecutionJournal.LOCK){
                                CleanupExecutionJournal.Session current = new CleanupExecutionJournal(a).read();
                                if(!same(s,current) || !"NEEDS_RECOVERY".equals(current.state))
                                    throw new IllegalStateException("복구 상태를 다시 확인해 주세요.");
                                if(s.restoreOffset>=CleanupSessionAudit.affectedCount(s))s.restoreOffset=0;
                                s.restoring=true;s.state="REQUESTED";new CleanupExecutionJournal(a).save(s);
                            }
                            launch(a,s,true);
                        } catch (Exception e) { fail(a,null,e,notice); } });
                    } catch (Exception e) { fail(a, null, e, notice); } });
                }).setNeutralButton("다시 확인", (d,w) -> {
                    if (!busy.compareAndSet(false, true)) return;
                    CleanupExecutionScreen p = progress(a, "보존 상태를 다시 확인하고 있어요");
                    worker.execute(() -> { try {
                        synchronized(CleanupExecutionJournal.LOCK){
                            CleanupExecutionJournal.Session current = new CleanupExecutionJournal(a).read();
                            if(!same(s,current) || !"NEEDS_RECOVERY".equals(current.state))
                                throw new IllegalStateException("작업 상태를 다시 확인해 주세요.");
                            s.restoring=false;s.state="AUDIT_PENDING";new CleanupExecutionJournal(a).save(s);
                        }
                        main.post(() -> audit(a, notice, p));
                    } catch (Exception e) { fail(a, p, e, notice); } });
                }).setNegativeButton("나중에 확인", null).show();
    }
    private static List<CleanupExecutionPlan.Proof> completed(CleanupExecutionJournal.Session s) {
        return CleanupSessionAudit.completed(s);
    }
    private static List<CleanupExecutionPlan.Proof> restorationBatch(CleanupExecutionJournal.Session s) {
        return CleanupSessionAudit.restorationBatch(s);
    }
    private CleanupExecutionScreen progress(Activity a, String message) {
        currentMessage=message;
        if(!visible(a))return null;
        try {
            if(currentScreen!=null && currentScreen.isShowing()) {
                currentScreen.setMessage(message);return currentScreen;
            }
            dismiss(currentScreen);
            CleanupExecutionScreen p=new CleanupExecutionScreen(a,()->{
                screenHidden=true;watchGeneration++;if(auditing)busy.set(false);
            });
            p.setMessage(message);p.show();currentScreen=p;return p;
        }catch(RuntimeException unavailable){return null;}
    }
    private void publishProgress(Activity a,String message) {
        main.post(()->{currentMessage=message;if(visible(a)&&currentScreen!=null&&currentScreen.isShowing())currentScreen.setMessage(message);});
    }
    private static boolean same(CleanupExecutionJournal.Session a, CleanupExecutionJournal.Session b) {
        return b != null && a.id.equals(b.id) && a.offset == b.offset && a.restoreOffset == b.restoreOffset;
    }
    private static void dismiss(CleanupExecutionScreen p) { try { if (p != null) p.dismiss(); } catch (RuntimeException ignored) { } }
    private void fail(Activity a, CleanupExecutionScreen p, Exception e, Consumer<String> notice) {
        android.util.Log.w("CleanupExecution", "Cleanup stopped; durable request retained", e);
        String detail = e.getMessage();
        String message = detail != null && detail.matches("(?s).*[가-힣].*") ? detail
                : "사진이나 정리 기록을 확인하지 못했어요. 원본 정리를 다시 열어 상태를 확인해 주세요.";
        main.post(() -> { dismiss(p); auditing=false;busy.set(false); if (visible(a) && notice != null) notice.accept("원본 정리를 중단했어요. " + message); });
    }
}
