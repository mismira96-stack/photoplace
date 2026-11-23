package com.example.gallerysorter;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
import java.util.function.Consumer;

/** The original Story composition, with per-photo gestures only. */
final class MemoryStoryPhotoPreview extends View {
    private List<Bitmap> photos=Collections.emptyList();
    private MemoryStoryConfig config;
    private final Consumer<MemoryStoryConfig> changed;
    private int selected;
    private boolean dragging,pinching,captionPressed;
    private Runnable captionAction;
    private boolean captionEditing;
    private float editingScale;
    void setCaptionEditing(boolean editing){
        if(editing&&!captionEditing&&config!=null) {
            float readable=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,16,getResources().getDisplayMetrics());
            editingScale=Math.max(scale(),readable/captionTextSize());
        }
        captionEditing=editing;if(!editing)editingScale=0;requestLayout();invalidate();
    }
    private MemoryStoryConfig layoutConfig(){return captionEditing&&config!=null&&config.captionText.isEmpty()?config.withCaptionText("\u200B"):config;}
    void setCaptionAction(Runnable action){captionAction=action;}
    void updateConfig(MemoryStoryConfig value){config=value;requestLayout();invalidate();}
    float displayScale(){return scale();}
    float captionTextSize() {
        float footerScale = MemoryStoryRowLayout.usesRow(config, photos.size())
                ? MemoryStoryRowLayout.footerScale(slots()) : 1f;
        return MemoryStoryRowLayout.captionTextSize(config, footerScale);
    }
    MemoryStoryComposer.Box captionBox(){
        MemoryStoryComposer.Box card=cardBounds();
        List<MemoryStoryComposer.Box> boxes=slots();
        if (boxes.isEmpty()) return card;
        return MemoryStoryComposer.footerBox(card, boxes, layoutConfig());
    }
    private float lastX,lastY,lastSpan;
    MemoryStoryPhotoPreview(Context c,Consumer<MemoryStoryConfig> changed){super(c);this.changed=changed;setContentDescription("스토리 사진 미리보기");}
    void show(List<Bitmap> photos,MemoryStoryConfig config){this.photos=Collections.unmodifiableList(new ArrayList<>(photos));this.config=config;selected=Math.min(selected,Math.max(0,photos.size()-1));requestLayout();invalidate();}
    MemoryStoryConfig snapshot(){return config;}
    private MemoryStoryComposer.Box cardBounds() {
        if (config == null || photos.isEmpty()) return new MemoryStoryComposer.Box(0, 0, 1080, 1920);
        if (config.layoutMode != MemoryStoryConfig.LayoutMode.SINGLE_FRAME && photos.size() >= 2)
            return MemoryStoryComposer.stripLayout(photos, layoutConfig()).card;
        Bitmap photo = photos.get(0);
        return MemoryStoryComposer.computeSingleCardBox(photo.getWidth(), photo.getHeight(), layoutConfig());
    }
    private float scale() {
        MemoryStoryComposer.Box card = cardBounds();
        if(captionEditing&&editingScale>0)return Math.min(editingScale,getWidth()*.90f/card.width());
        boolean row = config != null && MemoryStoryRowLayout.usesRow(config, photos.size());
        float fill = row ? .98f : .85f;
        return Math.min(getWidth() * fill / card.width(), getHeight() * (row ? .98f : .94f) / card.height());
    }
    float viewX(float storyX) {
        MemoryStoryComposer.Box card = cardBounds();
        return (getWidth() - card.width() * scale()) / 2 + (storyX - card.left) * scale();
    }
    float viewY(float storyY) {
        MemoryStoryComposer.Box card = cardBounds();
        float top=(getHeight()-card.height()*scale())/2;
        if(captionEditing)top=Math.min(top,getHeight()-CleanupUiStyle.dp(getContext(),12)-card.height()*scale());
        return top+(storyY-card.top)*scale();
    }
    private float x(float px) { return (px - viewX(0)) / scale(); }
    private float y(float py) { return (py - viewY(0)) / scale(); }
    List<MemoryStoryComposer.Box> slots(){if(config==null||photos.isEmpty())return Collections.emptyList();if(config.layoutMode!=MemoryStoryConfig.LayoutMode.SINGLE_FRAME&&photos.size()>=2)return MemoryStoryComposer.stripLayout(photos,layoutConfig()).slots;Bitmap b=photos.get(0);return Collections.singletonList(MemoryStoryComposer.computeSinglePhotoBox(b.getWidth(),b.getHeight(),layoutConfig()));}
    private PointF localPoint(float x, float y, int index) {
        MemoryStoryComposer.Box box = slots().get(index);
        float cx = (box.left + box.right) / 2, cy = (box.top + box.bottom) / 2;
        double angle = Math.toRadians(-MemoryStoryRowLayout.angle(config, index));
        return new PointF(cx + (float) ((x - cx) * Math.cos(angle) - (y - cy) * Math.sin(angle)),
                cy + (float) ((x - cx) * Math.sin(angle) + (y - cy) * Math.cos(angle)));
    }
    private int hit(float x,float y){List<MemoryStoryComposer.Box> slots=slots();for(int i=slots.size()-1;i>=0;i--){MemoryStoryComposer.Box b=slots.get(i);PointF p=localPoint(x,y,i);if(p.x>=b.left&&p.x<=b.right&&p.y>=b.top&&p.y<=b.bottom)return i;}return -1;}
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), MeasureSpec.getSize(heightSpec));
    }
    @Override protected void onDraw(Canvas canvas) {
        if (config == null || photos.isEmpty()) return;
        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.drawColor(MemoryStoryComposer.singleBackgroundColor(config));
        canvas.translate(viewX(0), viewY(0));
        canvas.scale(scale(), scale());
        MemoryStoryComposer.draw(getContext(), canvas, photos, layoutConfig());
        if(captionEditing) {
            // The editable overlay is the only caption while typing. Never paint duplicate text
            // underneath it, including during keyboard/preview resize animation frames.
            MemoryStoryComposer.Box footer=captionBox();Paint paper=new Paint();paper.setColor(Color.WHITE);
            canvas.drawRect(footer.left,footer.top,footer.right,footer.bottom,paper);
        }
        canvas.restore();
    }
    private void update(float zoom,float x,float y){config=config.withPhotoEdit(selected,zoom,x,y);changed.accept(config);invalidate();}
    private void zoom(float factor,float fx,float fy){
        PointF focus = localPoint(fx, fy, selected); fx = focus.x; fy = focus.y;
        MemoryStoryPhotoEdits.Transform old=config.photoEdits.get(selected);float next=Math.max(.5f,Math.min(4,old.zoom*factor));Bitmap p=photos.get(selected);MemoryStoryComposer.Box slot=slots().get(selected);
        MemoryStoryComposer.Box before=MemoryStoryPhotoEdits.imageBox(p.getWidth(),p.getHeight(),slot,old),after=MemoryStoryPhotoEdits.imageBox(p.getWidth(),p.getHeight(),slot,new MemoryStoryPhotoEdits.Transform(next,0,0));
        float cx=fx+((before.left+before.right)/2-fx)*next/old.zoom,cy=fy+((before.top+before.bottom)/2-fy)*next/old.zoom;
        float rx=Math.abs(after.width()-slot.width())/2,ry=Math.abs(after.height()-slot.height())/2;
        update(next,rx>1?(cx-(slot.left+slot.right)/2)/rx:0,ry>1?(cy-(slot.top+slot.bottom)/2)/ry:0);
    }
    private void movePhoto(float x, float y) {
        Bitmap photo = photos.get(selected);
        MemoryStoryComposer.Box slot = slots().get(selected);
        MemoryStoryPhotoEdits.Transform edit = config.photoEdits.get(selected);
        MemoryStoryComposer.Box image = MemoryStoryPhotoEdits.imageBox(photo.getWidth(), photo.getHeight(), slot, edit);
        PointF before = localPoint(lastX, lastY, selected), after = localPoint(x, y, selected);
        float rx = Math.abs(image.width() - slot.width()) / 2, ry = Math.abs(image.height() - slot.height()) / 2;
        update(edit.zoom, rx > 1 ? edit.x + (after.x - before.x) / rx : 0,
                ry > 1 ? edit.y + (after.y - before.y) / ry : 0);
        lastX = x;
        lastY = y;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled()||config==null||photos.isEmpty()||scale()<=0)return false;float x=x(event.getX()),y=y(event.getY());
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){int i=hit(x,y);if(i<0){MemoryStoryComposer.Box card=cardBounds();captionPressed=config.includeContext&&captionAction!=null&&x>=card.left&&x<=card.right&&y>=captionBox().top-16&&y<=card.bottom;return captionPressed;}selected=i;dragging=true;lastX=x;lastY=y;getParent().requestDisallowInterceptTouchEvent(true);return true;}
        if(captionPressed){if(event.getActionMasked()==MotionEvent.ACTION_UP){captionPressed=false;captionAction.run();performClick();}else if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)captionPressed=false;return true;}
        if(!dragging)return false;
        if(event.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN&&event.getPointerCount()>=2){int i=hit(x((event.getX(0)+event.getX(1))/2),y((event.getY(0)+event.getY(1))/2));if(i<0)return true;selected=i;pinching=true;lastSpan=(float)Math.hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0));return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_POINTER_UP){pinching=false;int remaining=event.getActionIndex()==0?1:0;lastX=x(event.getX(remaining));lastY=y(event.getY(remaining));return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE){
            if(pinching&&event.getPointerCount()>=2){float span=(float)Math.hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0));if(lastSpan>8&&span>8)zoom(span/lastSpan,x((event.getX(0)+event.getX(1))/2),y((event.getY(0)+event.getY(1))/2));lastSpan=span;}
            else if(event.getPointerCount()==1){movePhoto(x,y);}return true;
        }
        if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){dragging=pinching=false;getParent().requestDisallowInterceptTouchEvent(false);performClick();return true;}return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
