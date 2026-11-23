package com.example.gallerysorter;

import android.content.Context;
import java.util.*;
import org.json.JSONArray;

/** UI-only recent choices. No location, note, alias or media data is changed here. */
final class MemoryPlacePickerHistory {
    static List<String> read(Context c) {
        List<String> ids=new ArrayList<>();
        try{JSONArray a=new JSONArray(c.getSharedPreferences("memory_place_picker",0).getString("recent","[]"));
            for(int i=0;i<a.length();i++){String id=a.getString(i);if(id.startsWith("mem_")&&!ids.contains(id))ids.add(id);}}
        catch(Exception invalid){android.util.Log.w("PlacePicker","Recent choices unavailable");}
        return ids;
    }
    static void record(Context c,String id) {
        if(id==null||!id.startsWith("mem_"))return;
        try{List<String> ids=read(c);ids.remove(id);ids.add(0,id);
            c.getSharedPreferences("memory_place_picker",0).edit().putString("recent",new JSONArray(ids.subList(0,Math.min(8,ids.size()))).toString()).apply();}
        catch(Exception error){android.util.Log.w("PlacePicker","Recent choice not saved");}
    }
}
