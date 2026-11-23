package com.example.gallerysorter;

import android.app.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import java.util.function.Consumer;

/** Styled app-owned information, confirmation and name entry. Never substitutes for OS approval. */
final class MemoryActionDialog {
    static Dialog show(Activity a,String title,String message,String positive,Runnable yes,String negative,Runnable no) {
        return build(a,title,message,null,positive,yes,negative,no);
    }
    static Dialog input(Activity a,Consumer<String> saved) {
        EditText input=new EditText(a);input.setSingleLine(true);input.setTextSize(15);input.setHint("예: 제주 여행, 우리집");
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        int p=CleanupUiStyle.dp(a,14);input.setPadding(p,p,p,p);input.setBackground(CleanupUiStyle.card(a,0xfffaf7fd));
        Dialog[] dialog={null};
        dialog[0]=build(a,"새 위치기록 만들기","사진 파일의 GPS와 촬영일은 그대로 유지돼요.",input,"다음",()->{
            String name=input.getText().toString().trim();
            if(name.isEmpty()||name.matches("(?s).*\\p{Cntrl}.*")){input.setError("장소 이름을 입력해 주세요.");return;}
            dialog[0].dismiss();saved.accept(name);
        },"취소",()->{});
        return dialog[0];
    }
    private static Dialog build(Activity a,String title,String message,EditText input,String positive,Runnable yes,String negative,Runnable no) {
        Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        int p=CleanupUiStyle.dp(a,22);LinearLayout root=new LinearLayout(a);root.setOrientation(1);root.setPadding(p,p,p,p);
        root.setBackground(MemorySelectionStyle.surface(a,0xfffffbff,24));
        TextView brand=CleanupUiStyle.text(a,"PhotoPlace",11,true,CleanupUiStyle.PURPLE);brand.setGravity(Gravity.CENTER);root.addView(brand);
        TextView heading=CleanupUiStyle.text(a,title,20,true,CleanupUiStyle.INK);heading.setPadding(0,p/2,0,p/2);root.addView(heading);
        TextView text=CleanupUiStyle.text(a,message,14,false,CleanupUiStyle.MUTED);text.setPadding(0,0,0,p/2);
        ScrollView scroll=CleanupUiStyle.scroll(a);scroll.addView(text);root.addView(scroll);
        if(input!=null)root.addView(input,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout actions=new LinearLayout(a);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=p/2;root.addView(actions,ap);
        if(negative!=null){Button cancel=MemorySelectionStyle.action(a,negative,false);cancel.setOnClickListener(v->{dialog.dismiss();no.run();});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.rightMargin=CleanupUiStyle.dp(a,8);actions.addView(cancel,cp);}
        Button confirm=MemorySelectionStyle.action(a,positive,true);confirm.setOnClickListener(v->{if(input==null)dialog.dismiss();yes.run();});actions.addView(confirm,new LinearLayout.LayoutParams(0,-2,1));
        if(no!=null)dialog.setOnCancelListener(d->no.run());dialog.setContentView(root);dialog.show();
        Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));window.setLayout(Math.min(CleanupUiStyle.dp(a,420),a.getResources().getDisplayMetrics().widthPixels-CleanupUiStyle.dp(a,36)),-2);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        return dialog;
    }
}
