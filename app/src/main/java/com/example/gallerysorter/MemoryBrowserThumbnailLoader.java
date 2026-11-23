package com.example.gallerysorter;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.OperationCanceledException;
import android.util.LruCache;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/** Browsing covers never queue behind retired legacy screens. Visible requests own their results. */
final class MemoryBrowserThumbnailLoader {
    private final ContentResolver resolver;
    private final LruCache<String,Bitmap> cache;
    private final Supplier<Drawable> placeholder;
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final ExecutorService cancellations=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<ImageView,Request> pending=new WeakHashMap<>();
    private boolean closed;
    MemoryBrowserThumbnailLoader(ContentResolver resolver,LruCache<String,Bitmap> cache,Supplier<Drawable> placeholder) {
        this.resolver=resolver;this.cache=cache;this.placeholder=placeholder;
    }
    void load(ImageView target,String source,int requestedSize) {
        Request old=pending.remove(target);if(old!=null)old.cancel();
        if(closed)return;
        if(source==null||source.trim().isEmpty()){target.setImageDrawable(placeholder.get());return;}
        int size=Math.max(1,requestedSize);String key=source+"#"+size;target.setTag(key);
        Bitmap bitmap=cache.get(key);
        if(bitmap!=null&&!bitmap.isRecycled()){target.setImageBitmap(bitmap);return;}
        target.setImageDrawable(placeholder.get());Request request=new Request(target);pending.put(target,request);target.addOnAttachStateChangeListener(request);
        try {
            request.queue.load(()->decode(Uri.parse(source),size,request.signal),request::cancelDecode,
                result->{
                    ImageView view=request.view.get();
                    if(closed||view==null||pending.get(view)!=request)return;
                    pending.remove(view);view.removeOnAttachStateChangeListener(request);
                    if(result!=null){cache.put(key,result);view.setImageBitmap(result);}
                },error->{
                    ImageView view=request.view.get();
                    if(view!=null&&pending.get(view)==request){pending.remove(view);view.removeOnAttachStateChangeListener(request);}
                });
        }catch(RejectedExecutionException stopping){pending.remove(target);request.cancel();}
    }
    private Bitmap decode(Uri uri,int size,CancellationSignal signal)throws Exception {
        signal.throwIfCanceled();
        try {
            Bitmap bitmap=resolver.loadThumbnail(uri,new Size(size,size),signal);
            signal.throwIfCanceled();if(bitmap!=null)return bitmap;
        }catch(OperationCanceledException canceled){throw canceled;}catch(Exception unavailable){ }
        signal.throwIfCanceled();
        Bitmap bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver,uri),(decoder,info,source)->{
            signal.throwIfCanceled();int width=info.getSize().getWidth(),height=info.getSize().getHeight();
            double ratio=Math.min(1d,size/(double)Math.max(width,height));
            decoder.setTargetSize(Math.max(1,(int)Math.round(width*ratio)),Math.max(1,(int)Math.round(height*ratio)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        signal.throwIfCanceled();return bitmap;
    }
    private final class Request implements View.OnAttachStateChangeListener {
        final WeakReference<ImageView> view;
        final CancellationSignal signal=new CancellationSignal();
        final LatestImageLoadQueue<Bitmap> queue=new LatestImageLoadQueue<>(worker,main::post);
        Request(ImageView view){this.view=new WeakReference<>(view);}
        void cancelDecode(){try{cancellations.execute(signal::cancel);}catch(RejectedExecutionException closing){ }}
        void cancel(){queue.cancel();ImageView target=view.get();if(target!=null){target.removeOnAttachStateChangeListener(this);if(pending.get(target)==this)pending.remove(target);}}
        @Override public void onViewAttachedToWindow(View view){ }
        @Override public void onViewDetachedFromWindow(View view){cancel();}
    }
    void close(){closed=true;for(Request request:new ArrayList<>(pending.values()))request.cancel();pending.clear();worker.shutdownNow();cancellations.shutdown();}
}
