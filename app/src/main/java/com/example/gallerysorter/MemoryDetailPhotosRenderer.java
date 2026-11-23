package com.example.gallerysorter;

import android.app.Activity;
import android.content.Context;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** Search and pagination stay scoped to one displayed record, without another source query. */
final class MemoryDetailPhotosRenderer {
    interface Page { int add(LinearLayout parent,List<MemoryPhotoSection> sections,int offset,int limit,String key); }
    static Runnable bind(Activity a,LinearLayout parent,MemoryBrowserDetail detail,int limit,Page page,Consumer<MemoryPhotoSection> editNote, MemoryDetailSelection selection) {
        final PersonalPlaceDetailPresenter[] placePresenter={null};
        LinearLayout results=new LinearLayout(a);results.setOrientation(1);
        final EditText[] field={null};
        final int[] currentLimit = {limit};
        MemorySearchHeaderRenderer controls=new MemorySearchHeaderRenderer(a,new MemorySearchHeaderRenderer.Listener(){
            public void onSearchVisibilityChanged(boolean visible){}
            public void onQueryChanged(String query){render(query);}
            private void render(String query){
                if(a.isFinishing()||a.isDestroyed())return;
                List<MemoryPhotoSection> activeSections=placePresenter[0]==null?detail.photoSections:placePresenter[0].activePhotoSections();
                List<MemoryDateNote> activeNotes=placePresenter[0]==null?detail.notes:placePresenter[0].activeNotes(activeSections);
                List<MemoryPhotoSection> filtered=MemoryDetailSearch.filter(activeSections,activeNotes,query);
                results.removeAllViews();
                MemoryNoteOnlyRenderer.bind(a,results,filtered,activeNotes,editNote);
                int total=MemoryPhotoPage.totalItemCount(filtered);
                if(total==0) {
                    boolean hasNotes=false;for(MemoryPhotoSection s:filtered)if(s.photos.isEmpty())hasNotes=true;
                    if(!hasNotes)results.addView(CleanupUiStyle.text(a,query.trim().isEmpty()?MemoryNoteOnlyRenderer.emptyMessage(detail):"검색 결과가 없어요",14,false,CleanupUiStyle.MUTED));
                    return;
                }
                LinearLayout photos=new LinearLayout(a);photos.setOrientation(1);results.addView(photos);
                int shownCount = Math.max(limit, currentLimit[0]);
                int[] shown={page.add(photos,filtered,0,shownCount,detail.item.memoryKey)};
                Button more=MemorySelectionStyle.action(a,"사진 더 보기",false);
                if(shown[0]<total)results.addView(more);
                more.setOnClickListener(v->{
                    int added = page.add(photos,filtered,shown[0],limit,detail.item.memoryKey);
                    shown[0] += added;
                    currentLimit[0] = shown[0];
                    if(shown[0]>=total)results.removeView(more);
                });
            }
        });
        placePresenter[0]=PersonalPlaceDetailPresenter.attach(a,parent,detail,selection,()->{
            if(field[0]!=null)field[0].setText(field[0].getText().toString());
        });
        field[0]=controls.input("");field[0].setHint("날짜, 메모, 파일명 검색");field[0].setVisibility(View.GONE);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(-1, CleanupUiStyle.dp(a, 48));
        fieldParams.bottomMargin = CleanupUiStyle.dp(a, 8);
        parent.addView(field[0], fieldParams);
        parent.addView(results);

        // Initial render uses the same query path, including note-only sections.
        field[0].setText("");
        return () -> {
            if (field[0] != null && !a.isFinishing() && !a.isDestroyed()) {
                String q = field[0].getText() == null ? "" : field[0].getText().toString();
                field[0].setText(q);
            }
        };
    }
}
