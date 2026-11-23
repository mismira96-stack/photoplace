package com.example.gallerysorter;

import java.util.*;

/** URI-keyed edits survive strip reorder; originals remain read-only. */
final class MemoryStoryPhotoEdits {
    static final class Transform {
        final float zoom,x,y;
        Transform(float zoom,float x,float y){this.zoom=clamp(zoom,.5f,4);this.x=clamp(x,-1,1);this.y=clamp(y,-1,1);}
        private static float clamp(float n,float low,float high){return Float.isFinite(n)?Math.max(low,Math.min(high,n)):low<0?0:1;}
    }
    final List<String> keys;
    private final Map<String,Transform> edits;
    MemoryStoryPhotoEdits(){this(Collections.emptyList(),Collections.emptyMap());}
    private MemoryStoryPhotoEdits(List<String> keys,Map<String,Transform> edits){this.keys=Collections.unmodifiableList(new ArrayList<>(keys));this.edits=Collections.unmodifiableMap(new LinkedHashMap<>(edits));}
    MemoryStoryPhotoEdits ordered(List<String> keys){return new MemoryStoryPhotoEdits(keys,edits);}
    private String key(int index){return index>=0&&index<keys.size()?keys.get(index):"@index/"+index;}
    Transform get(int index){Transform value=edits.get(key(index));return value==null?new Transform(1,0,0):value;}
    MemoryStoryPhotoEdits with(int index,float zoom,float x,float y){if(index<0||index>3)throw new IllegalArgumentException("Invalid photo slot");Map<String,Transform> next=new LinkedHashMap<>(edits);next.put(key(index),new Transform(zoom,x,y));return new MemoryStoryPhotoEdits(keys,next);}
    MemoryStoryPhotoEdits reset(){return new MemoryStoryPhotoEdits(keys,Collections.emptyMap());}
    static MemoryStoryComposer.Box imageBox(int width,int height,MemoryStoryComposer.Box slot,Transform t){
        float fit=Math.min(slot.width()/Math.max(1,width),slot.height()/Math.max(1,height))*t.zoom;
        float w=Math.max(1,width)*fit,h=Math.max(1,height)*fit;
        float cx=(slot.left+slot.right)/2+t.x*Math.abs(w-slot.width())/2;
        float cy=(slot.top+slot.bottom)/2+t.y*Math.abs(h-slot.height())/2;
        return new MemoryStoryComposer.Box(cx-w/2,cy-h/2,cx+w/2,cy+h/2);
    }
}
