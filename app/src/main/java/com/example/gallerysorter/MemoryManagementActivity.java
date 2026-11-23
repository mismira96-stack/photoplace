package com.example.gallerysorter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Lifecycle-only host for selection, manual places, and intentional media trash. */
public final class MemoryManagementActivity extends Activity {
    private MemoryManagementController controller;
    @Override public void onCreate(Bundle state){super.onCreate(state);MemoryManagementEntry.prepareReturn(this,getIntent());controller=new MemoryManagementController(this);controller.start(getIntent(), state != null);}
    @Override protected void onResume(){super.onResume();if(controller!=null)controller.resume();}
    @Override protected void onPause(){if(controller!=null)controller.pause();super.onPause();}
    @Override protected void onDestroy(){if(controller!=null)controller.close();super.onDestroy();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);controller.result(request);}
}
