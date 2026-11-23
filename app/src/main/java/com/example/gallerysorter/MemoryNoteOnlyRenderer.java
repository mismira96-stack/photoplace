package com.example.gallerysorter;

import android.app.Activity;
import android.widget.*;
import java.util.function.Consumer;

/** Notes remain reachable when their date has no available photos. */
final class MemoryNoteOnlyRenderer {
    static void bind(Activity a,LinearLayout parent,MemoryBrowserDetail detail,Consumer<MemoryPhotoSection> edit) {
        bind(a,parent,detail.photoSections,detail.notes,edit);
    }
    static void bind(Activity a,LinearLayout parent,java.util.List<MemoryPhotoSection> sections,java.util.List<MemoryDateNote> notes,Consumer<MemoryPhotoSection> edit) {
        for(MemoryPhotoSection section:sections) {
            if(!section.photos.isEmpty())continue;
            for(MemoryDateNote note:notes)if(note.dateKey.equals(section.dateKey)) {
                LinearLayout card=new LinearLayout(a);card.setOrientation(1);int p=CleanupUiStyle.dp(a,14);card.setPadding(p,p,p,p);
                card.setBackground(CleanupUiStyle.card(a,android.graphics.Color.WHITE));
                card.addView(CleanupUiStyle.text(a,section.dateText+" · 메모",16,true,CleanupUiStyle.INK));
                card.addView(CleanupUiStyle.text(a,note.text,14,false,CleanupUiStyle.INK));
                Button button=CleanupUiStyle.button(a,"메모 수정",false);button.setOnClickListener(v->edit.accept(section));card.addView(button);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=p;parent.addView(card,lp);
            }
        }
    }
    static void memoHint(Activity a,LinearLayout header,ImageView edit) {
        TextView label=CleanupUiStyle.text(a,"메모",11,false,CleanupUiStyle.MUTED);
        label.setOnClickListener(v->edit.performClick());header.addView(label);
    }
    static String emptyMessage(MemoryBrowserDetail detail) {
        if(detail.item.countText.equals("불러오기 필요"))return "사진을 불러오지 못했지만 작성한 메모는 확인할 수 있어요.";
        if(!detail.notes.isEmpty())return "사진이 없어도 작성한 메모는 이 기록에 남아 있어요.";
        return detail.canOpenGalleryAlbum?"이 항목은 갤러리 앨범에서 볼 수 있어요.":"지금 볼 수 있는 사진이 없어요.";
    }
}
