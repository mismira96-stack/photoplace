package com.example.gallerysorter;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Virtualized selection UI; all MediaStore/SQLite work runs off the UI thread. */
final class MemoryManagementController {
    private final Activity activity;
    private final ExecutorService io=Executors.newSingleThreadExecutor(),thumbnails=new ThreadPoolExecutor(
            2,2,0L,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>(48),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<String> selected=new LinkedHashSet<>();
    private final LruCache<String,Bitmap> images=new LruCache<String,Bitmap>(12*1024*1024){protected int sizeOf(String key,Bitmap b){return b.getByteCount();}};
    private final MemoryTrashExecution trash;
    private List<UnresolvedMediaRecord> rows=Collections.emptyList();
    private List<MemoryPlaceChoice> placeChoices=Collections.emptyList();
    private List<String> recentChoices=Collections.emptyList();
    private MemoryPlacePicker placePicker;
    private Dialog confirmation;
    private MemoryManagementRoute route=MemoryManagementRoute.of(MemoryManagementRoute.ADD,"","","");
    private Set<String> eligiblePlaces=Collections.emptySet(),assigned=Collections.emptySet();
    private String key="",single="",title="위치 없는 사진";
    private long revision;
    private int generation;
    private boolean closed,busy,finishedHistory,storySelection;
    private LinearLayout root;
    private TextView status,total;
    private ProgressBar progress;
    private Button place,sendTrash,all,instagram;
    private RowAdapter adapter;
    private List<String> directTrashUris;
    private boolean directTrashStarted;

    MemoryManagementController(Activity a) {
        activity=a;trash=new MemoryTrashExecution(a,this::afterTrash,this::message);
    }
    void start(Intent intent, boolean restored){
        directTrashUris = intent.getStringArrayListExtra(MemoryManagementEntry.SELECTED_TRASH);
        directTrashStarted = restored;
        storySelection=intent.getBooleanExtra("story_selection",false)
                &&MemoryManagementRoute.RECORD.equals(MemoryManagementRoute.from(intent).mode);
        finishedHistory=!storySelection&&intent.getBooleanExtra("trash_resume",false);
        applyRoute(finishedHistory?MemoryManagementRoute.saved(activity):MemoryManagementRoute.from(intent));
    }
    private void applyRoute(MemoryManagementRoute value){route=value;key=value.key;single=value.uri;title=value.title;}
    private void afterTrash(){
        if (directTrashUris != null) { activity.finish(); return; }
        selected.clear();finishedHistory=false;
        io.execute(()->{
            MemoryManagementRoute saved=MemoryManagementRoute.saved(activity);
            if(!activity.getIntent().getBooleanExtra("trash_resume",false)
                    &&(MemoryManagementRoute.SINGLE.equals(route.mode)||MemoryManagementRoute.SINGLE.equals(saved.mode))) {
                main.post(()->{if(usable())activity.finish();});
                return;
            }
            if(MemoryManagementRoute.SINGLE.equals(saved.mode)&&!saved.key.isEmpty()) {
                saved=MemoryManagementRoute.of(MemoryManagementRoute.RECORD,saved.key,"",saved.title);
            }
            MemoryManagementRoute destination=saved;
            main.post(()->{if(usable()){applyRoute(destination);reload();}});
        });
    }
    void resume() {
        if (directTrashUris != null) trash.resume(false, this::startDirectTrash);
        else if (storySelection) reload();
        else trash.resume(finishedHistory, this::reload);
    }

    private void startDirectTrash() {
        if (Build.VERSION.SDK_INT < 30) {
            message("휴지통 이동은 Android 11 이상에서 지원해요.");
            return;
        }
        if (directTrashStarted || directTrashUris.isEmpty() || !MemoryManagementRoute.RECORD.equals(route.mode)) {
            activity.finish();
            return;
        }
        directTrashStarted = true;
        LinearLayout backdrop = new LinearLayout(activity);
        backdrop.setBackground(MemorySelectionStyle.background());
        activity.setContentView(backdrop);
        io.execute(() -> {
            try {
                List<UnresolvedMediaRecord> chosen = new MemoryMediaAccess(activity).observeExactUris(directTrashUris);
                main.post(() -> { if (usable()) trash.start(chosen, route); });
            } catch (Exception failure) {
                main.post(() -> message("선택한 사진을 다시 확인해 주세요."));
            }
        });
    }
    void pause(){trash.pause();}
    void close(){if(placePicker!=null)placePicker.dismiss();if(confirmation!=null)confirmation.dismiss();closed=true;generation++;trash.close();io.shutdownNow();thumbnails.shutdownNow();images.evictAll();}
    void result(int request){trash.result(request);}
    private boolean usable(){return !closed&&!activity.isFinishing()&&!activity.isDestroyed();}
    private static String orEmpty(String text){return text==null?"":text;}
    private void reload() {
        if(!usable())return;trash.showing=false;busy=true;int version=++generation;
        if(!single.isEmpty()&&!finishedHistory&&Build.VERSION.SDK_INT>=30) {
            final String singlePlaceAction=orEmpty(activity.getIntent().getStringExtra("single_place_action"));
            FrameLayout backdrop=new FrameLayout(activity);
            backdrop.setBackgroundColor(android.graphics.Color.rgb(12,14,20));
            activity.setContentView(backdrop);
            io.execute(()->{
                try {
                    List<UnresolvedMediaRecord> media=new MemoryMediaAccess(activity).observeUris(Collections.singletonList(single));
                    if(media.isEmpty())throw new IllegalStateException("선택한 사진을 찾지 못했어요.");
                    if(!singlePlaceAction.isEmpty()) {
                        ManualPlaceSnapshot snapshot=new ManualPlaceStore(activity).read();
                        List<StoredAlbumSummary> albums=MemoryManagementAlbums.read(activity);
                        DiscoverySnapshotController discovery=new DiscoverySnapshotController(activity);
                        MemoryRepository repository=discovery.repository(albums);
                        List<MemoryPlaceChoice> choices=new ArrayList<>();
                        for(MemoryRecord record:repository.memories())choices.add(new MemoryPlaceChoice(record,repository.stableId(record.memoryKey)));
                        List<String> history=MemoryPlacePickerHistory.read(activity);
                        main.post(()->{
                            if(!usable()||generation!=version)return;
                            rows=media;revision=snapshot.revision;placeChoices=choices;recentChoices=history;
                            selected.clear();selected.add(media.get(0).assetKey());busy=false;
                            if("unassign".equals(singlePlaceAction))confirmSingleUnassign();
                            else pickSinglePlace();
                        });
                        return;
                    }
                    main.post(()->{
                        if(!usable()||generation!=version)return;
                        rows=media;selected.clear();selected.add(media.get(0).assetKey());
                        busy=false;trash.start(media,route);
                    });
                }catch(Exception e){
                    main.post(()->{
                        if(usable()&&generation==version){
                            busy=false;
                            Toast.makeText(activity,e.getMessage()==null?"사진 정리를 시작하지 못했어요.":e.getMessage(),Toast.LENGTH_SHORT).show();
                            activity.finish();
                        }
                    });
                }
            });
            return;
        }
        render(storySelection?"공유할 사진을 불러오고 있어요.":"사진을 불러오고 있어요.");
        io.execute(()->{
            try {
                ManualPlaceSnapshot snapshot=new ManualPlaceStore(activity).read();
                List<StoredAlbumSummary> albums=MemoryManagementAlbums.read(activity);
                DiscoverySnapshotController discovery=new DiscoverySnapshotController(activity);
                MemoryRepository repository=discovery.repository(albums);
                List<MemoryRecord> records=repository.memories();
                List<MemoryPlaceChoice> choices=new ArrayList<>();for(MemoryRecord record:records)choices.add(new MemoryPlaceChoice(record,repository.stableId(record.memoryKey)));
                List<String> history=MemoryPlacePickerHistory.read(activity);
                MemoryMediaAccess access=new MemoryMediaAccess(activity);
                List<UnresolvedMediaRecord> expected=new ArrayList<>(snapshot.unresolved());
                Set<String> eligible=new HashSet<>(),manualKeys=new HashSet<>();
                for(UnresolvedMediaRecord r:expected)eligible.add(r.assetKey());
                for(ManualMediaAssignment a:snapshot.assignments()){expected.add(a.media);eligible.add(a.media.assetKey());manualKeys.add(a.media.assetKey());}
                List<UnresolvedMediaRecord> media;String heading="위치 없는 사진";
                if(!single.isEmpty()){media=access.observeUris(Collections.singletonList(single));heading="사진 정리";}
                else if(!key.isEmpty()) {
                    MemoryBrowserDetail detail=discovery.loadBrowserDetail(key,albums,true);
                    if(detail==null)throw new IllegalStateException("위치기록을 찾지 못했어요.");
                    media=access.observeUris(detail.sourceUris);heading=detail.item.title;
                } else if(route.addsPlace())media=EffectiveMediaPlaceResolver.unresolved(snapshot,access.available(expected,false),MemoryManagementAlbums.paths(albums));
                else {media=Collections.emptyList();heading="사진 정리";}
                List<UnresolvedMediaRecord> orderedMedia = MemoryManagementRoute.RECORD.equals(route.mode)
                        ? MemoryStoryPhotoOrder.newestFirst(media) : media;
                if(new ManualPlaceStore(activity).revision()!=snapshot.revision)throw new IllegalStateException("기록이 바뀌었어요. 다시 불러와 주세요.");
                String description=route.addsPlace()?ManualInventoryStatus.description(activity,snapshot):"";
                if(route.addsPlace()&&"SUCCEEDED".equals(ManualInventoryStatus.state(activity)))description="";
                String finalDescription=description;
                String finalHeading=heading;
                main.post(()->{
                    if(!usable()||generation!=version||trash.showing)return;
                    rows=orderedMedia;placeChoices=choices;recentChoices=history;eligiblePlaces=eligible;assigned=manualKeys;revision=snapshot.revision;title=finalHeading;
                    Set<String> keys=new HashSet<>();for(UnresolvedMediaRecord r:rows)keys.add(r.assetKey());selected.retainAll(keys);
                    if(!single.isEmpty()&&selected.isEmpty()&&rows.size()==1)selected.add(rows.get(0).assetKey());
                    busy=false;render(finalDescription);
                });
            }catch(Exception e){main.post(()->{if(usable()&&generation==version){busy=false;render("사진 목록을 불러오지 못했어요. 권한을 확인하고 다시 시도해 주세요.");message(e.getMessage());}});}
        });
    }
    private void render(String description) {
        root=new LinearLayout(activity);root.setOrientation(1);int p=CleanupUiStyle.dp(activity,16);
        root.setPadding(p,p,p,p);root.setBackground(MemorySelectionStyle.background());
        MediaScreenInsets.bind(root);
        TextView brand=CleanupUiStyle.text(activity,"PhotoPlace",11,true,CleanupUiStyle.PURPLE);brand.setGravity(Gravity.CENTER);root.addView(brand);
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView close=MemorySelectionStyle.icon(activity,"×",storySelection?"스토리 공유 닫기":"사진 정리 닫기");close.setOnClickListener(v->activity.finish());header.addView(close,new LinearLayout.LayoutParams(CleanupUiStyle.dp(activity,40),CleanupUiStyle.dp(activity,48)));
        TextView heading=CleanupUiStyle.text(activity,storySelection?"스토리 공유 · "+title:title,20,true,CleanupUiStyle.INK);heading.setSingleLine(true);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        TextView options=MemorySelectionStyle.icon(activity,"⋮",storySelection?"스토리 공유 옵션":"사진 정리 옵션");
        options.setOnClickListener(v->{PopupMenu menu=new PopupMenu(activity,options);
            menu.getMenu().add("새로고침").setEnabled(!busy).setOnMenuItemClickListener(item->{reload();return true;});
            if(route.addsPlace()){
                menu.getMenu().add("표시되는 사진 안내").setOnMenuItemClickListener(item->{confirmation=MemoryActionDialog.show(activity,"표시되는 사진","홈에서 검색한 폴더의 사진 중 위치가 확인되지 않은 사진을 보여드려요. 이미 연결한 기록과 앱에서 만든 위치앨범의 복사본은 제외해요.","확인",()->{},null,()->{});return true;});
                menu.getMenu().add("선택한 사진 휴지통 이동").setEnabled(!busy&&!selected.isEmpty()&&Build.VERSION.SDK_INT>=30).setOnMenuItemClickListener(item->{confirmTrash();return true;});
            } else if(!storySelection) {
                boolean canReassign=!busy&&!selected.isEmpty()&&eligiblePlaces.containsAll(selected);
                boolean canUnassign=!busy&&!selected.isEmpty()&&assigned.containsAll(selected);
                menu.getMenu().add("다른 위치기록으로 변경").setEnabled(canReassign).setOnMenuItemClickListener(item->{pickPlace();return true;});
                menu.getMenu().add("위치 연결 해제").setEnabled(canUnassign).setOnMenuItemClickListener(item->{confirmUnassign();return true;});
            }
            menu.show();});
        header.addView(options,new LinearLayout.LayoutParams(CleanupUiStyle.dp(activity,44),CleanupUiStyle.dp(activity,48)));root.addView(header);
        status=CleanupUiStyle.text(activity,description,12,false,CleanupUiStyle.MUTED);status.setVisibility(description.isEmpty()?View.GONE:View.VISIBLE);root.addView(status);
        progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PURPLE));
        progress.setVisibility(busy?View.VISIBLE:View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,CleanupUiStyle.dp(activity,10)));
        LinearLayout selection=new LinearLayout(activity);selection.setGravity(Gravity.CENTER_VERTICAL);
        total=CleanupUiStyle.text(activity,"전체 "+number(rows.size())+"개",13,false,CleanupUiStyle.MUTED);selection.addView(total,new LinearLayout.LayoutParams(0,-2,1));
        all=CleanupUiStyle.button(activity,"전체 선택",false);all.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        all.setOnClickListener(v->{if(selected.size()==rows.size())selected.clear();else for(UnresolvedMediaRecord r:rows)selected.add(r.assetKey());updateSelection();adapter.notifyDataSetChanged();});
        selection.addView(all,new LinearLayout.LayoutParams(-2,-2));root.addView(selection);
        GridView grid=new GridView(activity);grid.setNumColumns(3);grid.setColumnWidth(CleanupUiStyle.dp(activity,96));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setHorizontalSpacing(CleanupUiStyle.dp(activity,6));grid.setVerticalSpacing(CleanupUiStyle.dp(activity,6));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));grid.setClipToPadding(false);
        adapter=new RowAdapter();grid.setAdapter(adapter);
        grid.setOnItemClickListener((p0,v,pos,id)->{if(busy)return;String k=rows.get(pos).assetKey();if(!selected.add(k))selected.remove(k);updateSelection();adapter.notifyDataSetChanged();});
        grid.setOnItemLongClickListener((p0,v,pos,id)->{showInfo(rows.get(pos));return true;});
        root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=CleanupUiStyle.text(activity,busy?"":storySelection?"공유할 사진이 없어요.":"현재 정리할 사진이 없어요.",15,false,CleanupUiStyle.MUTED);
        empty.setGravity(Gravity.CENTER);root.addView(empty,new LinearLayout.LayoutParams(-1,0,1));grid.setEmptyView(empty);
        LinearLayout footer=new LinearLayout(activity);footer.setOrientation(1);footer.setPadding(p,p/2,p,p/2);
        footer.setBackground(CleanupUiStyle.card(activity,android.graphics.Color.WHITE));
        LinearLayout.LayoutParams footerParams=new LinearLayout.LayoutParams(-1,-2);footerParams.topMargin=CleanupUiStyle.dp(activity,12);root.addView(footer,footerParams);
        LinearLayout actions=new LinearLayout(activity);place=null;sendTrash=null;instagram=null;
        Button cancel=MemorySelectionStyle.action(activity,"닫기",false);cancel.setOnClickListener(v->activity.finish());
        LinearLayout.LayoutParams cancelParams=new LinearLayout.LayoutParams(0,-2,0.65f);cancelParams.rightMargin=CleanupUiStyle.dp(activity,8);actions.addView(cancel,cancelParams);
        if(storySelection) {
            instagram=MemorySelectionStyle.action(activity,"Instagram 공유",true);
            instagram.setOnClickListener(v->MemoryStoryShareSheet.showForSelection(activity,title,selection()));
            actions.addView(instagram,new LinearLayout.LayoutParams(0,-2,1));
        } else if(route.addsPlace()) {
            place=MemorySelectionStyle.action(activity,"기록 추가",true);place.setOnClickListener(v->pickPlace());actions.addView(place,new LinearLayout.LayoutParams(0,-2,1));
        } else {
            sendTrash=MemorySelectionStyle.action(activity,"휴지통으로 이동",true);sendTrash.setOnClickListener(v->confirmTrash());actions.addView(sendTrash,new LinearLayout.LayoutParams(0,-2,1));
        }
        footer.addView(actions);
        activity.setContentView(root);updateSelection();
    }
    private void showInfo(UnresolvedMediaRecord media) {
        String date=media.takenAt>0?new java.text.SimpleDateFormat("yyyy.MM.dd",java.util.Locale.KOREA).format(new Date(media.takenAt)):"알 수 없음";
        confirmation=MemoryActionDialog.show(activity,"사진 정보",media.name+"\n\n"+(media.volume.equals("external_primary")?"내장 저장소":"저장소 "+media.volume)+" / "+media.relativePath+"\n\n촬영일 · "+date,"확인",()->{},null,()->{});
    }
    private static String number(int n){return java.text.NumberFormat.getIntegerInstance(java.util.Locale.KOREA).format(n);}
    private List<UnresolvedMediaRecord> selection(){List<UnresolvedMediaRecord> result=new ArrayList<>();for(UnresolvedMediaRecord r:rows)if(selected.contains(r.assetKey()))result.add(r);return result;}
    private void updateSelection() {
        if(total==null)return;
        total.setText("선택 "+number(selected.size())+" · 전체 "+number(rows.size())+"개");
        boolean any=!selected.isEmpty()&&!busy;
        all.setEnabled(!busy&&!rows.isEmpty());all.setText(selected.size()==rows.size()&&!rows.isEmpty()?"선택 해제":"전체 선택");
        boolean canPlace=any;for(String k:selected)canPlace&=eligiblePlaces.contains(k);
        enabled(place,canPlace);enabled(sendTrash,any&&Build.VERSION.SDK_INT>=30);enabled(instagram,any);
        if(Build.VERSION.SDK_INT<30&&sendTrash!=null)sendTrash.setText("Android 11 이상에서 지원해요");
    }
    private void enabled(Button button,boolean enabled){if(button!=null){button.setEnabled(enabled);button.setAlpha(enabled?1f:0.4f);}}
    private String label(MemoryRecord record){MemoryBrowserItem item=MemoryBrowserItem.from(record);return item==null?record.title:item.title;}
    private void pickPlace() {
        if(selection().isEmpty())return;
        placePicker=MemoryPlacePicker.show(activity,placeChoices,recentChoices,"선택한 사진·동영상 "+selected.size()+"개",
                choice->confirmPlace(choice.record,choice.title),this::newPlace);
    }
    private void pickSinglePlace() {
        if(selection().isEmpty()){activity.finish();return;}
        placePicker=MemoryPlacePicker.show(activity,placeChoices,recentChoices,"이 사진을 다른 위치기록으로 이동",
                choice->confirmSinglePlace(choice.record,choice.title),this::newSinglePlace,activity::finish);
    }
    private void newSinglePlace() {
        confirmation=MemoryActionDialog.input(activity,name->confirmSinglePlace(null,name));
        confirmation.setOnCancelListener(d->activity.finish());
    }
    private void confirmSinglePlace(MemoryRecord target,String name) {
        List<UnresolvedMediaRecord> chosen=selection();
        confirmation=MemoryActionDialog.show(activity,"기록을 변경할까요?","이 사진을 ‘"+name+"’ 기록으로 이동해요. 실제 사진 파일의 저장 위치는 그대로예요.",
                "변경하기",()->assign(chosen,target,name),"취소",activity::finish);
    }
    private void confirmSingleUnassign() {
        List<UnresolvedMediaRecord> chosen=selection();if(chosen.isEmpty()){activity.finish();return;}
        confirmation=MemoryActionDialog.show(activity,"위치 연결을 해제할까요?",
                "이 사진의 위치기록 연결을 해제해요. 실제 사진 파일과 갤러리 앨범은 삭제되지 않아요.",
                "연결 해제",()->unassign(chosen),"취소",activity::finish);
    }
    private void newPlace() {
        confirmation=MemoryActionDialog.input(activity,name->confirmPlace(null,name));
    }
    private void confirmPlace(MemoryRecord target,String name) {
        List<UnresolvedMediaRecord> chosen=selection();
        confirmation=MemoryActionDialog.show(activity,"기록에 추가할까요?",chosen.size()+"개를 ‘"+name+"’ 기록에 연결해요. 확인된 원본·복사본의 장소 연결도 함께 바뀌어요. 사진 파일의 GPS와 저장 위치는 그대로예요.",
                "추가하기",()->assign(chosen,target,name),"취소",()->{});
    }
    private void assign(List<UnresolvedMediaRecord> chosen,MemoryRecord target,String name) {
        busy=true;if(progress!=null)progress.setVisibility(View.VISIBLE);updateSelection();if(status!=null){status.setVisibility(View.VISIBLE);status.setText("선택한 사진과 위치기록을 확인하고 있어요.");}
        io.execute(()->{
            try(MediaMutationCoordinator.Reservation reservation=MediaMutationCoordinator.reserve(activity,"places")) {
                List<UnresolvedMediaRecord> fresh=new MemoryMediaAccess(activity).requireFresh(chosen);MemoryTrashExecution.verifyObservations(chosen,fresh);
                String id=target==null?"":new MemoryIdentityRegistryStore(activity).resolveOrCreate(target.memoryKey);
                String destination=new ManualPlaceStore(activity).assign(fresh,target,id,name,revision);
                try{MemoryPlacePickerHistory.record(activity,new MemoryIdentityRegistryStore(activity).findStableId(destination));}catch(Exception ignored){}
                main.post(()->{if(!usable())return;busy=false;selected.clear();
                    if(!single.isEmpty()){Toast.makeText(activity,"‘"+name+"’ 기록으로 이동했어요.",Toast.LENGTH_SHORT).show();activity.finish();return;}
                    confirmation=MemoryActionDialog.show(activity,"기록에 추가했어요",chosen.size()+"개 · "+name+"\n위치기록에서 갤러리 앨범도 만들 수 있어요.",
                            "위치기록 보기",()->MemoryManagementEntry.openRecord(activity,destination),"계속 추가",this::reload);});
            }catch(Exception e){main.post(()->{message(e.getMessage());if(!single.isEmpty())activity.finish();});}
        });
    }
    private void confirmUnassign() {
        List<UnresolvedMediaRecord> chosen=selection();if(chosen.isEmpty())return;
        confirmation=MemoryActionDialog.show(activity,"위치 연결을 해제할까요?",
                chosen.size()+"개의 위치기록 연결을 해제해요. 확인된 원본·복사본의 장소 연결도 함께 해제되며, 실제 사진 파일과 갤러리 앨범은 삭제되지 않아요.",
                "연결 해제",()->unassign(chosen),"취소",()->{});
    }
    private void unassign(List<UnresolvedMediaRecord> chosen) {
        busy=true;if(progress!=null)progress.setVisibility(View.VISIBLE);updateSelection();if(status!=null){status.setVisibility(View.VISIBLE);status.setText("선택한 사진의 위치 연결을 해제하고 있어요.");}
        io.execute(()->{
            try(MediaMutationCoordinator.Reservation reservation=MediaMutationCoordinator.reserve(activity,"places")) {
                List<UnresolvedMediaRecord> fresh=new MemoryMediaAccess(activity).requireFresh(chosen);MemoryTrashExecution.verifyObservations(chosen,fresh);
                new ManualPlaceStore(activity).unassign(fresh,revision);
                main.post(()->{if(!usable())return;busy=false;selected.clear();
                    Toast.makeText(activity,chosen.size()+"개의 위치 연결을 해제했어요.",Toast.LENGTH_SHORT).show();
                    if(!single.isEmpty())activity.finish();else reload();});
            }catch(Exception e){main.post(()->{message(e.getMessage());if(!single.isEmpty())activity.finish();});}
        });
    }
    private void confirmTrash() {
        List<UnresolvedMediaRecord> chosen=selection();if(chosen.isEmpty())return;
        busy=true;generation++;progress.setVisibility(View.VISIBLE);status.setVisibility(View.GONE);
        updateSelection();trash.start(chosen,route);
    }
    private void message(String text) {
        if(!usable())return;busy=false;
        if(text==null||!text.matches("(?s).*[가-힣].*"))text="사진 정리를 완료하지 못했어요. 목록을 다시 불러와 주세요.";
        if(status!=null){status.setText(text);status.setVisibility(View.VISIBLE);}if(progress!=null)progress.setVisibility(View.GONE);updateSelection();Toast.makeText(activity,text,Toast.LENGTH_LONG).show();
        if (directTrashUris != null && !trash.showing) activity.finish();
    }
    private final class RowAdapter extends BaseAdapter {
        public int getCount(){return rows.size();}public Object getItem(int pos){return rows.get(pos);}public long getItemId(int pos){return pos;}
        public View getView(int pos,View recycled,ViewGroup parent) {
            MemorySelectionTile tile=recycled instanceof MemorySelectionTile?(MemorySelectionTile)recycled:new MemorySelectionTile(activity);
            tile.setLayoutParams(new AbsListView.LayoutParams(-1,CleanupUiStyle.dp(activity,96)));
            UnresolvedMediaRecord media=rows.get(pos);
            tile.bindState(selected.contains(media.assetKey()),media.kind.equals("VIDEO"),media.name);
            ImageView image=tile.image;image.setTag(media.uri);Bitmap cached=images.get(media.uri);image.setImageBitmap(cached);
            if(cached==null&&!closed)thumbnails.execute(()->{try{Bitmap bitmap=activity.getContentResolver().loadThumbnail(Uri.parse(media.uri),new Size(256,256),null);images.put(media.uri,bitmap);main.post(()->{if(usable()&&media.uri.equals(image.getTag()))image.setImageBitmap(bitmap);});}catch(Exception ignored){}});
            return tile;
        }
    }
}
