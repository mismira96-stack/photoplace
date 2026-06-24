package com.example.gallerysorter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.LruCache;
import android.util.Size;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.example.gallerysorter.MainActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MainActivity extends Activity {
    private static final String ACTION_REPAIR_DATES = "com.photoplace.app.REPAIR_DATES";
    private static final String ALBUM_SUMMARY_HISTORY_FILE = "album_summary_history.json";
    private static final String CAMERA_PATH = "DCIM/Camera/";
    private static final String LOCATION_NONE = "위치없음";
    private static final int MAX_MAIN_RESULT_GROUPS = 3;
    private static final int MAX_RESULT_SCREEN_GROUPS = 60;
    private static final int MAX_STORED_SUMMARY_SESSIONS = 20;
    private static final int MAX_VALID_TAKEN_YEAR = 2035;
    private static final int MIN_VALID_TAKEN_YEAR = 2000;
    private static final String PREFS_NAME = "album_sorter";
    private static final String PREF_ALBUM_ALIAS_PREFIX = "album_alias_";
    private static final String PREF_ALBUM_MEMORY_PREFIX = "album_memory_";
    private static final String PREF_MOVE_VIDEOS = "move_videos";
    private static final String PREF_SOURCE_PATHS = "source_paths";
    private static final int REQUEST_DELETE_ORIGINALS = 21;
    private static final int REQUEST_READ_IMAGES = 20;
    private static final int REQUEST_REPAIR_DATES = 24;
    private static final int REQUEST_WRITE_VIDEOS = 22;
    private static final String[] SEOUL_DISTRICTS = {"강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"};
    private Button cancelButton;
    private Button copyButton;
    private Button deleteOriginalsButton;
    private Button galleryButton;
    private TextView logText;
    private LinearLayout previewButton;
    private ProgressBar progressBar;
    private TextView progressDetailText;
    private TextView progressPercentText;
    private TextView progressText;
    private ScrollView recentPlacesScrollView;
    private LinearLayout resultList;
    private LinearLayout resultSummaryCard;
    private TextView resultSummaryTitle;
    private TextView sourceFoldersText;
    private TextView statBlockedLabel;
    private TextView statBlockedText;
    private TextView statReadyLabel;
    private TextView statReadyText;
    private TextView statTotalLabel;
    private TextView statTotalText;
    private TextView summaryText;
    private LinearLayout unclassifiedPreviewRow;
    private View unclassifiedSectionCard;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService thumbnailWorker = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> thumbnailCache = new LruCache<>(32);
    private final List<PhotoItem> previewItems = new ArrayList();
    private final List<Uri> copiedOriginalUris = new ArrayList();
    private final List<Uri> pendingTrashOriginalUris = new ArrayList();
    private final Set<String> recentlySortedUriKeys = new HashSet();
    private final Map<String, Long> pendingDateRepairs = new LinkedHashMap();
    private final Map<String, String> locationCache = new LinkedHashMap();
    private final Map<String, Long> albumDateFallbackCache = new LinkedHashMap();
    private volatile boolean cancelRequested = false;
    private boolean isWorking = false;
    private boolean resultScreenMode = false;
    private boolean recentPlacesScreenMode = false;
    private boolean recentPlaceDetailMode = false;
    private StoredAlbumSummary activePlaceDetailSummary = null;
    private int recentPlacesScrollY = 0;
    private boolean copyCompletedMode = false;
    private boolean originalsTrashCompleted = false;
    private boolean videoWritePermissionGranted = false;
    private String workingMessage = null;
    private String activeProgressLabel = null;
    private String activeProgressContext = null;
    private int activeProgressCurrent = 0;
    private int activeProgressTotal = 0;

    private int actionTextColor(int i) {
        if (i == -15368131) {
            return -14327494;
        }
        if (i == -2024120) {
            return -6145458;
        }
        return i == -8635667 ? -10139744 : -13672545;
    }

    private int softenColor(int i) {
        if (i == -15368131) {
            return -1575188;
        }
        if (i == -2024120) {
            return -4366;
        }
        return i == -8635667 ? -922113 : -1050881;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        buildUi();
        ensureReadPermission();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        refreshActivePlaceDetailAfterExternalChange();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.recentPlaceDetailMode) {
            returnToRecentPlacesScreen();
            return;
        }
        if (this.resultScreenMode) {
            returnToMainScreen();
        } else if (this.isWorking) {
            showToast("백그라운드에서 계속 진행됩니다.");
        } else {
            super.onBackPressed();
        }
    }

    private void returnToMainScreen() {
        buildUi();
        ensureReadPermission();
        restoreMainUiFromState();
    }

    private void buildUi() {
        this.resultScreenMode = false;
        this.recentPlacesScreenMode = false;
        this.recentPlaceDetailMode = false;
        this.activePlaceDetailSummary = null;
        this.recentPlacesScrollView = null;
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(-197377);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(18), dp(64), dp(18), dp(REQUEST_WRITE_VIDEOS));
        scrollView.addView(linearLayout);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setMinimumHeight(dp(44));
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(28)));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        linearLayout2.addView(linearLayout3, matchWidth());
        TextView textView = new TextView(this);
        textView.setText("앨범 정리");
        textView.setTextSize(23.0f);
        textView.setTextColor(-14735049);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setIncludeFontPadding(false);
        linearLayout3.addView(textView, weightedParams(1));
        TextView textView2 = new TextView(this);
        textView2.setText("갤러리");
        textView2.setTextSize(13.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-14326805);
        textView2.setGravity(17);
        textView2.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1050881);
        gradientDrawable.setCornerRadius(dp(14));
        textView2.setBackground(gradientDrawable);
        textView2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m16lambda$buildUi$0$comexamplegallerysorterMainActivity(view);
            }
        });
        linearLayout3.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText("위치별 앨범 정리 & 발견한 장소 기록");
        textView3.setTextSize(15.0f);
        textView3.setTextColor(-6511697);
        textView3.setIncludeFontPadding(false);
        textView3.setPadding(dp(4), dp(2), 0, 0);
        linearLayout2.addView(textView3, matchWidth());
        addWorkingBanner(linearLayout);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(17);
        linearLayout4.setPadding(dp(10), dp(12), dp(10), dp(12));
        linearLayout.addView(linearLayout4, matchWidthWithBottom(dp(24)));
        applyCardBackground(linearLayout4);
        this.statTotalText = statBlock(linearLayout4, "새 장소", "photoLibrary", "0개", -1, -10788888, true);
        this.statReadyText = statBlock(linearLayout4, "위치 없음", "locationOff", "0개", -1, -34257, true);
        this.statBlockedText = statBlock(linearLayout4, "정리 완료", "folder", "0개", -1, -14370705, false);
        LinearLayout linearLayoutCreateHeroStartCard = createHeroStartCard();
        this.previewButton = linearLayoutCreateHeroStartCard;
        linearLayoutCreateHeroStartCard.setEnabled(false);
        this.previewButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m17lambda$buildUi$1$comexamplegallerysorterMainActivity(view);
            }
        });
        linearLayout.addView(this.previewButton, fullWidthButtonParams(dp(20), dp(138)));
        Button button = new Button(this);
        this.copyButton = button;
        button.setText("바로 정리하기");
        this.copyButton.setEnabled(false);
        this.copyButton.setVisibility(8);
        this.copyButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m18lambda$buildUi$2$comexamplegallerysorterMainActivity(view);
            }
        });
        styleActionButton(this.copyButton, actionText("바로 정리하기", "확인한 항목을 앨범으로 정리"), "folder", -3542826, -10236022, -15368131);
        linearLayout.addView(this.copyButton, fullWidthButtonParams(dp(10), dp(82)));
        Button button2 = new Button(this);
        this.galleryButton = button2;
        button2.setText("결과 보기");
        this.galleryButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m19lambda$buildUi$3$comexamplegallerysorterMainActivity(view);
            }
        });
        Button button3 = new Button(this);
        this.deleteOriginalsButton = button3;
        button3.setText("남은 사진 원본 휴지통 이동");
        this.deleteOriginalsButton.setEnabled(false);
        this.deleteOriginalsButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m20lambda$buildUi$4$comexamplegallerysorterMainActivity(view);
            }
        });
        this.sourceFoldersText = null;
        updateSourceFoldersText();
        Button button4 = new Button(this);
        this.cancelButton = button4;
        button4.setText("중지");
        this.cancelButton.setEnabled(false);
        this.cancelButton.setVisibility(8);
        this.cancelButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m21lambda$buildUi$5$comexamplegallerysorterMainActivity(view);
            }
        });
        LinearLayout linearLayout5 = new LinearLayout(this);
        linearLayout5.setOrientation(1);
        linearLayout5.setPadding(dp(16), dp(14), dp(16), dp(14));
        linearLayout5.setVisibility(8);
        applyCardBackground(linearLayout5);
        linearLayout.addView(linearLayout5, matchWidthWithBottom(dp(18)));
        LinearLayout linearLayout6 = new LinearLayout(this);
        linearLayout6.setOrientation(0);
        linearLayout6.setGravity(16);
        linearLayout5.addView(linearLayout6, matchWidthWithBottom(dp(10)));
        TextView textView4 = new TextView(this);
        this.progressText = textView4;
        textView4.setTextSize(16.0f);
        this.progressText.setTypeface(Typeface.DEFAULT_BOLD);
        this.progressText.setTextColor(-15656921);
        linearLayout6.addView(this.progressText, weightedParams(1));
        TextView textView5 = new TextView(this);
        this.progressPercentText = textView5;
        textView5.setTextSize(16.0f);
        this.progressPercentText.setTypeface(Typeface.DEFAULT_BOLD);
        this.progressPercentText.setTextColor(-12010632);
        this.progressPercentText.setGravity(17);
        this.progressPercentText.setPadding(dp(8), 0, dp(8), 0);
        linearLayout6.addView(this.progressPercentText);
        styleProgressCancelButton(this.cancelButton);
        linearLayout6.addView(this.cancelButton);
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        this.progressBar = progressBar;
        progressBar.setMax(100);
        this.progressBar.setProgressDrawable(progressDrawable());
        linearLayout5.addView(this.progressBar, progressBarParams());
        this.progressBar.setTag(linearLayout5);
        TextView textView6 = new TextView(this);
        this.progressDetailText = textView6;
        textView6.setTextSize(13.0f);
        this.progressDetailText.setTextColor(-10193781);
        linearLayout5.addView(this.progressDetailText, matchWidth());
        addRecentPlacesSection(linearLayout);
        LinearLayout linearLayout7 = new LinearLayout(this);
        this.resultSummaryCard = linearLayout7;
        linearLayout7.setOrientation(1);
        linearLayout7.setPadding(dp(16), dp(13), dp(12), dp(13));
        linearLayout7.setClickable(true);
        linearLayout7.setFocusable(true);
        linearLayout7.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m22lambda$buildUi$6$comexamplegallerysorterMainActivity(view);
            }
        });
        linearLayout7.setVisibility(this.previewItems.isEmpty() ? 8 : 0);
        linearLayout.addView(linearLayout7, matchWidthWithBottom(dp(16)));
        applyCardBackground(linearLayout7);
        LinearLayout linearLayout8 = new LinearLayout(this);
        linearLayout8.setOrientation(0);
        linearLayout8.setGravity(16);
        linearLayout7.addView(linearLayout8, matchWidth());
        LinearLayout linearLayout9 = new LinearLayout(this);
        linearLayout9.setOrientation(1);
        linearLayout8.addView(linearLayout9, weightedParams(1));
        TextView textViewSectionTitle = sectionTitle("확인 필요");
        this.resultSummaryTitle = textViewSectionTitle;
        linearLayout9.addView(textViewSectionTitle);
        TextView textView7 = new TextView(this);
        this.summaryText = textView7;
        textView7.setText("앨범 정리 시작을 누르면 요약을 보여드려요.");
        this.summaryText.setTextSize(14.0f);
        this.summaryText.setTextColor(-10193781);
        this.summaryText.setPadding(0, dp(3), 0, 0);
        linearLayout9.addView(this.summaryText);
        TextView textView8 = new TextView(this);
        textView8.setText("›");
        textView8.setTextSize(28.0f);
        textView8.setTextColor(-7035976);
        textView8.setGravity(17);
        textView8.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m23lambda$buildUi$7$comexamplegallerysorterMainActivity(view);
            }
        });
        linearLayout8.addView(textView8, squareParams(dp(30)));
        LinearLayout linearLayout10 = new LinearLayout(this);
        this.resultList = linearLayout10;
        linearLayout10.setOrientation(1);
        this.resultList.setPadding(0, 0, 0, 0);
        this.resultList.setVisibility(8);
        linearLayout7.addView(this.resultList, matchWidth());
        LinearLayout linearLayout11 = new LinearLayout(this);
        this.unclassifiedSectionCard = linearLayout11;
        linearLayout11.setOrientation(1);
        linearLayout11.setPadding(dp(16), dp(16), dp(16), dp(16));
        linearLayout11.setVisibility(8);
        linearLayout.addView(linearLayout11, matchWidth());
        applyCardBackground(linearLayout11);
        LinearLayout linearLayout12 = new LinearLayout(this);
        linearLayout12.setOrientation(0);
        linearLayout12.setGravity(16);
        linearLayout11.addView(linearLayout12, matchWidthWithBottom(dp(10)));
        linearLayout12.addView(sectionTitle("위치 정보 없는 항목"), weightedParams(1));
        TextView textView9 = new TextView(this);
        textView9.setText("전체 보기");
        textView9.setTextSize(15.0f);
        textView9.setTypeface(Typeface.DEFAULT_BOLD);
        textView9.setTextColor(-10193781);
        textView9.setGravity(17);
        textView9.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m24lambda$buildUi$8$comexamplegallerysorterMainActivity(view);
            }
        });
        linearLayout12.addView(textView9);
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        linearLayout11.addView(horizontalScrollView, matchWidth());
        LinearLayout linearLayout13 = new LinearLayout(this);
        this.unclassifiedPreviewRow = linearLayout13;
        linearLayout13.setOrientation(0);
        horizontalScrollView.addView(this.unclassifiedPreviewRow);
        TextView textView10 = new TextView(this);
        this.logText = textView10;
        textView10.setText("위치 정보가 없는 사진/동영상은 여기에 표시됩니다.");
        this.logText.setTextSize(12.0f);
        this.logText.setTextColor(-7035976);
        this.logText.setPadding(0, dp(8), 0, 0);
        this.logText.setVisibility(8);
        linearLayout11.addView(this.logText, matchWidth());
        setContentViewWithBottomTabs(scrollView, 0);
        applyWorkingStateToViews();
    }

    /* renamed from: lambda$buildUi$0$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m16lambda$buildUi$0$comexamplegallerysorterMainActivity(View view) {
        openGallery();
    }

    /* renamed from: lambda$buildUi$1$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m17lambda$buildUi$1$comexamplegallerysorterMainActivity(View view) {
        runPreview();
    }

    /* renamed from: lambda$buildUi$2$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m18lambda$buildUi$2$comexamplegallerysorterMainActivity(View view) {
        startCopyFromPreviewContext();
    }

    /* renamed from: lambda$buildUi$3$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m19lambda$buildUi$3$comexamplegallerysorterMainActivity(View view) {
        showResultScreen();
    }

    /* renamed from: lambda$buildUi$4$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m20lambda$buildUi$4$comexamplegallerysorterMainActivity(View view) {
        deleteCopiedOriginals();
    }

    /* renamed from: lambda$buildUi$5$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m21lambda$buildUi$5$comexamplegallerysorterMainActivity(View view) {
        requestCancel();
    }

    /* renamed from: lambda$buildUi$6$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m22lambda$buildUi$6$comexamplegallerysorterMainActivity(View view) {
        showResultScreen();
    }

    /* renamed from: lambda$buildUi$7$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m23lambda$buildUi$7$comexamplegallerysorterMainActivity(View view) {
        showResultScreen();
    }

    /* renamed from: lambda$buildUi$8$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m24lambda$buildUi$8$comexamplegallerysorterMainActivity(View view) {
        showResultScreen();
    }

    private void restoreMainUiFromState() {
        String strCompactResultSummary;
        int i = 8;
        if (this.previewItems.isEmpty()) {
            setStatus("준비됨", "0", "0", "0");
            LinearLayout linearLayout = this.resultSummaryCard;
            if (linearLayout != null) {
                linearLayout.setVisibility(8);
                return;
            }
            return;
        }
        LinearLayout linearLayout2 = this.resultSummaryCard;
        boolean z = false;
        if (linearLayout2 != null) {
            linearLayout2.setVisibility(0);
        }
        Iterator<PhotoItem> it = this.previewItems.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            if (it.next().noLocation) {
                i2++;
            }
        }
        int iCountRecentlySortedItems = this.copyCompletedMode ? countRecentlySortedItems(this.previewItems) : countCopyableItems(this.previewItems);
        int iCountRecentlySortedGroups = this.copyCompletedMode ? countRecentlySortedGroups(this.previewItems) : countNewFolderItems(this.previewItems);
        int iCountNewFolderItems = countNewFolderItems(this.previewItems);
        int iCountAlreadySortedItems = countAlreadySortedItems(this.previewItems);
        String str = this.copyCompletedMode ? "정리 완료" : "확인 완료";
        String strValueOf = String.valueOf(iCountRecentlySortedGroups);
        String strValueOf2 = String.valueOf(i2);
        if (this.copyCompletedMode) {
            iCountAlreadySortedItems = iCountRecentlySortedItems;
        }
        setStatus(str, strValueOf, strValueOf2, String.valueOf(iCountAlreadySortedItems));
        TextView textView = this.resultSummaryTitle;
        if (textView != null) {
            textView.setText(this.copyCompletedMode ? "정리 결과" : "확인 필요");
        }
        TextView textView2 = this.summaryText;
        if (this.copyCompletedMode) {
            strCompactResultSummary = completedResultSummary(iCountRecentlySortedGroups, i2, iCountRecentlySortedItems, this.copiedOriginalUris.size());
        } else {
            strCompactResultSummary = compactResultSummary(iCountRecentlySortedItems, i2, iCountNewFolderItems);
        }
        textView2.setText(strCompactResultSummary);
        renderPreviewResults(this.previewItems);
        Button button = this.copyButton;
        if (iCountRecentlySortedItems > 0 && !this.copyCompletedMode) {
            i = 0;
        }
        button.setVisibility(i);
        this.copyButton.setEnabled(iCountRecentlySortedItems > 0 && !this.copyCompletedMode);
        Button button2 = this.deleteOriginalsButton;
        if (this.copyCompletedMode && !this.copiedOriginalUris.isEmpty()) {
            z = true;
        }
        button2.setEnabled(z);
    }

    private void ensureReadPermission() {
        ArrayList arrayList = new ArrayList();
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.READ_MEDIA_IMAGES") != 0) {
                arrayList.add("android.permission.READ_MEDIA_IMAGES");
            }
            if (checkSelfPermission("android.permission.READ_MEDIA_VIDEO") != 0) {
                arrayList.add("android.permission.READ_MEDIA_VIDEO");
            }
        } else if (checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            arrayList.add("android.permission.READ_EXTERNAL_STORAGE");
        }
        if (checkSelfPermission("android.permission.ACCESS_MEDIA_LOCATION") != 0) {
            arrayList.add("android.permission.ACCESS_MEDIA_LOCATION");
        }
        if (!arrayList.isEmpty()) {
            requestPermissions((String[]) arrayList.toArray(new String[0]), 20);
        } else {
            this.previewButton.setEnabled(true);
            setStatus("준비됨", "0", "0", "0");
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 20) {
            boolean z = true;
            for (int i2 : iArr) {
                z = z && i2 == 0;
            }
            this.previewButton.setEnabled(z);
            setStatus(z ? "준비됨" : "권한 필요", "0", "0", "0");
        }
    }

    private void runPreview() {
        if (this.isWorking) {
            return;
        }
        this.cancelRequested = false;
        this.copyCompletedMode = false;
        this.originalsTrashCompleted = false;
        this.videoWritePermissionGranted = false;
        this.previewItems.clear();
        this.copiedOriginalUris.clear();
        this.pendingTrashOriginalUris.clear();
        this.recentlySortedUriKeys.clear();
        this.locationCache.clear();
        this.deleteOriginalsButton.setEnabled(false);
        setWorking(true, "정리할 항목을 찾는 중...");
        setStatus("분석 중", "-", "-", "-");
        this.summaryText.setText("선택한 폴더와 기존 앨범을 확인하고 있어요.");
        this.logText.setText("사진과 동영상을 안전하게 읽는 중이에요.");
        clearResultViews();
        this.worker.execute(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda67
            @Override // java.lang.Runnable
            public final void run() {
                try {
                    MainActivity.this.m46lambda$runPreview$12$comexamplegallerysorterMainActivity();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /* renamed from: lambda$runPreview$12$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m46lambda$runPreview$12$comexamplegallerysorterMainActivity() throws Throwable {
        List<AlbumFolder> listLoadAlbumFolders = loadAlbumFolders();
        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m47lambda$runPreview$9$comexamplegallerysorterMainActivity();
            }
        });
        final List<PhotoItem> listLoadSourcePhotos = loadSourcePhotos(listLoadAlbumFolders);
        if (this.cancelRequested) {
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m44lambda$runPreview$10$comexamplegallerysorterMainActivity();
                }
            });
            return;
        }
        int copyableCount = 0;
        int noLocationCount = 0;
        int duplicateCount = 0;
        for (PhotoItem photoItem : listLoadSourcePhotos) {
            if (photoItem.noLocation) {
                noLocationCount++;
            } else if (!photoItem.targetExists) {
                copyableCount++;
            }
            if (photoItem.duplicateInTarget) {
                duplicateCount++;
            }
        }
        final int i = copyableCount;
        final int i2 = noLocationCount;
        final int i3 = duplicateCount;
        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m45lambda$runPreview$11$comexamplegallerysorterMainActivity(listLoadSourcePhotos, i, i2, i3);
            }
        });
    }

    /* renamed from: lambda$runPreview$9$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m47lambda$runPreview$9$comexamplegallerysorterMainActivity() {
        this.summaryText.setText("사진/동영상의 날짜와 위치 정보를 확인하고 있어요.");
        this.logText.setText("위치가 있는 항목은 앨범 후보로 묶고 있어요.");
    }

    /* renamed from: lambda$runPreview$10$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m44lambda$runPreview$10$comexamplegallerysorterMainActivity() {
        this.summaryText.setText("항목 확인을 취소했습니다.");
        this.logText.setText("");
        setWorking(false, null);
    }

    /* renamed from: lambda$runPreview$11$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m45lambda$runPreview$11$comexamplegallerysorterMainActivity(List list, int i, int i2, int i3) {
        this.previewItems.clear();
        this.previewItems.addAll(list);
        this.copiedOriginalUris.clear();
        setStatus("확인 완료", String.valueOf(i), String.valueOf(i2), String.valueOf(i3));
        this.summaryText.setText(compactResultSummary(countCopyableItems(list), i2, i));
        renderPreviewResults(list);
        setWorking(false, null);
        this.copyButton.setVisibility(hasCopyableItems(list) ? 0 : 8);
        this.copyButton.setEnabled(hasCopyableItems(list));
        this.deleteOriginalsButton.setEnabled(false);
        showPreviewCompleteDialog(countCopyableItems(list), i2, i, i3);
    }

    private void startCopyFromPreviewContext() {
        if (this.previewItems.isEmpty()) {
            showToast("먼저 앨범 정리 시작을 눌러 항목을 확인해 주세요.");
            return;
        }
        if (!hasCopyableItems(this.previewItems)) {
            showToast("새로 정리할 항목이 없어요.");
        } else if (this.copyCompletedMode) {
            showToast("이미 정리가 완료됐어요.");
        } else {
            runCopy();
        }
    }

    private void startCopyFromResultScreen() {
        if (this.previewItems.isEmpty()) {
            showToast("먼저 앨범 정리 시작을 눌러 항목을 확인해 주세요.");
            return;
        }
        if (!hasCopyableItems(this.previewItems)) {
            showToast("새로 정리할 항목이 없어요.");
            return;
        }
        if (this.copyCompletedMode) {
            showToast("이미 정리가 완료됐어요.");
            return;
        }
        buildUi();
        ensureReadPermission();
        restoreMainUiFromState();
        this.copyButton.post(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.runCopy();
            }
        });
    }

    private void showPreviewCompleteDialog(final int i, int i2, int i3, int i4) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(REQUEST_WRITE_VIDEOS), dp(REQUEST_WRITE_VIDEOS), dp(REQUEST_WRITE_VIDEOS), dp(18));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(24));
        linearLayout.setBackground(gradientDrawable);
        TextView textView = new TextView(this);
        textView.setText("정리할 항목 확인");
        textView.setTextSize(21.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        textView.setGravity(17);
        linearLayout.addView(textView, matchWidthWithBottom(dp(8)));
        TextView textView2 = new TextView(this);
        textView2.setText(i > 0 ? "앨범으로 정리할 항목을 찾았어요." : "새로 정리할 항목이 없어요.");
        textView2.setTextSize(15.0f);
        textView2.setTextColor(-10193781);
        textView2.setGravity(17);
        linearLayout.addView(textView2, matchWidthWithBottom(dp(18)));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setColor(-460033);
        gradientDrawable2.setCornerRadius(dp(18));
        gradientDrawable2.setStroke(1, -1709326);
        linearLayout2.setBackground(gradientDrawable2);
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(16)));
        addDialogStat(linearLayout2, "앨범으로 정리", i + "개", -15293622);
        addDialogStat(linearLayout2, "새로 만들 앨범", i3 + "개", -14326805);
        addDialogStat(linearLayout2, "위치 확인 필요", i2 + "개", -680437);
        addDialogStat(linearLayout2, "이미 정리됨", i4 + "개", -10193781);
        if (i > 0) {
            addDialogAlbumPreview(linearLayout);
        }
        Button button = new Button(this);
        button.setText(i > 0 ? "바로 정리하기" : "확인");
        styleDialogPrimaryButton(button);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda57
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m51xef5d6fd4(dialog, i, view);
            }
        });
        linearLayout.addView(button, matchWidthWithBottom(dp(10)));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout.addView(linearLayout3, matchWidth());
        Button button2 = new Button(this);
        button2.setText("결과 보기");
        styleDialogSecondaryButton(button2);
        button2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda58
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m52x467b60b3(dialog, view);
            }
        });
        linearLayout3.addView(button2, dialogButtonParams(true));
        Button button3 = new Button(this);
        button3.setText("갤러리 열기");
        styleDialogSecondaryButton(button3);
        button3.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda59
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m53x9d995192(dialog, view);
            }
        });
        linearLayout3.addView(button3, dialogButtonParams(false));
        dialog.setContentView(linearLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9d), -2);
        }
        dialog.show();
    }

    /* renamed from: lambda$showPreviewCompleteDialog$13$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m51xef5d6fd4(Dialog dialog, int i, View view) {
        dialog.dismiss();
        if (i > 0) {
            startCopyFromPreviewContext();
        }
    }

    /* renamed from: lambda$showPreviewCompleteDialog$14$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m52x467b60b3(Dialog dialog, View view) {
        dialog.dismiss();
        showResultScreen();
    }

    /* renamed from: lambda$showPreviewCompleteDialog$15$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m53x9d995192(Dialog dialog, View view) {
        dialog.dismiss();
        openGallery();
    }

    private void addDialogAlbumPreview(LinearLayout linearLayout) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        LinkedHashMap linkedHashMap3 = new LinkedHashMap();
        Iterator<PhotoItem> it = this.previewItems.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PhotoItem next = it.next();
            if (!next.noLocation && !next.duplicateInTarget) {
                String strAlbumCandidateGroupKey = albumCandidateGroupKey(next);
                Integer num = (Integer) linkedHashMap.get(strAlbumCandidateGroupKey);
                linkedHashMap.put(strAlbumCandidateGroupKey, Integer.valueOf(num != null ? 1 + num.intValue() : 1));
                if (!linkedHashMap2.containsKey(strAlbumCandidateGroupKey)) {
                    linkedHashMap2.put(strAlbumCandidateGroupKey, next);
                }
                DateRange dateRange = (DateRange) linkedHashMap3.get(strAlbumCandidateGroupKey);
                if (dateRange == null) {
                    dateRange = new DateRange();
                    linkedHashMap3.put(strAlbumCandidateGroupKey, dateRange);
                }
                dateRange.include(next.takenAt);
            }
        }
        if (linkedHashMap.isEmpty()) {
            return;
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(14), dp(12), dp(14), dp(8));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(18));
        gradientDrawable.setStroke(1, -1709326);
        linearLayout2.setBackground(gradientDrawable);
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(16)));
        TextView textView = new TextView(this);
        textView.setText("정리될 앨범");
        textView.setTextSize(14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        linearLayout2.addView(textView, matchWidthWithBottom(dp(8)));
        int i = 0;
        for (Object entryObj : linkedHashMap.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            if (i >= 3) {
                break;
            }
            addDialogAlbumRow(linearLayout2, (PhotoItem) linkedHashMap2.get(entry.getKey()), albumCandidateTitle((String) entry.getKey()), formatDateRange((DateRange) linkedHashMap3.get(entry.getKey())), entry.getValue() + "개");
            i++;
        }
        if (linkedHashMap.size() > i) {
            TextView textView2 = new TextView(this);
            textView2.setText("+ " + (linkedHashMap.size() - i) + "개 앨범 더 있어요");
            textView2.setTextSize(13.0f);
            textView2.setTypeface(Typeface.DEFAULT_BOLD);
            textView2.setTextColor(-14326805);
            textView2.setGravity(17);
            textView2.setPadding(0, dp(8), 0, 0);
            linearLayout2.addView(textView2, matchWidth());
        }
    }

    private void addDialogAlbumRow(LinearLayout linearLayout, PhotoItem photoItem, String str, String str2, String str3) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, dp(6), 0, dp(6));
        linearLayout.addView(linearLayout2, matchWidth());
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        linearLayout2.addView(imageView, new LinearLayout.LayoutParams(dp(46), dp(46)));
        if (photoItem != null) {
            loadThumbnailInto(imageView, photoItem.uri, dp(46));
        } else {
            imageView.setImageDrawable(thumbnailPlaceholder());
        }
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dp(10), 0, dp(8), 0);
        linearLayout2.addView(linearLayout3, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(15.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        textView.setSingleLine(true);
        linearLayout3.addView(textView);
        if (str2 != null && !str2.isEmpty()) {
            TextView textView2 = new TextView(this);
            textView2.setText(str2);
            textView2.setTextSize(12.0f);
            textView2.setTextColor(-10193781);
            textView2.setSingleLine(true);
            textView2.setPadding(0, dp(2), 0, 0);
            linearLayout3.addView(textView2);
        }
        TextView textView3 = new TextView(this);
        textView3.setText(str3);
        textView3.setTextSize(15.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setTextColor(-15293622);
        linearLayout2.addView(textView3);
    }

    private void addDialogStat(LinearLayout linearLayout, String str, String str2, int i) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, dp(5), 0, dp(5));
        linearLayout.addView(linearLayout2, matchWidth());
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(15.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-13418155);
        linearLayout2.addView(textView, weightedParams(1));
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextSize(17.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(i);
        linearLayout2.addView(textView2);
    }

    private boolean needsVideoWritePermission() {
        return Build.VERSION.SDK_INT >= 30 && shouldMoveVideos() && !this.videoWritePermissionGranted && !collectMovableVideoUris().isEmpty();
    }

    private List<Uri> collectMovableVideoUris() {
        ArrayList arrayList = new ArrayList();
        if (!shouldMoveVideos()) {
            return arrayList;
        }
        for (PhotoItem photoItem : this.previewItems) {
            if (photoItem.video && !photoItem.noLocation && !photoItem.duplicateInTarget) {
                arrayList.add(photoItem.uri);
            }
        }
        return arrayList;
    }

    private void requestVideoWritePermission() {
        List<Uri> listCollectMovableVideoUris = collectMovableVideoUris();
        if (listCollectMovableVideoUris.isEmpty()) {
            this.videoWritePermissionGranted = true;
            runCopy();
            return;
        }
        if (Build.VERSION.SDK_INT < 30) {
            this.videoWritePermissionGranted = true;
            runCopy();
            return;
        }
        try {
            PendingIntent pendingIntentCreateWriteRequest = MediaStore.createWriteRequest(getContentResolver(), listCollectMovableVideoUris);
            this.summaryText.setText("동영상을 앨범으로 이동할 권한을 확인해 주세요.");
            startIntentSenderForResult(pendingIntentCreateWriteRequest.getIntentSender(), REQUEST_WRITE_VIDEOS, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException unused) {
            showToast("동영상 이동 권한 확인창을 열 수 없습니다.");
        } catch (Exception e) {
            showToast("동영상 이동 준비 실패: " + e.getMessage());
        }
    }

    private void startDateRepair() {
        showToast("날짜 복구 기능은 V1에서 비활성화되었습니다.");
        return;
        /*
        if (this.isWorking) {
            showToast("작업 중에는 날짜 복구를 시작할 수 없어요.");
        } else {
            setWorking(true, "정리 앨범 날짜를 확인하는 중...");
            this.worker.execute(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda28
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m66lambda$startDateRepair$17$comexamplegallerysorterMainActivity();
                }
            });
        }
        */
    }

    /* renamed from: lambda$startDateRepair$17$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m66lambda$startDateRepair$17$comexamplegallerysorterMainActivity() {
        final Map<String, Long> mapCollectDateRepairTargets = collectDateRepairTargets();
        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda38
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m65lambda$startDateRepair$16$comexamplegallerysorterMainActivity(mapCollectDateRepairTargets);
            }
        });
    }

    /* renamed from: lambda$startDateRepair$16$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m65lambda$startDateRepair$16$comexamplegallerysorterMainActivity(Map map) {
        setWorking(false, null);
        if (map.isEmpty()) {
            this.summaryText.setText("날짜를 복구할 항목이 없습니다.");
            showToast("날짜를 복구할 항목이 없습니다.");
        } else {
            this.pendingDateRepairs.clear();
            this.pendingDateRepairs.putAll(map);
            requestDateRepairPermission();
        }
    }

    private Map<String, Long> collectDateRepairTargets() {
        Cursor cursorQuery = null;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try {
            cursorQuery = contentResolver.query(uri, new String[]{"_id", "_display_name", "relative_path", "datetaken", "date_added", "date_modified"}, "relative_path LIKE ?", new String[]{"Pictures/%에서/%"}, null);
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda39
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m25xd130c41a(e);
                }
            });
        }
        if (cursorQuery == null) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return linkedHashMap;
        }
        try {
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
            int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow("_display_name");
            int columnIndex = cursorQuery.getColumnIndex("datetaken");
            int columnIndex2 = cursorQuery.getColumnIndex("date_added");
            int columnIndex3 = cursorQuery.getColumnIndex("date_modified");
            while (cursorQuery.moveToNext()) {
                Date dateFromName = parseDateFromName(cursorQuery.getString(columnIndexOrThrow2));
                if (dateFromName != null) {
                    long time = dateFromName.getTime();
                    long j = time / 1000;
                    long optionalLong = readOptionalLong(cursorQuery, columnIndex);
                    long optionalLong2 = readOptionalLong(cursorQuery, columnIndex2);
                    long optionalLong3 = readOptionalLong(cursorQuery, columnIndex3);
                    if (!isSameDay(optionalLong, time) || optionalLong2 != j || optionalLong3 != j) {
                        linkedHashMap.put(ContentUris.withAppendedId(uri, cursorQuery.getLong(columnIndexOrThrow)).toString(), Long.valueOf(time));
                    }
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return linkedHashMap;
        } finally {
        }
    }

    /* renamed from: lambda$collectDateRepairTargets$18$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m25xd130c41a(Exception exc) {
        showToast("날짜 복구 대상 확인 실패: " + exc.getMessage());
    }

    private void requestDateRepairPermission() {
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = this.pendingDateRepairs.keySet().iterator();
        while (it.hasNext()) {
            arrayList.add(Uri.parse(it.next()));
        }
        if (arrayList.isEmpty()) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                PendingIntent pendingIntentCreateWriteRequest = MediaStore.createWriteRequest(getContentResolver(), arrayList);
                this.summaryText.setText("날짜를 복구할 사진 " + arrayList.size() + "개 권한을 확인해 주세요.");
                startIntentSenderForResult(pendingIntentCreateWriteRequest.getIntentSender(), REQUEST_REPAIR_DATES, null, 0, 0, 0);
            } else {
                runDateRepairUpdates();
            }
        } catch (IntentSender.SendIntentException unused) {
            showToast("날짜 복구 권한 확인창을 열 수 없습니다.");
        } catch (Exception e) {
            showToast("날짜 복구 준비 실패: " + e.getMessage());
        }
    }

    private void runDateRepairUpdates() {
        this.pendingDateRepairs.clear();
        setWorking(false, null);
        showToast("날짜 복구 기능은 V1에서 비활성화되었습니다.");
    }

    /* renamed from: lambda$runDateRepairUpdates$20$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m43x12acea6a(Map map) {
        int updatedCount = 0;
        int processedCount = 0;
        for (Object entryObj : map.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            try {
                Uri.parse((String) entry.getKey());
                new ContentValues().put("datetaken", Long.valueOf(((Long) entry.getValue()).longValue()));
                updatedCount++;
            } catch (Exception unused) {
            }
            processedCount++;
        }
        final int i = updatedCount;
        final int i2 = processedCount;
        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m42x961a3740(i, i2);
            }
        });
    }

    /* renamed from: lambda$runDateRepairUpdates$19$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m42x961a3740(int i, int i2) {
        setWorking(false, null);
        this.summaryText.setText("날짜 복구 완료\n수정 " + i + "개\n실패 " + i2 + "개");
        showToast("날짜 복구 완료: 수정 " + i + "개");
    }

    private boolean isSameDay(long j, long j2) {
        if (j <= 0 || j2 <= 0) {
            return false;
        }
        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        Calendar calendar2 = Calendar.getInstance(Locale.KOREA);
        calendar.setTimeInMillis(j);
        calendar2.setTimeInMillis(j2);
        return calendar.get(1) == calendar2.get(1) && calendar.get(6) == calendar2.get(6);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runCopy() {
        if (this.isWorking) {
            return;
        }
        if (this.previewItems.isEmpty()) {
            showToast("먼저 정리 시작을 눌러 항목을 확인해 주세요.");
            return;
        }
        if (needsVideoWritePermission()) {
            requestVideoWritePermission();
            return;
        }
        this.cancelRequested = false;
        this.copyCompletedMode = false;
        this.originalsTrashCompleted = false;
        this.copiedOriginalUris.clear();
        this.pendingTrashOriginalUris.clear();
        this.recentlySortedUriKeys.clear();
        final boolean zShouldMoveVideos = shouldMoveVideos();
        setStatus("정리 중", String.valueOf(countCopyableItems(this.previewItems)), String.valueOf(countNoLocationItems(this.previewItems)), String.valueOf(countAlreadySortedItems(this.previewItems)));
        setWorking(true, "앨범으로 정리하는 중...");
        final ArrayList arrayList = new ArrayList(this.previewItems);
        this.logText.setText("앨범으로 정리하는 중이에요. 잠시만 기다려 주세요.");
        this.worker.execute(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda49
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m41lambda$runCopy$23$comexamplegallerysorterMainActivity(arrayList, zShouldMoveVideos);
            }
        });
    }

    /* renamed from: lambda$runCopy$23$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m41lambda$runCopy$23$comexamplegallerysorterMainActivity(final List list, final boolean z) {
        StringBuilder sb = new StringBuilder();
        final ArrayList arrayList = new ArrayList();
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            if (i >= list.size()) {
                break;
            }
            if (this.cancelRequested) {
                sb.append("사용자 취소\n");
                break;
            }
            final PhotoItem photoItem = (PhotoItem) list.get(i);
            final int i5 = i + 1;
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda63
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m39lambda$runCopy$21$comexamplegallerysorterMainActivity(i5, list, z, photoItem);
                }
            });
            if (photoItem.noLocation) {
                i3++;
                sb.append("위치없음, 건너뜀: ").append(photoItem.name).append("\n");
            } else if (photoItem.video && !z) {
                i3++;
                sb.append("동영상 이동 안 함, 건너뜀: ").append(photoItem.name).append("\n");
            } else if (photoItem.duplicateInTarget) {
                i3++;
                if (!photoItem.video) {
                    addUniqueUri(this.copiedOriginalUris, photoItem.uri);
                }
                sb.append("복사본 있음, 건너뜀: ").append(photoItem.name).append(" -> ").append(photoItem.targetRelativePath).append("\n");
            } else {
                try {
                    copyMediaItem(photoItem);
                    i2++;
                    arrayList.add(photoItem.uri);
                    if (!photoItem.video) {
                        addUniqueUri(this.copiedOriginalUris, photoItem.uri);
                    }
                    sb.append(photoItem.video ? "이동: " : "복사: ").append(photoItem.name).append(" -> ").append(photoItem.targetRelativePath).append("\n");
                } catch (Exception e) {
                    i4++;
                    sb.append("실패: ").append(photoItem.name).append(" / ").append(e.getMessage()).append("\n");
                }
            }
            i = i5;
        }
        final int i6 = i2;
        final int i7 = i3;
        final int i8 = i4;
        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda64
            @Override // java.lang.Runnable
            public final void run() {
                try {
                    MainActivity.this.m40lambda$runCopy$22$comexamplegallerysorterMainActivity(arrayList, i6, i7, i8);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /* renamed from: lambda$runCopy$21$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m39lambda$runCopy$21$comexamplegallerysorterMainActivity(int i, List list, boolean z, PhotoItem photoItem) {
        this.summaryText.setText("앨범으로 정리 중... " + i + " / " + list.size() + "개");
        this.logText.setText(z ? "사진은 복사하고 동영상은 앨범으로 이동하고 있어요." : "사진만 앨범으로 복사하고 있어요.");
        updateProgress("앨범으로 정리 중", i, list.size(), photoItem.noLocation ? "위치 정보 없음" : albumCandidateTitle(albumCandidateGroupKey(photoItem)));
    }

    /* renamed from: lambda$runCopy$22$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m40lambda$runCopy$22$comexamplegallerysorterMainActivity(List list, int i, int i2, int i3) throws JSONException {
        rememberRecentlySortedItems(list);
        markItemsAsSorted(list);
        saveAlbumSummaryHistory(this.previewItems, list, i, i2, i3);
        int iCountRecentlySortedItems = countRecentlySortedItems(this.previewItems);
        int iCountNoLocationItems = countNoLocationItems(this.previewItems);
        countAlreadySortedItems(this.previewItems);
        int iCountRecentlySortedGroups = countRecentlySortedGroups(this.previewItems);
        this.copyCompletedMode = true;
        setStatus("정리 완료", String.valueOf(iCountRecentlySortedGroups), String.valueOf(iCountNoLocationItems), String.valueOf(iCountRecentlySortedItems));
        this.summaryText.setText("정리 결과\n정리됨 " + i + "개\n건너뜀 " + i2 + "개\n실패 " + i3 + "개");
        this.logText.setText("앨범 정리가 끝났어요. 결과 보기에서 앨범별 내용을 확인할 수 있어요.");
        setWorking(false, null);
        this.copyCompletedMode = true;
        this.summaryText.setText(completedResultSummary(iCountRecentlySortedGroups, iCountNoLocationItems, iCountRecentlySortedItems, this.copiedOriginalUris.size()));
        this.copyButton.setEnabled(false);
        this.copyButton.setVisibility(8);
        this.deleteOriginalsButton.setEnabled(!this.copiedOriginalUris.isEmpty());
        showResultScreen();
    }

    private List<PhotoItem> loadSourcePhotos(List<AlbumFolder> list) throws Throwable {
        ArrayList arrayList = new ArrayList();
        List<String> selectedSourcePaths = getSelectedSourcePaths();
        loadSourceImages(arrayList, list, selectedSourcePaths);
        loadSourceVideos(arrayList, list, selectedSourcePaths);
        arrayList.sort(new Comparator() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda7
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                MainActivity.PhotoItem photoItem = (MainActivity.PhotoItem) obj;
                MainActivity.PhotoItem photoItem2 = (MainActivity.PhotoItem) obj2;
                return Long.compare(photoItem2.takenAt != null ? photoItem2.takenAt.getTime() : 0L, photoItem.takenAt == null ? 0L : photoItem.takenAt.getTime());
            }
        });
        return arrayList;
    }

    private void loadSourceImages(List<PhotoItem> list, List<AlbumFolder> list2, List<String> list3) throws Throwable {
        Cursor cursor;
        int i;
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try {
            Cursor cursorQuery = contentResolver.query(uri, new String[]{"_id", "_display_name", "mime_type", "date_modified", "date_added", "datetaken", "latitude", "longitude"}, visibleMediaSelection(buildRelativePathSelection("relative_path", list3)), (String[]) list3.toArray(new String[0]), "date_modified DESC");
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
                int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow("_display_name");
                int columnIndexOrThrow3 = cursorQuery.getColumnIndexOrThrow("mime_type");
                int columnIndexOrThrow4 = cursorQuery.getColumnIndexOrThrow("date_modified");
                int columnIndex = cursorQuery.getColumnIndex("date_added");
                int columnIndex2 = cursorQuery.getColumnIndex("datetaken");
                int columnIndex3 = cursorQuery.getColumnIndex("latitude");
                int columnIndex4 = cursorQuery.getColumnIndex("longitude");
                int count = cursorQuery.getCount();
                while (cursorQuery.moveToNext()) {
                    if (this.cancelRequested) {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                            return;
                        }
                        return;
                    }
                    Uri uriWithAppendedPath = Uri.withAppendedPath(uri, String.valueOf(cursorQuery.getLong(columnIndexOrThrow)));
                    String strSafeName = safeName(cursorQuery.getString(columnIndexOrThrow2));
                    cursor = cursorQuery;
                    int i2 = columnIndexOrThrow2;
                    int i3 = count;
                    int i4 = columnIndexOrThrow3;
                    int i5 = columnIndexOrThrow4;
                    int i6 = columnIndex;
                    int i7 = columnIndex2;
                    int i8 = columnIndex3;
                    try {
                        final PhotoItem photoItemBuildPhotoItem = buildPhotoItem(uriWithAppendedPath, strSafeName, cursorQuery.getString(columnIndexOrThrow3), readLocation(uriWithAppendedPath, strSafeName, cursorQuery.getLong(columnIndexOrThrow4), readOptionalLong(cursorQuery, columnIndex), readOptionalLong(cursorQuery, columnIndex2), readOptionalDouble(cursorQuery, columnIndex3), readOptionalDouble(cursorQuery, columnIndex4), false), list2, false);
                        list.add(photoItemBuildPhotoItem);
                        final int size = list.size();
                        if (size == 1 || size % 25 == 0) {
                            i = i3;
                        } else {
                            i = i3;
                            if (size != i) {
                                count = i;
                                columnIndexOrThrow2 = i2;
                                columnIndexOrThrow3 = i4;
                                columnIndexOrThrow4 = i5;
                                columnIndex = i6;
                                columnIndex2 = i7;
                                columnIndex3 = i8;
                                cursorQuery = cursor;
                            }
                        }
                        final int iMax = Math.max(i, size);
                        final int iMin = Math.min(size, iMax);
                        final int i9 = i;
                        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda45
                            @Override // java.lang.Runnable
                            public final void run() {
                                MainActivity.this.m28x61d3495a(size, i9, photoItemBuildPhotoItem, iMin, iMax);
                            }
                        });
                        count = i;
                        columnIndexOrThrow2 = i2;
                        columnIndexOrThrow3 = i4;
                        columnIndexOrThrow4 = i5;
                        columnIndex = i6;
                        columnIndex2 = i7;
                        columnIndex3 = i8;
                        cursorQuery = cursor;
                    } catch (Throwable th) {
                        th = th;
                        Throwable th2 = th;
                        if (cursor == null) {
                            throw th2;
                        }
                        try {
                            cursor.close();
                            throw th2;
                        } catch (Throwable th3) {
                            th2.addSuppressed(th3);
                            throw th2;
                        }
                    }
                }
                Cursor cursor2 = cursorQuery;
                if (cursor2 != null) {
                    cursor2.close();
                }
            } catch (Throwable th4) {
                throw new RuntimeException(th4);
            }
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda46
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m29xb8f13a39(e);
                }
            });
        }
    }

    /* renamed from: lambda$loadSourceImages$25$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m28x61d3495a(int i, int i2, PhotoItem photoItem, int i3, int i4) {
        this.logText.setText("선택한 폴더 읽는 중: " + i + " / " + i2 + "개");
        updateProgress("위치 정보 분석 중", i3, i4, photoItem.noLocation ? "위치 정보 없음" : albumCandidateTitle(albumCandidateGroupKey(photoItem)));
    }

    /* renamed from: lambda$loadSourceImages$26$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m29xb8f13a39(Exception exc) {
        this.logText.setText("선택한 폴더 읽기 실패: " + exc.getMessage());
    }

    private void loadSourceVideos(List<PhotoItem> list, List<AlbumFolder> list2, List<String> list3) throws Throwable {
        Cursor cursor;
        int i;
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        try {
            Cursor cursorQuery = contentResolver.query(uri, new String[]{"_id", "_display_name", "mime_type", "date_modified", "date_added", "datetaken", "latitude", "longitude"}, visibleMediaSelection(buildRelativePathSelection("relative_path", list3)), (String[]) list3.toArray(new String[0]), "date_modified DESC");
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
                int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow("_display_name");
                int columnIndexOrThrow3 = cursorQuery.getColumnIndexOrThrow("mime_type");
                int columnIndexOrThrow4 = cursorQuery.getColumnIndexOrThrow("date_modified");
                int columnIndex = cursorQuery.getColumnIndex("date_added");
                int columnIndex2 = cursorQuery.getColumnIndex("datetaken");
                int columnIndex3 = cursorQuery.getColumnIndex("latitude");
                int columnIndex4 = cursorQuery.getColumnIndex("longitude");
                int count = cursorQuery.getCount();
                int i2 = 0;
                while (cursorQuery.moveToNext()) {
                    if (this.cancelRequested) {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                            return;
                        }
                        return;
                    }
                    Uri uriWithAppendedPath = Uri.withAppendedPath(uri, String.valueOf(cursorQuery.getLong(columnIndexOrThrow)));
                    String strSafeName = safeName(cursorQuery.getString(columnIndexOrThrow2));
                    cursor = cursorQuery;
                    int i3 = columnIndex4;
                    int i4 = count;
                    int i5 = columnIndexOrThrow2;
                    int i6 = columnIndexOrThrow3;
                    int i7 = columnIndexOrThrow4;
                    int i8 = columnIndex;
                    int i9 = columnIndex2;
                    try {
                        final PhotoItem photoItemBuildPhotoItem = buildPhotoItem(uriWithAppendedPath, strSafeName, cursorQuery.getString(columnIndexOrThrow3), readLocation(uriWithAppendedPath, strSafeName, cursorQuery.getLong(columnIndexOrThrow4), readOptionalLong(cursorQuery, columnIndex), readOptionalLong(cursorQuery, columnIndex2), readOptionalDouble(cursorQuery, columnIndex3), readOptionalDouble(cursorQuery, columnIndex4), true), list2, true);
                        list.add(photoItemBuildPhotoItem);
                        final int i10 = i2 + 1;
                        if (i10 == 1 || i10 % 10 == 0) {
                            i = i4;
                        } else {
                            i = i4;
                            if (i10 != i) {
                                i2 = i10;
                                count = i;
                                columnIndex4 = i3;
                                columnIndexOrThrow2 = i5;
                                columnIndexOrThrow3 = i6;
                                columnIndexOrThrow4 = i7;
                                columnIndex = i8;
                                columnIndex2 = i9;
                                cursorQuery = cursor;
                            }
                        }
                        final int iMax = Math.max(i, i10);
                        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda35
                            @Override // java.lang.Runnable
                            public final void run() {
                                MainActivity.this.m30xbdd20bf8(i10, iMax, photoItemBuildPhotoItem);
                            }
                        });
                        i2 = i10;
                        count = i;
                        columnIndex4 = i3;
                        columnIndexOrThrow2 = i5;
                        columnIndexOrThrow3 = i6;
                        columnIndexOrThrow4 = i7;
                        columnIndex = i8;
                        columnIndex2 = i9;
                        cursorQuery = cursor;
                    } catch (Throwable th) {
                        th = th;
                        Throwable th2 = th;
                        if (cursor == null) {
                            throw th2;
                        }
                        try {
                            cursor.close();
                            throw th2;
                        } catch (Throwable th3) {
                            th2.addSuppressed(th3);
                            throw th2;
                        }
                    }
                }
                Cursor cursor2 = cursorQuery;
                if (cursor2 != null) {
                    cursor2.close();
                }
            } catch (Throwable th4) {
                throw new RuntimeException(th4);
            }
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda36
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m31x14effcd7(e);
                }
            });
        }
    }

    /* renamed from: lambda$loadSourceVideos$27$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m30xbdd20bf8(int i, int i2, PhotoItem photoItem) {
        this.logText.setText("동영상 위치 정보 확인 중: " + i + " / " + i2);
        updateProgress("위치 정보 분석 중", i, i2, photoItem.noLocation ? "위치 정보 없음" : albumCandidateTitle(albumCandidateGroupKey(photoItem)));
    }

    /* renamed from: lambda$loadSourceVideos$28$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m31x14effcd7(Exception exc) {
        this.logText.setText("동영상 읽기 실패: " + exc.getMessage());
    }

    private String buildRelativePathSelection(String str, List<String> list) {
        StringBuilder sb = new StringBuilder(str);
        sb.append(" IN (");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private List<String> getSelectedSourcePaths() {
        Set<String> stringSet = getSharedPreferences(PREFS_NAME, 0).getStringSet(PREF_SOURCE_PATHS, null);
        ArrayList arrayList = new ArrayList();
        if (stringSet != null) {
            for (String str : stringSet) {
                if (str != null && !str.trim().isEmpty() && !isExcludedSourcePath(str)) {
                    arrayList.add(str);
                }
            }
        }
        if (arrayList.isEmpty()) {
            arrayList.add(CAMERA_PATH);
        }
        return arrayList;
    }

    private void saveSelectedSourcePaths(List<String> list) {
        HashSet hashSet = new HashSet();
        for (String str : list) {
            if (str != null && !str.trim().isEmpty() && !isExcludedSourcePath(str)) {
                hashSet.add(str);
            }
        }
        if (hashSet.isEmpty()) {
            hashSet.add(CAMERA_PATH);
        }
        getSharedPreferences(PREFS_NAME, 0).edit().putStringSet(PREF_SOURCE_PATHS, hashSet).apply();
        updateSourceFoldersText();
    }

    private boolean shouldMoveVideos() {
        return getSharedPreferences(PREFS_NAME, 0).getBoolean(PREF_MOVE_VIDEOS, true);
    }

    private void setMoveVideos(boolean z) {
        getSharedPreferences(PREFS_NAME, 0).edit().putBoolean(PREF_MOVE_VIDEOS, z).apply();
        this.videoWritePermissionGranted = false;
    }

    private void updateSourceFoldersText() {
        if (this.sourceFoldersText == null) {
            return;
        }
        List<String> selectedSourcePaths = getSelectedSourcePaths();
        if (selectedSourcePaths.size() == 1) {
            this.sourceFoldersText.setText(displaySourcePath(selectedSourcePaths.get(0)));
        } else {
            this.sourceFoldersText.setText(displaySourcePath(selectedSourcePaths.get(0)) + " 외 " + (selectedSourcePaths.size() - 1) + "개");
        }
    }

    private String displaySourcePath(String str) {
        if (CAMERA_PATH.equals(str)) {
            return "카메라";
        }
        if (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        int iLastIndexOf = str.lastIndexOf(47);
        return iLastIndexOf >= 0 ? str.substring(iLastIndexOf + 1) : str;
    }

    private boolean isExcludedSourcePath(String str) {
        String strLastFolderName;
        if (str == null) {
            return true;
        }
        String strTrim = str.trim();
        if (strTrim.isEmpty() || (strLastFolderName = lastFolderName(strTrim)) == null || strLastFolderName.trim().isEmpty()) {
            return true;
        }
        return strLastFolderName.endsWith("에서") && strTrim.startsWith(new StringBuilder().append(Environment.DIRECTORY_PICTURES).append("/").toString());
    }

    private void showSourceFolderDialog() {
        if (this.isWorking) {
            showToast("작업 중에는 폴더를 바꿀 수 없어요.");
        } else {
            this.logText.setText("사진/동영상 폴더를 불러오는 중입니다.");
            this.worker.execute(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda27
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m63x3ed27e75();
                }
            });
        }
    }

    /* renamed from: lambda$showSourceFolderDialog$30$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m63x3ed27e75() {
        final List<SourceFolder> listLoadSourceFolders = loadSourceFolders();
        runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda62
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m62xc23fcb4b(listLoadSourceFolders);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r12v5, types: [android.widget.LinearLayout] */
    /* JADX WARN: Type inference failed for: r12v8 */
    /* JADX WARN: Type inference failed for: r12v9 */
    /* JADX WARN: Type inference failed for: r17v0, types: [android.content.Context, com.example.gallerysorter.MainActivity] */
    /* JADX WARN: Type inference failed for: r2v2, types: [android.app.Dialog] */
    /* JADX WARN: Type inference failed for: r5v2, types: [android.view.View, android.widget.LinearLayout] */
    /* JADX WARN: Type inference failed for: r6v1 */
    /* JADX WARN: Type inference failed for: r6v17 */
    /* JADX WARN: Type inference failed for: r6v2, types: [boolean, int] */
    /* JADX WARN: Type inference failed for: r8v11, types: [android.view.View, android.widget.LinearLayout] */
    /* JADX WARN: Type inference failed for: r9v27, types: [android.view.View, android.widget.LinearLayout] */
    /* renamed from: showSourceFolderDialog, reason: merged with bridge method [inline-methods] */
    public void m62xc23fcb4b(final List<SourceFolder> list) {
        if (list.isEmpty()) {
            showToast("선택할 사진/동영상 폴더를 찾지 못했어요.");
            this.logText.setText("사진/동영상 폴더를 찾지 못했습니다.");
            return;
        }
        List<String> selectedSourcePaths = getSelectedSourcePaths();
        final boolean[] zArr = new boolean[list.size()];
        int i = 0;
        for (int i2 = 0; i2 < list.size(); i2++) {
            zArr[i2] = selectedSourcePaths.contains(list.get(i2).relativePath);
        }
        final Dialog dialog = new Dialog(this);
        LinearLayout linearLayout = new LinearLayout(this);
        final boolean enabled = true;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(20), dp(18), dp(20), dp(14));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(24));
        linearLayout.setBackground(gradientDrawable);
        TextView textView = new TextView(this);
        textView.setText("분석할 폴더 선택");
        textView.setTextSize(20.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        linearLayout.addView(textView, matchWidthWithBottom(dp(6)));
        TextView textViewBodyText = bodyText("선택한 폴더의 사진/동영상에서 위치 정보를 찾아 장소별 앨범을 만들어요.");
        textViewBodyText.setTextSize(13.0f);
        textViewBodyText.setLineSpacing(dp(2), 1.0f);
        linearLayout.addView(textViewBodyText, matchWidthWithBottom(dp(14)));
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        scrollView.addView(linearLayout2);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, dp(420)));
        final TextView textView2 = new TextView(this);
        textView2.setTextSize(13.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-9609738);
        final Runnable runnable = new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.lambda$showSourceFolderDialog$31(list, zArr, textView2);
            }
        };
        LinearLayout obj = linearLayout2;
        for (int i3 = 0; i3 < list.size(); i3++) {
            final int folderIndex = i3;
            SourceFolder sourceFolder = list.get(folderIndex);
            LinearLayout linearLayout3 = new LinearLayout(this);
            linearLayout3.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout3.setGravity(16);
            linearLayout3.setPadding(dp(12), dp(10), dp(10), dp(10));
            linearLayout3.setClickable(enabled);
            linearLayout3.setFocusable(enabled);
            obj.addView(linearLayout3, matchWidthWithBottom(dp(8)));
            applyCardBackground(linearLayout3);
            final CheckBox checkBox = new CheckBox(this);
            checkBox.setChecked(zArr[folderIndex]);
            linearLayout3.addView(checkBox, squareParams(dp(42)));
            LinearLayout linearLayout4 = new LinearLayout(this);
            linearLayout4.setOrientation(LinearLayout.VERTICAL);
            linearLayout4.setPadding(dp(8), 0, 0, 0);
            linearLayout3.addView(linearLayout4, weightedParams(1));
            TextView textView3 = new TextView(this);
            textView3.setText(sourceFolder.displayName);
            textView3.setTextSize(15.0f);
            textView3.setTypeface(Typeface.DEFAULT_BOLD);
            textView3.setTextColor(-14735049);
            linearLayout4.addView(textView3);
            TextView textView4 = new TextView(this);
            textView4.setText(String.format(Locale.KOREA, "%,d", Integer.valueOf(sourceFolder.count)) + "개 항목");
            textView4.setTextSize(12.0f);
            textView4.setTextColor(-10193781);
            textView4.setPadding(0, dp(2), 0, 0);
            linearLayout4.addView(textView4);
            linearLayout3.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda22
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.lambda$showSourceFolderDialog$32(zArr, folderIndex, checkBox, runnable, view);
                }
            });
            checkBox.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda33
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.lambda$showSourceFolderDialog$33(zArr, folderIndex, checkBox, runnable, view);
                }
            });
        }
        LinearLayout linearLayout5 = new LinearLayout(this);
        linearLayout5.setOrientation(0);
        linearLayout5.setGravity(16);
        linearLayout5.setPadding(0, dp(8), 0, 0);
        linearLayout.addView(linearLayout5, matchWidth());
        linearLayout5.addView(textView2, weightedParams(1));
        Button button = new Button(this);
        button.setText("취소");
        button.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda44
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                dialog.dismiss();
            }
        });
        linearLayout5.addView(button, new LinearLayout.LayoutParams(dp(86), dp(48)));
        Button button2 = new Button(this);
        button2.setText("저장");
        button2.setTextColor(-1);
        applyGradientBackground(button2, -9609738, -8635667, dp(14));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(92), dp(48));
        layoutParams.setMargins(dp(8), 0, 0, 0);
        linearLayout5.addView(button2, layoutParams);
        button2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda55
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m64xf26832d0(list, zArr, dialog, view);
            }
        });
        runnable.run();
        dialog.setContentView(linearLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
        }
        dialog.show();
    }

    static /* synthetic */ void lambda$showSourceFolderDialog$31(List list, boolean[] zArr, TextView textView) {
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < list.size(); i3++) {
            if (zArr[i3]) {
                i++;
                i2 += ((SourceFolder) list.get(i3)).count;
            }
        }
        textView.setText("선택 " + i + "개 · 총 " + String.format(Locale.KOREA, "%,d", Integer.valueOf(i2)) + "개 항목");
    }

    static /* synthetic */ void lambda$showSourceFolderDialog$32(boolean[] zArr, int i, CheckBox checkBox, Runnable runnable, View view) {
        boolean z = !zArr[i];
        zArr[i] = z;
        checkBox.setChecked(z);
        runnable.run();
    }

    static /* synthetic */ void lambda$showSourceFolderDialog$33(boolean[] zArr, int i, CheckBox checkBox, Runnable runnable, View view) {
        zArr[i] = checkBox.isChecked();
        runnable.run();
    }

    /* renamed from: lambda$showSourceFolderDialog$35$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m64xf26832d0(List list, boolean[] zArr, Dialog dialog, View view) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            if (zArr[i]) {
                arrayList.add(((SourceFolder) list.get(i)).relativePath);
            }
        }
        saveSelectedSourcePaths(arrayList);
        this.copyCompletedMode = false;
        this.videoWritePermissionGranted = false;
        this.previewItems.clear();
        this.copiedOriginalUris.clear();
        this.copyButton.setEnabled(false);
        this.copyButton.setVisibility(8);
        this.deleteOriginalsButton.setEnabled(false);
        clearResultViews();
        setStatus("준비됨", "-", "-", "-");
        this.summaryText.setText("정리 시작을 누르면 선택한 폴더에서 앨범 후보를 찾아줘요.");
        dialog.dismiss();
    }

    private String folderIconKey(String str) {
        String lowerCase = str == null ? "" : str.toLowerCase(Locale.US);
        if (lowerCase.contains("camera")) {
            return "camera";
        }
        if (lowerCase.contains("download")) {
            return "download";
        }
        return "folder";
    }

    private List<SourceFolder> loadSourceFolders() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        accumulateSourceFolderCounts(linkedHashMap, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "relative_path", "date_modified");
        accumulateSourceFolderCounts(linkedHashMap, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "relative_path", "date_modified");
        if (!linkedHashMap.containsKey(CAMERA_PATH)) {
            linkedHashMap.put(CAMERA_PATH, 0);
        }
        ArrayList arrayList = new ArrayList();
        Integer numRemove = (Integer) linkedHashMap.remove(CAMERA_PATH);
        arrayList.add(new SourceFolder(CAMERA_PATH, displaySourcePath(CAMERA_PATH), numRemove != null ? numRemove.intValue() : 0));
        for (Object entryObj : linkedHashMap.entrySet()) {
            Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) entryObj;
            arrayList.add(new SourceFolder(entry.getKey(), displaySourcePath(entry.getKey()), entry.getValue().intValue()));
        }
        return arrayList;
    }

    private void accumulateSourceFolderCounts(Map<String, Integer> map, Uri uri, String str, String str2) {
        try {
            Cursor cursorQuery = getContentResolver().query(uri, new String[]{str}, visibleMediaSelection(null), null, str2 + " DESC");
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str);
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(columnIndexOrThrow);
                    if (string != null && !string.trim().isEmpty() && !isExcludedSourcePath(string)) {
                        Integer num = map.get(string);
                        map.put(string, Integer.valueOf(num == null ? 1 : num.intValue() + 1));
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } finally {
            }
        } catch (Exception unused) {
        }
    }

    private PhotoItem buildPhotoItem(Uri uri, String str, String str2, LocationResult locationResult, List<AlbumFolder> list, boolean z) {
        String str3;
        if (LOCATION_NONE.equals(locationResult.folderKey)) {
            return new PhotoItem(uri, str, str2, locationResult.takenAt, locationResult.folderKey, true, false, false, "", z);
        }
        AlbumFolder albumFolderFindMatchingAlbum = findMatchingAlbum(locationResult.folderKey, list);
        boolean z2 = albumFolderFindMatchingAlbum != null;
        if (z2) {
            str3 = albumFolderFindMatchingAlbum.relativePath;
        } else {
            str3 = Environment.DIRECTORY_PICTURES + "/" + locationResult.folderKey + "에서/";
        }
        String str4 = str3;
        return new PhotoItem(uri, str, str2, locationResult.takenAt, locationResult.folderKey, false, z2, hasDisplayNameInPath(str, str4, z), str4, z);
    }

    private long readOptionalLong(Cursor cursor, int i) {
        if (i < 0 || cursor.isNull(i)) {
            return 0L;
        }
        return cursor.getLong(i);
    }

    private Double readOptionalDouble(Cursor cursor, int i) {
        if (i < 0 || cursor.isNull(i)) {
            return null;
        }
        return Double.valueOf(cursor.getDouble(i));
    }

    private Date parseDateFromName(String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("((?:19|20)\\d{2})[-_.]?(0[1-9]|1[0-2])[-_.]?([0-2]\\d|3[01])(?:[_\\-. ]?([01]\\d|2[0-3])([0-5]\\d)([0-5]\\d)?)?").matcher(str);
        if (!matcher.find()) {
            return null;
        }
        try {
            int i = Integer.parseInt(matcher.group(1));
            int i2 = Integer.parseInt(matcher.group(2));
            int i3 = Integer.parseInt(matcher.group(3));
            int i4 = matcher.group(4) == null ? 12 : Integer.parseInt(matcher.group(4));
            int i5 = matcher.group(5) == null ? 0 : Integer.parseInt(matcher.group(5));
            int i6 = matcher.group(6) == null ? 0 : Integer.parseInt(matcher.group(6));
            Calendar calendar = Calendar.getInstance(Locale.KOREA);
            calendar.setLenient(false);
            calendar.set(1, i);
            calendar.set(2, i2 - 1);
            calendar.set(5, i3);
            calendar.set(11, i4);
            calendar.set(12, i5);
            calendar.set(13, i6);
            calendar.set(14, 0);
            return sanitizeTakenAt(calendar.getTime());
        } catch (Exception unused) {
            return null;
        }
    }

    private Date sanitizeTakenAt(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        calendar.setTime(date);
        int i = calendar.get(1);
        if (i < MIN_VALID_TAKEN_YEAR || i > MAX_VALID_TAKEN_YEAR) {
            return null;
        }
        return date;
    }

    private LocationResult readLocation(Uri uri, String name, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, Double mediaLatitude, Double mediaLongitude, boolean video) {
        Date fileNameTakenAt = sanitizeTakenAt(parseDateFromName(name));
        Date mediaTakenAt = sanitizeTakenAt(mediaTakenMillis > 0 ? new Date(mediaTakenMillis) : null);
        Date modifiedAt = sanitizeTakenAt(modifiedSeconds > 0 ? new Date(modifiedSeconds * 1000L) : null);
        Date addedAt = sanitizeTakenAt(addedSeconds > 0 ? new Date(addedSeconds * 1000L) : null);
        Date takenAt = null;
        String folderKey = LOCATION_NONE;

        if (video) {
            VideoMetadataResult videoResult = readVideoMetadata(uri);
            Date videoTakenAt = sanitizeTakenAt(videoResult.takenAt);
            if (videoTakenAt != null) {
                takenAt = videoTakenAt;
            }
            if (hasUsableCoordinates(videoResult.latitude, videoResult.longitude)) {
                folderKey = safeResolveLocationKey(videoResult.latitude, videoResult.longitude);
            }
        }

        Uri exifUri = uri;
        if (Build.VERSION.SDK_INT >= 29) {
            exifUri = MediaStore.setRequireOriginal(uri);
        }

        ExifReadResult exifResult = readExifData(exifUri);
        Date exifTakenAt = sanitizeTakenAt(exifResult.takenAt);
        if (exifTakenAt != null) {
            takenAt = exifTakenAt;
        }
        if (hasUsableCoordinates(exifResult.latitude, exifResult.longitude)) {
            folderKey = safeResolveLocationKey(exifResult.latitude, exifResult.longitude);
        }

        if (LOCATION_NONE.equals(folderKey) && !exifUri.equals(uri)) {
            ExifReadResult fallbackResult = readExifData(uri);
            Date fallbackTakenAt = sanitizeTakenAt(fallbackResult.takenAt);
            if (takenAt == null && fallbackTakenAt != null) {
                takenAt = fallbackTakenAt;
            }
            if (hasUsableCoordinates(fallbackResult.latitude, fallbackResult.longitude)) {
                folderKey = safeResolveLocationKey(fallbackResult.latitude, fallbackResult.longitude);
            }
        }

        if (LOCATION_NONE.equals(folderKey)
                && mediaLatitude != null
                && mediaLongitude != null
                && hasUsableCoordinates(mediaLatitude, mediaLongitude)) {
            folderKey = safeResolveLocationKey(mediaLatitude, mediaLongitude);
        }

        if (takenAt == null) {
            takenAt = mediaTakenAt;
        }
        if (takenAt == null) {
            takenAt = fileNameTakenAt;
        }
        if (takenAt == null) {
            takenAt = modifiedAt;
        }
        if (takenAt == null) {
            takenAt = addedAt;
        }

        return new LocationResult(sanitizeTakenAt(takenAt), folderKey);
    }

    private ExifReadResult readExifData(Uri uri) {
        ExifReadResult exifReadResult = new ExifReadResult();
        try {
            ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptorOpenFileDescriptor != null) {
                try {
                    readExifIntoResult(new ExifInterface(parcelFileDescriptorOpenFileDescriptor.getFileDescriptor()), exifReadResult);
                    if (hasUsableCoordinates(exifReadResult.latitude, exifReadResult.longitude)) {
                        if (parcelFileDescriptorOpenFileDescriptor != null) {
                            parcelFileDescriptorOpenFileDescriptor.close();
                        }
                        return exifReadResult;
                    }
                } finally {
                }
            }
            if (parcelFileDescriptorOpenFileDescriptor != null) {
                parcelFileDescriptorOpenFileDescriptor.close();
            }
        } catch (Exception unused) {
        }
        try {
            InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
            if (inputStreamOpenInputStream != null) {
                try {
                    readExifIntoResult(new ExifInterface(inputStreamOpenInputStream), exifReadResult);
                } finally {
                }
            }
            if (inputStreamOpenInputStream != null) {
                inputStreamOpenInputStream.close();
            }
        } catch (Exception unused2) {
        }
        return exifReadResult;
    }

    private void readExifIntoResult(ExifInterface exifInterface, ExifReadResult exifReadResult) throws Exception {
        String attribute = exifInterface.getAttribute("DateTimeOriginal");
        if (attribute == null) {
            attribute = exifInterface.getAttribute("DateTime");
        }
        if (attribute != null && exifReadResult.takenAt == null) {
            exifReadResult.takenAt = sanitizeTakenAt(new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(attribute));
        }
        float[] latLong = new float[2];
        if (exifInterface.getLatLong(latLong) && hasUsableCoordinates(latLong[0], latLong[1])) {
            exifReadResult.latitude = latLong[0];
            exifReadResult.longitude = latLong[1];
        }
    }

    private VideoMetadataResult readVideoMetadata(Uri uri) {
        VideoMetadataResult videoMetadataResult = new VideoMetadataResult();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(this, uri);
            double[] iso6709Location = parseIso6709Location(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION));
            if (iso6709Location != null) {
                videoMetadataResult.latitude = iso6709Location[0];
                videoMetadataResult.longitude = iso6709Location[1];
            }
            Date videoDate = parseVideoDate(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
            if (videoDate != null) {
                videoMetadataResult.takenAt = videoDate;
            }
        } catch (Exception unused) {
        } catch (Throwable th) {
            try {
                mediaMetadataRetriever.release();
            } catch (Exception unused2) {
            }
            throw th;
        }
        try {
            mediaMetadataRetriever.release();
        } catch (Exception unused3) {
        }
        return videoMetadataResult;
    }

    private double[] parseIso6709Location(String str) throws NumberFormatException {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        String strTrim = str.trim();
        if (strTrim.endsWith("/")) {
            strTrim = strTrim.substring(0, strTrim.length() - 1);
        }
        int i = 1;
        while (true) {
            if (i >= strTrim.length()) {
                i = -1;
                break;
            }
            char cCharAt = strTrim.charAt(i);
            if (cCharAt == '+' || cCharAt == '-') {
                break;
            }
            i++;
        }
        if (i <= 0 || i >= strTrim.length() - 1) {
            return null;
        }
        try {
            double d = Double.parseDouble(strTrim.substring(0, i));
            double d2 = Double.parseDouble(strTrim.substring(i));
            if (hasUsableCoordinates(d, d2)) {
                return new double[]{d, d2};
            }
            return null;
        } catch (Exception unused) {
            return null;
        }
    }

    private Date parseVideoDate(String str) {
        if (str != null && !str.trim().isEmpty()) {
            String strTrim = str.trim();
            String[] strArr = {"yyyyMMdd'T'HHmmss.SSS'Z'", "yyyyMMdd'T'HHmmss'Z'", "yyyy:MM:dd HH:mm:ss"};
            for (int i = 0; i < 3; i++) {
                try {
                    return this.sanitizeTakenAt(new SimpleDateFormat(strArr[i], Locale.US).parse(strTrim));
                } catch (Exception unused) {
                }
            }
        }
        return null;
    }

    private String resolveLocationKey(double d, double d2) throws IOException {
        String strRoundedLocationKey = roundedLocationKey(d, d2);
        String str = this.locationCache.get(strRoundedLocationKey);
        if (str != null) {
            return str;
        }
        String strResolveLocationKey = resolveLocationKey(d, d2, Locale.KOREA);
        if (LOCATION_NONE.equals(strResolveLocationKey)) {
            strResolveLocationKey = resolveLocationKey(d, d2, Locale.getDefault());
        }
        if (!LOCATION_NONE.equals(strResolveLocationKey)) {
            this.locationCache.put(strRoundedLocationKey, strResolveLocationKey);
        }
        return strResolveLocationKey;
    }

    private String safeResolveLocationKey(double latitude, double longitude) {
        try {
            return resolveLocationKey(latitude, longitude);
        } catch (Exception unused) {
            return LOCATION_NONE;
        }
    }

    private String resolveLocationKey(double d, double d2, Locale locale) throws IOException {
        try {
            List<Address> fromLocation = new Geocoder(this, locale).getFromLocation(d, d2, 1);
            if (fromLocation != null && !fromLocation.isEmpty()) {
                Address address = fromLocation.get(0);
                String strPreferredLocationName = preferredLocationName(address);
                if (strPreferredLocationName == null) {
                    strPreferredLocationName = address.getAddressLine(0);
                }
                if (strPreferredLocationName == null) {
                    return LOCATION_NONE;
                }
                return normalizeLocationKey(strPreferredLocationName);
            }
        } catch (Exception unused) {
        }
        return LOCATION_NONE;
    }

    private String preferredLocationName(Address address) {
        if (!isSeoulAddress(address)) {
            return firstNonEmpty(address.getLocality(), address.getSubAdminArea(), address.getAdminArea(), address.getCountryName());
        }
        String strCleanSeoulDetailName = cleanSeoulDetailName(address.getSubLocality(), address.getThoroughfare(), address.getFeatureName(), address.getAddressLine(0));
        if (strCleanSeoulDetailName != null) {
            return strCleanSeoulDetailName;
        }
        String strFirstDistrictName = firstDistrictName(address.getSubAdminArea(), address.getSubLocality(), address.getLocality(), address.getAddressLine(0));
        return strFirstDistrictName != null ? strFirstDistrictName : "서울";
    }

    private boolean isSeoulAddress(Address address) {
        return normalizeForMatch(address.getAdminArea()).contains("서울") || normalizeForMatch(address.getLocality()).contains("서울") || normalizeForMatch(address.getAddressLine(0)).contains("서울");
    }

    private String firstDistrictName(String... strArr) {
        for (String str : strArr) {
            String strExtractDistrictName = extractDistrictName(str);
            if (strExtractDistrictName != null) {
                return strExtractDistrictName;
            }
        }
        return null;
    }

    private String extractDistrictName(String str) {
        if (str != null && !str.trim().isEmpty()) {
            String strNormalizeForMatch = normalizeForMatch(str);
            for (String str2 : SEOUL_DISTRICTS) {
                if (strNormalizeForMatch.contains(normalizeForMatch(str2))) {
                    return str2;
                }
            }
            for (String str3 : str.trim().split("[\\s,]+")) {
                String strSafeFolderName = safeFolderName(str3);
                if (strSafeFolderName.endsWith("구") && strSafeFolderName.length() >= 2) {
                    return strSafeFolderName;
                }
            }
        }
        return null;
    }

    private String cleanSeoulDetailName(String... strArr) {
        for (String str : strArr) {
            String strCleanSeoulDetailName = cleanSeoulDetailName(str);
            if (strCleanSeoulDetailName != null) {
                return strCleanSeoulDetailName;
            }
        }
        return null;
    }

    private String cleanSeoulDetailName(String str) {
        if (str != null && !str.trim().isEmpty()) {
            String strReplace = str.trim().replace("대한민국", "").replace("서울특별시", "").replace("서울시", "").replace("서울", "");
            for (String str2 : SEOUL_DISTRICTS) {
                strReplace = strReplace.replace(str2, "");
            }
            String strTrim = strReplace.replaceAll("\\d+[\\-\\d]*", "").replaceAll("(대로|로|길)$", "").replaceAll("[,()\\[\\]]", " ").replaceAll("\\s+", " ").trim();
            if (normalizeForMatch(strTrim).contains("예술의전당")) {
                return "예술의전당";
            }
            if (strTrim.length() >= 2 && !isNoisySeoulDetailName(strTrim)) {
                return strTrim;
            }
        }
        return null;
    }

    private boolean isNoisySeoulDetailName(String str) {
        String strNormalizeForMatch = normalizeForMatch(str);
        return strNormalizeForMatch.contains("mall") || strNormalizeForMatch.contains("센터") || strNormalizeForMatch.contains("건물") || strNormalizeForMatch.contains("층") || strNormalizeForMatch.contains("대로") || strNormalizeForMatch.contains("로") || strNormalizeForMatch.contains("길") || strNormalizeForMatch.contains("어린이집") || strNormalizeForMatch.contains("아파트") || strNormalizeForMatch.contains("오피스텔") || strNormalizeForMatch.contains("상가") || strNormalizeForMatch.contains("b1") || strNormalizeForMatch.contains("b2") || strNormalizeForMatch.contains("bf") || strNormalizeForMatch.matches(".*\\d+f.*") || strNormalizeForMatch.matches(".*\\d+호.*");
    }

    private String roundedLocationKey(double d, double d2) {
        return (Math.round(d * 1000.0d) / 1000.0d) + "," + (Math.round(d2 * 1000.0d) / 1000.0d);
    }

    private String normalizeLocationKey(String str) {
        String strSafeFolderName = safeFolderName(stripAdministrativeSuffix(str.trim().replace("대한민국", "").replace("특별자치도", "").replace("특별자치시", "").replace("특별시", "").replace("광역시", "").replace("자치시", "")).replaceAll("\\s+", ""));
        return strSafeFolderName.isEmpty() ? LOCATION_NONE : strSafeFolderName;
    }

    private String stripAdministrativeSuffix(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return strTrim.length() <= 2 ? strTrim : (!strTrim.endsWith("구") || strTrim.length() > 3) ? strTrim.replaceAll("(시|군|구)$", "") : strTrim;
    }

    private List<AlbumFolder> loadAlbumFolders() {
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        addAlbumFolders(arrayList, hashSet, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "relative_path");
        addAlbumFolders(arrayList, hashSet, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "relative_path");
        return arrayList;
    }

    private void addAlbumFolders(List<AlbumFolder> list, Set<String> set, Uri uri, String str) {
        try {
            Cursor cursorQuery = getContentResolver().query(uri, new String[]{str}, visibleMediaSelection(null), null, null);
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str);
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(columnIndexOrThrow);
                    if (string != null && !set.contains(string) && !isIgnoredAlbumPath(string)) {
                        set.add(string);
                        String strLastFolderName = lastFolderName(string);
                        list.add(new AlbumFolder(string, strLastFolderName, normalizeForMatch(strLastFolderName)));
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } finally {
            }
        } catch (Exception unused) {
        }
    }

    private boolean isIgnoredAlbumPath(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        return CAMERA_PATH.equals(str) || lowerCase.contains("/screenshots/") || lowerCase.contains("/screenshot/") || lowerCase.contains("/kakaotalk/") || lowerCase.contains("/instagram/") || lowerCase.contains("/photogrid/") || lowerCase.contains("/download/") || str.startsWith("Pictures/사진정리");
    }

    private AlbumFolder findMatchingAlbum(String str, List<AlbumFolder> list) {
        String strNormalizeForMatch = normalizeForMatch(str);
        AlbumFolder albumFolder = null;
        int i = -1;
        for (AlbumFolder albumFolder2 : list) {
            if (albumFolder2.matchName.contains(strNormalizeForMatch)) {
                int i2 = (albumFolder2.matchName.equals(new StringBuilder().append(strNormalizeForMatch).append("에서").toString()) || albumFolder2.matchName.equals(strNormalizeForMatch)) ? 100 : 10;
                if (albumFolder2.matchName.startsWith(strNormalizeForMatch)) {
                    i2 += 20;
                }
                if (i2 > i) {
                    albumFolder = albumFolder2;
                    i = i2;
                }
            }
        }
        return albumFolder;
    }

    private boolean hasDisplayNameInPath(String str, String str2, boolean z) {
        Cursor cursorQuery;
        String strDuplicateFileSignature = "";
        ContentResolver contentResolver = getContentResolver();
        Uri uri = z ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try {
            cursorQuery = contentResolver.query(uri, new String[]{"_id"}, visibleMediaSelection("relative_path=? AND _display_name=?"), new String[]{str2, str}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return true;
                    }
                } finally {
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            strDuplicateFileSignature = duplicateFileSignature(str);
        } catch (Exception unused) {
        }
        if (strDuplicateFileSignature.isEmpty()) {
            return false;
        }
        cursorQuery = contentResolver.query(uri, new String[]{"_display_name"}, "relative_path".concat("=?"), new String[]{str2}, null);
        if (cursorQuery == null) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return false;
        }
        try {
            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_display_name");
            while (cursorQuery.moveToNext()) {
                if (strDuplicateFileSignature.equals(duplicateFileSignature(cursorQuery.getString(columnIndexOrThrow)))) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return true;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return false;
        } finally {
        }
    }

    private String duplicateFileSignature(String str) {
        String lowerCase = safeName(str).trim().toLowerCase(Locale.US);
        int iLastIndexOf = lowerCase.lastIndexOf(46);
        return (iLastIndexOf > 0 ? lowerCase.substring(0, iLastIndexOf) : lowerCase).replaceAll("\\s*\\(\\d+\\)$", "") + (iLastIndexOf > 0 ? lowerCase.substring(iLastIndexOf) : "");
    }

    private Uri copyMediaItem(PhotoItem photoItem) throws Exception {
        if (photoItem.video) {
            return moveVideoItem(photoItem);
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("_display_name", safeName(photoItem.name));
        contentValues.put("mime_type", photoItem.mimeType == null ? "image/jpeg" : photoItem.mimeType);
        contentValues.put("relative_path", photoItem.targetRelativePath);
        putMediaOwner(contentValues);
        putMediaDates(contentValues, photoItem.takenAt);
        contentValues.put("is_pending", (Integer) 1);
        ContentResolver contentResolver = getContentResolver();
        Uri uriInsert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uriInsert == null) {
            throw new IllegalStateException("복사 위치를 만들 수 없습니다.");
        }
        InputStream inputStreamOpenInputStream = contentResolver.openInputStream(photoItem.uri);
        try {
            OutputStream outputStreamOpenOutputStream = contentResolver.openOutputStream(uriInsert);
            try {
                if (inputStreamOpenInputStream == null || outputStreamOpenOutputStream == null) {
                    throw new IllegalStateException("사진을 열 수 없습니다.");
                }
                byte[] bArr = new byte[65536];
                while (true) {
                    int i = inputStreamOpenInputStream.read(bArr);
                    if (i == -1) {
                        break;
                    }
                    outputStreamOpenOutputStream.write(bArr, 0, i);
                }
                if (outputStreamOpenOutputStream != null) {
                    outputStreamOpenOutputStream.close();
                }
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put("is_pending", (Integer) 0);
                putMediaOwner(contentValues2);
                putMediaDates(contentValues2, photoItem.takenAt);
                contentResolver.update(uriInsert, contentValues2, null, null);
                setCopiedFileModifiedTime(uriInsert, photoItem.takenAt);
                return uriInsert;
            } finally {
            }
        } catch (Throwable th) {
            if (inputStreamOpenInputStream != null) {
                try {
                    inputStreamOpenInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private Uri moveVideoItem(PhotoItem photoItem) throws Exception {
        ContentValues contentValues = new ContentValues();
        contentValues.put("relative_path", photoItem.targetRelativePath);
        contentValues.put("_display_name", safeName(photoItem.name));
        putMediaOwner(contentValues);
        if (photoItem.mimeType != null) {
            contentValues.put("mime_type", photoItem.mimeType);
        }
        if (getContentResolver().update(photoItem.uri, contentValues, null, null) <= 0) {
            throw new IllegalStateException("동영상을 이동할 수 없습니다.");
        }
        return photoItem.uri;
    }

    private void putMediaDates(ContentValues contentValues, Date date) {
        Date dateSanitizeTakenAt = sanitizeTakenAt(date);
        if (dateSanitizeTakenAt == null) {
            return;
        }
        long time = dateSanitizeTakenAt.getTime();
        contentValues.put("datetaken", Long.valueOf(time));
        long j = time / 1000;
        contentValues.put("date_added", Long.valueOf(j));
        contentValues.put("date_modified", Long.valueOf(j));
    }

    /* JADX WARN: Removed duplicated region for block: B:22:0x003e  */
    private void setCopiedFileModifiedTime(Uri uri, Date takenAt) {
        Date safeDate = sanitizeTakenAt(takenAt);
        if (uri == null || safeDate == null) {
            return;
        }
        String path = null;
        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                if (dataIndex >= 0) {
                    path = cursor.getString(dataIndex);
                }
            }
        } catch (Exception ignored) {
        }

        if (path == null || path.trim().isEmpty()) {
            path = resolveFilePathFromMediaUri(uri);
        }
        if (path == null || path.trim().isEmpty()) {
            return;
        }

        try {
            File file = new File(path);
            if (file.exists()) {
                file.setLastModified(safeDate.getTime());
                MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
            }
        } catch (Exception ignored) {
        }
    }

    private String resolveFilePathFromMediaUri(Uri uri) {
        String lastPathSegment = uri == null ? null : uri.getLastPathSegment();
        if (lastPathSegment != null && !lastPathSegment.trim().isEmpty()) {
            try {
                Cursor cursorQuery = getContentResolver().query(uri, new String[]{"relative_path", "_display_name"}, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            int columnIndex = cursorQuery.getColumnIndex("relative_path");
                            int columnIndex2 = cursorQuery.getColumnIndex("_display_name");
                            if (columnIndex >= 0 && columnIndex2 >= 0) {
                                String string = cursorQuery.getString(columnIndex);
                                String string2 = cursorQuery.getString(columnIndex2);
                                if (string != null && string2 != null) {
                                    String absolutePath = new File(Environment.getExternalStorageDirectory(), string + string2).getAbsolutePath();
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    return absolutePath;
                                }
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return null;
                            }
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return null;
                        }
                    } finally {
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Exception unused) {
            }
        }
        return null;
    }

    private void updateMediaDates(Uri uri, Date date, boolean z) {
        if (System.currentTimeMillis() >= 0) {
            return;
        }
        Date dateSanitizeTakenAt = sanitizeTakenAt(date);
        if (uri == null || dateSanitizeTakenAt == null) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        long time = dateSanitizeTakenAt.getTime();
        if (z) {
            contentValues.put("datetaken", Long.valueOf(time));
        } else {
            contentValues.put("datetaken", Long.valueOf(time));
        }
        putMediaOwner(contentValues);
        try {
            getContentResolver().update(uri, contentValues, null, null);
        } catch (Exception unused) {
        }
    }

    private void putMediaOwner(ContentValues contentValues) {
        contentValues.putNull("owner_package_name");
    }

    private void clearMediaOwnerPackage(Uri uri) {
        if (System.currentTimeMillis() < 0 && uri != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.putNull("owner_package_name");
            try {
                getContentResolver().update(uri, contentValues, null, null);
            } catch (Exception unused) {
            }
        }
    }

    private void deleteCopiedOriginals() {
        if (this.copiedOriginalUris.isEmpty()) {
            showToast("휴지통으로 보낼 사진 원본이 없습니다.");
        } else {
            new AlertDialog.Builder(this).setTitle("복사된 사진 원본 휴지통 이동").setMessage("앨범으로 복사된 카메라 사진 원본 " + this.copiedOriginalUris.size() + "개를 휴지통으로 보냅니다. 휴지통에서 복구할 수 있어요. 동영상은 정리할 때 이동되므로 여기서 따로 처리하지 않습니다.").setNegativeButton("취소", (DialogInterface.OnClickListener) null).setPositiveButton("휴지통 이동", new DialogInterface.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda5
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.m26x20d84b9a(dialogInterface, i);
                }
            }).show();
        }
    }

    /* renamed from: lambda$deleteCopiedOriginals$36$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m26x20d84b9a(DialogInterface dialogInterface, int i) {
        requestTrashCopiedOriginals();
    }

    private void requestTrashCopiedOriginals() {
        if (this.copiedOriginalUris.isEmpty()) {
            showToast("휴지통으로 보낼 사진 원본이 없습니다.");
            return;
        }
        this.pendingTrashOriginalUris.clear();
        this.pendingTrashOriginalUris.addAll(this.copiedOriginalUris);
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                startIntentSenderForResult(MediaStore.createTrashRequest(getContentResolver(), new ArrayList(this.pendingTrashOriginalUris), true).getIntentSender(), REQUEST_DELETE_ORIGINALS, null, 0, 0, 0);
            } else {
                showToast("이 Android 버전에서는 휴지통 이동을 지원하지 않습니다.");
            }
        } catch (IntentSender.SendIntentException unused) {
            showToast("휴지통 이동 확인창을 열 수 없습니다.");
        } catch (Exception e) {
            showToast("휴지통 이동 실패: " + e.getMessage());
        }
    }

    private void applyOriginalTrashState(String str) {
        this.pendingTrashOriginalUris.clear();
        this.copiedOriginalUris.clear();
        this.originalsTrashCompleted = true;
        this.copyCompletedMode = false;
        this.resultScreenMode = false;
        this.previewItems.clear();
        this.recentlySortedUriKeys.clear();
        showToast(str);
        returnToMainScreen();
    }

    private int reinforceTrashState(List<Uri> list) {
        int iUpdate = 0;
        if (System.currentTimeMillis() >= 0) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 30 && list != null && !list.isEmpty()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("is_trashed", (Integer) 1);
            for (Uri uri : list) {
                if (uri != null) {
                    try {
                        iUpdate += getContentResolver().update(uri, contentValues, null, null);
                    } catch (Exception unused) {
                    }
                }
            }
        }
        return iUpdate;
    }

    private void removeItemsByUri(List<Uri> list) {
        if (list == null || list.isEmpty() || this.previewItems.isEmpty()) {
            return;
        }
        HashSet hashSet = new HashSet();
        for (Uri uri : list) {
            if (uri != null) {
                hashSet.add(uri.toString());
            }
        }
        if (hashSet.isEmpty()) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (PhotoItem photoItem : this.previewItems) {
            if (!hashSet.contains(photoItem.uri.toString())) {
                arrayList.add(photoItem);
            }
        }
        this.previewItems.clear();
        this.previewItems.addAll(arrayList);
    }

    private void markItemsAsSorted(List<Uri> list) {
        if (list == null || list.isEmpty() || this.previewItems.isEmpty()) {
            return;
        }
        HashSet hashSet = new HashSet();
        for (Uri uri : list) {
            if (uri != null) {
                hashSet.add(uri.toString());
            }
        }
        if (hashSet.isEmpty()) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (PhotoItem photoItem : this.previewItems) {
            if (hashSet.contains(photoItem.uri.toString())) {
                arrayList.add(new PhotoItem(photoItem.uri, photoItem.name, photoItem.mimeType, photoItem.takenAt, photoItem.locationKey, photoItem.noLocation, true, true, photoItem.targetRelativePath, photoItem.video));
            } else {
                arrayList.add(photoItem);
            }
        }
        this.previewItems.clear();
        this.previewItems.addAll(arrayList);
    }

    private void rememberRecentlySortedItems(List<Uri> list) {
        this.recentlySortedUriKeys.clear();
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Uri uri : list) {
            if (uri != null) {
                this.recentlySortedUriKeys.add(uri.toString());
            }
        }
    }

    private boolean wasRecentlySorted(PhotoItem photoItem) {
        return photoItem != null && this.recentlySortedUriKeys.contains(photoItem.uri.toString());
    }

    private void rebuildAlbumSummaryHistoryFromExistingAlbums() {
        if (this.isWorking) {
            showToast("작업 중에는 기록을 재생성할 수 없어요.");
        } else {
            setWorking(true, "정리된 앨범 기록을 다시 만드는 중...");
            this.worker.execute(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m36xd2bb4ed9();
                }
            });
        }
    }

    /* renamed from: lambda$rebuildAlbumSummaryHistoryFromExistingAlbums$39$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m36xd2bb4ed9() {
        try {
            final Map<String, AlbumSummary> mapCollectExistingAlbumSummaries = collectExistingAlbumSummaries();
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda42
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m34x247f6d1b(mapCollectExistingAlbumSummaries);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda43
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m35x7b9d5dfa(e);
                }
            });
        }
    }

    /* renamed from: lambda$rebuildAlbumSummaryHistoryFromExistingAlbums$37$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m34x247f6d1b(Map map) {
        setWorking(false, null);
        if (map.isEmpty()) {
            showToast("재생성할 정리 앨범을 찾지 못했어요.");
            return;
        }
        try {
            writeRebuiltAlbumSummaryHistory(map);
            showToast("최근 발견 장소를 다시 만들었어요.");
            buildUi();
            ensureReadPermission();
            restoreMainUiFromState();
        } catch (Exception e) {
            showToast("기록 재생성 실패: " + e.getMessage());
        }
    }

    /* renamed from: lambda$rebuildAlbumSummaryHistoryFromExistingAlbums$38$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m35x7b9d5dfa(Exception exc) {
        setWorking(false, null);
        showToast("기록 재생성 실패: " + exc.getMessage());
    }

    private Map<String, AlbumSummary> collectExistingAlbumSummaries() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        collectExistingAlbumSummaries(linkedHashMap, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "_id", "_display_name", "relative_path", "datetaken", "date_added", "date_modified", true, false);
        collectExistingAlbumSummaries(linkedHashMap, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "_id", "_display_name", "relative_path", "datetaken", "date_added", "date_modified", false, true);
        return linkedHashMap;
    }

    private void collectExistingAlbumSummaries(Map<String, AlbumSummary> map, Uri uri, String str, String str2, String str3, String str4, String str5, String str6, boolean z, boolean z2) {
        MainActivity mainActivity = this;
        boolean z3 = true;
        try {
            Cursor cursorQuery = getContentResolver().query(uri, new String[]{str, str2, str3, str4, str5, str6}, mainActivity.visibleMediaSelection(str3 + " LIKE ?"), new String[]{"Pictures/%에서/"}, str3 + " ASC, " + str4 + " DESC, " + str6 + " DESC");
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str);
                int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow(str2);
                int columnIndexOrThrow3 = cursorQuery.getColumnIndexOrThrow(str3);
                int columnIndex = cursorQuery.getColumnIndex(str4);
                int columnIndex2 = cursorQuery.getColumnIndex(str5);
                int columnIndex3 = cursorQuery.getColumnIndex(str6);
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(columnIndexOrThrow3);
                    if (mainActivity.isGeneratedPlaceAlbumPath(string)) {
                        String strLastFolderName = mainActivity.lastFolderName(string);
                        AlbumSummary albumSummary = map.get(strLastFolderName);
                        Uri uriWithAppendedId = ContentUris.withAppendedId(uri, cursorQuery.getLong(columnIndexOrThrow));
                        if (albumSummary == null) {
                            albumSummary = new AlbumSummary(strLastFolderName, string, uriWithAppendedId.toString());
                            map.put(strLastFolderName, albumSummary);
                        }
                        AlbumSummary albumSummary2 = albumSummary;
                        boolean z4 = z3;
                        long albumMediaDateMillis = readAlbumMediaDateMillis(uriWithAppendedId, cursorQuery.getString(columnIndexOrThrow2), cursorQuery, columnIndex, columnIndex2, columnIndex3, z2);
                        albumSummary2.itemCount++;
                        if (albumMediaDateMillis > 0) {
                            albumSummary2.dateRange.include(new Date(albumMediaDateMillis));
                        }
                        if (albumSummary2.thumbnailUri == null || albumSummary2.thumbnailUri.isEmpty() || (z && albumMediaDateMillis >= albumSummary2.thumbnailDateMillis)) {
                            albumSummary2.thumbnailUri = uriWithAppendedId.toString();
                            albumSummary2.thumbnailDateMillis = albumMediaDateMillis;
                        }
                        mainActivity = this;
                        z3 = z4;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } finally {
            }
        } catch (Exception unused) {
        }
    }

    private boolean isGeneratedPlaceAlbumPath(String str) {
        if (str == null || !str.startsWith(Environment.DIRECTORY_PICTURES + "/")) {
            return false;
        }
        String strLastFolderName = lastFolderName(str);
        return strLastFolderName.endsWith("에서") && !LOCATION_NONE.equals(strLastFolderName);
    }

    private long readMediaDateMillis(Cursor cursor, int i, int i2, int i3) {
        long optionalLong = readOptionalLong(cursor, i);
        if (optionalLong > 0) {
            return optionalLong;
        }
        long optionalLong2 = readOptionalLong(cursor, i3);
        if (optionalLong2 > 0) {
            return optionalLong2 * 1000;
        }
        long optionalLong3 = readOptionalLong(cursor, i2);
        if (optionalLong3 > 0) {
            return optionalLong3 * 1000;
        }
        return 0L;
    }

    private void writeRebuiltAlbumSummaryHistory(Map<String, AlbumSummary> map) throws Exception {
        long jCurrentTimeMillis = System.currentTimeMillis();
        JSONObject jSONObject = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(buildAlbumSummarySessionJson(jCurrentTimeMillis, countAlbumSummaryItems(map), 0, 0, map));
        jSONObject.put("schemaVersion", 1);
        jSONObject.put("rebuiltFromExistingAlbums", true);
        jSONObject.put("updatedAt", formatTimestamp(jCurrentTimeMillis));
        jSONObject.put("updatedAtMillis", jCurrentTimeMillis);
        jSONObject.put("sessions", jSONArray);
        writeAlbumSummaryHistoryRoot(jSONObject);
    }

    private int countAlbumSummaryItems(Map<String, AlbumSummary> map) {
        Iterator<AlbumSummary> it = map.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            i += it.next().itemCount;
        }
        return i;
    }

    private void saveAlbumSummaryHistory(List<PhotoItem> list, List<Uri> list2, int i, int i2, int i3) throws JSONException {
        if (list == null || list.isEmpty() || list2 == null || list2.isEmpty()) {
            return;
        }
        HashSet hashSet = new HashSet();
        for (Uri uri : list2) {
            if (uri != null) {
                hashSet.add(uri.toString());
            }
        }
        if (hashSet.isEmpty()) {
            return;
        }
        Map<String, AlbumSummary> linkedHashMap = new LinkedHashMap<>();
        for (PhotoItem photoItem : list) {
            if (photoItem != null && !photoItem.noLocation && hashSet.contains(photoItem.uri.toString())) {
                String strAlbumCandidateFolderName = albumCandidateFolderName(albumCandidateGroupKey(photoItem));
                AlbumSummary albumSummary = linkedHashMap.get(strAlbumCandidateFolderName);
                if (albumSummary == null) {
                    albumSummary = new AlbumSummary(strAlbumCandidateFolderName, photoItem.targetRelativePath, photoItem.uri.toString());
                    linkedHashMap.put(strAlbumCandidateFolderName, albumSummary);
                }
                albumSummary.itemCount++;
                albumSummary.dateRange.include(photoItem.takenAt);
            }
        }
        if (linkedHashMap.isEmpty()) {
            return;
        }
        try {
            long jCurrentTimeMillis = System.currentTimeMillis();
            JSONObject albumSummaryHistoryRoot = readAlbumSummaryHistoryRoot();
            JSONArray jSONArrayOptJSONArray = albumSummaryHistoryRoot.optJSONArray("sessions");
            JSONArray jSONArray = new JSONArray();
            jSONArray.put(buildAlbumSummarySessionJson(jCurrentTimeMillis, i, i2, i3, linkedHashMap));
            if (jSONArrayOptJSONArray != null) {
                int iMin = Math.min(jSONArrayOptJSONArray.length(), 19);
                for (int i4 = 0; i4 < iMin; i4++) {
                    jSONArray.put(jSONArrayOptJSONArray.getJSONObject(i4));
                }
            }
            albumSummaryHistoryRoot.put("schemaVersion", 1);
            albumSummaryHistoryRoot.put("updatedAt", formatTimestamp(jCurrentTimeMillis));
            albumSummaryHistoryRoot.put("updatedAtMillis", jCurrentTimeMillis);
            albumSummaryHistoryRoot.put("sessions", jSONArray);
            writeAlbumSummaryHistoryRoot(albumSummaryHistoryRoot);
        } catch (Exception e) {
            this.logText.setText("정리 기록 저장 실패: " + e.getMessage());
        }
    }

    private JSONObject buildAlbumSummarySessionJson(long j, int i, int i2, int i3, Map<String, AlbumSummary> map) throws JSONException {
        JSONArray jSONArray = new JSONArray();
        for (AlbumSummary albumSummary : map.values()) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("albumName", albumSummary.albumName);
            jSONObject.put("relativePath", albumSummary.relativePath);
            jSONObject.put("itemCount", albumSummary.itemCount);
            jSONObject.put("startDate", formatDateForJson(albumSummary.dateRange.start));
            jSONObject.put("endDate", formatDateForJson(albumSummary.dateRange.end));
            jSONObject.put("startDateMillis", albumSummary.dateRange.start == null ? JSONObject.NULL : Long.valueOf(albumSummary.dateRange.start.getTime()));
            jSONObject.put("endDateMillis", albumSummary.dateRange.end == null ? JSONObject.NULL : Long.valueOf(albumSummary.dateRange.end.getTime()));
            jSONObject.put("thumbnailUri", albumSummary.thumbnailUri);
            jSONObject.put("createdAt", formatTimestamp(j));
            jSONObject.put("createdAtMillis", j);
            jSONArray.put(jSONObject);
        }
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("createdAt", formatTimestamp(j));
        jSONObject2.put("createdAtMillis", j);
        jSONObject2.put("sortedItemCount", i);
        jSONObject2.put("skippedItemCount", i2);
        jSONObject2.put("failedItemCount", i3);
        jSONObject2.put("albumCount", map.size());
        jSONObject2.put("albums", jSONArray);
        return jSONObject2;
    }

    private JSONObject readAlbumSummaryHistoryRoot() {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream input = openFileInput(ALBUM_SUMMARY_HISTORY_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            if (sb.length() > 0) {
                return new JSONObject(sb.toString());
            }
        } catch (Exception unused) {
        }
        return new JSONObject();
    }

    private void writeAlbumSummaryHistoryRoot(JSONObject jSONObject) throws Exception {
        FileOutputStream fileOutputStreamOpenFileOutput = openFileOutput(ALBUM_SUMMARY_HISTORY_FILE, 0);
        try {
            fileOutputStreamOpenFileOutput.write(jSONObject.toString(2).getBytes(StandardCharsets.UTF_8));
            if (fileOutputStreamOpenFileOutput != null) {
                fileOutputStreamOpenFileOutput.close();
            }
        } catch (Throwable th) {
            if (fileOutputStreamOpenFileOutput != null) {
                try {
                    fileOutputStreamOpenFileOutput.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void refreshActivePlaceDetailAfterExternalChange() {
        StoredAlbumSummary storedAlbumSummary;
        if (!this.recentPlaceDetailMode || (storedAlbumSummary = this.activePlaceDetailSummary) == null || this.isWorking) {
            return;
        }
        StoredAlbumSummary storedAlbumSummaryFindLiveStoredAlbumSummary = findLiveStoredAlbumSummary(storedAlbumSummary);
        if (storedAlbumSummaryFindLiveStoredAlbumSummary == null || storedAlbumSummaryFindLiveStoredAlbumSummary.itemCount <= 0) {
            this.activePlaceDetailSummary = null;
            showToast("앨범이 비었어요. 목록을 새로고침했어요.");
            returnToRecentPlacesScreen();
        } else if (hasAlbumSummaryChanged(this.activePlaceDetailSummary, storedAlbumSummaryFindLiveStoredAlbumSummary)) {
            showRecentPlaceDetailScreen(storedAlbumSummaryFindLiveStoredAlbumSummary);
        }
    }

    private StoredAlbumSummary findLiveStoredAlbumSummary(StoredAlbumSummary storedAlbumSummary) {
        if (storedAlbumSummary == null) {
            return null;
        }
        Map<String, AlbumSummary> mapCollectExistingAlbumSummaries = collectExistingAlbumSummaries();
        long jCurrentTimeMillis = System.currentTimeMillis();
        for (AlbumSummary albumSummary : mapCollectExistingAlbumSummaries.values()) {
            if (albumSummary != null) {
                boolean z = (storedAlbumSummary.relativePath == null || albumSummary.relativePath == null || !storedAlbumSummary.relativePath.equals(albumSummary.relativePath)) ? false : true;
                boolean z2 = (storedAlbumSummary.albumName == null || albumSummary.albumName == null || !storedAlbumSummary.albumName.equals(albumSummary.albumName)) ? false : true;
                if (z || z2) {
                    return StoredAlbumSummary.fromAlbumSummary(albumSummary, jCurrentTimeMillis);
                }
            }
        }
        return null;
    }

    private boolean hasAlbumSummaryChanged(StoredAlbumSummary storedAlbumSummary, StoredAlbumSummary storedAlbumSummary2) {
        return (storedAlbumSummary == null || storedAlbumSummary2 == null) ? storedAlbumSummary != storedAlbumSummary2 : (storedAlbumSummary.itemCount == storedAlbumSummary2.itemCount && Objects.equals(storedAlbumSummary.startDate, storedAlbumSummary2.startDate) && Objects.equals(storedAlbumSummary.endDate, storedAlbumSummary2.endDate) && Objects.equals(storedAlbumSummary.thumbnailUri, storedAlbumSummary2.thumbnailUri) && Objects.equals(storedAlbumSummary.relativePath, storedAlbumSummary2.relativePath)) ? false : true;
    }

    private List<StoredAlbumSummary> loadRecentAlbumSummaries() {
        JSONArray jSONArrayOptJSONArray;
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        Map<String, AlbumSummary> mapCollectExistingAlbumSummaries = collectExistingAlbumSummaries();
        HashSet hashSet2 = new HashSet();
        HashSet hashSet3 = new HashSet();
        if (!mapCollectExistingAlbumSummaries.isEmpty()) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            for (AlbumSummary albumSummary : mapCollectExistingAlbumSummaries.values()) {
                if (albumSummary.albumName != null && !albumSummary.albumName.isEmpty()) {
                    hashSet2.add(albumSummary.albumName);
                }
                if (albumSummary.relativePath != null && !albumSummary.relativePath.isEmpty()) {
                    hashSet3.add(albumSummary.relativePath);
                }
                StoredAlbumSummary storedAlbumSummaryFromAlbumSummary = StoredAlbumSummary.fromAlbumSummary(albumSummary, jCurrentTimeMillis);
                if (!storedAlbumSummaryFromAlbumSummary.albumName.isEmpty() && !hashSet.contains(storedAlbumSummaryFromAlbumSummary.albumName)) {
                    hashSet.add(storedAlbumSummaryFromAlbumSummary.albumName);
                    arrayList.add(storedAlbumSummaryFromAlbumSummary);
                }
            }
        }
        JSONArray jSONArrayOptJSONArray2 = readAlbumSummaryHistoryRoot().optJSONArray("sessions");
        if (jSONArrayOptJSONArray2 != null) {
            for (int i = 0; i < jSONArrayOptJSONArray2.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray2.optJSONObject(i);
                if (jSONObjectOptJSONObject != null && (jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("albums")) != null) {
                    for (int i2 = 0; i2 < jSONArrayOptJSONArray.length(); i2++) {
                        JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray.optJSONObject(i2);
                        if (jSONObjectOptJSONObject2 != null) {
                            StoredAlbumSummary storedAlbumSummaryFromJson = StoredAlbumSummary.fromJson(jSONObjectOptJSONObject2);
                            if (!storedAlbumSummaryFromJson.albumName.isEmpty() && !hashSet.contains(storedAlbumSummaryFromJson.albumName) && (mapCollectExistingAlbumSummaries.isEmpty() || hashSet2.contains(storedAlbumSummaryFromJson.albumName) || hashSet3.contains(storedAlbumSummaryFromJson.relativePath))) {
                                hashSet.add(storedAlbumSummaryFromJson.albumName);
                                arrayList.add(storedAlbumSummaryFromJson);
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(arrayList, new Comparator() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda68
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return MainActivity.this.m27x4836e99e((MainActivity.StoredAlbumSummary) obj, (MainActivity.StoredAlbumSummary) obj2);
            }
        });
        return arrayList;
    }

    /* renamed from: lambda$loadRecentAlbumSummaries$40$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ int m27x4836e99e(StoredAlbumSummary storedAlbumSummary, StoredAlbumSummary storedAlbumSummary2) {
        int iCompareTo = comparableDate(storedAlbumSummary2.endDate).compareTo(comparableDate(storedAlbumSummary.endDate));
        if (iCompareTo != 0) {
            return iCompareTo;
        }
        int iCompareTo2 = comparableDate(storedAlbumSummary2.startDate).compareTo(comparableDate(storedAlbumSummary.startDate));
        return iCompareTo2 != 0 ? iCompareTo2 : Integer.compare(storedAlbumSummary2.itemCount, storedAlbumSummary.itemCount);
    }

    private String comparableDate(String str) {
        return str == null ? "" : str;
    }

    private String visibleMediaSelection(String str) {
        String strAppendSelection = appendSelection(str == null ? "" : str.trim(), "is_pending = 0");
        if (Build.VERSION.SDK_INT >= 30) {
            strAppendSelection = appendSelection(strAppendSelection, "is_trashed = 0");
        }
        if (strAppendSelection.isEmpty()) {
            return null;
        }
        return strAppendSelection;
    }

    private String appendSelection(String str, String str2) {
        return (str2 == null || str2.trim().isEmpty()) ? str == null ? "" : str : (str == null || str.trim().isEmpty()) ? str2 : "(" + str + ") AND (" + str2 + ")";
    }

    private String albumPreferenceKey(String str, StoredAlbumSummary storedAlbumSummary) {
        return str + Uri.encode(storedAlbumSummary != null ? firstNonEmpty(storedAlbumSummary.relativePath, storedAlbumSummary.albumName, "") : "");
    }

    private String albumMemory(StoredAlbumSummary storedAlbumSummary) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        String string = sharedPreferences.getString(albumPreferenceKey(PREF_ALBUM_MEMORY_PREFIX, storedAlbumSummary), "");
        return (string == null || string.trim().isEmpty()) ? sharedPreferences.getString(albumPreferenceKey(PREF_ALBUM_ALIAS_PREFIX, storedAlbumSummary), "") : string;
    }

    private void saveAlbumMemory(StoredAlbumSummary storedAlbumSummary, String str) {
        SharedPreferences.Editor editorEdit = getSharedPreferences(PREFS_NAME, 0).edit();
        String strAlbumPreferenceKey = albumPreferenceKey(PREF_ALBUM_ALIAS_PREFIX, storedAlbumSummary);
        String strAlbumPreferenceKey2 = albumPreferenceKey(PREF_ALBUM_MEMORY_PREFIX, storedAlbumSummary);
        String strTrim = str == null ? "" : str.trim();
        editorEdit.remove(strAlbumPreferenceKey);
        if (strTrim.isEmpty()) {
            editorEdit.remove(strAlbumPreferenceKey2);
        } else {
            editorEdit.putString(strAlbumPreferenceKey2, strTrim);
        }
        editorEdit.apply();
    }

    private void showAlbumMemoryEditor(final StoredAlbumSummary storedAlbumSummary) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(20), dp(8), dp(20), 0);
        linearLayout.addView(bodyText("원본 앨범: " + storedAlbumSummary.albumName), matchWidthWithBottom(dp(12)));
        TextView textViewBodyText = bodyText("기억 한 줄");
        textViewBodyText.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayout.addView(textViewBodyText, matchWidthWithBottom(dp(5)));
        final EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setMaxLines(1);
        editText.setText(albumMemory(storedAlbumSummary));
        editText.setHint("이 장소에 대한 기억을 남겨보세요");
        editText.setInputType(16385);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        linearLayout.addView(editText, matchWidth());
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle("기억 편집").setView(linearLayout).setNegativeButton("취소", (DialogInterface.OnClickListener) null).setNeutralButton("기억 삭제", (DialogInterface.OnClickListener) null).setPositiveButton("저장", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda70
            @Override // android.content.DialogInterface.OnShowListener
            public final void onShow(DialogInterface dialogInterface) {
                MainActivity.this.m50x55cae6eb(alertDialogCreate, storedAlbumSummary, editText, dialogInterface);
            }
        });
        alertDialogCreate.show();
    }

    /* renamed from: lambda$showAlbumMemoryEditor$43$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m50x55cae6eb(final AlertDialog alertDialog, final StoredAlbumSummary storedAlbumSummary, final EditText editText, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda29
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m48xa78f052d(storedAlbumSummary, editText, alertDialog, view);
            }
        });
        alertDialog.getButton(-3).setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda30
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m49xfeacf60c(storedAlbumSummary, alertDialog, view);
            }
        });
    }

    /* renamed from: lambda$showAlbumMemoryEditor$41$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m48xa78f052d(StoredAlbumSummary storedAlbumSummary, EditText editText, AlertDialog alertDialog, View view) {
        saveAlbumMemory(storedAlbumSummary, editText.getText().toString());
        alertDialog.dismiss();
        showRecentPlaceDetailScreen(storedAlbumSummary);
    }

    /* renamed from: lambda$showAlbumMemoryEditor$42$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m49xfeacf60c(StoredAlbumSummary storedAlbumSummary, AlertDialog alertDialog, View view) {
        saveAlbumMemory(storedAlbumSummary, "");
        alertDialog.dismiss();
        showRecentPlaceDetailScreen(storedAlbumSummary);
    }

    private String formatDateForJson(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(date);
    }

    private String formatStoredYearRange(StoredAlbumSummary storedAlbumSummary) {
        String strYearFromDate = yearFromDate(storedAlbumSummary.startDate);
        String strYearFromDate2 = yearFromDate(storedAlbumSummary.endDate);
        if (strYearFromDate == null && strYearFromDate2 == null) {
            return "날짜 정보 없음";
        }
        return strYearFromDate == null ? strYearFromDate2 : (strYearFromDate2 == null || strYearFromDate.equals(strYearFromDate2)) ? strYearFromDate : strYearFromDate + "~" + strYearFromDate2;
    }

    private String formatStoredRecentDate(StoredAlbumSummary storedAlbumSummary) {
        String strFirstNonEmpty = firstNonEmpty(storedAlbumSummary.endDate, storedAlbumSummary.startDate);
        if (strFirstNonEmpty == null || strFirstNonEmpty.isEmpty()) {
            return "최근 날짜 정보 없음";
        }
        return "최근 " + formatDateWithDots(strFirstNonEmpty);
    }

    private String formatDateWithDots(String str) {
        if (str == null || str.isEmpty()) {
            return "날짜 정보 없음";
        }
        return str.replace('-', '.');
    }

    private String formatStoredMonthRange(StoredAlbumSummary storedAlbumSummary) throws NumberFormatException {
        String strMonthFromDate = monthFromDate(storedAlbumSummary.startDate);
        String strMonthFromDate2 = monthFromDate(storedAlbumSummary.endDate);
        if (strMonthFromDate == null && strMonthFromDate2 == null) {
            return "날짜 정보 없음";
        }
        return strMonthFromDate == null ? strMonthFromDate2 : (strMonthFromDate2 == null || strMonthFromDate.equals(strMonthFromDate2)) ? strMonthFromDate : strMonthFromDate + "~" + strMonthFromDate2;
    }

    private String formatStoredDateRangeMultiline(StoredAlbumSummary storedAlbumSummary) {
        if ((storedAlbumSummary.startDate == null || storedAlbumSummary.startDate.isEmpty()) && (storedAlbumSummary.endDate == null || storedAlbumSummary.endDate.isEmpty())) {
            return "날짜 정보 없음";
        }
        return (storedAlbumSummary.endDate == null || storedAlbumSummary.endDate.isEmpty() || storedAlbumSummary.startDate == null || storedAlbumSummary.startDate.isEmpty() || storedAlbumSummary.startDate.equals(storedAlbumSummary.endDate)) ? firstNonEmpty(storedAlbumSummary.startDate, storedAlbumSummary.endDate) : storedAlbumSummary.startDate + "\n~\n" + storedAlbumSummary.endDate;
    }

    private String yearFromDate(String str) {
        if (str == null || str.length() < 4) {
            return null;
        }
        return str.substring(0, 4);
    }

    private String monthFromDate(String str) throws NumberFormatException {
        if (str == null || str.length() < 7) {
            return null;
        }
        try {
            return str.substring(0, 4) + "." + Integer.parseInt(str.substring(5, 7));
        } catch (Exception unused) {
            return str.substring(0, 7).replace('-', '.');
        }
    }

    private long readAlbumMediaDateMillis(Uri uri, String str, Cursor cursor, int i, int i2, int i3, boolean z) {
        Date dateSanitizeTakenAt;
        long time;
        long optionalLong = readOptionalLong(cursor, i);
        if (optionalLong > 0) {
            return optionalLong;
        }
        String string = uri == null ? "" : uri.toString();
        Long l = this.albumDateFallbackCache.get(string);
        if (l != null) {
            return l.longValue();
        }
        if (z) {
            dateSanitizeTakenAt = sanitizeTakenAt(readVideoMetadata(uri).takenAt);
        } else {
            dateSanitizeTakenAt = sanitizeTakenAt(readExifData(uri).takenAt);
        }
        Date dateSanitizeTakenAt2 = sanitizeTakenAt(parseDateFromName(str));
        if (dateSanitizeTakenAt != null) {
            time = dateSanitizeTakenAt.getTime();
        } else {
            time = dateSanitizeTakenAt2 != null ? dateSanitizeTakenAt2.getTime() : readMediaDateMillis(cursor, i, i2, i3);
        }
        this.albumDateFallbackCache.put(string, Long.valueOf(time));
        return time;
    }

    private String formatTimestamp(long j) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.KOREA).format(new Date(j));
    }

    private void addUniqueUri(List<Uri> list, Uri uri) {
        if (uri == null) {
            return;
        }
        Iterator<Uri> it = list.iterator();
        while (it.hasNext()) {
            if (uri.equals(it.next())) {
                return;
            }
        }
        list.add(uri);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == REQUEST_DELETE_ORIGINALS) {
            if (i2 == -1) {
                applyOriginalTrashState("사진 원본을 휴지통으로 이동했습니다.");
                return;
            } else {
                this.pendingTrashOriginalUris.clear();
                this.summaryText.setText("원본 휴지통 이동을 취소했습니다.");
                return;
            }
        }
        if (i == REQUEST_WRITE_VIDEOS) {
            if (i2 == -1) {
                this.videoWritePermissionGranted = true;
                this.summaryText.setText("동영상 이동 권한을 확인했습니다. 정리를 계속합니다.");
                runCopy();
                return;
            } else {
                this.videoWritePermissionGranted = false;
                this.summaryText.setText("동영상 이동 권한 확인을 취소했습니다.");
                return;
            }
        }
        if (i == REQUEST_REPAIR_DATES) {
            if (i2 == -1) {
                runDateRepairUpdates();
            } else {
                this.pendingDateRepairs.clear();
                this.summaryText.setText("날짜 복구 권한 확인을 취소했습니다.");
            }
        }
    }

    private boolean hasCopyableItems(List<PhotoItem> list) {
        return countCopyableItems(list) > 0;
    }

    private int countCopyableItems(List<PhotoItem> list) {
        boolean zShouldMoveVideos = shouldMoveVideos();
        int i = 0;
        for (PhotoItem photoItem : list) {
            if (!photoItem.noLocation && !photoItem.duplicateInTarget && (!photoItem.video || zShouldMoveVideos)) {
                i++;
            }
        }
        return i;
    }

    private int countNewFolderItems(List<PhotoItem> list) {
        boolean zShouldMoveVideos = shouldMoveVideos();
        int i = 0;
        for (PhotoItem photoItem : list) {
            if (!photoItem.noLocation && !photoItem.targetExists && !photoItem.duplicateInTarget && (!photoItem.video || zShouldMoveVideos)) {
                i++;
            }
        }
        return i;
    }

    private int countAlreadySortedItems(List<PhotoItem> list) {
        Iterator<PhotoItem> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().duplicateInTarget) {
                i++;
            }
        }
        return i;
    }

    private int countRecentlySortedItems(List<PhotoItem> list) {
        Iterator<PhotoItem> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (wasRecentlySorted(it.next())) {
                i++;
            }
        }
        return i;
    }

    private int countRecentlySortedGroups(List<PhotoItem> list) {
        HashSet hashSet = new HashSet();
        for (PhotoItem photoItem : list) {
            if (wasRecentlySorted(photoItem) && !photoItem.noLocation) {
                hashSet.add(albumCandidateGroupKey(photoItem));
            }
        }
        return hashSet.size();
    }

    private int countNoLocationItems(List<PhotoItem> list) {
        Iterator<PhotoItem> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().noLocation) {
                i++;
            }
        }
        return i;
    }

    private String compactResultSummary(int i, int i2, int i3) {
        if (i2 > 0) {
            return "위치 없음 " + i2 + "개 · 정리 예정 " + i + "개";
        }
        if (i > 0) {
            return "정리 예정 " + i + "개 · 새 앨범 " + i3 + "개";
        }
        return "확인할 항목이 없어요";
    }

    private String completedResultSummary(int i, int i2, int i3, int i4) {
        if (System.currentTimeMillis() >= 0) {
            StringBuilder sb = new StringBuilder("새 장소 ");
            sb.append(i).append("개");
            if (i2 > 0) {
                sb.append(" · 위치 없음 ").append(i2).append("개");
            }
            if (i3 > 0) {
                sb.append(" · 정리 완료 ").append(i3).append("개");
            }
            if (i4 > 0) {
                sb.append(" · 남은 원본 ").append(i4).append("개");
            }
            return sb.toString();
        }
        StringBuilder sb2 = new StringBuilder("이미 정리됨 ");
        sb2.append(i3).append("개");
        if (i2 > 0) {
            sb2.append(" · 위치 없음 ").append(i2).append("개");
        }
        if (i4 > 0) {
            sb2.append(" · 남은 원본 ").append(i4).append("개");
        }
        if (i <= 0) {
            return sb2.toString();
        }
        return "추가 확인 " + i + "개 · " + ((Object) sb2);
    }

    private String buildPreviewLog(List<PhotoItem> list) {
        StringBuilder sb = new StringBuilder();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        int i = 0;
        int i2 = 0;
        for (PhotoItem photoItem : list) {
            if (photoItem.noLocation) {
                i++;
            } else if (photoItem.duplicateInTarget) {
                i2++;
            } else {
                String strAlbumCandidateGroupKey = albumCandidateGroupKey(photoItem);
                Integer num = (Integer) linkedHashMap.get(strAlbumCandidateGroupKey);
                linkedHashMap.put(strAlbumCandidateGroupKey, Integer.valueOf(num != null ? 1 + num.intValue() : 1));
            }
        }
        sb.append("정리 예정\n");
        if (linkedHashMap.isEmpty()) {
            sb.append("- 정리할 항목 없음\n");
        } else {
            for (Object entryObj : linkedHashMap.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObj;
                sb.append("- ").append(albumCandidateTitle((String) entry.getKey())).append(" (").append(albumCandidateEyebrow((String) entry.getKey())).append("): ").append(entry.getValue()).append("개\n");
            }
        }
        sb.append("\n건너뜀\n- 위치 없음: ");
        sb.append(i).append("개\n- 이미 복사됨: ");
        sb.append(i2).append("개\n\n샘플\n");
        int iMin = Math.min(list.size(), 20);
        for (int i3 = 0; i3 < iMin; i3++) {
            PhotoItem photoItem2 = list.get(i3);
            sb.append("- ").append(photoItem2.name).append(": ");
            if (photoItem2.noLocation) {
                sb.append("위치 없음");
            } else if (photoItem2.duplicateInTarget) {
                sb.append("이미 복사됨");
            } else {
                sb.append(lastFolderName(photoItem2.targetRelativePath));
            }
            sb.append("\n");
        }
        if (list.size() > iMin) {
            sb.append("- 외 ").append(list.size() - iMin).append("개\n");
        }
        return sb.toString();
    }

    private String firstNonEmpty(String... strArr) {
        for (String str : strArr) {
            if (str != null && !str.trim().isEmpty()) {
                return str.trim();
            }
        }
        return null;
    }

    private String lastFolderName(String str) {
        if (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        int iLastIndexOf = str.lastIndexOf(47);
        return iLastIndexOf >= 0 ? str.substring(iLastIndexOf + 1) : str;
    }

    private String albumCandidateGroupKey(PhotoItem photoItem) {
        return (photoItem.targetExists ? "기존 앨범" : "새 앨범") + "|" + lastFolderName(photoItem.targetRelativePath);
    }

    private String albumCandidateEyebrow(String str) {
        int iIndexOf = str.indexOf(124);
        return iIndexOf >= 0 ? str.substring(0, iIndexOf) : "새 앨범";
    }

    private String albumCandidateTitle(String str) {
        int iIndexOf = str.indexOf(124);
        if (iIndexOf >= 0) {
            str = str.substring(iIndexOf + 1);
        }
        return str.endsWith("에서") ? str.substring(0, str.length() - 2) : str;
    }

    private String albumCandidateFolderName(String str) {
        int iIndexOf = str.indexOf(124);
        return iIndexOf >= 0 ? str.substring(iIndexOf + 1) : str;
    }

    private String formatDateRange(DateRange dateRange) {
        if (dateRange == null || dateRange.start == null || dateRange.end == null) {
            return "날짜 정보 없음";
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String str = simpleDateFormat.format(dateRange.start);
        String str2 = simpleDateFormat.format(dateRange.end);
        return str.equals(str2) ? str : str + " ~ " + str2;
    }

    private String normalizeForMatch(String str) {
        return str == null ? "" : str.replaceAll("\\s+", "").toLowerCase(Locale.KOREA);
    }

    private boolean hasUsableCoordinates(double d, double d2) {
        return Math.abs(d) > 1.0E-4d || Math.abs(d2) > 1.0E-4d;
    }

    private String safeName(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "photo_" + System.currentTimeMillis() + ".jpg";
        }
        return str.replace("/", "_");
    }

    private String safeFolderName(String str) {
        if (str == null || str.trim().isEmpty()) {
            return LOCATION_NONE;
        }
        return str.trim().replace("/", "_").replace("\\", "_").replace(":", "_").replace("*", "_").replace("?", "_").replace("\"", "_").replace("<", "_").replace(">", "_").replace("|", "_");
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void setStatus(String str, String str2, String str3, String str4) {
        TextView textView = this.statTotalText;
        if (textView == null || this.statReadyText == null || this.statBlockedText == null) {
            return;
        }
        textView.setText(formatCount(str2));
        this.statReadyText.setText(formatCount(str3));
        this.statBlockedText.setText(formatCount(str4));
        TextView textView2 = this.statTotalLabel;
        if (textView2 != null) {
            textView2.setText("새 장소");
        }
        TextView textView3 = this.statReadyLabel;
        if (textView3 != null) {
            textView3.setText("위치 없음");
        }
        TextView textView4 = this.statBlockedLabel;
        if (textView4 != null) {
            textView4.setText("정리 완료");
        }
    }

    private String formatCount(String str) {
        if (str == null || str.equals("-")) {
            return "0개";
        }
        return str + "개";
    }

    private TextView statBlock(LinearLayout linearLayout, String str, String str2, String str3, int i, int i2, boolean z) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(17);
        linearLayout2.setPadding(dp(4), 0, dp(4), 0);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        if (str2 != null && !str2.isEmpty()) {
            ImageView imageView = new ImageView(this);
            int iStatIconAsset = statIconAsset(str2);
            if (iStatIconAsset != 0) {
                imageView.setImageResource(iStatIconAsset);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView.setImageDrawable(new IconBubbleDrawable(str2, i, i2, dp(58)));
            }
            linearLayout2.addView(imageView, squareParams(dp(58)));
        }
        TextView textView = new TextView(this);
        textView.setText(str3);
        textView.setTextSize(19.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setIncludeFontPadding(false);
        textView.setGravity(17);
        textView.setTextColor(-15656921);
        textView.setPadding(0, dp(4), 0, 0);
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextSize(13.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setIncludeFontPadding(false);
        textView2.setGravity(17);
        textView2.setTextColor(-10193781);
        textView2.setPadding(0, dp(2), 0, 0);
        linearLayout2.addView(textView2);
        rememberStatLabel(str, textView2);
        if (z) {
            View view = new View(this);
            view.setBackgroundColor(-1709326);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(1, dp(58));
            layoutParams.setMargins(dp(2), 0, dp(2), 0);
            linearLayout.addView(view, layoutParams);
        }
        return textView;
    }

    private void rememberStatLabel(String str, TextView textView) {
        if ("새 장소".equals(str)) {
            this.statTotalLabel = textView;
        } else if ("위치 없음".equals(str)) {
            this.statReadyLabel = textView;
        } else if ("정리 완료".equals(str)) {
            this.statBlockedLabel = textView;
        }
    }

    private int statIconAsset(String str) {
        if ("photoLibrary".equals(str)) {
            return R.drawable.stat_sortable;
        }
        if ("locationOff".equals(str)) {
            return R.drawable.stat_no_location;
        }
        if ("folder".equals(str)) {
            return R.drawable.stat_sorted;
        }
        return 0;
    }

    private LinearLayout createHeroStartCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(20), dp(18), dp(18), dp(18));
        linearLayout.setClickable(true);
        linearLayout.setFocusable(true);
        applyGradientBackground(linearLayout, -11565321, -8635667, dp(REQUEST_WRITE_VIDEOS));
        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(new HeroStartDrawable());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(118), -1);
        layoutParams.setMargins(0, 0, dp(14), 0);
        linearLayout.addView(imageView, layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(16);
        linearLayout.addView(linearLayout2, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText("앨범 정리 시작");
        textView.setTextSize(20.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-1);
        textView.setIncludeFontPadding(false);
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("사진과 동영상을\n위치별 앨범으로 정리해요");
        textView2.setTextSize(13.0f);
        textView2.setTextColor(-419430401);
        textView2.setLineSpacing(dp(2), 1.0f);
        textView2.setPadding(0, dp(8), 0, 0);
        linearLayout2.addView(textView2);
        ImageView imageView2 = new ImageView(this);
        imageView2.setImageDrawable(new IconBubbleDrawable("arrow", -9740826, -1, dp(48)));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(48), dp(48));
        layoutParams2.setMargins(dp(12), 0, 0, 0);
        linearLayout.addView(imageView2, layoutParams2);
        return linearLayout;
    }

    private void clearResultViews() {
        LinearLayout linearLayout = this.resultSummaryCard;
        if (linearLayout != null) {
            linearLayout.setVisibility(8);
        }
        this.resultList.removeAllViews();
        this.unclassifiedPreviewRow.removeAllViews();
        View view = this.unclassifiedSectionCard;
        if (view != null) {
            view.setVisibility(8);
        }
        this.logText.setVisibility(8);
    }

    private void addLoadingThumbnailCard(String str) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, 0, dp(7), 0);
        TextView textView = new TextView(this);
        textView.setText("…");
        textView.setTextSize(28.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-12619789);
        textView.setGravity(17);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(dp(76), dp(76)));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1050881);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -2562817);
        textView.setBackground(gradientDrawable);
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextSize(11.0f);
        textView2.setTextColor(-10193781);
        textView2.setGravity(17);
        textView2.setPadding(0, dp(4), 0, 0);
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(dp(76), -2));
        this.unclassifiedPreviewRow.addView(linearLayout);
    }

    private void addEmptyResultHint() {
        this.resultList.removeAllViews();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(0, dp(8), 0, dp(8));
        this.resultList.addView(linearLayout, matchWidth());
        TextView textView = new TextView(this);
        textView.setText("▣");
        textView.setTextSize(18.0f);
        textView.setTextColor(-12619789);
        textView.setGravity(17);
        linearLayout.addView(textView, squareParams(dp(38)));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1050881);
        gradientDrawable.setCornerRadius(dp(12));
        textView.setBackground(gradientDrawable);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(12), 0, 0, 0);
        linearLayout.addView(linearLayout2, weightedParams(1));
        TextView textView2 = new TextView(this);
        textView2.setText("아직 발견한 장소가 없어요");
        textView2.setTextSize(15.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-13418155);
        linearLayout2.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText("앨범 정리 시작을 누르면 장소별로 정리해드릴게요.");
        textView3.setTextSize(13.0f);
        textView3.setTextColor(-7035976);
        textView3.setPadding(0, dp(2), 0, 0);
        linearLayout2.addView(textView3);
    }

    private void addRecentPlacesSection(LinearLayout linearLayout) {
        List<StoredAlbumSummary> listLoadRecentAlbumSummaries = loadRecentAlbumSummaries();
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(0), dp(14), dp(0), dp(12));
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(14)));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        linearLayout3.setPadding(dp(2), 0, dp(2), 0);
        linearLayout2.addView(linearLayout3, matchWidthWithBottom(dp(8)));
        linearLayout3.addView(sectionTitle("최근 발견한 장소"), weightedParams(1));
        if (listLoadRecentAlbumSummaries.isEmpty()) {
            linearLayout2.addView(bodyText("앨범 정리를 실행하면 새로 발견한 장소를 보여드려요."), matchWidthWithBottom(dp(12)));
            return;
        }
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(17);
        linearLayout2.addView(linearLayout4, matchWidthWithBottom(dp(10)));
        int iMin = Math.min(4, listLoadRecentAlbumSummaries.size());
        int i = 0;
        while (i < iMin) {
            addStoredAlbumCompactCard(linearLayout4, listLoadRecentAlbumSummaries.get(i), i == iMin + (-1));
            i++;
        }
    }

    private void showRecentPlacesScreen() {
        this.resultScreenMode = true;
        this.recentPlacesScreenMode = true;
        this.recentPlaceDetailMode = false;
        final ScrollView scrollView = new ScrollView(this);
        this.recentPlacesScrollView = scrollView;
        scrollView.setBackgroundColor(-197377);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(18), dp(56), dp(18), dp(REQUEST_WRITE_VIDEOS));
        scrollView.addView(linearLayout);
        addListHeader(linearLayout, "최근 발견한 장소");
        List<StoredAlbumSummary> listLoadRecentAlbumSummaries = loadRecentAlbumSummaries();
        addWorkingBanner(linearLayout);
        if (listLoadRecentAlbumSummaries.isEmpty()) {
            LinearLayout linearLayout2 = new LinearLayout(this);
            linearLayout2.setOrientation(1);
            linearLayout2.setPadding(dp(16), dp(18), dp(16), dp(18));
            linearLayout.addView(linearLayout2, matchWidth());
            applyCardBackground(linearLayout2);
            linearLayout2.addView(sectionTitle("아직 저장된 장소가 없어요"), matchWidthWithBottom(dp(6)));
            linearLayout2.addView(bodyText("앨범 정리를 실행하면 새로 발견한 장소가 여기에 쌓여요."), matchWidthWithBottom(dp(12)));
        } else {
            addRecentPlacesSummaryCard(linearLayout, listLoadRecentAlbumSummaries);
            GridLayout gridLayout = new GridLayout(this);
            gridLayout.setColumnCount(2);
            linearLayout.addView(gridLayout, matchWidthWithBottom(dp(12)));
            Iterator<StoredAlbumSummary> it = listLoadRecentAlbumSummaries.iterator();
            while (it.hasNext()) {
                addStoredAlbumGridCard(gridLayout, it.next());
            }
        }
        setContentViewWithBottomTabs(scrollView, 1);
        if (this.recentPlacesScrollY > 0) {
            scrollView.post(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda37
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m56xa4116934(scrollView);
                }
            });
        }
    }

    /* renamed from: lambda$showRecentPlacesScreen$44$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m56xa4116934(ScrollView scrollView) {
        scrollView.scrollTo(0, this.recentPlacesScrollY);
    }

    private void returnToRecentPlacesScreen() {
        this.recentPlaceDetailMode = false;
        this.activePlaceDetailSummary = null;
        showRecentPlacesScreen();
    }

    private void showRecentPlaceDetailScreen(final StoredAlbumSummary storedAlbumSummary) {
        ScrollView scrollView = this.recentPlacesScrollView;
        if (scrollView != null) {
            this.recentPlacesScrollY = scrollView.getScrollY();
        }
        this.resultScreenMode = true;
        this.recentPlacesScreenMode = false;
        this.recentPlaceDetailMode = true;
        this.activePlaceDetailSummary = storedAlbumSummary;
        ScrollView scrollView2 = new ScrollView(this);
        scrollView2.setBackgroundColor(-197377);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(18), dp(56), dp(18), dp(REQUEST_WRITE_VIDEOS));
        scrollView2.addView(linearLayout);
        addSimpleHeader(linearLayout, storedAlbumSummary.albumName);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(0, 0, 0, dp(16));
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(14)));
        applyCardBackground(linearLayout2);
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(260));
        layoutParams.setMargins(0, 0, 0, 0);
        linearLayout2.addView(imageView, layoutParams);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1050881);
        gradientDrawable.setCornerRadii(new float[]{dp(20), dp(20), dp(20), dp(20), 0.0f, 0.0f, 0.0f, 0.0f});
        imageView.setBackground(gradientDrawable);
        imageView.setClipToOutline(true);
        if (storedAlbumSummary.thumbnailUri != null && !storedAlbumSummary.thumbnailUri.isEmpty()) {
            loadThumbnailInto(imageView, Uri.parse(storedAlbumSummary.thumbnailUri), dp(900));
        } else {
            imageView.setImageDrawable(thumbnailPlaceholder());
        }
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dp(16), dp(16), dp(16), 0);
        linearLayout2.addView(linearLayout3, matchWidth());
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(16);
        linearLayout3.addView(linearLayout4, matchWidthWithBottom(dp(18)));
        LinearLayout linearLayout5 = new LinearLayout(this);
        linearLayout5.setOrientation(1);
        linearLayout4.addView(linearLayout5, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText(storedAlbumSummary.albumName + "의 기록");
        textView.setTextSize(15.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout5.addView(textView);
        String strTrim = albumMemory(storedAlbumSummary).trim();
        if (!strTrim.isEmpty()) {
            TextView textView2 = new TextView(this);
            textView2.setText(strTrim);
            textView2.setTextSize(13.0f);
            textView2.setTextColor(-10193781);
            textView2.setSingleLine(true);
            textView2.setPadding(0, dp(4), dp(4), 0);
            linearLayout5.addView(textView2);
        }
        TextView textView3 = new TextView(this);
        textView3.setText(storedAlbumSummary.itemCount + "개");
        textView3.setTextSize(38.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setTextColor(-9609738);
        textView3.setPadding(0, dp(4), 0, 0);
        linearLayout5.addView(textView3);
        TextView textViewCompactCardMeta = compactCardMeta(formatStoredMonthRange(storedAlbumSummary));
        textViewCompactCardMeta.setTextSize(13.0f);
        linearLayout5.addView(textViewCompactCardMeta);
        View view = new View(this);
        view.setBackgroundColor(-1709326);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(1, dp(72));
        layoutParams2.setMargins(dp(12), 0, dp(12), 0);
        linearLayout4.addView(view, layoutParams2);
        LinearLayout linearLayout6 = new LinearLayout(this);
        linearLayout6.setOrientation(1);
        linearLayout4.addView(linearLayout6, new LinearLayout.LayoutParams(dp(118), -2));
        LinearLayout linearLayoutAddCompactDateStat = addCompactDateStat(linearLayout6, "첫 방문", formatDateWithDots(firstNonEmpty(storedAlbumSummary.startDate, "날짜 정보 없음")));
        ImageView imageView2 = new ImageView(this);
        imageView2.setImageResource(R.drawable.ic_edit_memory);
        imageView2.setContentDescription("기억 편집");
        imageView2.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setColor(-921857);
        gradientDrawable2.setCornerRadius(dp(10));
        imageView2.setBackground(gradientDrawable2);
        imageView2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda32
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                MainActivity.this.m54x3a6254f5(storedAlbumSummary, view2);
            }
        });
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(dp(30), dp(30));
        layoutParams3.setMargins(dp(4), 0, 0, 0);
        linearLayoutAddCompactDateStat.addView(imageView2, layoutParams3);
        addCompactDateStat(linearLayout6, "최근 방문", formatDateWithDots(firstNonEmpty(storedAlbumSummary.endDate, storedAlbumSummary.startDate, "날짜 정보 없음")));
        TextView textViewSectionTitle = sectionTitle("이 장소의 기억");
        textViewSectionTitle.setTextSize(16.0f);
        linearLayout3.addView(textViewSectionTitle, matchWidthWithBottom(dp(10)));
        addDetailMemoryPreviewStrip(linearLayout3, storedAlbumSummary);
        Button button = new Button(this);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda34
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                MainActivity.this.m55x918045d4(storedAlbumSummary, view2);
            }
        });
        stylePurpleCtaButton(button, "갤러리에서 보기     →");
        linearLayout3.addView(button, fullWidthButtonParams(0, dp(58)));
        setContentViewWithBottomTabs(scrollView2, -1);
    }

    /* renamed from: lambda$showRecentPlaceDetailScreen$45$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m54x3a6254f5(StoredAlbumSummary storedAlbumSummary, View view) {
        showAlbumMemoryEditor(storedAlbumSummary);
    }

    /* renamed from: lambda$showRecentPlaceDetailScreen$46$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m55x918045d4(StoredAlbumSummary storedAlbumSummary, View view) {
        openAlbumInSamsungGallery(storedAlbumSummary);
    }

    private void addMemoryInfoRow(LinearLayout linearLayout, String str, String str2) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, dp(7), 0, dp(7));
        linearLayout.addView(linearLayout2, matchWidth());
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(14.0f);
        textView.setTextColor(-10193781);
        linearLayout2.addView(textView, weightedParams(1));
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextSize(15.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-15656921);
        textView2.setGravity(8388613);
        linearLayout2.addView(textView2);
    }

    private LinearLayout addCompactDateStat(LinearLayout linearLayout, String str, String str2) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, dp(3), 0, dp(7));
        linearLayout.addView(linearLayout2, matchWidth());
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(0, 0, 0, 0);
        linearLayout2.addView(linearLayout3, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(11.0f);
        textView.setTextColor(-7035976);
        linearLayout3.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextSize(12.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-15656921);
        linearLayout3.addView(textView2);
        return linearLayout2;
    }

    private void addDetailMemoryPreviewStrip(LinearLayout linearLayout, StoredAlbumSummary storedAlbumSummary) {
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        linearLayout.addView(horizontalScrollView, matchWidthWithBottom(dp(12)));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        horizontalScrollView.addView(linearLayout2);
        List<Uri> listLoadLatestAlbumPreviewUris = loadLatestAlbumPreviewUris(storedAlbumSummary.relativePath, 5);
        for (int i = 0; i < Math.max(1, listLoadLatestAlbumPreviewUris.size()); i++) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(62), dp(62));
            layoutParams.setMargins(0, 0, dp(8), 0);
            linearLayout2.addView(imageView, layoutParams);
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(-1050881);
            gradientDrawable.setCornerRadius(dp(12));
            imageView.setBackground(gradientDrawable);
            imageView.setClipToOutline(true);
            if (i < listLoadLatestAlbumPreviewUris.size()) {
                final Uri uri = listLoadLatestAlbumPreviewUris.get(i);
                imageView.setClickable(true);
                imageView.setFocusable(true);
                imageView.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda10
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m5x93bb3fca(uri, view);
                    }
                });
                loadThumbnailInto(imageView, uri, dp(124));
            } else {
                imageView.setImageDrawable(thumbnailPlaceholder());
            }
        }
    }

    /* renamed from: lambda$addDetailMemoryPreviewStrip$47$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m5x93bb3fca(Uri uri, View view) {
        openPhoto(uri);
    }

    private void addAlbumPreviewStrip(LinearLayout linearLayout, StoredAlbumSummary storedAlbumSummary) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(16)));
        List<Uri> listLoadLatestAlbumPreviewUris = loadLatestAlbumPreviewUris(storedAlbumSummary.relativePath, 3);
        int i = 0;
        while (i < 3) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, dp(92), 1.0f);
            layoutParams.setMargins(i == 0 ? 0 : dp(5), 0, i == 2 ? 0 : dp(5), 0);
            linearLayout2.addView(imageView, layoutParams);
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(i == 0 ? -1386295 : i == 1 ? -2759694 : -2495275);
            gradientDrawable.setCornerRadius(dp(10));
            imageView.setBackground(gradientDrawable);
            imageView.setClipToOutline(true);
            if (i < listLoadLatestAlbumPreviewUris.size()) {
                loadThumbnailInto(imageView, listLoadLatestAlbumPreviewUris.get(i), dp(160));
            } else {
                imageView.setImageDrawable(thumbnailPlaceholder());
            }
            i++;
        }
    }

    private void addRecentPlacesSummaryCard(LinearLayout linearLayout, List<StoredAlbumSummary> list) {
        Iterator<StoredAlbumSummary> it = list.iterator();
        int iMax = 0;
        while (it.hasNext()) {
            iMax += Math.max(0, it.next().itemCount);
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(dp(14), dp(12), dp(14), dp(12));
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(14)));
        applyCardBackground(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dp(12), 0, 0, 0);
        linearLayout2.addView(linearLayout3, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText("총 " + list.size() + "개 장소 발견");
        textView.setTextSize(16.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        linearLayout3.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("총 " + String.format(Locale.KOREA, "%,d", Integer.valueOf(iMax)) + "개 사진 · " + overallYearRange(list));
        textView2.setTextSize(13.0f);
        textView2.setTextColor(-10193781);
        textView2.setPadding(0, dp(2), 0, 0);
        linearLayout3.addView(textView2);
    }

    private String overallYearRange(List<StoredAlbumSummary> list) {
        String str = null;
        String str2 = null;
        for (StoredAlbumSummary storedAlbumSummary : list) {
            String strYearFromDate = yearFromDate(storedAlbumSummary.startDate);
            String strYearFromDate2 = yearFromDate(storedAlbumSummary.endDate);
            if (strYearFromDate != null && (str == null || strYearFromDate.compareTo(str) < 0)) {
                str = strYearFromDate;
            }
            if (strYearFromDate2 != null && (str2 == null || strYearFromDate2.compareTo(str2) > 0)) {
                str2 = strYearFromDate2;
            }
        }
        if (str == null && str2 == null) {
            return "날짜 정보 없음";
        }
        return (str == null || str.equals(str2)) ? firstNonEmpty(str2, str) : str2 == null ? str : str + " ~ " + str2;
    }

    private void addStoredAlbumCompactCard(LinearLayout linearLayout, final StoredAlbumSummary storedAlbumSummary, boolean z) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(0, 0, 0, dp(8));
        linearLayout2.setClickable(true);
        linearLayout2.setFocusable(true);
        linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m12x51b9360e(storedAlbumSummary, view);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.setMargins(0, 0, z ? 0 : dp(8), 0);
        linearLayout.addView(linearLayout2, layoutParams);
        applyCardBackground(linearLayout2);
        linearLayout2.addView(placePhotoFrame(storedAlbumSummary, -1, dp(74), dp(16)));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dp(9), dp(8), dp(7), dp(8));
        linearLayout2.addView(linearLayout3, matchWidth());
        linearLayout3.addView(compactCardTitle(storedAlbumSummary.albumName, 13));
        linearLayout3.addView(compactCardCount(storedAlbumSummary.itemCount + "개", 14));
        linearLayout3.addView(compactCardMetaSmall(formatStoredRecentDate(storedAlbumSummary)));
    }

    /* renamed from: lambda$addStoredAlbumCompactCard$48$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m12x51b9360e(StoredAlbumSummary storedAlbumSummary, View view) {
        showRecentPlaceDetailScreen(storedAlbumSummary);
    }

    private void addStoredAlbumGridCard(GridLayout gridLayout, final StoredAlbumSummary storedAlbumSummary) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, 0, 0, dp(8));
        linearLayout.setClickable(true);
        linearLayout.setFocusable(true);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda60
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m13xa178aa60(storedAlbumSummary, view);
            }
        });
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.width = 0;
        layoutParams.height = -2;
        layoutParams.columnSpec = GridLayout.spec(Integer.MIN_VALUE, 1.0f);
        layoutParams.setMargins(dp(4), 0, dp(4), dp(10));
        gridLayout.addView(linearLayout, layoutParams);
        applyCardBackground(linearLayout);
        linearLayout.addView(placePhotoFrame(storedAlbumSummary, -1, dp(112), dp(18)));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(12), dp(8), dp(10), dp(10));
        linearLayout.addView(linearLayout2, matchWidth());
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        linearLayout2.addView(linearLayout3, matchWidth());
        linearLayout3.addView(compactCardTitle(storedAlbumSummary.albumName, 15), weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText("⋮");
        textView.setTextSize(18.0f);
        textView.setTextColor(-7035976);
        textView.setGravity(17);
        textView.setClickable(true);
        textView.setFocusable(true);
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda61
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m14x1e0b5d8a(storedAlbumSummary, view);
            }
        });
        linearLayout3.addView(textView);
        String strTrim = albumMemory(storedAlbumSummary).trim();
        if (!strTrim.isEmpty()) {
            TextView textViewCompactCardMeta = compactCardMeta(strTrim);
            textViewCompactCardMeta.setTextColor(-8622181);
            linearLayout2.addView(textViewCompactCardMeta);
        }
        linearLayout2.addView(compactCardCount(storedAlbumSummary.itemCount + "개 사진", 16));
        linearLayout2.addView(compactCardMeta(formatStoredMonthRange(storedAlbumSummary)));
    }

    /* renamed from: lambda$addStoredAlbumGridCard$49$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m13xa178aa60(StoredAlbumSummary storedAlbumSummary, View view) {
        showRecentPlaceDetailScreen(storedAlbumSummary);
    }

    /* renamed from: lambda$addStoredAlbumGridCard$50$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m14x1e0b5d8a(StoredAlbumSummary storedAlbumSummary, View view) {
        showAlbumMemoryEditor(storedAlbumSummary);
    }

    private FrameLayout placePhotoFrame(StoredAlbumSummary storedAlbumSummary, int i, int i2, int i3) {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setClipToOutline(true);
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(i, i2));
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frameLayout.addView(imageView, new FrameLayout.LayoutParams(-1, -1));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1050881);
        float f = i3;
        gradientDrawable.setCornerRadii(new float[]{f, f, f, f, 0.0f, 0.0f, 0.0f, 0.0f});
        imageView.setBackground(gradientDrawable);
        imageView.setClipToOutline(true);
        if (storedAlbumSummary.thumbnailUri != null && !storedAlbumSummary.thumbnailUri.isEmpty()) {
            loadThumbnailInto(imageView, Uri.parse(storedAlbumSummary.thumbnailUri), Math.max(i, i2));
        } else {
            imageView.setImageDrawable(thumbnailPlaceholder());
        }
        return frameLayout;
    }

    private TextView compactCardTitle(String str, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        return textView;
    }

    private TextView compactCardCount(String str, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-9609738);
        textView.setPadding(0, dp(4), 0, 0);
        textView.setSingleLine(true);
        return textView;
    }

    private TextView compactCardMeta(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(12.0f);
        textView.setTextColor(-10193781);
        textView.setPadding(0, dp(1), 0, 0);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        return textView;
    }

    private TextView compactCardMetaSmall(String str) {
        TextView textViewCompactCardMeta = compactCardMeta(str);
        textViewCompactCardMeta.setTextSize(11.0f);
        return textViewCompactCardMeta;
    }

    private void addListHeader(LinearLayout linearLayout, String str) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(18)));
        TextView textView = new TextView(this);
        textView.setText("‹");
        textView.setTextSize(36.0f);
        textView.setGravity(17);
        textView.setTextColor(-15656921);
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda65
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m6lambda$addListHeader$51$comexamplegallerysorterMainActivity(view);
            }
        });
        linearLayout2.addView(textView, squareParams(dp(44)));
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextSize(19.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-15656921);
        linearLayout2.addView(textView2, weightedParams(1));
    }

    /* renamed from: lambda$addListHeader$51$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m6lambda$addListHeader$51$comexamplegallerysorterMainActivity(View view) {
        returnToMainScreen();
    }

    private LinearLayout addSimpleHeader(LinearLayout linearLayout, String str) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(18)));
        TextView textView = new TextView(this);
        textView.setText("‹");
        textView.setTextSize(36.0f);
        textView.setGravity(17);
        textView.setTextColor(-15656921);
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda26
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m11lambda$addSimpleHeader$52$comexamplegallerysorterMainActivity(view);
            }
        });
        linearLayout2.addView(textView, squareParams(dp(44)));
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextSize(22.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-15656921);
        linearLayout2.addView(textView2, weightedParams(1));
        return linearLayout2;
    }

    /* renamed from: lambda$addSimpleHeader$52$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m11lambda$addSimpleHeader$52$comexamplegallerysorterMainActivity(View view) {
        returnToRecentPlacesScreen();
    }

    private void addStoredAlbumRow(LinearLayout linearLayout, final StoredAlbumSummary storedAlbumSummary, boolean z) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, dp(9), 0, dp(9));
        if (z) {
            linearLayout2.setClickable(true);
            linearLayout2.setFocusable(true);
            linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda66
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.m15xa5ca04fb(storedAlbumSummary, view);
                }
            });
        }
        linearLayout.addView(linearLayout2, matchWidth());
        if (storedAlbumSummary.thumbnailUri != null && !storedAlbumSummary.thumbnailUri.isEmpty()) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            linearLayout2.addView(imageView, squareParams(dp(58)));
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(-1378321);
            gradientDrawable.setCornerRadius(dp(12));
            imageView.setBackground(gradientDrawable);
            imageView.setClipToOutline(true);
            loadThumbnailInto(imageView, Uri.parse(storedAlbumSummary.thumbnailUri), dp(58));
        } else {
            TextView textView = new TextView(this);
            textView.setText("📍");
            textView.setTextSize(22.0f);
            textView.setGravity(17);
            textView.setTextColor(-8635667);
            linearLayout2.addView(textView, squareParams(dp(58)));
            GradientDrawable gradientDrawable2 = new GradientDrawable();
            gradientDrawable2.setColor(-1185282);
            gradientDrawable2.setCornerRadius(dp(12));
            textView.setBackground(gradientDrawable2);
        }
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dp(12), 0, dp(8), 0);
        linearLayout2.addView(linearLayout3, weightedParams(1));
        TextView textView2 = new TextView(this);
        textView2.setText(storedAlbumSummary.albumName);
        textView2.setTextSize(15.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-14735049);
        linearLayout3.addView(textView2);
        String strTrim = albumMemory(storedAlbumSummary).trim();
        if (!strTrim.isEmpty()) {
            TextView textViewCompactCardMeta = compactCardMeta(strTrim);
            textViewCompactCardMeta.setTextColor(-8622181);
            linearLayout3.addView(textViewCompactCardMeta);
        }
        TextView textView3 = new TextView(this);
        textView3.setText(storedAlbumSummary.itemCount + "개 사진 · " + formatStoredMonthRange(storedAlbumSummary));
        textView3.setTextSize(13.0f);
        textView3.setTextColor(-10193781);
        textView3.setPadding(0, dp(2), 0, 0);
        linearLayout3.addView(textView3);
        TextView textView4 = new TextView(this);
        textView4.setText("›");
        textView4.setTextSize(24.0f);
        textView4.setTextColor(-3418655);
        linearLayout2.addView(textView4);
    }

    /* renamed from: lambda$addStoredAlbumRow$53$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m15xa5ca04fb(StoredAlbumSummary storedAlbumSummary, View view) {
        showRecentPlaceDetailScreen(storedAlbumSummary);
    }

    private TextView inlineAction(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-14326805);
        textView.setGravity(17);
        textView.setPadding(dp(8), dp(6), 0, dp(6));
        return textView;
    }

    private TextView bodyText(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(14.0f);
        textView.setTextColor(-10193781);
        return textView;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void renderPreviewResults(List<PhotoItem> list) {
        int i;
        DateRange dateRange;
        ArrayList arrayList;
        int i2;
        PhotoItem photoItem;
        DateRange dateRange2;
        LinearLayout linearLayout = this.resultSummaryCard;
        if (linearLayout != null) {
            linearLayout.setVisibility(list.isEmpty() ? 8 : 0);
        }
        this.resultList.removeAllViews();
        this.unclassifiedPreviewRow.removeAllViews();
        View view = this.unclassifiedSectionCard;
        if (view != null) {
            view.setVisibility(this.resultScreenMode ? 0 : 8);
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        LinkedHashMap linkedHashMap3 = new LinkedHashMap();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        PhotoItem photoItem2 = null;
        Object[] objArr = null;
        Object[] objArr2 = null;
        DateRange dateRange3 = new DateRange();
        int i3 = 0;
        int i4 = 0;
        for (PhotoItem photoItem3 : list) {
            if (photoItem3.noLocation) {
                i4++;
                dateRange3.include(photoItem3.takenAt);
                if (arrayList3.size() < 8) {
                    arrayList3.add(photoItem3);
                }
            } else if (this.copyCompletedMode && wasRecentlySorted(photoItem3)) {
                i3++;
                if (!this.originalsTrashCompleted && !photoItem3.video && photoItem3.duplicateInTarget) {
                    addUniqueUri(arrayList2, photoItem3.uri);
                }
                String strAlbumCandidateGroupKey = albumCandidateGroupKey(photoItem3);
                Integer num = (Integer) linkedHashMap.get(strAlbumCandidateGroupKey);
                linkedHashMap.put(strAlbumCandidateGroupKey, Integer.valueOf(num != null ? num.intValue() + 1 : 1));
                if (!linkedHashMap2.containsKey(strAlbumCandidateGroupKey)) {
                    linkedHashMap2.put(strAlbumCandidateGroupKey, photoItem3);
                }
                DateRange dateRange4 = (DateRange) linkedHashMap3.get(strAlbumCandidateGroupKey);
                if (dateRange4 == null) {
                    dateRange4 = new DateRange();
                    linkedHashMap3.put(strAlbumCandidateGroupKey, dateRange4);
                }
                dateRange4.include(photoItem3.takenAt);
            } else if (this.copyCompletedMode && photoItem3.duplicateInTarget) {
                i3++;
                if (!this.originalsTrashCompleted && !photoItem3.video) {
                    addUniqueUri(arrayList2, photoItem3.uri);
                }
            } else if (photoItem3.duplicateInTarget) {
                i3++;
                if (!this.originalsTrashCompleted && !photoItem3.video) {
                    addUniqueUri(arrayList2, photoItem3.uri);
                }
            } else {
                String strAlbumCandidateGroupKey2 = albumCandidateGroupKey(photoItem3);
                Integer num2 = (Integer) linkedHashMap.get(strAlbumCandidateGroupKey2);
                linkedHashMap.put(strAlbumCandidateGroupKey2, Integer.valueOf(num2 != null ? num2.intValue() + 1 : 1));
                if (!linkedHashMap2.containsKey(strAlbumCandidateGroupKey2)) {
                    linkedHashMap2.put(strAlbumCandidateGroupKey2, photoItem3);
                }
                DateRange dateRange5 = (DateRange) linkedHashMap3.get(strAlbumCandidateGroupKey2);
                if (dateRange5 == null) {
                    dateRange5 = new DateRange();
                    linkedHashMap3.put(strAlbumCandidateGroupKey2, dateRange5);
                }
                dateRange5.include(photoItem3.takenAt);
            }
        }
        String str = "개 앨범";
        String str2 = "나머지 ";
        if (this.copyCompletedMode) {
            if (linkedHashMap.isEmpty()) {
                i2 = i4;
                photoItem = null;
                dateRange2 = dateRange3;
                if (i3 > 0) {
                    addResultRow(null, "folder", "이미 정리됨", "앨범에 정리된 항목", i3 + "개", "", -1378321, -15293622);
                }
            } else {
                addResultSectionLabel("정리된 앨범");
                int i5 = this.resultScreenMode ? MAX_RESULT_SCREEN_GROUPS : 3;
                int i6 = 0;
                for (Object entryObj : linkedHashMap.entrySet()) {
                    Map.Entry entry = (Map.Entry) entryObj;
                    if (i6 >= i5) {
                        break;
                    }
                    addResultRow((PhotoItem) linkedHashMap2.get(entry.getKey()), "folder", "정리됨", albumCandidateFolderName((String) entry.getKey()), entry.getValue() + "개", formatDateRange((DateRange) linkedHashMap3.get(entry.getKey())), -1378321, -15293622);
                    i6++;
                    dateRange3 = dateRange3;
                    i4 = i4;
                    i5 = i5;
                    photoItem2 = null;
                    linkedHashMap3 = linkedHashMap3;
                    linkedHashMap2 = linkedHashMap2;
                    str2 = str2;
                    str = str;
                }
                int i7 = i6;
                String str3 = str2;
                String str4 = str;
                i2 = i4;
                photoItem = photoItem2;
                dateRange2 = dateRange3;
                if (linkedHashMap.size() > i7) {
                    addResultRow(null, "+", "전체 보기", str3 + (linkedHashMap.size() - i7) + str4, "›", "", -1050881, -14326805, new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda53
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view2) {
                            MainActivity.this.m37x62514d09(view2);
                        }
                    });
                }
            }
            if (!this.originalsTrashCompleted && !this.copiedOriginalUris.isEmpty()) {
                addOriginalDeleteAction(this.copiedOriginalUris.size(), new ArrayList(this.copiedOriginalUris));
            } else if (!this.originalsTrashCompleted && !arrayList2.isEmpty()) {
                addOriginalDeleteAction(arrayList2.size(), arrayList2);
            }
            if (i2 > 0) {
                addResultRow(arrayList3.isEmpty() ? photoItem : (PhotoItem) arrayList3.get(0), "alert", "확인 필요", "위치 정보 없음", i2 + "개", formatDateRange(dateRange2), -2067, -680437);
            }
            renderNoLocationSamples(arrayList3, i2);
            return;
        }
        LinkedHashMap linkedHashMap4 = linkedHashMap3;
        String str5 = "개 앨범";
        int i8 = i4;
        DateRange dateRange6 = dateRange3;
        String str6 = "나머지 ";
        if (linkedHashMap.isEmpty()) {
            i = i3;
            addResultRow(null, "▣", "완료", "정리할 항목 없음", "0개", "", -1117441, -12619789);
            dateRange = dateRange6;
            arrayList = arrayList2;
        } else {
            i = i3;
            int i9 = this.resultScreenMode ? MAX_RESULT_SCREEN_GROUPS : 3;
            int i10 = 0;
            for (Object entryObj2 : linkedHashMap.entrySet()) {
                Map.Entry entry2 = (Map.Entry) entryObj2;
                if (i10 >= i9) {
                    break;
                }
                LinkedHashMap linkedHashMap5 = linkedHashMap4;
                addResultRow((PhotoItem) linkedHashMap2.get(entry2.getKey()), "▣", albumCandidateEyebrow((String) entry2.getKey()), albumCandidateTitle((String) entry2.getKey()), entry2.getValue() + "개", formatDateRange((DateRange) linkedHashMap5.get(entry2.getKey())), -1378321, -15293622);
                i10++;
                str6 = str6;
                str5 = str5;
                i9 = i9;
                arrayList2 = arrayList2;
                dateRange6 = dateRange6;
                linkedHashMap4 = linkedHashMap5;
            }
            int i11 = i10;
            dateRange = dateRange6;
            arrayList = arrayList2;
            String str7 = str5;
            String str8 = str6;
            if (linkedHashMap.size() > i11) {
                addResultRow(null, "+", "전체 보기", str8 + (linkedHashMap.size() - i11) + str7, "›", "", -1050881, -14326805, new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda54
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view2) {
                        MainActivity.this.m38xb96f3de8(view2);
                    }
                });
            }
        }
        if (this.resultScreenMode || i8 > 0) {
            addResultRow(arrayList3.isEmpty() ? null : (PhotoItem) arrayList3.get(0), "!", "확인 필요", "위치 정보 없음", i8 + "개", formatDateRange(dateRange), -2067, -680437);
        }
        int i12 = i;
        if (this.resultScreenMode || i12 > 0) {
            addResultRow(null, "✓", "이미 정리됨", "복사본이 있는 항목", i12 + "개", "", -920071, -10193781);
            if (!arrayList.isEmpty()) {
                addOriginalDeleteAction(arrayList.size(), arrayList);
            }
        }
        renderNoLocationSamples(arrayList3, i8);
    }

    /* renamed from: lambda$renderPreviewResults$54$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m37x62514d09(View view) {
        showResultScreen();
    }

    /* renamed from: lambda$renderPreviewResults$55$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m38xb96f3de8(View view) {
        showResultScreen();
    }

    private void addResultSectionLabel(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-14735049);
        textView.setPadding(0, dp(2), 0, dp(8));
        this.resultList.addView(textView, matchWidth());
    }

    private void addOriginalDeleteAction(int i, final List<Uri> list) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(50), dp(4), 0, dp(8));
        this.resultList.addView(linearLayout, matchWidth());
        TextView textView = new TextView(this);
        textView.setText("사진 원본 " + i + "개가 남아 있어요");
        textView.setTextSize(12.0f);
        textView.setTextColor(-7035976);
        textView.setSingleLine(true);
        linearLayout.addView(textView, weightedParams(1));
        TextView textView2 = new TextView(this);
        textView2.setText("휴지통 이동");
        textView2.setTextSize(12.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-2024120);
        textView2.setGravity(17);
        textView2.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-3598);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -13099);
        textView2.setBackground(gradientDrawable);
        textView2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda48
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7xd0eada1c(list, view);
            }
        });
        linearLayout.addView(textView2);
    }

    /* renamed from: lambda$addOriginalDeleteAction$56$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m7xd0eada1c(List list, View view) {
        this.copiedOriginalUris.clear();
        this.copiedOriginalUris.addAll(list);
        deleteCopiedOriginals();
    }

    private void renderNoLocationSamples(List<PhotoItem> list, int i) {
        String str;
        Iterator<PhotoItem> it = list.iterator();
        while (it.hasNext()) {
            this.unclassifiedPreviewRow.addView(thumbnailCard(it.next()));
        }
        if (list.isEmpty()) {
            this.unclassifiedPreviewRow.addView(emptyThumbnailCard("없음"));
        }
        TextView textView = this.logText;
        if (i > list.size()) {
            str = "+" + (i - list.size()) + "개 더 있어요";
        } else {
            str = i > 0 ? "위치 정보가 없는 항목입니다." : "위치 정보 없는 항목이 없습니다.";
        }
        textView.setText(str);
        this.logText.setVisibility(8);
    }

    private void addResultRow(PhotoItem photoItem, String str, String str2, String str3, String str4, String str5, int i, int i2) {
        addResultRow(photoItem, str, str2, str3, str4, str5, i, i2, null);
    }

    private void addResultRow(final PhotoItem photoItem, String str, String str2, String str3, String str4, String str5, int i, int i2, View.OnClickListener onClickListener) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(0, dp(9), 0, dp(9));
        if (onClickListener == null && photoItem != null) {
            onClickListener = new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda69
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.m8lambda$addResultRow$57$comexamplegallerysorterMainActivity(photoItem, view);
                }
            };
        }
        if (onClickListener != null) {
            linearLayout.setClickable(true);
            linearLayout.setFocusable(true);
            linearLayout.setOnClickListener(onClickListener);
        }
        this.resultList.addView(linearLayout, matchWidth());
        if (photoItem != null) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            linearLayout.addView(imageView, squareParams(dp(40)));
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(i);
            gradientDrawable.setCornerRadius(dp(12));
            imageView.setBackground(gradientDrawable);
            imageView.setClipToOutline(true);
            loadThumbnailInto(imageView, photoItem.uri, dp(40));
        } else {
            ImageView imageView2 = new ImageView(this);
            imageView2.setImageDrawable(new IconBubbleDrawable(str, i2, i, dp(40)));
            linearLayout.addView(imageView2, squareParams(dp(40)));
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(12), 0, dp(8), 0);
        linearLayout.addView(linearLayout2, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText(str2);
        textView.setTextSize(13.0f);
        textView.setTextColor(-6511697);
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str3);
        textView2.setTextSize(15.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-14735049);
        linearLayout2.addView(textView2);
        if (str5 != null && !str5.isEmpty()) {
            TextView textView3 = new TextView(this);
            textView3.setText(str5);
            textView3.setTextSize(13.0f);
            textView3.setTextColor(-10193781);
            textView3.setPadding(0, dp(1), 0, 0);
            linearLayout2.addView(textView3);
        }
        TextView textView4 = new TextView(this);
        textView4.setText(str4);
        textView4.setTextSize(17.0f);
        textView4.setTypeface(Typeface.DEFAULT_BOLD);
        textView4.setTextColor(i2);
        linearLayout.addView(textView4);
    }

    /* renamed from: lambda$addResultRow$57$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m8lambda$addResultRow$57$comexamplegallerysorterMainActivity(PhotoItem photoItem, View view) {
        openMedia(photoItem);
    }

    private View thumbnailCard(final PhotoItem photoItem) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, 0, dp(7), 0);
        linearLayout.setClickable(true);
        linearLayout.setFocusable(true);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m67lambda$thumbnailCard$58$comexamplegallerysorterMainActivity(photoItem, view);
            }
        });
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        linearLayout.addView(imageView, new LinearLayout.LayoutParams(dp(76), dp(76)));
        loadThumbnailInto(imageView, photoItem.uri, dp(76));
        TextView textView = new TextView(this);
        textView.setText("위치 없음");
        textView.setTextSize(11.0f);
        textView.setTextColor(-1);
        textView.setGravity(17);
        textView.setPadding(0, dp(5), 0, dp(5));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(76), -2);
        layoutParams.setMargins(0, dp(5), 0, 0);
        linearLayout.addView(textView, layoutParams);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1728053248);
        gradientDrawable.setCornerRadius(dp(11));
        textView.setBackground(gradientDrawable);
        return linearLayout;
    }

    /* renamed from: lambda$thumbnailCard$58$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m67lambda$thumbnailCard$58$comexamplegallerysorterMainActivity(PhotoItem photoItem, View view) {
        openMedia(photoItem);
    }

    private View emptyThumbnailCard(String str) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, 0, dp(7), 0);
        TextView textView = new TextView(this);
        textView.setText("");
        textView.setTextSize(24.0f);
        textView.setTextColor(-3418655);
        textView.setGravity(17);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(dp(76), dp(76)));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-460036);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -1906448);
        textView.setBackground(gradientDrawable);
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextSize(11.0f);
        textView2.setTextColor(-7035976);
        textView2.setGravity(17);
        textView2.setPadding(0, dp(4), 0, 0);
        linearLayout.addView(textView2, new LinearLayout.LayoutParams(dp(76), -2));
        return linearLayout;
    }

    private void loadThumbnailInto(final ImageView imageView, final Uri uri, final int i) {
        final String str = uri.toString() + "#" + i;
        imageView.setTag(str);
        imageView.setImageDrawable(thumbnailPlaceholder());
        Bitmap bitmap = this.thumbnailCache.get(str);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            if (this.thumbnailWorker.isShutdown() || this.thumbnailWorker.isTerminated()) {
                return;
            }
            try {
                this.thumbnailWorker.execute(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda47
                    @Override // java.lang.Runnable
                    public final void run() {
                        MainActivity.this.m33x8a0aaf5(uri, i, str, imageView);
                    }
                });
            } catch (RejectedExecutionException unused) {
            }
        }
    }

    /* renamed from: lambda$loadThumbnailInto$61$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m33x8a0aaf5(Uri uri, int i, final String str, final ImageView imageView) {
        try {
            final Bitmap bitmapLoadThumbnailBitmap = loadThumbnailBitmap(uri, i);
            if (bitmapLoadThumbnailBitmap == null) {
                throw new IllegalStateException("thumbnail decode returned null");
            }
            this.thumbnailCache.put(str, bitmapLoadThumbnailBitmap);
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda51
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.lambda$loadThumbnailInto$59(imageView, str, bitmapLoadThumbnailBitmap);
                }
            });
        } catch (Exception unused) {
            runOnUiThread(new Runnable() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda52
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m32xb182ba16(imageView, str);
                }
            });
        }
    }

    static /* synthetic */ void lambda$loadThumbnailInto$59(ImageView imageView, String str, Bitmap bitmap) {
        if (str.equals(imageView.getTag())) {
            imageView.setImageBitmap(bitmap);
        }
    }

    /* renamed from: lambda$loadThumbnailInto$60$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m32xb182ba16(ImageView imageView, String str) {
        if (str.equals(imageView.getTag())) {
            imageView.setImageDrawable(thumbnailPlaceholder());
        }
    }

    private Bitmap loadThumbnailBitmap(Uri uri, int i) {
        try {
            return getContentResolver().loadThumbnail(uri, new Size(i, i), null);
        } catch (Exception unused) {
            return this.decodeThumbnailFallback(uri, i);
        }
    }

    private Bitmap decodeThumbnailFallback(Uri uri, int i) {
        try {
            InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
            if (inputStreamOpenInputStream == null) {
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
                return null;
            }
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
                if (options.outWidth > 0 && options.outHeight > 0) {
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    options2.inSampleSize = calculateThumbnailSampleSize(options.outWidth, options.outHeight, i);
                    options2.inPreferredConfig = Bitmap.Config.RGB_565;
                    InputStream inputStreamOpenInputStream2 = getContentResolver().openInputStream(uri);
                    if (inputStreamOpenInputStream2 == null) {
                        if (inputStreamOpenInputStream2 != null) {
                            inputStreamOpenInputStream2.close();
                        }
                        if (inputStreamOpenInputStream != null) {
                            inputStreamOpenInputStream.close();
                        }
                        return null;
                    }
                    try {
                        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream2, null, options2);
                        if (inputStreamOpenInputStream2 != null) {
                            inputStreamOpenInputStream2.close();
                        }
                        if (inputStreamOpenInputStream != null) {
                            inputStreamOpenInputStream.close();
                        }
                        return bitmapDecodeStream;
                    } finally {
                    }
                }
                if (inputStreamOpenInputStream != null) {
                    inputStreamOpenInputStream.close();
                }
                return null;
            } finally {
            }
        } catch (Exception unused) {
            return null;
        }
    }

    private int calculateThumbnailSampleSize(int i, int i2, int i3) {
        int iMax = Math.max(i, i2);
        int i4 = 1;
        while (iMax / i4 > Math.max(i3 * 2, 1)) {
            i4 *= 2;
        }
        return Math.max(i4, 1);
    }

    private Drawable thumbnailPlaceholder() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1050881);
        gradientDrawable.setCornerRadius(dp(11));
        gradientDrawable.setStroke(1, -1709326);
        return gradientDrawable;
    }

    private TextView sectionTitle(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(16.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        return textView;
    }

    private void addWorkingBanner(LinearLayout linearLayout) {
        if (this.isWorking) {
            LinearLayout linearLayout2 = new LinearLayout(this);
            linearLayout2.setOrientation(1);
            linearLayout2.setPadding(dp(16), dp(13), dp(16), dp(13));
            linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(16)));
            applyCardBackground(linearLayout2);
            TextView textView = new TextView(this);
            String str = this.activeProgressLabel;
            if (str == null && (str = this.workingMessage) == null) {
                str = "분석 진행 중";
            }
            textView.setText(str);
            textView.setTextSize(15.0f);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextColor(-15656921);
            linearLayout2.addView(textView, matchWidthWithBottom(dp(4)));
            TextView textView2 = new TextView(this);
            int i = this.activeProgressTotal;
            if (i > 0) {
                textView2.setText(this.activeProgressCurrent + " / " + this.activeProgressTotal + " 완료 · " + Math.min(100, Math.max(0, Math.round((this.activeProgressCurrent * 100.0f) / i))) + "%");
            } else {
                textView2.setText("백그라운드에서 계속 진행됩니다.");
            }
            textView2.setTextSize(13.0f);
            textView2.setTextColor(-10193781);
            linearLayout2.addView(textView2, matchWidthWithBottom(dp(2)));
            TextView textView3 = new TextView(this);
            textView3.setText("백그라운드에서 계속 진행됩니다.");
            textView3.setTextSize(12.0f);
            textView3.setTextColor(-7035976);
            linearLayout2.addView(textView3, matchWidth());
        }
    }

    private void showSettingsScreen() {
        this.resultScreenMode = true;
        this.recentPlacesScreenMode = false;
        this.recentPlaceDetailMode = false;
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(-197377);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(18), dp(56), dp(18), dp(REQUEST_WRITE_VIDEOS));
        scrollView.addView(linearLayout);
        addListHeader(linearLayout, "설정");
        addWorkingBanner(linearLayout);
        addSettingsSourceFolderCard(linearLayout);
        addSettingsVideoMoveCard(linearLayout);
        linearLayout.addView(sectionTitle("데이터 관리"), matchWidthWithBottom(dp(10)));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(dp(16), dp(14), dp(14), dp(14));
        linearLayout2.setClickable(true);
        linearLayout2.setFocusable(true);
        linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda40
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m61x4b73e414(view);
            }
        });
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(18)));
        applyCardBackground(linearLayout2);
        TextView textView = new TextView(this);
        textView.setText("↻");
        textView.setTextSize(24.0f);
        textView.setTextColor(-9609738);
        textView.setGravity(17);
        linearLayout2.addView(textView, squareParams(dp(46)));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-922113);
        gradientDrawable.setCornerRadius(dp(16));
        textView.setBackground(gradientDrawable);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dp(14), 0, dp(8), 0);
        linearLayout2.addView(linearLayout3, weightedParams(1));
        TextView textView2 = new TextView(this);
        textView2.setText("발견 장소 다시 만들기");
        textView2.setTextSize(16.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-15656921);
        linearLayout3.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText("이미 정리된 앨범을 다시 읽어 장소 목록을 새로 생성합니다.");
        textView3.setTextSize(13.0f);
        textView3.setTextColor(-10193781);
        textView3.setLineSpacing(dp(2), 1.0f);
        textView3.setPadding(0, dp(3), 0, 0);
        linearLayout3.addView(textView3);
        TextView textView4 = new TextView(this);
        textView4.setText("›");
        textView4.setTextSize(28.0f);
        textView4.setTextColor(-7035976);
        textView4.setGravity(17);
        linearLayout2.addView(textView4, squareParams(dp(30)));
        setContentViewWithBottomTabs(scrollView, 2);
    }

    /* renamed from: lambda$showSettingsScreen$62$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m61x4b73e414(View view) {
        rebuildAlbumSummaryHistoryFromExistingAlbums();
    }

    private void addSettingsSourceFolderCard(LinearLayout linearLayout) {
        linearLayout.addView(sectionTitle("앨범 생성 옵션"), matchWidthWithBottom(dp(10)));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(dp(16), dp(14), dp(14), dp(14));
        linearLayout2.setClickable(true);
        linearLayout2.setFocusable(true);
        linearLayout2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda56
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m9xde27f92a(view);
            }
        });
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(18)));
        applyCardBackground(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout2.addView(linearLayout3, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText("분석할 폴더");
        textView.setTextSize(16.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        linearLayout3.addView(textView);
        TextView textView2 = new TextView(this);
        this.sourceFoldersText = textView2;
        textView2.setTextSize(13.0f);
        this.sourceFoldersText.setTextColor(-10193781);
        this.sourceFoldersText.setPadding(0, dp(3), dp(8), 0);
        linearLayout3.addView(this.sourceFoldersText);
        updateSourceFoldersText();
        TextView textView3 = new TextView(this);
        textView3.setText("›");
        textView3.setTextSize(28.0f);
        textView3.setTextColor(-7035976);
        textView3.setGravity(17);
        linearLayout2.addView(textView3, squareParams(dp(30)));
    }

    /* renamed from: lambda$addSettingsSourceFolderCard$63$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m9xde27f92a(View view) {
        showSourceFolderDialog();
    }

    private void addSettingsVideoMoveCard(LinearLayout linearLayout) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(dp(16), dp(14), dp(14), dp(14));
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(18)));
        applyCardBackground(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout2.addView(linearLayout3, weightedParams(1));
        TextView textView = new TextView(this);
        textView.setText("동영상도 앨범으로 이동");
        textView.setTextSize(16.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(-15656921);
        linearLayout3.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("끄면 동영상은 원본 폴더에 그대로 두고 사진만 정리합니다.");
        textView2.setTextSize(13.0f);
        textView2.setTextColor(-10193781);
        textView2.setLineSpacing(dp(2), 1.0f);
        textView2.setPadding(0, dp(3), dp(8), 0);
        linearLayout3.addView(textView2);
        Switch r7 = new Switch(this);
        r7.setChecked(shouldMoveVideos());
        r7.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda41
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MainActivity.this.m10x7cd57cde(compoundButton, z);
            }
        });
        linearLayout2.addView(r7);
    }

    /* renamed from: lambda$addSettingsVideoMoveCard$64$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m10x7cd57cde(CompoundButton compoundButton, boolean z) {
        setMoveVideos(z);
    }

    private void addBottomTabs(LinearLayout linearLayout, int i) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(17);
        linearLayout2.setPadding(dp(12), dp(8), dp(12), dp(8));
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(4)));
        applyCardBackground(linearLayout2);
        addBottomTab(linearLayout2, "home", "홈", i == 0, new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda71
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m2lambda$addBottomTabs$65$comexamplegallerysorterMainActivity(view);
            }
        });
        addBottomTab(linearLayout2, "grid", "정리 기록", i == 1, new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda72
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m3lambda$addBottomTabs$66$comexamplegallerysorterMainActivity(view);
            }
        });
        addBottomTab(linearLayout2, "settings", "설정", i == 2, new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m4lambda$addBottomTabs$67$comexamplegallerysorterMainActivity(view);
            }
        });
    }

    /* renamed from: lambda$addBottomTabs$65$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m2lambda$addBottomTabs$65$comexamplegallerysorterMainActivity(View view) {
        returnToMainScreen();
    }

    /* renamed from: lambda$addBottomTabs$66$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m3lambda$addBottomTabs$66$comexamplegallerysorterMainActivity(View view) {
        showRecentPlacesScreen();
    }

    /* renamed from: lambda$addBottomTabs$67$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m4lambda$addBottomTabs$67$comexamplegallerysorterMainActivity(View view) {
        showSettingsScreen();
    }

    private void setContentViewWithBottomTabs(ScrollView scrollView, int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-197377);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(dp(18), dp(8), dp(18), dp(34));
        linearLayout.addView(linearLayout2, matchWidth());
        addBottomTabs(linearLayout2, i);
        setContentView(linearLayout);
    }

    private void addBottomTab(LinearLayout linearLayout, String str, String str2, boolean z, View.OnClickListener onClickListener) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(17);
        linearLayout2.setPadding(dp(12), dp(9), dp(12), dp(9));
        linearLayout2.setClickable(true);
        linearLayout2.setFocusable(true);
        linearLayout2.setOnClickListener(onClickListener);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        if (z) {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(-922113);
            gradientDrawable.setCornerRadius(dp(REQUEST_WRITE_VIDEOS));
            linearLayout2.setBackground(gradientDrawable);
        }
        TextView textView = new TextView(this);
        textView.setText(str2);
        textView.setTextSize(13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setTextColor(z ? -9609738 : -10193781);
        textView.setCompoundDrawablePadding(dp(6));
        textView.setCompoundDrawables(new IconBubbleDrawable(str, z ? -9609738 : -10193781, 0, dp(18)), null, null, null);
        linearLayout2.addView(textView);
    }

    private void addNavItem(LinearLayout linearLayout, String str, String str2, int i) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(17);
        linearLayout2.setPadding(0, 0, 0, 0);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(23.0f);
        textView.setGravity(17);
        textView.setTextColor(i);
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextSize(11.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setGravity(17);
        textView2.setTextColor(i);
        linearLayout2.addView(textView2);
    }

    private LinearLayout actionRow() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        return linearLayout;
    }

    private LinearLayout.LayoutParams rowButtonParams(boolean z) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, dp(84), 1.0f);
        layoutParams.setMargins(z ? 0 : dp(6), 0, z ? dp(6) : 0, 0);
        return layoutParams;
    }

    private LinearLayout.LayoutParams fullWidthButtonParams(int i, int i2) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, i2);
        layoutParams.setMargins(0, 0, 0, i);
        return layoutParams;
    }

    private LinearLayout.LayoutParams progressBarParams() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(14));
        layoutParams.setMargins(0, dp(8), 0, dp(10));
        return layoutParams;
    }

    private LinearLayout.LayoutParams compactButtonParams(boolean z) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, dp(54), 1.0f);
        layoutParams.setMargins(z ? 0 : dp(6), 0, z ? dp(6) : 0, 0);
        return layoutParams;
    }

    private LinearLayout.LayoutParams dialogButtonParams(boolean z) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
        layoutParams.setMargins(z ? 0 : dp(5), 0, z ? dp(5) : 0, 0);
        return layoutParams;
    }

    private LinearLayout.LayoutParams squareParams(int i) {
        return new LinearLayout.LayoutParams(i, i);
    }

    private LinearLayout.LayoutParams weightedParams(int i) {
        return new LinearLayout.LayoutParams(0, -2, i);
    }

    private void styleActionButton(Button button, CharSequence charSequence, String str, int i, int i2, int i3) {
        button.setText(charSequence);
        button.setTextSize(16.0f);
        button.setAllCaps(false);
        button.setTextColor(-14735049);
        button.setGravity(16);
        button.setTypeface(Typeface.DEFAULT);
        button.setIncludeFontPadding(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(14), dp(6), dp(10), dp(6));
        button.setCompoundDrawablePadding(dp(10));
        button.setCompoundDrawables(new IconBubbleDrawable(str, i3, softenColor(i3), dp(42)), null, null, null);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -1709326);
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void stylePurpleCtaButton(Button button, CharSequence charSequence) {
        button.setText(charSequence);
        button.setTextSize(16.0f);
        button.setAllCaps(false);
        button.setTextColor(-1);
        button.setGravity(17);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setIncludeFontPadding(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(14), dp(6), dp(14), dp(6));
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{-9609738, -8635667});
        gradientDrawable.setCornerRadius(dp(14));
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void styleHeroStartButton(Button button, CharSequence charSequence) {
        button.setText(charSequence);
        button.setTextSize(17.0f);
        button.setAllCaps(false);
        button.setTextColor(-1);
        button.setGravity(16);
        button.setTypeface(Typeface.DEFAULT);
        button.setIncludeFontPadding(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(24), dp(18), dp(18), dp(18));
        button.setCompoundDrawablePadding(dp(18));
        button.setCompoundDrawables(new IconBubbleDrawable("gallery", -9609738, -1, dp(58)), null, new IconBubbleDrawable("arrow", -9609738, -1, dp(42)), null);
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{-12619789, -8635667});
        gradientDrawable.setCornerRadius(dp(18));
        gradientDrawable.setStroke(1, -9609738);
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void stylePrimaryActionButton(Button button, CharSequence charSequence, int i) {
        button.setText(charSequence);
        button.setTextSize(15.0f);
        button.setAllCaps(false);
        button.setTextColor(i);
        button.setGravity(17);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(18));
        gradientDrawable.setStroke(2, -4725561);
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void styleDialogPrimaryButton(Button button) {
        button.setTextSize(15.0f);
        button.setAllCaps(false);
        button.setTextColor(-1);
        button.setGravity(17);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-12619789);
        gradientDrawable.setCornerRadius(dp(16));
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void styleDialogSecondaryButton(Button button) {
        button.setTextSize(14.0f);
        button.setAllCaps(false);
        button.setTextColor(-14326805);
        button.setGravity(17);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1050881);
        gradientDrawable.setCornerRadius(dp(16));
        gradientDrawable.setStroke(1, -2562817);
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void styleTertiaryActionButton(Button button, CharSequence charSequence, int i) {
        button.setText(charSequence);
        button.setTextSize(14.0f);
        button.setAllCaps(false);
        button.setTextColor(i);
        button.setGravity(17);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -993058);
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void styleSubtleActionButton(Button button, String str) {
        button.setText(str);
        button.setTextSize(13.0f);
        button.setAllCaps(false);
        button.setTextColor(-10193781);
        button.setGravity(17);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -1709326);
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private SpannableString actionText(String str, String str2) {
        String str3 = str + "\n" + str2;
        SpannableString spannableString = new SpannableString(str3);
        int length = str.length();
        int i = length + 1;
        spannableString.setSpan(new StyleSpan(1), 0, length, 33);
        spannableString.setSpan(new RelativeSizeSpan(1.08f), 0, length, 33);
        spannableString.setSpan(new RelativeSizeSpan(0.82f), i, str3.length(), 33);
        spannableString.setSpan(new ForegroundColorSpan(-7035976), i, str3.length(), 33);
        return spannableString;
    }

    private SpannableString compactActionText(String str, String str2) {
        String str3 = str + "  " + str2;
        SpannableString spannableString = new SpannableString(str3);
        int length = str.length();
        spannableString.setSpan(new RelativeSizeSpan(1.2f), 0, length, 33);
        spannableString.setSpan(new StyleSpan(1), length + 2, str3.length(), 33);
        return spannableString;
    }

    private void emphasizePrimaryAction(Button button) {
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private void styleDangerButton(Button button) {
        button.setTextSize(14.0f);
        button.setAllCaps(false);
        button.setTextColor(-2024120);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-3598);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -13355);
        button.setBackground(gradientDrawable);
    }

    private void styleProgressCancelButton(Button button) {
        button.setTextSize(13.0f);
        button.setAllCaps(false);
        button.setTextColor(-10193781);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-920071);
        gradientDrawable.setCornerRadius(dp(14));
        gradientDrawable.setStroke(1, -1906448);
        button.setBackground(gradientDrawable);
        button.setElevation(0.0f);
        button.setStateListAnimator(null);
    }

    private Drawable progressDrawable() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1906448);
        gradientDrawable.setCornerRadius(dp(8));
        GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setColor(-12010632);
        gradientDrawable2.setCornerRadius(dp(8));
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{gradientDrawable, new ClipDrawable(gradientDrawable2, 8388611, 1)});
        layerDrawable.setId(0, android.R.id.background);
        layerDrawable.setId(1, android.R.id.progress);
        return layerDrawable;
    }

    private LinearLayout.LayoutParams matchWidthWithBottom(int i) {
        LinearLayout.LayoutParams layoutParamsMatchWidth = matchWidth();
        layoutParamsMatchWidth.setMargins(0, 0, 0, i);
        return layoutParamsMatchWidth;
    }

    private void applyCardBackground(View view) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(getColor(R.color.surface));
        gradientDrawable.setCornerRadius(dp(24));
        gradientDrawable.setStroke(1, -1511945);
        view.setBackground(gradientDrawable);
        view.setElevation(0.0f);
        view.setStateListAnimator(null);
    }

    private void applyGradientBackground(View view, int i, int i2, int i3) {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{i, i2});
        gradientDrawable.setCornerRadius(i3);
        view.setBackground(gradientDrawable);
    }

    private void showResultScreen() {
        String str;
        int i;
        this.resultScreenMode = true;
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(-197377);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(18), dp(56), dp(18), dp(REQUEST_WRITE_VIDEOS));
        scrollView.addView(linearLayout);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout.addView(linearLayout2, matchWidthWithBottom(dp(18)));
        TextView textView = new TextView(this);
        textView.setText("‹");
        textView.setTextSize(36.0f);
        textView.setGravity(17);
        textView.setTextColor(-15656921);
        textView.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m57x247527c8(view);
            }
        });
        linearLayout2.addView(textView, squareParams(dp(44)));
        TextView textView2 = new TextView(this);
        textView2.setText("정리 결과");
        textView2.setTextSize(22.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(-15656921);
        linearLayout2.addView(textView2, weightedParams(1));
        addWorkingBanner(linearLayout);
        int size = this.previewItems.size();
        Iterator<PhotoItem> it = this.previewItems.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            if (it.next().noLocation) {
                i2++;
            }
        }
        int iCountCopyableItems = countCopyableItems(this.previewItems);
        int iCountRecentlySortedItems = countRecentlySortedItems(this.previewItems);
        int iCountRecentlySortedGroups = countRecentlySortedGroups(this.previewItems);
        countAlreadySortedItems(this.previewItems);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setGravity(17);
        linearLayout3.setPadding(dp(18), dp(18), dp(18), dp(18));
        linearLayout.addView(linearLayout3, matchWidthWithBottom(dp(14)));
        boolean z = this.copyCompletedMode;
        applyGradientBackground(linearLayout3, z ? -11550817 : -8477448, z ? -13652327 : -10780696, dp(16));
        TextView textView3 = new TextView(this);
        textView3.setText(z ? "✓ 정리 완료" : "✓ 확인 완료");
        textView3.setTextSize(20.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setTextColor(-1);
        textView3.setGravity(17);
        linearLayout3.addView(textView3, matchWidthWithBottom(dp(8)));
        TextView textView4 = new TextView(this);
        if (this.copyCompletedMode) {
            str = "총 " + iCountRecentlySortedItems + "개 정리됨";
        } else {
            str = z ? "앨범에서 결과를 확인해요." : "정리할 항목을 확인해요.";
        }
        textView4.setText(str);
        if (this.copyCompletedMode) {
            textView4.setText("새로 발견한 장소 " + iCountRecentlySortedGroups + "개\n총 " + iCountRecentlySortedItems + "개 사진 정리");
        }
        textView4.setTextSize(13.0f);
        textView4.setTextColor(-268435457);
        textView4.setGravity(17);
        linearLayout3.addView(textView4, matchWidth());
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setPadding(dp(14), dp(12), dp(14), dp(12));
        linearLayout.addView(linearLayout4, matchWidthWithBottom(dp(14)));
        applyCardBackground(linearLayout4);
        if (this.copyCompletedMode) {
            i = 12;
            addPlainStat(linearLayout4, "folder", "정리됨", iCountRecentlySortedItems + "개", -15293622, true);
            addPlainStat(linearLayout4, "alert", "위치 없음", i2 + "개", -680437, false);
        } else {
            i = 12;
            addPlainStat(linearLayout4, "grid", "전체", size + "개", -14326805, true);
            addPlainStat(linearLayout4, "check", "정리 예정", iCountCopyableItems + "개", -15293622, true);
            addPlainStat(linearLayout4, "alert", "위치 없음", i2 + "개", -680437, false);
        }
        if (hasCopyableItems(this.previewItems) && !this.copyCompletedMode) {
            Button button = new Button(this);
            button.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda23
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.m58x7b9318a7(view);
                }
            });
            styleActionButton(button, actionText("바로 정리하기", "사진은 복사하고 동영상은 이동"), "folder", -3542826, -10236022, -15368131);
            linearLayout.addView(button, matchWidthWithBottom(dp(14)));
        }
        linearLayout.addView(sectionTitle(this.copyCompletedMode ? "이번에 발견한 장소" : "정리될 앨범"), matchWidthWithBottom(dp(10)));
        LinearLayout linearLayout5 = new LinearLayout(this);
        this.resultList = linearLayout5;
        linearLayout5.setOrientation(1);
        this.resultList.setPadding(dp(14), dp(6), dp(14), dp(6));
        linearLayout.addView(this.resultList, matchWidthWithBottom(dp(14)));
        applyCardBackground(this.resultList);
        linearLayout.addView(sectionTitle("위치 정보 없는 항목"), matchWidthWithBottom(dp(10)));
        LinearLayout linearLayout6 = new LinearLayout(this);
        linearLayout6.setOrientation(1);
        linearLayout6.setPadding(dp(i), dp(i), dp(i), dp(i));
        linearLayout.addView(linearLayout6, matchWidthWithBottom(dp(14)));
        applyCardBackground(linearLayout6);
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        linearLayout6.addView(horizontalScrollView, matchWidth());
        LinearLayout linearLayout7 = new LinearLayout(this);
        this.unclassifiedPreviewRow = linearLayout7;
        linearLayout7.setOrientation(0);
        horizontalScrollView.addView(this.unclassifiedPreviewRow);
        TextView textView5 = new TextView(this);
        this.logText = textView5;
        textView5.setTextSize(13.0f);
        this.logText.setTextColor(-9735552);
        this.logText.setPadding(0, dp(10), 0, 0);
        linearLayout6.addView(this.logText, matchWidth());
        if (this.previewItems.isEmpty()) {
            addEmptyResultHint();
            this.logText.setText("아직 분석한 항목이 없습니다.");
        } else {
            renderPreviewResults(this.previewItems);
        }
        Button button2 = new Button(this);
        button2.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m59xf825cbd1(view);
            }
        });
        styleActionButton(button2, "갤러리에서 보기", "gallery", -1050881, -4203522, -14326805);
        linearLayout.addView(button2, matchWidthWithBottom(dp(10)));
        Button button3 = new Button(this);
        button3.setOnClickListener(new View.OnClickListener() { // from class: com.example.gallerysorter.MainActivity$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m60x4f43bcb0(view);
            }
        });
        styleActionButton(button3, "다시 확인하기", "refresh", -658433, -2238722, -8635667);
        linearLayout.addView(button3, matchWidth());
        setContentViewWithBottomTabs(scrollView, -1);
    }

    /* renamed from: lambda$showResultScreen$68$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m57x247527c8(View view) {
        returnToMainScreen();
    }

    /* renamed from: lambda$showResultScreen$69$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m58x7b9318a7(View view) {
        startCopyFromResultScreen();
    }

    /* renamed from: lambda$showResultScreen$70$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m59xf825cbd1(View view) {
        openGallery();
    }

    /* renamed from: lambda$showResultScreen$71$com-example-gallerysorter-MainActivity, reason: not valid java name */
    /* synthetic */ void m60x4f43bcb0(View view) {
        buildUi();
        ensureReadPermission();
        restoreMainUiFromState();
        runPreview();
    }

    private void addPlainStat(LinearLayout linearLayout, String str, String str2, String str3, int i, boolean z) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(17);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(new IconBubbleDrawable(str, i, softenColor(i), dp(34)));
        linearLayout2.addView(imageView, squareParams(dp(34)));
        TextView textView = new TextView(this);
        textView.setText(str2);
        textView.setTextSize(11.0f);
        textView.setTextColor(-10193781);
        textView.setGravity(17);
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str3);
        textView2.setTextSize(20.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setTextColor(i);
        textView2.setGravity(17);
        linearLayout2.addView(textView2);
        if (z) {
            View view = new View(this);
            view.setBackgroundColor(-1710101);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(1, dp(54));
            layoutParams.setMargins(dp(2), 0, dp(2), 0);
            linearLayout.addView(view, layoutParams);
        }
    }

    private void openGallery() {
        Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage("com.sec.android.gallery3d");
        if (launchIntentForPackage != null) {
            try {
                startActivity(launchIntentForPackage);
                return;
            } catch (ActivityNotFoundException unused) {
            }
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.APP_GALLERY");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unused2) {
            try {
                startActivity(new Intent("android.intent.action.VIEW", MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            } catch (ActivityNotFoundException unused3) {
                showToast("갤러리 앱을 열 수 없습니다.");
            }
        }
    }

    private void openAlbumInGallery(StoredAlbumSummary storedAlbumSummary) {
        if (storedAlbumSummary == null || storedAlbumSummary.relativePath == null || storedAlbumSummary.relativePath.trim().isEmpty()) {
            openGallery();
            return;
        }
        MediaOpenTarget mediaOpenTargetFindFirstMediaInAlbum = findFirstMediaInAlbum(storedAlbumSummary.relativePath);
        if (mediaOpenTargetFindFirstMediaInAlbum != null) {
            openMediaUri(mediaOpenTargetFindFirstMediaInAlbum.uri, mediaOpenTargetFindFirstMediaInAlbum.mimeType);
        } else if (storedAlbumSummary.thumbnailUri != null && !storedAlbumSummary.thumbnailUri.trim().isEmpty()) {
            openMediaUri(Uri.parse(storedAlbumSummary.thumbnailUri), "image/*");
        } else {
            openGallery();
        }
    }

    private void openAlbumInSamsungGallery(StoredAlbumSummary storedAlbumSummary) {
        if (storedAlbumSummary == null || storedAlbumSummary.relativePath == null || storedAlbumSummary.relativePath.trim().isEmpty()) {
            showToast("앨범 정보를 찾을 수 없어 갤러리를 엽니다.");
            openGallery();
            return;
        }
        Long lFindAlbumBucketId = findAlbumBucketId(storedAlbumSummary.relativePath);
        if (lFindAlbumBucketId == null) {
            showToast("앨범이 없거나 비어 있어 갤러리를 엽니다.");
            openGallery();
            return;
        }
        Intent intent = new Intent("com.android.gallery.action.SHORTCUT_ALBUM_VIEW");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setClassName("com.sec.android.gallery3d", "com.samsung.android.gallery.app.activity.external.GalleryExternalActivity");
        intent.putExtra("ALBUM_ID", lFindAlbumBucketId.intValue());
        intent.putExtra("key-album-type", 0);
        intent.putExtra("android.intent.extra.shortcut.NAME", storedAlbumSummary.albumName);
        intent.putExtra("key-show-album-info", false);
        intent.putExtra("IS_VIRTUAL_ALBUM", false);
        try {
            if (intent.resolveActivity(getPackageManager()) == null) {
                showToast("삼성 갤러리 직접 이동을 지원하지 않아 일반 갤러리를 엽니다.");
                openGallery();
            } else {
                startActivity(intent);
            }
        } catch (Exception unused) {
            showToast("해당 앨범을 열 수 없어 일반 갤러리를 엽니다.");
            openGallery();
        }
    }

    private Long findAlbumBucketId(String str) {
        Long lFindAlbumBucketId = findAlbumBucketId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "relative_path", "bucket_id", str);
        return lFindAlbumBucketId != null ? lFindAlbumBucketId : findAlbumBucketId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "relative_path", "bucket_id", str);
    }

    private Long findAlbumBucketId(Uri uri, String str, String str2, String str3) {
        try {
            Cursor cursorQuery = getContentResolver().query(uri, new String[]{str2}, visibleMediaSelection(str + " = ?"), new String[]{str3}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        int columnIndex = cursorQuery.getColumnIndex(str2);
                        Long lValueOf = (columnIndex < 0 || cursorQuery.isNull(columnIndex)) ? null : Long.valueOf(cursorQuery.getLong(columnIndex));
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return lValueOf;
                    }
                } finally {
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Exception unused) {
        }
        return null;
    }

    private MediaOpenTarget findFirstMediaInAlbum(String str) {
        MediaOpenTarget mediaOpenTargetFindFirstMediaInAlbum = findFirstMediaInAlbum(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "_id", "relative_path", "mime_type", "datetaken", "date_modified", str);
        return mediaOpenTargetFindFirstMediaInAlbum != null ? mediaOpenTargetFindFirstMediaInAlbum : findFirstMediaInAlbum(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "_id", "relative_path", "mime_type", "datetaken", "date_modified", str);
    }

    private List<Uri> loadLatestAlbumPreviewUris(String str, int i) {
        ArrayList arrayList = new ArrayList();
        addLatestAlbumPreviewUris(arrayList, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "_id", "relative_path", "datetaken", "date_modified", str, i);
        if (arrayList.size() < i) {
            addLatestAlbumPreviewUris(arrayList, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "_id", "relative_path", "datetaken", "date_modified", str, i);
        }
        return arrayList;
    }

    private void addLatestAlbumPreviewUris(List<Uri> list, Uri uri, String str, String str2, String str3, String str4, String str5, int i) {
        if (str5 == null || str5.trim().isEmpty() || list.size() >= i) {
            return;
        }
        try {
            Cursor cursorQuery = getContentResolver().query(uri, new String[]{str}, visibleMediaSelection(str2 + " = ?"), new String[]{str5}, str3 + " DESC, " + str4 + " DESC");
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(str);
                while (cursorQuery.moveToNext() && list.size() < i) {
                    list.add(ContentUris.withAppendedId(uri, cursorQuery.getLong(columnIndexOrThrow)));
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } finally {
            }
        } catch (Exception unused) {
        }
    }

    private MediaOpenTarget findFirstMediaInAlbum(Uri uri, String str, String str2, String str3, String str4, String str5, String str6) {
        try {
            Cursor cursorQuery = getContentResolver().query(uri, new String[]{str, str3}, visibleMediaSelection(str2 + " = ?"), new String[]{str6}, str4 + " DESC, " + str5 + " DESC");
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        MediaOpenTarget mediaOpenTarget = new MediaOpenTarget(ContentUris.withAppendedId(uri, cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow(str))), cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(str3)));
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return mediaOpenTarget;
                    }
                } finally {
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Exception unused) {
        }
        return null;
    }

    private void openMediaUri(Uri uri, String str) {
        Intent intent = new Intent("android.intent.action.VIEW");
        if (str == null || str.trim().isEmpty()) {
            str = "*/*";
        }
        intent.setDataAndType(uri, str);
        intent.addFlags(1);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            openGallery();
        }
    }

    private void openPhoto(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(1);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            openGallery();
        }
    }

    private void openMedia(PhotoItem photoItem) {
        Intent intent = new Intent("android.intent.action.VIEW");
        String str = photoItem.video ? "video/*" : "image/*";
        if (photoItem.mimeType != null && !photoItem.mimeType.trim().isEmpty()) {
            str = photoItem.mimeType;
        }
        intent.setDataAndType(photoItem.uri, str);
        intent.addFlags(1);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            openGallery();
        }
    }

    private int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    private void setWorking(boolean z, String str) {
        this.isWorking = z;
        this.workingMessage = z ? str : null;
        if (!z) {
            updateProgress(null, 0, 0);
        }
        applyWorkingStateToViews();
        if (str != null) {
            this.summaryText.setText(str);
        }
    }

    private boolean blockNavigationWhileWorking() {
        if (!this.isWorking) {
            return false;
        }
        showToast("정리 중에는 화면을 이동할 수 없어요.");
        applyWorkingStateToViews();
        return true;
    }

    private void applyWorkingStateToViews() {
        String str;
        String str2;
        TextView textView;
        LinearLayout linearLayout = this.previewButton;
        if (linearLayout != null) {
            linearLayout.setEnabled(!this.isWorking);
        }
        boolean z = (this.isWorking || !hasCopyableItems(this.previewItems) || this.copyCompletedMode) ? false : true;
        Button button = this.copyButton;
        if (button != null) {
            button.setEnabled(z);
            this.copyButton.setVisibility(z ? 0 : 8);
        }
        Button button2 = this.deleteOriginalsButton;
        if (button2 != null) {
            button2.setEnabled((this.isWorking || !this.copyCompletedMode || this.copiedOriginalUris.isEmpty()) ? false : true);
        }
        Button button3 = this.cancelButton;
        if (button3 != null) {
            button3.setEnabled(this.isWorking);
            this.cancelButton.setVisibility(this.isWorking ? 0 : 8);
        }
        ProgressBar progressBar = this.progressBar;
        if (progressBar != null && (progressBar.getTag() instanceof View)) {
            ((View) this.progressBar.getTag()).setVisibility(this.isWorking ? 0 : 8);
        }
        if (this.isWorking && (str2 = this.workingMessage) != null && (textView = this.summaryText) != null) {
            textView.setText(str2);
        }
        if (!this.isWorking || (str = this.activeProgressLabel) == null) {
            return;
        }
        renderProgress(str, this.activeProgressCurrent, this.activeProgressTotal, this.activeProgressContext);
    }

    private void updateProgress(String str, int i, int i2) {
        updateProgress(str, i, i2, null);
    }

    private void updateProgress(String str, int i, int i2, String str2) {
        this.activeProgressLabel = str;
        this.activeProgressCurrent = i;
        this.activeProgressTotal = i2;
        this.activeProgressContext = str2;
        renderProgress(str, i, i2, str2);
    }

    private void renderProgress(String str, int i, int i2, String str2) {
        ProgressBar progressBar = this.progressBar;
        if (progressBar == null || this.progressText == null || this.progressPercentText == null || this.progressDetailText == null) {
            return;
        }
        if (str == null || i2 <= 0) {
            progressBar.setIndeterminate(false);
            this.progressBar.setProgress(0);
            this.progressText.setText("");
            this.progressPercentText.setText("");
            this.progressDetailText.setText("");
            return;
        }
        progressBar.setIndeterminate(false);
        this.progressBar.setMax(i2);
        this.progressBar.setProgress(Math.min(i, i2));
        int iRound = Math.round((Math.min(i, i2) * 100.0f) / i2);
        this.progressText.setText(str);
        this.progressPercentText.setText(iRound + "%");
        String str3 = i + " / " + i2 + "개 완료";
        if (str2 != null && !str2.trim().isEmpty()) {
            str3 = str3 + " · " + str2;
        }
        this.progressDetailText.setText(str3);
    }

    private void requestCancel() {
        this.cancelRequested = true;
        this.cancelButton.setEnabled(false);
        this.summaryText.setText("취소 요청 중...");
    }

    private void showToast(String str) {
        Toast.makeText(this, str, 0).show();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        this.worker.shutdownNow();
        this.thumbnailWorker.shutdownNow();
    }

    private static class AlbumSummary {
        final String albumName;
        final String relativePath;
        String thumbnailUri;
        final DateRange dateRange = new DateRange();
        int itemCount = 0;
        long thumbnailDateMillis = 0;

        AlbumSummary(String str, String str2, String str3) {
            this.albumName = str;
            this.relativePath = str2;
            this.thumbnailUri = str3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class StoredAlbumSummary {
        final String albumName;
        final String createdAt;
        final long createdAtMillis;
        final String endDate;
        final int itemCount;
        final String relativePath;
        final String startDate;
        final String thumbnailUri;

        StoredAlbumSummary(String str, String str2, int i, String str3, String str4, String str5, String str6, long j) {
            this.albumName = str == null ? "" : str;
            this.relativePath = str2 == null ? "" : str2;
            this.itemCount = i;
            this.startDate = str3;
            this.endDate = str4;
            this.thumbnailUri = str5;
            this.createdAt = str6;
            this.createdAtMillis = j;
        }

        static StoredAlbumSummary fromJson(JSONObject jSONObject) {
            return new StoredAlbumSummary(jSONObject.optString("albumName", ""), jSONObject.optString("relativePath", ""), jSONObject.optInt("itemCount", 0), jSONObject.optString("startDate", null), jSONObject.optString("endDate", null), jSONObject.optString("thumbnailUri", null), jSONObject.optString("createdAt", null), jSONObject.optLong("createdAtMillis", 0L));
        }

        static StoredAlbumSummary fromAlbumSummary(AlbumSummary albumSummary, long j) {
            if (albumSummary == null) {
                return new StoredAlbumSummary("", "", 0, null, null, null, null, 0L);
            }
            String str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date(j));
            Date date = albumSummary.dateRange.start;
            Date date2 = albumSummary.dateRange.end;
            return new StoredAlbumSummary(albumSummary.albumName, albumSummary.relativePath, albumSummary.itemCount, date == null ? null : new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(date), date2 == null ? null : new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(date2), albumSummary.thumbnailUri, str, j);
        }
    }

    private static class MediaOpenTarget {
        final String mimeType;
        final Uri uri;

        MediaOpenTarget(Uri uri, String str) {
            this.uri = uri;
            this.mimeType = str;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class PhotoItem {
        final boolean duplicateInTarget;
        final String locationKey;
        final String mimeType;
        final String name;
        final boolean noLocation;
        final Date takenAt;
        final boolean targetExists;
        final String targetRelativePath;
        final Uri uri;
        final boolean video;

        PhotoItem(Uri uri, String str, String str2, Date date, String str3, boolean z, boolean z2, boolean z3, String str4, boolean z4) {
            this.uri = uri;
            this.name = str;
            this.mimeType = str2;
            this.takenAt = date;
            this.locationKey = str3;
            this.noLocation = z;
            this.targetExists = z2;
            this.duplicateInTarget = z3;
            this.targetRelativePath = str4;
            this.video = z4;
        }
    }

    private static class LocationResult {
        final String folderKey;
        final Date takenAt;

        LocationResult(Date date, String str) {
            this.takenAt = date;
            this.folderKey = str;
        }
    }

    private static class ExifReadResult {
        double latitude;
        double longitude;
        Date takenAt;

        private ExifReadResult() {
        }
    }

    private static class VideoMetadataResult {
        double latitude;
        double longitude;
        Date takenAt;

        private VideoMetadataResult() {
        }
    }

    private static class DateRange {
        Date end;
        Date start;

        private DateRange() {
        }

        void include(Date date) {
            if (date == null) {
                return;
            }
            Date date2 = this.start;
            if (date2 == null || date.before(date2)) {
                this.start = date;
            }
            Date date3 = this.end;
            if (date3 == null || date.after(date3)) {
                this.end = date;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class SourceFolder {
        final int count;
        final String displayName;
        final String relativePath;

        SourceFolder(String str, String str2, int i) {
            this.relativePath = str;
            this.displayName = str2;
            this.count = i;
        }
    }

    private static class AlbumFolder {
        final String folderName;
        final String matchName;
        final String relativePath;

        AlbumFolder(String str, String str2, String str3) {
            this.relativePath = str;
            this.folderName = str2;
            this.matchName = str3;
        }
    }

    private static class IconBubbleDrawable extends Drawable {
        private final int backgroundColor;
        private final String icon;
        private final int iconColor;
        private final int size;
        private final Paint paint = new Paint(1);
        private final RectF rect = new RectF();
        private final Path path = new Path();

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }

        IconBubbleDrawable(String str, int i, int i2, int i3) {
            this.icon = str;
            this.iconColor = i;
            this.backgroundColor = i2;
            this.size = i3;
            setBounds(0, 0, i3, i3);
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            float f = this.size * 0.27f;
            this.paint.setStyle(Paint.Style.FILL);
            if (Color.alpha(this.backgroundColor) > 0) {
                this.paint.setColor(this.backgroundColor);
                RectF rectF = this.rect;
                int i = this.size;
                rectF.set(0.0f, 0.0f, i, i);
                canvas.drawRoundRect(this.rect, f, f, this.paint);
            }
            this.paint.setColor(this.iconColor);
            this.paint.setStrokeWidth(Math.max(2.0f, this.size * 0.075f));
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
            this.paint.setStyle(Paint.Style.STROKE);
            drawGlyph(canvas, this.icon);
        }

        private void drawGlyph(Canvas canvas, String str) {
            float f = this.size;
            float f2 = f / 2.0f;
            String str2 = str == null ? "" : str;
            if ("photoLibrary".equals(str2)) {
                float f3 = f * 0.25f;
                this.rect.set(f3, 0.29f * f, 0.75f * f, 0.73f * f);
                float f4 = 0.06f * f;
                canvas.drawRoundRect(this.rect, f4, f4, this.paint);
                this.rect.set(0.18f * f, 0.23f * f, 0.68f * f, 0.67f * f);
                canvas.drawRoundRect(this.rect, f4, f4, this.paint);
                canvas.drawCircle(0.34f * f, 0.38f * f, 0.04f * f, this.paint);
                float f5 = f * 0.39f;
                float f6 = f * 0.49f;
                canvas.drawLine(f3, f * 0.61f, f5, f6, this.paint);
                float f7 = f * 0.5f;
                float f8 = f * 0.59f;
                canvas.drawLine(f5, f6, f7, f8, this.paint);
                canvas.drawLine(f7, f8, f * 0.62f, f * 0.45f, this.paint);
                return;
            }
            if ("camera".equals(str2)) {
                float f9 = f * 0.34f;
                this.rect.set(0.23f * f, f9, 0.77f * f, 0.72f * f);
                float f10 = 0.08f * f;
                canvas.drawRoundRect(this.rect, f10, f10, this.paint);
                canvas.drawCircle(f2, 0.53f * f, 0.11f * f, this.paint);
                float f11 = f * 0.42f;
                float f12 = f * 0.25f;
                canvas.drawLine(f * 0.36f, f9, f11, f12, this.paint);
                float f13 = f * 0.58f;
                canvas.drawLine(f11, f12, f13, f12, this.paint);
                canvas.drawLine(f13, f12, f * 0.64f, f9, this.paint);
                return;
            }
            if ("calendar".equals(str2)) {
                float f14 = f * 0.24f;
                float f15 = f * 0.76f;
                this.rect.set(f14, f * 0.28f, f15, f15);
                float f16 = 0.07f * f;
                canvas.drawRoundRect(this.rect, f16, f16, this.paint);
                float f17 = 0.42f * f;
                canvas.drawLine(f14, f17, f15, f17, this.paint);
                float f18 = f * 0.36f;
                float f19 = f * 0.22f;
                float f20 = f * 0.34f;
                canvas.drawLine(f18, f19, f18, f20, this.paint);
                float f21 = f * 0.64f;
                canvas.drawLine(f21, f19, f21, f20, this.paint);
                float f22 = 0.018f * f;
                this.paint.setStyle(Paint.Style.FILL);
                for (int i = 0; i < 2; i++) {
                    for (int i2 = 0; i2 < 3; i2++) {
                        canvas.drawCircle(((i2 * 0.14f) + 0.36f) * f, ((i * 0.13f) + 0.53f) * f, f22, this.paint);
                    }
                }
                this.paint.setStyle(Paint.Style.STROKE);
                return;
            }
            if ("alert".equals(str2)) {
                this.path.reset();
                this.path.moveTo(f2, 0.23f * f);
                float f23 = 0.72f * f;
                this.path.lineTo(f * 0.78f, f23);
                this.path.lineTo(f * 0.22f, f23);
                this.path.close();
                canvas.drawPath(this.path, this.paint);
                canvas.drawLine(f2, f * 0.4f, f2, f * 0.55f, this.paint);
                canvas.drawPoint(f2, f * 0.64f, this.paint);
                return;
            }
            if ("locationOff".equals(str2)) {
                str2 = "pinOff";
            }
            if ("pinOff".equals(str2)) {
                this.path.reset();
                float f24 = f * 0.78f;
                this.path.moveTo(f2, f24);
                float f25 = 0.55f * f;
                float f26 = f * 0.24f;
                float f27 = 0.43f * f;
                float f28 = f * 0.36f;
                this.path.cubicTo(f * 0.28f, f25, f26, f27, f26, f28);
                float f29 = f * 0.22f;
                float f30 = f * 0.16f;
                this.path.cubicTo(f26, f29, f28, f30, f2, f30);
                float f31 = f * 0.76f;
                this.path.cubicTo(f * 0.64f, f30, f31, f29, f31, f28);
                this.path.cubicTo(f31, f27, f * 0.72f, f25, f2, f24);
                canvas.drawPath(this.path, this.paint);
                canvas.drawCircle(f2, f28, f * 0.08f, this.paint);
                canvas.drawLine(f26, f26, f31, f31, this.paint);
                return;
            }
            if ("folder".equals(str2)) {
                this.path.reset();
                float f32 = 0.2f * f;
                float f33 = 0.34f * f;
                this.path.moveTo(f32, f33);
                this.path.lineTo(0.42f * f, f33);
                float f34 = 0.43f * f;
                this.path.lineTo(0.49f * f, f34);
                float f35 = 0.8f * f;
                this.path.lineTo(f35, f34);
                float f36 = f * 0.72f;
                this.path.lineTo(f35, f36);
                this.path.lineTo(f32, f36);
                this.path.close();
                canvas.drawPath(this.path, this.paint);
                return;
            }
            if ("download".equals(str2)) {
                canvas.drawLine(f2, f * 0.22f, f2, f * 0.58f, this.paint);
                float f37 = f * 0.46f;
                float f38 = f * 0.6f;
                canvas.drawLine(f * 0.36f, f37, f2, f38, this.paint);
                float f39 = f * 0.64f;
                canvas.drawLine(f2, f38, f39, f37, this.paint);
                this.rect.set(0.25f * f, f39, 0.75f * f, f * 0.76f);
                float f40 = f * 0.04f;
                canvas.drawRoundRect(this.rect, f40, f40, this.paint);
                return;
            }
            if ("imageCheck".equals(str2)) {
                this.rect.set(f * 0.24f, 0.27f * f, f * 0.76f, 0.73f * f);
                float f41 = 0.07f * f;
                canvas.drawRoundRect(this.rect, f41, f41, this.paint);
                float f42 = 0.45f * f;
                float f43 = f * 0.65f;
                canvas.drawLine(f * 0.34f, f * 0.54f, f42, f43, this.paint);
                canvas.drawLine(f42, f43, f * 0.68f, f * 0.4f, this.paint);
                return;
            }
            if ("gallery".equals(str2)) {
                float f44 = 0.25f * f;
                float f45 = 0.75f * f;
                this.rect.set(f44, f44, f45, f45);
                float f46 = 0.06f * f;
                canvas.drawRoundRect(this.rect, f46, f46, this.paint);
                float f47 = f * 0.4f;
                canvas.drawCircle(f47, f47, 0.04f * f, this.paint);
                float f48 = 0.45f * f;
                float f49 = f * 0.54f;
                canvas.drawLine(f * 0.31f, f * 0.67f, f48, f49, this.paint);
                float f50 = f * 0.58f;
                float f51 = f * 0.65f;
                canvas.drawLine(f48, f49, f50, f51, this.paint);
                canvas.drawLine(f50, f51, f * 0.7f, f * 0.51f, this.paint);
                return;
            }
            if ("app".equals(str2)) {
                this.paint.setStyle(Paint.Style.FILL);
                float f52 = 0.18f * f;
                float f53 = 0.1f * f;
                float f54 = 0.27f * f;
                for (int i3 = 0; i3 < 2; i3++) {
                    for (int i4 = 0; i4 < 2; i4++) {
                        float f55 = f52 + f53;
                        float f56 = (i4 * f55) + f54;
                        float f57 = (i3 * f55) + f54;
                        this.rect.set(f56, f57, f56 + f52, f57 + f52);
                        float f58 = 0.035f * f;
                        canvas.drawRoundRect(this.rect, f58, f58, this.paint);
                    }
                }
                this.paint.setStyle(Paint.Style.STROKE);
                return;
            }
            if ("grid".equals(str2)) {
                float f59 = 0.16f * f;
                float f60 = 0.1f * f;
                float f61 = 0.28f * f;
                for (int i5 = 0; i5 < 2; i5++) {
                    for (int i6 = 0; i6 < 2; i6++) {
                        float f62 = f59 + f60;
                        float f63 = f61 + (i6 * f62);
                        float f64 = f61 + (i5 * f62);
                        this.rect.set(f63, f64, f63 + f59, f64 + f59);
                        float f65 = 0.035f * f;
                        canvas.drawRoundRect(this.rect, f65, f65, this.paint);
                    }
                }
                return;
            }
            if ("home".equals(str2)) {
                this.path.reset();
                float f66 = 0.5f * f;
                this.path.moveTo(f * 0.24f, f66);
                this.path.lineTo(f2, 0.27f * f);
                float f67 = f * 0.76f;
                this.path.lineTo(f67, f66);
                canvas.drawPath(this.path, this.paint);
                this.rect.set(0.32f * f, 0.49f * f, f * 0.68f, f67);
                float f68 = f * 0.04f;
                canvas.drawRoundRect(this.rect, f68, f68, this.paint);
                return;
            }
            if ("settings".equals(str2)) {
                canvas.drawCircle(f2, f2, 0.12f * f, this.paint);
                for (int i7 = 0; i7 < 8; i7++) {
                    double d = (i7 * 3.141592653589793d) / 4.0d;
                    canvas.drawLine(f2 + (((float) Math.cos(d)) * f * 0.22f), f2 + (((float) Math.sin(d)) * f * 0.22f), f2 + (((float) Math.cos(d)) * f * 0.3f), f2 + (((float) Math.sin(d)) * f * 0.3f), this.paint);
                }
                return;
            }
            if ("refresh".equals(str2)) {
                float f69 = f * 0.28f;
                float f70 = 0.72f * f;
                this.rect.set(f69, f69, f70, f70);
                canvas.drawArc(this.rect, 35.0f, 270.0f, false, this.paint);
                float f71 = f * 0.3f;
                float f72 = f * 0.78f;
                canvas.drawLine(f * 0.69f, f71, f72, f71, this.paint);
                canvas.drawLine(f72, f71, f * 0.76f, f * 0.4f, this.paint);
                return;
            }
            if ("check".equals(str2)) {
                float f73 = f * 0.44f;
                float f74 = f * 0.68f;
                canvas.drawLine(f * 0.28f, f * 0.52f, f73, f74, this.paint);
                canvas.drawLine(f73, f74, f * 0.74f, f * 0.34f, this.paint);
                return;
            }
            if ("arrow".equals(str2)) {
                float f75 = f * 0.34f;
                float f76 = f * 0.62f;
                canvas.drawLine(f75, f * 0.26f, f76, f2, this.paint);
                canvas.drawLine(f76, f2, f75, f * 0.74f, this.paint);
                return;
            }
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setTextAlign(Paint.Align.CENTER);
            this.paint.setTypeface(Typeface.DEFAULT_BOLD);
            this.paint.setTextSize(this.size * 0.42f);
            Paint.FontMetrics fontMetrics = this.paint.getFontMetrics();
            canvas.drawText(str2, f2, f2 - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.paint);
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
            this.paint.setAlpha(i);
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.paint.setColorFilter(colorFilter);
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.size;
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.size;
        }
    }

    private static class HeroStartDrawable extends Drawable {
        private final Paint paint;
        private final RectF rect;

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }

        private HeroStartDrawable() {
            this.paint = new Paint(1);
            this.rect = new RectF();
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            float fWidth = getBounds().width();
            float fHeight = getBounds().height();
            float fMin = Math.min(fWidth / 118.0f, fHeight / 96.0f);
            canvas.save();
            canvas.translate((fWidth - (118.0f * fMin)) / 2.0f, (fHeight - (fMin * 96.0f)) / 2.0f);
            canvas.scale(fMin, fMin);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(1442840575);
            this.rect.set(18.0f, 62.0f, 96.0f, 84.0f);
            canvas.drawRoundRect(this.rect, 18.0f, 18.0f, this.paint);
            this.paint.setColor(-2497281);
            this.rect.set(12.0f, 34.0f, 96.0f, 82.0f);
            canvas.drawRoundRect(this.rect, 17.0f, 17.0f, this.paint);
            this.paint.setColor(-1380097);
            this.rect.set(26.0f, 20.0f, 72.0f, 42.0f);
            canvas.drawRoundRect(this.rect, 11.0f, 11.0f, this.paint);
            this.paint.setColor(1728053247);
            this.rect.set(36.0f, 48.0f, 68.0f, 74.0f);
            canvas.drawRoundRect(this.rect, 9.0f, 9.0f, this.paint);
            this.paint.setColor(-1711276033);
            this.paint.setStrokeWidth(3.0f);
            this.paint.setStyle(Paint.Style.STROKE);
            this.rect.set(42.0f, 54.0f, 62.0f, 68.0f);
            canvas.drawRoundRect(this.rect, 4.0f, 4.0f, this.paint);
            canvas.drawLine(45.0f, 67.0f, 52.0f, 60.0f, this.paint);
            canvas.drawLine(52.0f, 60.0f, 64.0f, 70.0f, this.paint);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(-15770);
            canvas.drawCircle(86.0f, 28.0f, 24.0f, this.paint);
            this.paint.setColor(-1);
            canvas.drawCircle(86.0f, 28.0f, 8.0f, this.paint);
            canvas.restore();
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
            this.paint.setAlpha(i);
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.paint.setColorFilter(colorFilter);
        }
    }
}
