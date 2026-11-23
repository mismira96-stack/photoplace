package com.example.gallerysorter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PhotoPlace Instagram Story Share Dialog/Sheet.
 * Supports Single Frame and Multi-Photo Vertical Strip ("사진띠") with QuickBeauty's
 * 3 fonts (Sans / NanumPen / GowunBatang), text scale slider, and Cream/Dark frame tones.
 */
final class MemoryStoryShareSheet {
    private MemoryStoryShareSheet() {
    }

    static void showForViewerPhoto(Context context,
                                   String fallbackTitle,
                                   MemoryPhotoSection section,
                                   MemoryPhotoItem currentItem,
                                   String noteText) {
        if (context == null || currentItem == null || currentItem.sourceUri.isEmpty()) {
            return;
        }
        String place = section != null && !section.placeText.isEmpty()
                ? section.placeText
                : (fallbackTitle == null ? "" : fallbackTitle);
        String date = section != null ? section.dateText : "";
        String meta = MemoryStoryCaption.date(date);
        String defaultCaption = MemoryStoryCaption.place(place);

        List<String> candidateUris = new ArrayList<>();
        candidateUris.add(currentItem.sourceUri);
        if (section != null && section.photos != null) {
            for (MemoryPhotoItem item : section.photos) {
                if (item != null && !item.sourceUri.isEmpty() && !candidateUris.contains(item.sourceUri)) {
                    candidateUris.add(item.sourceUri);
                    if (candidateUris.size() >= 12) {
                        break;
                    }
                }
            }
        }

        // Single-photo polaroid defaults to the place/date caption; originals and notes are unchanged.
        MemoryStoryConfig initialConfig = new MemoryStoryConfig(
                MemoryStoryConfig.LayoutMode.SINGLE_FRAME,
                true,
                defaultCaption,
                meta,
                MemoryStoryConfig.FontKind.SANS,
                MemoryStoryConfig.ThemeTone.CREAM,
                1.0f);
        showInternal(context, candidateUris, Collections.singletonList(currentItem.sourceUri), initialConfig);
    }

    /** Receives a snapshot of the existing selection; there is no second picker or selection mode. */
    static void showForSelection(Context context, String title, List<UnresolvedMediaRecord> selected) {
        try {
            MemoryStorySelection plan = MemoryStorySelection.from(title, selected);
            showInternal(context, plan.uris, plan.uris, plan.config);
        } catch (IllegalArgumentException invalid) {
            Toast.makeText(context, invalid.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    static void showForCollection(Context context,
                                  MemoryCollection collection,
                                  GroupMemoryDetail detail) {
        if (context == null || collection == null || detail == null || detail.dates == null) {
            return;
        }
        List<String> candidateUris = new ArrayList<>();
        List<String> initialStripUris = new ArrayList<>();
        String firstPlace = "";
        String firstDate = "";

        // First pass: pick 1 representative photo from each place/date for an authentic trip strip
        for (GroupMemoryDateSection dateSection : detail.dates) {
            if (dateSection == null || dateSection.places == null) {
                continue;
            }
            if (firstDate.isEmpty() && !dateSection.dateText.isEmpty()) {
                firstDate = dateSection.dateText;
            }
            for (GroupMemoryPlaceSection placeSection : dateSection.places) {
                if (placeSection == null || placeSection.photos == null || placeSection.photos.isEmpty()) {
                    continue;
                }
                if (firstPlace.isEmpty() && !placeSection.placeTitle.isEmpty()) {
                    firstPlace = placeSection.placeTitle;
                }
                MemoryPhotoItem firstPhoto = placeSection.photos.get(0);
                if (firstPhoto != null && !firstPhoto.sourceUri.isEmpty()
                        && !initialStripUris.contains(firstPhoto.sourceUri)
                        && initialStripUris.size() < 3) {
                    initialStripUris.add(firstPhoto.sourceUri);
                }
                for (MemoryPhotoItem item : placeSection.photos) {
                    if (item != null && !item.sourceUri.isEmpty() && !candidateUris.contains(item.sourceUri)) {
                        candidateUris.add(item.sourceUri);
                        if (candidateUris.size() >= 16) {
                            break;
                        }
                    }
                }
            }
        }
        for (String uri : candidateUris) {
            if (initialStripUris.size() >= Math.min(3, candidateUris.size())) {
                break;
            }
            if (!initialStripUris.contains(uri)) {
                initialStripUris.add(uri);
            }
        }
        if (candidateUris.isEmpty()) {
            Toast.makeText(context, "스토리에 공유할 사진이 없어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int memberCount = collection.members == null ? 0 : collection.members.size();
        String placeSummary = firstPlace.isEmpty()
                ? ""
                : (memberCount > 1 ? firstPlace + " 외 " + (memberCount - 1) + "곳" : firstPlace);
        String meta = MemoryStoryCaption.date(firstDate);
        MemoryStoryConfig.LayoutMode mode = MemoryStoryConfig.LayoutMode.SINGLE_FRAME;

        MemoryStoryConfig initialConfig = new MemoryStoryConfig(
                mode,
                true,
                MemoryStoryCaption.place(firstPlace),
                meta,
                MemoryStoryConfig.FontKind.SANS,
                MemoryStoryConfig.ThemeTone.CREAM,
                1.0f);
        showInternal(context, candidateUris, initialStripUris, initialConfig);
    }

    private static void showInternal(final Context context,
                                     final List<String> candidateUris,
                                     List<String> initialSelectedUris,
                                     MemoryStoryConfig initialConfig) {
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Material_Light_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final ExecutorService worker = Executors.newSingleThreadExecutor();
        final Handler main = new Handler(Looper.getMainLooper());
        final Map<String, Bitmap> decodedPreviewCache = new LinkedHashMap<>();
        final Set<String> selectedUris = new LinkedHashSet<>(initialSelectedUris);
        if (selectedUris.isEmpty() && !candidateUris.isEmpty()) {
            selectedUris.add(candidateUris.get(0));
        }
        final MemoryStoryConfig[] configHolder = {initialConfig.withThemeTone(MemoryStoryConfig.ThemeTone.CREAM)};
        final MemoryStoryTemplateSelection templateSelection = new MemoryStoryTemplateSelection();
        final boolean[] closed = {false};
        final int[] renderGeneration = {0};

        dialog.setOnDismissListener(d -> {
            closed[0] = true;
            worker.shutdownNow();
        });

        LinearLayout shell=new LinearLayout(context);shell.setOrientation(LinearLayout.VERTICAL);shell.setFocusableInTouchMode(true);
        shell.setBackgroundColor(Color.rgb(248, 247, 252));
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), 0, dp(context, 16), dp(context, 8));

        // 1. Header Row
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = new TextView(context);
        titleView.setText("스토리 편집");titleView.setSingleLine(true);titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setTextSize(18.0f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Color.rgb(17, 24, 39));
        header.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView closeBtn = new TextView(context);
        closeBtn.setText("닫기");
        closeBtn.setTextSize(14.0f);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setTextColor(Color.rgb(107, 114, 128));
        closeBtn.setPadding(dp(context, 10), dp(context, 6), dp(context, 6), dp(context, 6));
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBtn, new LinearLayout.LayoutParams(-2, -2));
        header.setPadding(dp(context,18),dp(context,12),dp(context,18),dp(context,8));
        shell.addView(header,new LinearLayout.LayoutParams(-1,-2));shell.addView(root,new LinearLayout.LayoutParams(-1,0,1));

        // 2. Live 9:16 Story Preview Stage
        MemoryStoryPreviewStage previewStage = new MemoryStoryPreviewStage(context);
        GradientDrawable stageBg = new GradientDrawable();
        stageBg.setColor(MemoryStoryComposer.singleBackgroundColor(configHolder[0]));
        stageBg.setCornerRadius(dp(context, 18));
        previewStage.setBackground(stageBg);
        previewStage.setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));

        final MemoryStoryPhotoPreview previewImage=new MemoryStoryPhotoPreview(context,value->configHolder[0]=value);
        FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        previewStage.addView(previewImage, previewParams);

        final ProgressBar spinner = new ProgressBar(context);
        previewStage.addView(spinner, new FrameLayout.LayoutParams(
                dp(context, 28), dp(context, 28), Gravity.CENTER));
        root.addView(previewStage, new LinearLayout.LayoutParams(-1, 0, 1));

        // Forward declaration of preview updater
        final Runnable[] refreshPreviewRef = new Runnable[1];

        // Existing selection is fixed; only photo and style tools remain.
        LinearLayout tabs = new LinearLayout(context);
        tabs.setPadding(0, dp(context, 10), 0, dp(context, 8));
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout panelHost = new LinearLayout(context);
        panelHost.setOrientation(LinearLayout.VERTICAL);
        panelHost.setPadding(0, dp(context, 4), 0, dp(context, 8));
        root.addView(panelHost, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout photoPanel = new LinearLayout(context);
        photoPanel.setOrientation(LinearLayout.VERTICAL);
        photoPanel.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout templateOptions = new LinearLayout(context);
        templateOptions.setPadding(0, 0, 0, dp(context, 8));
        photoPanel.addView(templateOptions, new LinearLayout.LayoutParams(-1, -2));
        TextView resetPhoto = createSegmentPill(context, "사진 위치 초기화");
        resetPhoto.setOnClickListener(v -> {
            configHolder[0] = configHolder[0].withPhotoEdit(0, 1f, 0f, 0f);
            if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
        });
        photoPanel.addView(resetPhoto, new LinearLayout.LayoutParams(-1, dp(context, 44)));
        LinearLayout captionPanel = new LinearLayout(context);
        captionPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout stylePanel = new LinearLayout(context);
        stylePanel.setOrientation(LinearLayout.VERTICAL);
        View[] panels = {photoPanel, stylePanel};
        String[] tabLabels = {"사진", "스타일"};
        TextView[] tabButtons = new TextView[2];
        for (int i = 0; i < panels.length; i++) {
            final int selectedTab = i;
            panelHost.addView(panels[i], new LinearLayout.LayoutParams(-1, -2));
            panels[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
            TextView tab = createSegmentPill(context, tabLabels[i]);
            tabButtons[i] = tab;
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(context, 44), 1);
            if (i < 1) tp.rightMargin = dp(context, 6);
            tabs.addView(tab, tp);
            styleSegmentPill(context, tab, i == 0);
            tab.setOnClickListener(v -> {
                for (int j = 0; j < panels.length; j++) {
                    panels[j].setVisibility(j == selectedTab ? View.VISIBLE : View.GONE);
                    styleSegmentPill(context, tabButtons[j], j == selectedTab);
                }
                shell.requestFocus();
                android.view.inputmethod.InputMethodManager keyboard =
                        (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (keyboard != null) keyboard.hideSoftInputFromWindow(shell.getWindowToken(), 0);

            });
        }

        // 4. Context Toggle: [사진만 공유] vs [장소·문구 포함]
        LinearLayout contextToggleRow = new LinearLayout(context);
        contextToggleRow.setOrientation(LinearLayout.HORIZONTAL);
        final TextView photoOnlyBtn = createSegmentPill(context, "사진만 공유");
        final TextView includeInfoBtn = createSegmentPill(context, "캡션·날짜 표시");
        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, dp(context, 38), 1.0f);
        c1.rightMargin = dp(context, 8);
        contextToggleRow.addView(photoOnlyBtn, c1);
        contextToggleRow.addView(includeInfoBtn, new LinearLayout.LayoutParams(0, dp(context, 38), 1.0f));
        stylePanel.addView(contextToggleRow, marginBottom(context, 8));

        // 5. Text Customization Panel (QuickBeauty-style Caption + Font + Tone + Size Slider)
        final LinearLayout textPanel = new LinearLayout(context);
        textPanel.setOrientation(LinearLayout.VERTICAL);
        textPanel.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.rgb(248, 249, 252));
        panelBg.setCornerRadius(dp(context, 14));
        panelBg.setStroke(dp(context, 1), Color.rgb(233, 236, 244));
        textPanel.setBackground(panelBg);

        EditText captionInput = new EditText(context);
        captionInput.setSingleLine(true);captionInput.setMinimumWidth(0);captionInput.setMinWidth(0);captionInput.setMinEms(0);captionInput.setMinHeight(0);captionInput.setMinimumHeight(0);captionInput.setIncludeFontPadding(false);
        captionInput.setText(initialConfig.captionText);
        captionInput.setHint("캡션 추가…");
        captionInput.setContentDescription("캡션 편집");
        captionInput.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        captionInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        captionInput.setBackgroundColor(Color.WHITE);
        captionInput.setTextSize(14.0f);
        captionInput.setTextColor(Color.rgb(17, 24, 39));
        captionInput.setHintTextColor(Color.rgb(148, 156, 172));
        captionInput.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        captionInput.setBackgroundColor(Color.WHITE);
        captionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                configHolder[0] = configHolder[0].withCaptionText(editable == null ? "" : editable.toString());
                previewImage.updateConfig(configHolder[0]);
            }
        });
        textPanel.addView(captionInput, new LinearLayout.LayoutParams(-1,0,1));

        // Font & Theme chips row
        LinearLayout styleRow = new LinearLayout(context);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleRow.setGravity(Gravity.CENTER_VERTICAL);
        final List<TextView> fontChips = new ArrayList<>();
        final MemoryStoryConfig.FontKind[] fontKinds = MemoryStoryConfig.FontKind.values();
        for (int i = 0; i < fontKinds.length; i++) {
            final MemoryStoryConfig.FontKind fk = fontKinds[i];
            TextView chip = createSmallChip(context, fk.label);
            chip.setPadding(dp(context,4),0,dp(context,4),0);
            chip.setTypeface(MemoryStoryComposer.resolveTypeface(context, fk));
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, dp(context, 34), 1.0f);
            fp.rightMargin = dp(context, 6);
            styleRow.addView(chip, fp);
            fontChips.add(chip);
        }

        stylePanel.addView(styleRow, marginBottom(context, 8));

        // Size Slider Row
        LinearLayout sizeRow = new LinearLayout(context);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setGravity(Gravity.CENTER_VERTICAL);
        final TextView sizeLabel = new TextView(context);
        sizeLabel.setText("글자 크기 100%");
        sizeLabel.setTextSize(12.0f);
        sizeLabel.setTypeface(Typeface.DEFAULT_BOLD);
        sizeLabel.setTextColor(Color.rgb(82, 91, 108));
        sizeRow.addView(sizeLabel, new LinearLayout.LayoutParams(dp(context, 88), -2));

        SeekBar sizeSlider = new SeekBar(context);
        sizeSlider.setMax(100); // maps 0..100 -> 60%..160%
        sizeSlider.setProgress(Math.round((initialConfig.textScale - 0.60f) * 100f));
        sizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = 0.60f + (progress / 100.0f);
                sizeLabel.setText("글자 크기 " + Math.round(scale * 100f) + "%");
                if (fromUser) {
                    configHolder[0] = configHolder[0].withTextScale(scale);
                    if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        sizeRow.addView(sizeSlider, new LinearLayout.LayoutParams(0, -2, 1.0f));
        stylePanel.addView(sizeRow, new LinearLayout.LayoutParams(-1, dp(context, 44)));
        textPanel.setPadding(0,0,0,0);textPanel.setBackgroundColor(Color.WHITE);textPanel.setVisibility(View.GONE);
        previewStage.addView(textPanel,new FrameLayout.LayoutParams(-2,-2));
        previewStage.bindCaption(previewImage,textPanel,captionInput,()->configHolder[0]);
        Runnable positionCaption=previewStage::requestLayout;
        captionInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){} public void onTextChanged(CharSequence s,int st,int before,int count){} public void afterTextChanged(Editable e){previewImage.post(positionCaption);}});
        Runnable finishCaption=()->{
            textPanel.setVisibility(View.GONE);previewImage.setCaptionEditing(false);tabs.setVisibility(View.VISIBLE);panelHost.setVisibility(View.VISIBLE);closeBtn.setText("닫기");closeBtn.setOnClickListener(v->dialog.dismiss());shell.requestFocus();
            android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(captionInput.getWindowToken(),0);
        };
        captionInput.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){finishCaption.run();return true;}return false;});
        captionInput.setOnFocusChangeListener((v,focused)->{if(!focused){textPanel.setVisibility(View.GONE);previewImage.setCaptionEditing(false);tabs.setVisibility(View.VISIBLE);panelHost.setVisibility(View.VISIBLE);closeBtn.setText("닫기");closeBtn.setOnClickListener(view->dialog.dismiss());}});
        previewImage.setCaptionAction(()->{
            previewImage.setCaptionEditing(true);tabs.setVisibility(View.GONE);panelHost.setVisibility(View.GONE);closeBtn.setText("완료");closeBtn.setOnClickListener(v->finishCaption.run());textPanel.setVisibility(View.VISIBLE);positionCaption.run();captionInput.requestFocus();captionInput.setSelection(captionInput.length());
            android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)captionInput.post(()->keyboard.showSoftInput(captionInput,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
        });

        previewImage.setOnTouchListener((v,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN&&textPanel.getVisibility()==View.VISIBLE)finishCaption.run();return false;});

        final Runnable syncStyleControls = () -> {
            boolean include = configHolder[0].includeContext;
            styleSegmentPill(context, photoOnlyBtn, !include);
            styleSegmentPill(context, includeInfoBtn, include);
            if (!include) textPanel.setVisibility(View.GONE);

            for (int i = 0; i < fontKinds.length; i++) {
                boolean active = configHolder[0].fontKind == fontKinds[i];
                styleSmallChip(context, fontChips.get(i), active);
            }
        };

        for (int i = 0; i < fontKinds.length; i++) {
            final MemoryStoryConfig.FontKind fk = fontKinds[i];
            fontChips.get(i).setOnClickListener(v -> {
                configHolder[0] = configHolder[0].withFontKind(fk);
                syncStyleControls.run();
                if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
            });
        }
        photoOnlyBtn.setOnClickListener(v -> {
            configHolder[0] = configHolder[0].withIncludeContext(false);
            syncStyleControls.run();
            if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
        });
        includeInfoBtn.setOnClickListener(v -> {
            configHolder[0] = configHolder[0].withIncludeContext(true);
            syncStyleControls.run();
            if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
        });
        syncStyleControls.run();

        // 6. Primary Share Button
        final Button shareBtn = new Button(context);
        shareBtn.setText("스토리 공유");shareBtn.setContentDescription("Instagram 스토리 공유");
        shareBtn.setAllCaps(false);
        shareBtn.setTextSize(13.0f);shareBtn.setSingleLine(true);shareBtn.setPadding(dp(context,8),0,dp(context,8),0);
        shareBtn.setTypeface(Typeface.DEFAULT_BOLD);
        shareBtn.setTextColor(Color.WHITE);
        shareBtn.setMinHeight(dp(context, 48));
        GradientDrawable ctaBg = new GradientDrawable();
        ctaBg.setColor(Color.rgb(104, 82, 190));
        ctaBg.setCornerRadius(dp(context, 14));
        shareBtn.setBackground(ctaBg);

        final FrameLayout saveBtn = new FrameLayout(context);
        saveBtn.setContentDescription("카드 저장");
        saveBtn.setFocusable(true);
        saveBtn.setBackground(MemorySelectionStyle.surface(context, 0xffeee9f8, 14));
        ImageView saveIcon = new ImageView(context);
        saveIcon.setImageDrawable(MediaVectorIconDrawable.save(Color.rgb(104,82,190), dp(context,22)));
        saveBtn.addView(saveIcon, new FrameLayout.LayoutParams(dp(context,22), dp(context,22), Gravity.CENTER));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(context,44), dp(context,48));
        saveParams.rightMargin = dp(context,6);
        header.addView(saveBtn, saveParams);
        saveBtn.setOnClickListener(v -> {
            saveBtn.setEnabled(false); shareBtn.setEnabled(false); spinner.setVisibility(View.VISIBLE);
            final List<String> inputs = resolveActiveUris(configHolder[0], candidateUris, selectedUris);
            final MemoryStoryConfig savedConfig = configHolder[0].withPhotoOrder(inputs);
            worker.execute(() -> {
                Bitmap card = null;
                String message;
                try {
                    card = renderCardForUris(context, inputs, savedConfig);
                    MemoryStoryGallerySaver.save(context, card);
                    message = "PhotoPlace Stories에 저장했어요.";
                } catch (Exception failure) {
                    message = "카드를 저장하지 못했어요. 저장 공간을 확인해 주세요.";
                } finally {
                    if (card != null) card.recycle();
                }
                final String result = message;
                main.post(() -> {
                    if (closed[0]) return;
                    spinner.setVisibility(View.GONE); saveBtn.setEnabled(true); shareBtn.setEnabled(true);
                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
                });
            });
        });

        shareBtn.setOnClickListener(v -> {
            shareBtn.setEnabled(false); saveBtn.setEnabled(false);
            shareBtn.setAlpha(0.6f);
            spinner.setVisibility(View.VISIBLE);
            final List<String> urisToExport=resolveActiveUris(configHolder[0],candidateUris,selectedUris);
            final MemoryStoryConfig exportConfig=configHolder[0].withPhotoOrder(urisToExport);
            worker.execute(() -> {
                try {
                    Bitmap composed = renderCardForUris(context, urisToExport, exportConfig);
                    Uri storyUri;
                    try {
                        storyUri = MemoryStoryExporter.exportToCacheUri(context, composed);
                    } finally {
                        composed.recycle();
                    }
                    main.post(() -> {
                        if (closed[0]) return;
                        dialog.dismiss();
                        MemoryStoryExporter.launchInstagramStory(context, storyUri);
                    });
                } catch (final Exception e) {
                    main.post(() -> {
                        if (closed[0]) return;
                        spinner.setVisibility(View.GONE);
                        shareBtn.setEnabled(true); saveBtn.setEnabled(true);
                        shareBtn.setAlpha(1.0f);
                        Toast.makeText(context,
                                e.getMessage() == null ? "스토리 공유를 준비하지 못했어요." : e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        header.addView(shareBtn,new LinearLayout.LayoutParams(dp(context,104),dp(context,48)));

        refreshPreviewRef[0] = () -> {
            final int gen = ++renderGeneration[0];
            final List<String> activeUris=resolveActiveUris(configHolder[0],candidateUris,selectedUris);
            configHolder[0]=configHolder[0].withPhotoOrder(activeUris);
            final MemoryStoryConfig snap=configHolder[0];
            stageBg.setColor(MemoryStoryComposer.singleBackgroundColor(snap));
            spinner.setVisibility(View.VISIBLE);previewImage.setEnabled(false);shareBtn.setEnabled(false);saveBtn.setEnabled(false);
            worker.execute(() -> {
                List<Bitmap> previewInputs = new ArrayList<>();
                for (String u : activeUris) {
                    Bitmap cached = decodedPreviewCache.get(u);
                    if (cached == null || cached.isRecycled()) {
                        cached = MemoryStoryExporter.decodeSingle(
                                context.getContentResolver(), Uri.parse(u), 640);
                        if (cached != null) {
                            decodedPreviewCache.put(u, cached);
                        }
                    }
                    if (cached != null) {
                        previewInputs.add(cached);
                    }
                }
                main.post(() -> {
                    if(closed[0]||gen!=renderGeneration[0])return;
                    spinner.setVisibility(View.GONE);
                    if(previewInputs.size()!=activeUris.size()){Toast.makeText(context,"사진을 불러오지 못했어요. 다시 열어 주세요.",Toast.LENGTH_SHORT).show();return;}
                    int[] widths = new int[previewInputs.size()], heights = new int[previewInputs.size()];
                    for (int i = 0; i < previewInputs.size(); i++) {
                        widths[i] = previewInputs.get(i).getWidth();
                        heights[i] = previewInputs.get(i).getHeight();
                    }
                    configHolder[0] = configHolder[0].withLayoutMode(templateSelection.resolve(widths, heights));
                    templateOptions.removeAllViews();
                    for (MemoryStoryConfig.LayoutMode mode : MemoryStoryTemplateSelection.options(widths, heights)) {
                        LinearLayout option = new LinearLayout(context);
                        option.setOrientation(LinearLayout.VERTICAL);
                        option.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
                        boolean chosen = configHolder[0].layoutMode == mode;
                        option.setBackground(MemorySelectionStyle.surface(context,
                                chosen ? 0xffeee8ff : Color.WHITE, 14));
                        option.setSelected(chosen);
                        String label = MemoryStoryTemplateSelection.label(mode);
                        option.setContentDescription(label + (chosen ? ", 선택됨" : ""));
                        MemoryStoryPhotoPreview thumbnail = new MemoryStoryPhotoPreview(context, ignored -> { });
                        thumbnail.show(previewInputs, configHolder[0].withLayoutMode(mode));
                        thumbnail.setEnabled(false);
                        thumbnail.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                        option.addView(thumbnail, new LinearLayout.LayoutParams(-1, dp(context, 76)));
                        TextView name = createSegmentPill(context, label);
                        name.setTextSize(11);
                        name.setSingleLine(false);
                        name.setMaxLines(2);
                        name.setTextColor(chosen ? 0xff6852be : 0xff646f84);
                        option.addView(name, new LinearLayout.LayoutParams(-1, dp(context, 36)));
                        option.setOnClickListener(v -> {
                            templateSelection.select(mode);
                            configHolder[0] = configHolder[0].withLayoutMode(mode);
                            refreshPreviewRef[0].run();
                        });
                        LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(0, -2, 1);
                        optionParams.setMargins(dp(context, 2), 0, dp(context, 2), 0);
                        templateOptions.addView(option, optionParams);
                    }
                    templateOptions.setVisibility(previewInputs.size() >= 2 ? View.VISIBLE : View.GONE);
                    previewImage.show(previewInputs,configHolder[0]);previewImage.setEnabled(true);shareBtn.setEnabled(true);saveBtn.setEnabled(true);
                });
            });
        };

        dialog.setContentView(shell);
        dialog.show();shell.requestFocus();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setSoftInputMode((android.os.Build.VERSION.SDK_INT>=30?WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING:WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            MemoryStoryEditorInsets.bind(shell);
        }
        refreshPreviewRef[0].run();
    }

    private static Bitmap renderCardForUris(Context context, List<String> uris, MemoryStoryConfig config) {
        List<Bitmap> photos = MemoryStoryExporter.decodeForExport(context, uris, config);
        try {
            if (photos.size() != uris.size()) throw new IllegalStateException("사진을 불러오지 못했어요.");
            return MemoryStoryComposer.renderCard(context, photos, config, MemoryStoryExportQuality.OUTPUT_WIDTH);
        } catch (OutOfMemoryError failure) {
            throw new IllegalStateException("고화질 카드를 만들 메모리가 부족해요.", failure);
        } finally {
            for (Bitmap photo : photos) if (!photo.isRecycled()) photo.recycle();
        }
    }

    private static List<String> resolveActiveUris(MemoryStoryConfig config,
                                                  List<String> candidateUris,
                                                  Set<String> selectedUris) {
        if (!selectedUris.isEmpty()) {
            return new ArrayList<>(selectedUris).subList(0, Math.min(4, selectedUris.size()));
        }
        if (!candidateUris.isEmpty()) {
            return Collections.singletonList(candidateUris.get(0));
        }
        return Collections.emptyList();
    }

    private static TextView createSegmentPill(Context context, String label) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextSize(13.0f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        return tv;
    }

    private static void styleSegmentPill(Context context, TextView tv, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, 12));
        if (selected) {
            bg.setColor(Color.rgb(243, 240, 255));
            bg.setStroke(dp(context, 1), Color.rgb(104, 82, 190));
            tv.setTextColor(Color.rgb(104, 82, 190));
        } else {
            bg.setColor(Color.rgb(245, 246, 250));
            bg.setStroke(dp(context, 1), Color.rgb(229, 232, 240));
            tv.setTextColor(Color.rgb(107, 114, 128));
        }
        tv.setBackground(bg);
    }

    private static TextView createSmallChip(Context context, String label) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextSize(12.5f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        return tv;
    }

    private static void styleSmallChip(Context context, TextView tv, boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, 10));
        if (active) {
            bg.setColor(Color.rgb(104, 82, 190));
            tv.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(context, 1), Color.rgb(222, 226, 236));
            tv.setTextColor(Color.rgb(55, 65, 81));
        }
        tv.setBackground(bg);
    }

    private static LinearLayout.LayoutParams marginBottom(Context context, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(context, bottomDp);
        return lp;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
