package com.example.gallerysorter;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Searchable, virtualized place sheet; all thumbnails load asynchronously. No media mutations. */
final class MemoryPlacePicker {
    private final Activity activity;private final Dialog dialog;
    private final List<MemoryPlaceChoice> choices;private List<MemoryPlaceChoice> visible;
    private final Consumer<MemoryPlaceChoice> selected;private final Runnable create;private final Runnable canceled;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService images=new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>(48),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(6*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getByteCount();}};
    private boolean closed,byName,forwarded;private String query="";private Adapter adapter;
    private LinearLayout recent;
    static MemoryPlacePicker show(Activity activity,List<MemoryPlaceChoice> choices,List<String> history,String selection,
                     Consumer<MemoryPlaceChoice> selected,Runnable create) {
        return show(activity,choices,history,selection,selected,create,()->{});
    }
    static MemoryPlacePicker show(Activity activity,List<MemoryPlaceChoice> choices,List<String> history,String selection,
                     Consumer<MemoryPlaceChoice> selected,Runnable create,Runnable canceled) {
        MemoryPlacePicker picker=new MemoryPlacePicker(activity,choices,history,selection,selected,create,canceled);picker.show();return picker;
    }
    void dismiss(){dialog.dismiss();}
    private MemoryPlacePicker(Activity a,List<MemoryPlaceChoice> choices,List<String> history,String selection,Consumer<MemoryPlaceChoice> selected,Runnable create,Runnable canceled) {
        activity=a;this.choices=new ArrayList<>(choices);this.selected=selected;this.create=create;this.canceled=canceled;visible=MemoryPlaceChoice.filter(choices,"",false);
        dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(a);root.setOrientation(1);int p=dp(18);root.setPadding(p,dp(10),p,dp(14));
        root.setFocusableInTouchMode(true);root.requestFocus();
        root.setBackground(MemorySelectionStyle.surface(a,0xfffffbff,26));MediaScreenInsets.bind(root);
        TextView handle=new TextView(a);handle.setBackground(MemorySelectionStyle.surface(a,0xffd5cddd,2));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(34),dp(4));hp.gravity=Gravity.CENTER;hp.bottomMargin=dp(10);root.addView(handle,hp);
        TextView brand=CleanupUiStyle.text(a,"PhotoPlace",11,true,CleanupUiStyle.PURPLE);brand.setGravity(Gravity.CENTER);root.addView(brand);
        TextView title=CleanupUiStyle.text(a,"어느 위치기록에 추가할까요?",18,true,CleanupUiStyle.INK);title.setGravity(Gravity.CENTER);add(root,title,10);
        TextView count=CleanupUiStyle.text(a,selection,12,false,CleanupUiStyle.MUTED);count.setGravity(Gravity.CENTER);add(root,count,4);
        EditText search=new EditText(a);search.setTextSize(14);search.setSingleLine(true);search.setHint("위치 기록 검색");search.setSelectAllOnFocus(false);
        search.setPadding(dp(14),dp(8),dp(14),dp(8));search.setBackground(CleanupUiStyle.card(a,0xfffaf7fc));
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);add(root,search,12);
        Button fresh=MemorySelectionStyle.action(a,"＋ 새 위치기록 만들기",false);fresh.setTextColor(CleanupUiStyle.PURPLE);
        fresh.setOnClickListener(v->{forwarded=true;dialog.dismiss();create.run();});add(root,fresh,10);
        recent=new LinearLayout(a);recent.setOrientation(1);List<MemoryPlaceChoice> shortcuts=MemoryPlaceChoice.recent(choices,history);
        boolean used=!shortcuts.isEmpty();if(!used){shortcuts=MemoryPlaceChoice.filter(choices,"",false);shortcuts=shortcuts.subList(0,Math.min(3,shortcuts.size()));}
        if(!shortcuts.isEmpty()) {
            add(recent,CleanupUiStyle.text(a,used?"최근 선택한 기록":"최근 기록",12,true,CleanupUiStyle.INK),10);
            LinearLayout strip=new LinearLayout(a);
            for(MemoryPlaceChoice choice:shortcuts) {
                LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setBackground(MemorySelectionStyle.surface(a,0xfff2edf8,12));card.setClipToOutline(true);
                ImageView image=image();card.addView(image,new LinearLayout.LayoutParams(-1,dp(46)));load(image,choice.coverUri);
                TextView name=CleanupUiStyle.text(a,choice.title,12,true,CleanupUiStyle.INK);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);name.setPadding(dp(7),dp(4),dp(7),dp(6));card.addView(name);
                card.setOnClickListener(v->choose(choice));card.setContentDescription("위치기록 "+choice.title);
                int width=Math.min(dp(108),(Math.min(a.getResources().getDisplayMetrics().widthPixels-dp(56),dp(564))-dp(10))/3);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(width,-2);cp.rightMargin=dp(5);strip.addView(card,cp);
            }
            add(recent,strip,6);root.addView(recent);
        }
        LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(CleanupUiStyle.text(a,"모든 위치기록",12,true,CleanupUiStyle.INK),new LinearLayout.LayoutParams(0,-2,1));
        TextView sort=CleanupUiStyle.text(a,"최근 기록순 ⌄",11,false,CleanupUiStyle.MUTED);sort.setPadding(dp(6),dp(10),dp(6),dp(10));
        sort.setOnClickListener(v->{PopupMenu menu=new PopupMenu(a,sort);menu.getMenu().add("최근 기록순").setOnMenuItemClickListener(item->{byName=false;sort.setText("최근 기록순 ⌄");filter();return true;});
            menu.getMenu().add("이름순").setOnMenuItemClickListener(item->{byName=true;sort.setText("이름순 ⌄");filter();return true;});menu.show();});heading.addView(sort);add(root,heading,8);
        ListView list=new ListView(a);list.setDivider(null);list.setSelector(new ColorDrawable(Color.TRANSPARENT));adapter=new Adapter();list.setAdapter(adapter);
        list.setOnItemClickListener((parent,v,pos,id)->choose(visible.get(pos)));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=CleanupUiStyle.text(a,"일치하는 위치기록이 없어요.",13,false,CleanupUiStyle.MUTED);empty.setGravity(Gravity.CENTER);root.addView(empty,new LinearLayout.LayoutParams(-1,0,1));list.setEmptyView(empty);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();filter();}public void afterTextChanged(Editable e){}});
        Button cancel=MemorySelectionStyle.action(a,"취소",false);cancel.setOnClickListener(v->dialog.dismiss());add(root,cancel,8);
        dialog.setContentView(root);dialog.setOnDismissListener(d->{closed=true;images.shutdownNow();cache.evictAll();InputMethodManager keyboard=a.getSystemService(InputMethodManager.class);if(keyboard!=null)keyboard.hideSoftInputFromWindow(root.getWindowToken(),0);if(!forwarded&&canceled!=null)canceled.run();});
    }
    private void show() {
        dialog.show();Window window=dialog.getWindow();if(window==null)return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setGravity(Gravity.BOTTOM);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        window.setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels-dp(20),dp(600)),(int)(activity.getResources().getDisplayMetrics().heightPixels*0.87));
    }
    private void choose(MemoryPlaceChoice choice){forwarded=true;dialog.dismiss();selected.accept(choice);}
    private void filter(){visible=MemoryPlaceChoice.filter(choices,query,byName);recent.setVisibility(query.trim().isEmpty()?View.VISIBLE:View.GONE);adapter.notifyDataSetChanged();}
    private int dp(int n){return CleanupUiStyle.dp(activity,n);}
    private void add(LinearLayout root,View child,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);root.addView(child,p);}
    private ImageView image(){ImageView image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(MemorySelectionStyle.surface(activity,0xffeae3f4,10));image.setClipToOutline(true);return image;}
    private void load(ImageView image,String uri) {
        image.setTag(uri);image.setImageBitmap(cache.get(uri));if(uri.isEmpty()||cache.get(uri)!=null||closed)return;
        images.execute(()->{try{Bitmap b=activity.getContentResolver().loadThumbnail(Uri.parse(uri),new Size(160,160),null);cache.put(uri,b);main.post(()->{if(!closed&&uri.equals(image.getTag()))image.setImageBitmap(b);});}catch(Exception ignored){}});
    }
    private static final class Holder{ImageView image;TextView name,detail;}
    private final class Adapter extends BaseAdapter {
        public int getCount(){return visible.size();}public Object getItem(int pos){return visible.get(pos);}public long getItemId(int pos){return pos;}
        public View getView(int pos,View recycled,ViewGroup parent) {
            LinearLayout outer;Holder h;
            if(recycled instanceof LinearLayout&&recycled.getTag() instanceof Holder){outer=(LinearLayout)recycled;h=(Holder)outer.getTag();}
            else {
                outer=new LinearLayout(activity);outer.setOrientation(1);outer.setPadding(0,0,0,dp(6));
                LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(9),dp(8),dp(9),dp(8));row.setBackground(CleanupUiStyle.card(activity,Color.WHITE));outer.addView(row);
                h=new Holder();h.image=image();row.addView(h.image,new LinearLayout.LayoutParams(dp(52),dp(52)));
                LinearLayout text=new LinearLayout(activity);text.setOrientation(1);text.setPadding(dp(10),0,dp(5),0);
                h.name=CleanupUiStyle.text(activity,"",14,true,CleanupUiStyle.INK);h.detail=CleanupUiStyle.text(activity,"",11,false,CleanupUiStyle.MUTED);
                h.name.setSingleLine(true);h.name.setEllipsize(TextUtils.TruncateAt.END);h.detail.setSingleLine(true);h.detail.setEllipsize(TextUtils.TruncateAt.END);
                text.addView(h.name);text.addView(h.detail);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.addView(CleanupUiStyle.text(activity,"›",22,false,CleanupUiStyle.MUTED));outer.setTag(h);
            }
            MemoryPlaceChoice c=visible.get(pos);h.name.setText(c.title);h.detail.setText(c.subtitle);load(h.image,c.coverUri);return outer;
        }
    }
}
