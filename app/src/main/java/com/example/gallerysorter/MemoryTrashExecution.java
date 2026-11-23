package com.example.gallerysorter;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.MediaStore;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.lifecycle.Observer;
import androidx.work.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Intentional selected-file trash, with no dependency on duplicate-copy cleanup policy. */
final class MemoryTrashExecution {
    static final int REQUEST=7412;
    private final Activity activity;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Consumer<String> notice;
    private final Runnable backToList;
    private androidx.lifecycle.LiveData<WorkInfo> observed;
    private Observer<WorkInfo> observer;
    private volatile boolean foreground,closed;
    private volatile boolean busy;
    private LinearLayout shownRoot;
    private String shownKey="";
    private String returnedSession="";
    private TextView shownCount,shownMessage;
    private ProgressBar shownProgress;

    MemoryTrashExecution(Activity a,Runnable backToList,Consumer<String> notice){activity=a;this.backToList=backToList;this.notice=notice;}
    volatile boolean showing;
    void resume(boolean includeFinished,Runnable noActive) {
        foreground=true;
        if(Build.VERSION.SDK_INT<30){noActive.run();return;}
        io.execute(()->{
            try {
                MemoryTrashJournal.Session s=new MemoryTrashJournal(activity).read();
                if(s==null||(!s.active()&&!includeFinished)||(s.state.equals("PAUSED")&&s.offset==0&&s.moved==0&&!includeFinished&&(s.message==null||s.message.isEmpty()))){main.post(()->{if(usable())noActive.run();});return;}
                showing=true;
                if(s.state.equals("REQUESTED")){s.state="AUDITING";new MemoryTrashJournal(activity).save(s);}
                if(s.state.equals("AUDITING"))MemoryTrashAuditWorker.enqueue(activity,s.id);
                displayCurrent();
            }catch(Exception e){error(e);}
        });
    }
    void pause(){foreground=false;detach();}
    void close(){closed=true;pause();io.shutdownNow();}
    private boolean usable(){return !closed&&!activity.isFinishing()&&!activity.isDestroyed();}
    private void error(Exception error){busy=false;main.post(()->{if(usable())notice.accept(error.getMessage()==null?"사진 정리 상태를 확인하지 못했어요.":error.getMessage());});}

    void start(List<UnresolvedMediaRecord> selected) {
        start(selected,MemoryManagementRoute.of(MemoryManagementRoute.SINGLE,"","","사진 정리"));
    }
    void start(List<UnresolvedMediaRecord> selected,MemoryManagementRoute route) {
        if(Build.VERSION.SDK_INT<30){notice.accept("휴지통 이동은 Android 11 이상에서 지원해요.");return;}
        if(busy)return;busy=true;showing=true;
        io.execute(()->{
            try(MediaMutationCoordinator.Reservation reservation=MediaMutationCoordinator.reserve(activity,"trash")) {
                MemoryTrashJournal journal=new MemoryTrashJournal(activity);MemoryTrashJournal.Session old=journal.read();
                if(old!=null&&old.active()&&old.state.equals("PAUSED")&&old.offset==0&&old.moved==0&&(old.message==null||old.message.isEmpty())) {
                    old.state="CANCELED";journal.save(old);old=null;
                }
                if(old!=null&&old.active())throw new IllegalStateException("이전 사진 정리를 먼저 마쳐 주세요.");
                List<UnresolvedMediaRecord> fresh=new MemoryMediaAccess(activity).requireFresh(selected);
                verifyObservations(selected,fresh);
                MemoryManagementRoute.save(activity,route);
                journal.create(new MemoryTrashJournal.Session(fresh));
                busy=false;main.post(()->{if(usable())prepare();});
            }catch(Exception error){error(error);showing=false;}
        });
    }
    static void verifyObservations(List<UnresolvedMediaRecord> expected,List<UnresolvedMediaRecord> fresh) {
        Map<String,UnresolvedMediaRecord> current=new HashMap<>();for(UnresolvedMediaRecord r:fresh)current.put(r.assetKey(),r);
        for(UnresolvedMediaRecord old:expected) {
            UnresolvedMediaRecord now=current.get(old.assetKey());
            if(now==null||old.size!=now.size||old.modified!=now.modified||old.takenAt!=now.takenAt
                    ||!old.name.equals(now.name)||!old.relativePath.equals(now.relativePath))
                throw new IllegalStateException("선택한 사진 상태가 바뀌었어요. 목록을 다시 불러와 주세요.");
        }
    }
    private void prepare() {
        if(Build.VERSION.SDK_INT<30){notice.accept("휴지통 이동은 Android 11 이상에서 지원해요.");return;}
        if(busy)return;busy=true;detach();
        if(shownRoot!=null) {
            shownMessage.setText("시스템 확인 전에 이번 묶음의 파일을 다시 확인하고 있어요.");
            shownProgress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PURPLE));
            shownProgress.setIndeterminate(true);
            for(int i=0;i<shownRoot.getChildCount();i++)if(shownRoot.getChildAt(i) instanceof Button)shownRoot.getChildAt(i).setEnabled(false);
        }
        io.execute(()->{
            try {
                MemoryTrashJournal journal=new MemoryTrashJournal(activity);MemoryTrashJournal.Session s=journal.read();
                if(s==null||!(s.state.equals("READY")||s.state.equals("PAUSED")))throw new IllegalStateException("작업 상태를 다시 확인해 주세요.");
                List<UnresolvedMediaRecord> fresh=new MemoryMediaAccess(activity).requireFresh(s.batch());
                verifyObservations(s.batch(),fresh);
                synchronized(MemoryTrashJournal.LOCK) {
                    MemoryTrashJournal.Session latest=journal.read();
                    if(!latest.id.equals(s.id)||latest.offset!=s.offset||!latest.state.equals(s.state))throw new IllegalStateException("작업이 바뀌었어요.");
                    s.state="REQUESTED";s.audited=s.batchMoved=s.batchSkipped=s.batchUnknown=0;s.workId="";journal.save(s);
                }
                main.post(()->{
                    if(!usable()||!foreground) {
                        io.execute(()->{try{MemoryTrashJournal.Session current=journal.read();if(current!=null&&current.id.equals(s.id)&&current.state.equals("REQUESTED")){current.state="READY";journal.save(current);}}catch(Exception e){error(e);}finally{busy=false;}});
                        return;
                    }
                    try {
                        InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if(activity.getCurrentFocus()!=null)keyboard.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),0);
                        List<Uri> uris=new ArrayList<>();for(UnresolvedMediaRecord row:s.batch())uris.add(Uri.parse(row.uri));
                        activity.startIntentSenderForResult(MediaStore.createTrashRequest(activity.getContentResolver(),uris,true)
                                .getIntentSender(),REQUEST,null,0,0,0);
                        busy=false;
                    }catch(Exception e){busy=false;io.execute(()->{try{MemoryTrashJournal.Session current=journal.read();if(current!=null&&current.id.equals(s.id)&&current.state.equals("REQUESTED")){current.state="PAUSED";current.message="시스템 확인창을 열지 못했어요. 다시 시도해 주세요.";journal.save(current);}displayCurrent();}catch(Exception failed){error(failed);}});error(e);}
                });
            }catch(Exception e){error(e);displayCurrentSafely();}
        });
    }
    void result(int requestCode) {
        if(requestCode!=REQUEST)return;
        showing=true;busy=true;
        io.execute(()->{
            try {
                MemoryTrashJournal journal=new MemoryTrashJournal(activity);MemoryTrashJournal.Session s=journal.read();
                if(s!=null&&s.active()) {
                    if(s.state.equals("REQUESTED")||s.state.equals("PAUSED")){s.state="AUDITING";s.workId="";journal.save(s);}
                    if(s.state.equals("AUDITING"))MemoryTrashAuditWorker.enqueue(activity,s.id);
                }
                busy=false;displayCurrent();
            }catch(Exception e){error(e);}
        });
    }
    private void displayCurrentSafely(){try{displayCurrent();}catch(Exception e){error(e);}}
    private void displayCurrent()throws Exception {
        MemoryTrashJournal.Session s=new MemoryTrashJournal(activity).read();
        if(s!=null)main.post(()->{if(usable()&&foreground){show(s);if(s.state.equals("AUDITING"))observe(s);}});
    }
    private void detach(){if(observed!=null&&observer!=null)observed.removeObserver(observer);observed=null;observer=null;}
    private void observe(MemoryTrashJournal.Session s) {
        detach();if(s.workId.isEmpty())return;
        observed=WorkManager.getInstance(activity).getWorkInfoByIdLiveData(UUID.fromString(s.workId));
        observer=info->{if(info!=null&&usable()&&foreground)io.execute(()->{
            try {
                MemoryTrashJournal.Session current=new MemoryTrashJournal(activity).read();
                if(current==null||!current.id.equals(s.id)||!current.workId.equals(info.getId().toString()))return;
                if(info.getState().isFinished()&&current.state.equals("AUDITING")) {
                    main.post(()->{if(usable()&&foreground)showRetry(current);});
                } else main.post(()->{if(usable()&&foreground)show(current);});
            }catch(Exception e){error(e);}
        });};
        observed.observeForever(observer);
    }
    private void action(String state) {
        io.execute(()->{
            try {
                MemoryTrashJournal journal=new MemoryTrashJournal(activity);
                synchronized(MemoryTrashJournal.LOCK) {
                    MemoryTrashJournal.Session s=journal.read();
                    if(s==null||s.state.equals("AUDITING")||s.state.equals("REQUESTED"))return;
                    s.state=state;
                    if(state.equals("AUDITING")){s.audited=s.batchMoved=s.batchSkipped=s.batchUnknown=0;s.workId="";}
                    journal.save(s);
                    if(state.equals("AUDITING"))MemoryTrashAuditWorker.enqueue(activity,s.id);
                }
                displayCurrent();
            }catch(Exception e){error(e);}
        });
    }
    private LinearLayout page(String title) {
        LinearLayout root=new LinearLayout(activity);root.setOrientation(1);int p=CleanupUiStyle.dp(activity,18);
        root.setPadding(p,p*2,p,p);root.setBackground(MemorySelectionStyle.background());
        MediaScreenInsets.bind(root);
        root.addView(CleanupUiStyle.header(activity,title,activity::finish));activity.setContentView(root);return root;
    }
    private void button(LinearLayout root,String label,boolean primary,Runnable action) {
        Button b=MemorySelectionStyle.action(activity,label,primary);b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=CleanupUiStyle.dp(activity,12);root.addView(b,lp);
    }
    private void show(MemoryTrashJournal.Session s) {
        if(s.state.equals("DONE")) {
            if(!s.id.equals(returnedSession)) {
                returnedSession=s.id;detach();showing=false;shownRoot=null;shownKey="";
                if(s.moved>0)Toast.makeText(activity,s.moved+"개를 휴지통으로 이동했어요."+(s.skipped>0?" · "+s.skipped+"개 이동 안 됨":""),Toast.LENGTH_SHORT).show();
                backToList.run();
            }
            return;
        }
        if(s.state.equals("PAUSED")&&s.offset==0&&s.moved==0&&s.rows.size()<=1000&&(s.message==null||s.message.isEmpty())) {
            detach();showing=false;shownRoot=null;shownKey="";
            backToList.run();
            return;
        }
        if(s.state.equals("AUDITING")&&s.rows.size()==1&&shownRoot==null) {
            return;
        }
        String key=s.id+"|"+s.state+"|"+s.offset;
        if(key.equals(shownKey)&&shownRoot!=null&&s.state.equals("AUDITING")) {
            shownCount.setText(s.moved+"개 이동 · "+s.skipped+"개 이동 안 됨");
            shownMessage.setText("기기 휴지통의 실제 이동 결과를 확인하고 있어요. "+s.audited+" / "+s.batch().size());
            shownProgress.setProgress(s.offset+s.audited);return;
        }
        shownKey=key;
        LinearLayout root=page("사진 휴지통 이동");
        shownRoot=root;
        shownCount=CleanupUiStyle.text(activity,s.moved+"개 이동 · "+s.skipped+"개 이동 안 됨",20,true,CleanupUiStyle.PURPLE);
        root.addView(shownCount);
        root.addView(CleanupUiStyle.text(activity,"선택한 사진·동영상 "+s.rows.size()+"개",14,false,CleanupUiStyle.MUTED));
        ProgressBar progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(s.rows.size());progress.setProgress(s.offset+(s.state.equals("AUDITING")?s.audited:0));
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PURPLE));
        root.addView(progress,new LinearLayout.LayoutParams(-1,CleanupUiStyle.dp(activity,20)));
        shownProgress=progress;
        shownMessage=CleanupUiStyle.text(activity,s.state.equals("AUDITING")?"기기 휴지통의 실제 이동 결과를 확인하고 있어요.":s.state.equals("DONE")?"선택한 파일의 이동 결과를 모두 확인했어요.":s.message,15,false,CleanupUiStyle.INK);
        root.addView(shownMessage);
        if(s.state.equals("READY")||s.state.equals("PAUSED")) {
            button(root,"다음 "+s.batch().size()+"개 휴지통 이동",true,this::prepare);
            button(root,"여기서 중단",false,()->action("CANCELED"));
        } else if(s.state.equals("NEEDS_ATTENTION")) {
            button(root,"사진 상태 다시 확인",true,()->action("AUDITING"));
            button(root,"여기서 중단",false,()->action("CANCELED"));
        } else if(!s.active())button(root,"사진 목록으로",true,()->{detach();showing=false;backToList.run();});
    }
    private void showRetry(MemoryTrashJournal.Session s) {
        shownKey="";show(s);
        shownMessage.setText("사진 상태 확인이 중단됐어요. 이어서 확인할 수 있어요.");
        button(shownRoot,"확인 이어하기",true,()->resume(false,()->{}));
        // Resume enqueues a new request only after the previous WorkInfo is terminal.
    }
}
