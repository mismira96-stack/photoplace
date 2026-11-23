package com.example.gallerysorter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Folder-first review. Source originals and retained Gallery copies are named separately. */
final class CleanupReviewDialog {
    interface Listener { void onConfirm(List<CleanupCandidate> selected); void onCancel(); }
    private static final int INK=0xFF1B2438, MUTED=0xFF647085, PURPLE=0xFF6852BE;
    private final Context context;
    private final CleanupReviewAssessment assessment;
    private final Listener listener;
    private final String diagnostics;
    private final CleanupReviewModel model;
    private final Set<String> selected=new HashSet<>(), expanded=new HashSet<>();
    private Dialog dialog;
    private boolean showHeld;
    private GroupAdapter adapter;
    private Button availableTab, heldTab, selectAll, confirm;
    private TextView selectedLabel, actionHelp;
    private CleanupThumbnailLoader thumbnails;

    CleanupReviewDialog(Context c,CleanupReviewAssessment a,Listener l) { this(c,a,l,""); }
    CleanupReviewDialog(Context c,CleanupReviewAssessment a,Listener l,String d) { this(c,a,l,d,Collections.emptyMap()); }
    CleanupReviewDialog(Context c,CleanupReviewAssessment a,Listener l,String d,Map<String,CleanupReviewModel.Source> sources) {
        context=c;assessment=a;listener=l;diagnostics=d;model=new CleanupReviewModel(a,sources);
        showHeld=model.availableCount()==0&&model.heldCount()>0;
    }
    void show() {
        thumbnails=new CleanupThumbnailLoader(context);
        LinearLayout root=column();root.setBackgroundColor(CleanupUiStyle.BACKGROUND);root.setPadding(dp(20),dp(16),dp(20),dp(16));
        LinearLayout header=row();
        header.addView(text("원본 정리",20,true,INK),new LinearLayout.LayoutParams(0,-2,1));
        Button close=button("닫기",false);close.setOnClickListener(v->{dialog.dismiss();if(listener!=null)listener.onCancel();});
        header.addView(close,new LinearLayout.LayoutParams(dp(72),-2));root.addView(header);

        LinearLayout notice=column();notice.setPadding(dp(14),dp(12),dp(14),dp(12));notice.setBackground(CleanupUiStyle.card(context,Color.WHITE));
        notice.addView(text("위치 앨범에 복사된 원본을 정리해요",14,true,INK));
        TextView keep=text("카메라·다운로드의 원본만 휴지통으로 이동해요.\n위치 앨범의 사진은 그대로 유지돼요.",12,false,MUTED);keep.setPadding(0,dp(5),0,0);notice.addView(keep);
        LinearLayout.LayoutParams noticeParams=new LinearLayout.LayoutParams(-1,-2);noticeParams.topMargin=dp(12);root.addView(notice,noticeParams);

        LinearLayout tabs=row();tabs.setPadding(0,dp(12),0,dp(8));
        availableTab=button("정리 가능 "+number(model.availableCount()),false);
        heldTab=button("보류 "+number(model.heldCount()),false);
        LinearLayout.LayoutParams tabParams=new LinearLayout.LayoutParams(0,-2,1);tabParams.setMarginEnd(dp(8));
        tabs.addView(availableTab,tabParams);tabs.addView(heldTab,new LinearLayout.LayoutParams(0,-2,1));
        availableTab.setOnClickListener(v->changeTab(false));heldTab.setOnClickListener(v->changeTab(true));root.addView(tabs);

        LinearLayout selectionBar=row();
        TextView hint=text("폴더별 원본 사진",13,true,INK);
        selectionBar.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
        if(!diagnostics.isEmpty()) {
            Button details=button("검사 내역",false);details.setTextSize(12);
            details.setOnClickListener(v->new AlertDialog.Builder(context).setTitle("검사 내역").setMessage(diagnostics).setPositiveButton("확인",null).show());
            selectionBar.addView(details,new LinearLayout.LayoutParams(dp(82),-2));
        }
        root.addView(selectionBar);
        selectAll=button("정리 가능한 원본 전체 선택",false);
        selectAll.setOnClickListener(v->{model.selectAll(selected,selected.size()!=model.availableCount());updateSelection();adapter.notifyDataSetChanged();});
        root.addView(selectAll,new LinearLayout.LayoutParams(-1,-2));

        ListView list=new ListView(context);list.setDivider(null);list.setClipToPadding(false);list.setPadding(0,dp(8),0,dp(8));
        adapter=new GroupAdapter();list.setAdapter(adapter);
        TextView empty=text("검토할 원본이 없어요.",14,false,MUTED);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(20),dp(20),dp(20));
        LinearLayout listArea=column();listArea.addView(empty,new LinearLayout.LayoutParams(-1,-1));listArea.addView(list,new LinearLayout.LayoutParams(-1,-1));list.setEmptyView(empty);
        root.addView(listArea,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout footer=column();footer.setPadding(0,dp(8),0,0);
        selectedLabel=text("선택한 원본 0장",15,true,INK);footer.addView(selectedLabel);
        actionHelp=text("원본을 선택하면 휴지통 이동 버튼이 켜져요",12,false,MUTED);actionHelp.setPadding(0,dp(4),0,dp(8));footer.addView(actionHelp);
        confirm=button("정리할 원본을 선택해 주세요",true);
        confirm.setOnClickListener(v->{List<CleanupCandidate> chosen=model.chosen(selected);if(chosen.isEmpty())return;dialog.dismiss();if(listener!=null)listener.onConfirm(chosen);});
        footer.addView(confirm,new LinearLayout.LayoutParams(-1,-2));root.addView(footer);
        dialog=new Dialog(context);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setContentView(root,new ViewGroup.LayoutParams(-1,-1));
        dialog.setCanceledOnTouchOutside(false);dialog.setOnDismissListener(d->thumbnails.close());dialog.show();
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setGravity(Gravity.BOTTOM);dialog.getWindow().setLayout(-1,(int)(context.getResources().getDisplayMetrics().heightPixels*0.88f));}
        changeTab(showHeld);updateSelection();
    }
    private void changeTab(boolean held) {
        showHeld=held;
        availableTab.setBackground(shape(held?Color.WHITE:0xFFEAE5FA,12,held?0xFFE0E2EB:PURPLE));
        heldTab.setBackground(shape(held?0xFFFFF3DC:Color.WHITE,12,held?0xFFC89B4C:0xFFE0E2EB));
        availableTab.setSelected(!held);heldTab.setSelected(held);
        selectAll.setVisibility(held?View.GONE:View.VISIBLE);
        adapter.rebuild();updateSelection();
    }
    private void updateSelection() {
        int count=model.chosen(selected).size();
        selectedLabel.setText(showHeld?"보류한 원본 "+number(model.heldCount())+"장":"선택한 원본 "+number(count)+"장");
        boolean enabled=CleanupExecutionPolicy.isEnabled()&&count>0&&!showHeld;
        confirm.setEnabled(enabled);confirm.setAlpha(enabled?1f:0.45f);
        confirm.setVisibility(showHeld?View.GONE:View.VISIBLE);
        confirm.setText(!CleanupExecutionPolicy.isEnabled()?"검토 전용 화면이에요":count==0?"원본 선택 후 이동":"원본 "+number(count)+"장 휴지통으로 이동");
        actionHelp.setText(showHeld?"보류 사진은 선택할 수 없어요. 펼치면 이유를 볼 수 있어요."
                :count==0?"원본을 선택하면 이동할 수 있어요. 위치 앨범은 유지돼요.":"위치 앨범은 유지돼요. 최대 "+number(CleanupExecutionPlan.BATCH_SIZE)+"장씩 시스템 확인을 요청해요.");
        selectAll.setEnabled(CleanupExecutionPolicy.isEnabled()&&model.availableCount()>0);
        selectAll.setText(count>0&&count==model.availableCount()?"선택한 원본 모두 해제":"정리 가능 "+number(model.availableCount())+"장 전체 선택");
    }
    private final class GroupAdapter extends BaseAdapter {
        final List<Object> rows=new ArrayList<>();
        void rebuild(){rows.clear();for(CleanupReviewModel.Group g:model.groups(showHeld)){rows.add(g);if(expanded.contains(g.key))rows.addAll(g.items);}notifyDataSetChanged();}
        public int getCount(){return rows.size();}public Object getItem(int p){return rows.get(p);}public long getItemId(int p){return p;}
        public int getViewTypeCount(){return 2;}public int getItemViewType(int p){return rows.get(p) instanceof CleanupReviewModel.Group?0:1;}
        public View getView(int p,View recycled,ViewGroup parent){Object item=rows.get(p);return item instanceof CleanupReviewModel.Group?group((CleanupReviewModel.Group)item):photo((CleanupCandidate)item);}
        private View group(CleanupReviewModel.Group g) {
            LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(CleanupUiStyle.card(context,Color.WHITE));
            LinearLayout top=row();CheckBox cb=new CheckBox(context);int count=model.selectedCount(g,selected);
            cb.setButtonTintList(ColorStateList.valueOf(PURPLE));cb.setChecked(count==g.items.size()&&g.selectable);cb.setContentDescription(g.source.folderLabel()+" 원본 "+g.items.size()+"장 선택");
            cb.setVisibility(g.selectable&&CleanupExecutionPolicy.isEnabled()?View.VISIBLE:View.GONE);
            cb.setOnClickListener(v->{model.toggle(g,selected);updateSelection();notifyDataSetChanged();});top.addView(cb,new LinearLayout.LayoutParams(dp(44),dp(48)));
            LinearLayout title=column();title.addView(text(g.source.folderLabel()+" 원본 "+number(g.items.size())+"장",15,true,INK));
            title.addView(text(g.selectable?(count==0?"선택 안 됨":number(count)+"장 선택됨"):"보류 · 확인이 필요해요",12,false,g.selectable?MUTED:0xFF8A641C));top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            Button expand=button(expanded.contains(g.key)?"접기 ▴":"사진 보기 ▾",false);expand.setTextSize(12);
            expand.setContentDescription(g.source.folderLabel()+"에서 "+g.albumName()+"(으)로 복사한 사진 "+g.items.size()+"장 "+(expanded.contains(g.key)?"접기":"보기"));
            expand.setOnClickListener(v->{if(!expanded.remove(g.key))expanded.add(g.key);rebuild();});top.addView(expand,new LinearLayout.LayoutParams(dp(92),-2));card.addView(top);
            TextView source=text((g.selectable?"정리할 원본 · ":"원본 위치 · ")+g.source.location(),12,false,g.selectable?0xFFA44346:MUTED);source.setPadding(0,dp(6),0,0);card.addView(source);
            TextView target=text("그대로 유지 · "+g.keptPath+"/",12,true,0xFF367760);target.setPadding(0,dp(5),0,0);card.addView(target);
            if(!g.selectable){TextView reason=text(model.explanation(g.items.get(0)),12,false,MUTED);reason.setPadding(0,dp(6),0,0);card.addView(reason);}
            LinearLayout wrapper=column();wrapper.setPadding(0,0,0,dp(8));wrapper.addView(card);return wrapper;
        }
        private View photo(CleanupCandidate c) {
            CleanupReviewModel.Source s=model.source(c);LinearLayout row=row();row.setPadding(dp(8),dp(6),dp(8),dp(6));row.setBackgroundColor(Color.WHITE);
            boolean selectable=c.isEligible()&&s.known()&&CleanupExecutionPolicy.isEnabled();CheckBox cb=new CheckBox(context);cb.setButtonTintList(ColorStateList.valueOf(PURPLE));cb.setChecked(selected.contains(c.evidence.sourceUri));cb.setVisibility(selectable?View.VISIBLE:View.GONE);
            cb.setOnClickListener(v->{model.toggle(c,selected);updateSelection();notifyDataSetChanged();});row.addView(cb,new LinearLayout.LayoutParams(dp(44),dp(48)));
            ImageView thumb=new ImageView(context);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(thumb,new LinearLayout.LayoutParams(dp(52),dp(52)));thumbnails.load(thumb,c.evidence.sourceUri);
            LinearLayout labels=column();labels.setPadding(dp(10),0,0,0);labels.addView(text(s.name.isEmpty()?"원본 사진":s.name,13,true,INK));labels.addView(text(selectable?"선택 시 원본만 휴지통으로 이동":model.explanation(c),12,false,MUTED));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            if(selectable)row.setOnClickListener(v->{model.toggle(c,selected);updateSelection();notifyDataSetChanged();});return row;
        }
    }
    private LinearLayout column(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String value,int size,boolean bold,int color){TextView v=new TextView(context);v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
    private Button button(String label,boolean primary){return CleanupUiStyle.button(context,label,primary);}
    private GradientDrawable shape(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;}
    private String number(int value){return CleanupReviewDiagnostics.number(value);}
    private int dp(int v){return Math.round(v*context.getResources().getDisplayMetrics().density);}
}
