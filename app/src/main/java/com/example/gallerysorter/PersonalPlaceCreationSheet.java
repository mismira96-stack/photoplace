package com.example.gallerysorter;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * App-styled sheet for creating or editing a Personal Place ('우리집', '회사', '단골 카페')
 * from anchor photos, with 50m / 80m / 120m radius preview and manual photo inclusion/exclusion.
 */
final class PersonalPlaceCreationSheet {
    private static final String[] PRESET_NAMES = {"우리집", "회사", "단골 카페", "자주 가는 곳"};
    private static final int[] RADIUS_OPTIONS = {
            PersonalPlace.RADIUS_NARROW,
            PersonalPlace.RADIUS_DEFAULT,
            PersonalPlace.RADIUS_WIDE,
            PersonalPlace.RADIUS_CAMPUS,
            PersonalPlace.RADIUS_COMPLEX,
            PersonalPlace.RADIUS_TOWN
    };

    private final Activity activity;
    private final MemoryBrowserDetail detail;
    private final String parentStableId;
    private final PersonalPlace existingPlace;
    private final Consumer<PersonalPlace> onSaved;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService thumbPool = new ThreadPoolExecutor(
            2, 2, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(48),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    private final LruCache<String, Bitmap> thumbCache = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 1 : value.getByteCount();
        }
    };

    private final List<DiscoveryPhotoRef> allRefs;
    private final Map<String, DiscoveryPhotoRef> refByUri = new LinkedHashMap<>();
    private final LinkedHashSet<String> anchorUris = new LinkedHashSet<>();
    private final LinkedHashSet<String> checkedUris = new LinkedHashSet<>();
    private final LinkedHashSet<String> initialRadiusMatchedUris = new LinkedHashSet<>();
    private final LinkedHashSet<String> manuallyToggledInUris = new LinkedHashSet<>();
    private final LinkedHashSet<String> manuallyToggledOutUris = new LinkedHashSet<>();
    private final Set<String> lockedInSiblingPlaces = new LinkedHashSet<>();

    private Map<String, PersonalPlaceGpsPoint> gpsByUri = new LinkedHashMap<>();
    private PersonalPlaceMatcher.AnchorAnalysis anchorAnalysis =
            new PersonalPlaceMatcher.AnchorAnalysis(false, false, 0.0d, 0.0d, 0.0d, 0);
    private int selectedRadius = PersonalPlace.RADIUS_DEFAULT;
    private boolean stepAnchorSelection;
    private boolean showAllPhotosInPreview;
    private boolean gpsLoading;
    private boolean closed;

    private Dialog dialog;
    private LinearLayout rootContainer;
    private EditText nameInput;
    private TextView statusText;
    private ProgressBar progressBar;
    private LinearLayout radiusRow;
    private LinearLayout modeToggleRow;
    private GridView gridView;
    private PhotoGridAdapter gridAdapter;
    private Button primaryButton;
    private List<DiscoveryPhotoRef> displayedRefs = Collections.emptyList();

    static void show(Activity activity,
                     MemoryBrowserDetail detail,
                     Collection<String> initialSelectedUris,
                     PersonalPlace existingPlace,
                     Consumer<PersonalPlace> onSaved) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || detail == null) {
            return;
        }
        new PersonalPlaceCreationSheet(activity, detail, initialSelectedUris, existingPlace, onSaved).open();
    }

    private PersonalPlaceCreationSheet(Activity activity,
                                       MemoryBrowserDetail detail,
                                       Collection<String> initialSelectedUris,
                                       PersonalPlace existingPlace,
                                       Consumer<PersonalPlace> onSaved) {
        this.activity = activity;
        this.detail = detail;
        this.existingPlace = existingPlace;
        this.onSaved = onSaved;
        this.parentStableId = resolveStableId(activity, detail.record.memoryKey);
        this.allRefs = extractAllRefs(detail);
        for (DiscoveryPhotoRef ref : allRefs) {
            refByUri.put(ref.sourceUri, ref);
        }

        List<PersonalPlace> siblings = new PersonalPlaceStore(activity)
                .listForParent(parentStableId, detail.record.memoryKey);
        for (PersonalPlace sibling : siblings) {
            if (sibling == null) {
                continue;
            }
            if (existingPlace != null && existingPlace.placeId.equals(sibling.placeId)) {
                continue;
            }
            lockedInSiblingPlaces.addAll(sibling.includedUris);
        }

        if (existingPlace != null) {
            this.selectedRadius = existingPlace.selectedRadiusMeters;
            this.anchorUris.addAll(existingPlace.includedUris);
            if (this.anchorUris.isEmpty()) {
                this.anchorUris.addAll(existingPlace.autoMatchedUris);
            }
            this.checkedUris.addAll(existingPlace.includedUris);
            this.checkedUris.addAll(existingPlace.autoMatchedUris);
            this.manuallyToggledOutUris.addAll(existingPlace.excludedUris);
            this.stepAnchorSelection = false;
        } else {
            if (initialSelectedUris != null) {
                for (String uri : initialSelectedUris) {
                    if (uri != null && refByUri.containsKey(uri.trim())) {
                        anchorUris.add(uri.trim());
                    }
                }
            }
            this.stepAnchorSelection = anchorUris.isEmpty();
        }
    }

    private void open() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnDismissListener(d -> close());

        rootContainer = new LinearLayout(activity);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = CleanupUiStyle.dp(activity, 18);
        rootContainer.setPadding(pad, pad, pad, pad);
        rootContainer.setBackground(MemorySelectionStyle.surface(activity, 0xfffffbff, 24));

        dialog.setContentView(rootContainer);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int maxW = Math.min(CleanupUiStyle.dp(activity, 520),
                    activity.getResources().getDisplayMetrics().widthPixels - CleanupUiStyle.dp(activity, 20));
            int maxH = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.88f);
            window.setLayout(maxW, maxH);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        if (stepAnchorSelection) {
            renderAnchorSelectionStep();
        } else {
            renderPreviewAndSaveStep();
            startGpsResolutionAndPreview();
        }
    }

    private void renderAnchorSelectionStep() {
        rootContainer.removeAllViews();
        TextView brand = CleanupUiStyle.text(activity, "PhotoPlace · 개인 장소", 11, true, CleanupUiStyle.PURPLE);
        brand.setGravity(Gravity.CENTER);
        rootContainer.addView(brand);

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, CleanupUiStyle.dp(activity, 6), 0, CleanupUiStyle.dp(activity, 4));
        TextView title = CleanupUiStyle.text(activity, "기준 사진 고르기", 19, true, CleanupUiStyle.INK);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        Button closeBtn = CleanupUiStyle.button(activity, "닫기", false);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBtn, new LinearLayout.LayoutParams(-2, -2));
        rootContainer.addView(header);

        statusText = CleanupUiStyle.text(
                activity,
                "‘우리집’이나 ‘회사’의 기준이 될 사진 1장 이상을 골라 주세요. 선택한 사진 주변에서 찍은 사진들을 자동으로 찾아드려요.",
                13, false, CleanupUiStyle.MUTED);
        statusText.setPadding(0, 0, 0, CleanupUiStyle.dp(activity, 10));
        rootContainer.addView(statusText);

        displayedRefs = new ArrayList<>(allRefs);
        gridView = createPhotoGridView();
        gridAdapter = new PhotoGridAdapter(true);
        gridView.setAdapter(gridAdapter);
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= displayedRefs.size()) {
                return;
            }
            String uri = displayedRefs.get(position).sourceUri;
            if (!anchorUris.add(uri)) {
                anchorUris.remove(uri);
            }
            updateAnchorButtonState();
            gridAdapter.notifyDataSetChanged();
        });
        rootContainer.addView(gridView, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout footer = new LinearLayout(activity);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, CleanupUiStyle.dp(activity, 12), 0, 0);
        Button cancel = MemorySelectionStyle.action(activity, "취소", false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, -2, 0.6f);
        cancelLp.rightMargin = CleanupUiStyle.dp(activity, 8);
        footer.addView(cancel, cancelLp);

        primaryButton = MemorySelectionStyle.action(activity, "근처 사진 찾기", true);
        primaryButton.setOnClickListener(v -> {
            if (anchorUris.isEmpty()) {
                Toast.makeText(activity, "기준 사진 1장 이상을 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            stepAnchorSelection = false;
            renderPreviewAndSaveStep();
            startGpsResolutionAndPreview();
        });
        footer.addView(primaryButton, new LinearLayout.LayoutParams(0, -2, 1f));
        rootContainer.addView(footer);
        updateAnchorButtonState();
    }

    private void updateAnchorButtonState() {
        if (primaryButton == null) {
            return;
        }
        int count = anchorUris.size();
        primaryButton.setEnabled(count > 0);
        primaryButton.setAlpha(count > 0 ? 1.0f : 0.45f);
        primaryButton.setText(count > 0 ? "근처 사진 찾기 (" + count + "장 선택)" : "기준 사진을 선택해 주세요");
    }

    private void renderPreviewAndSaveStep() {
        String previousName = nameInput != null ? nameInput.getText().toString()
                : (existingPlace != null ? existingPlace.name : "");
        rootContainer.removeAllViews();

        TextView brand = CleanupUiStyle.text(activity, "PhotoPlace · 개인 장소", 11, true, CleanupUiStyle.PURPLE);
        brand.setGravity(Gravity.CENTER);
        rootContainer.addView(brand);

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, CleanupUiStyle.dp(activity, 4), 0, CleanupUiStyle.dp(activity, 6));
        TextView heading = CleanupUiStyle.text(
                activity,
                existingPlace != null ? "개인 장소 수정" : "개인 장소 만들기",
                19, true, CleanupUiStyle.INK);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        if (existingPlace == null) {
            Button reselectBtn = CleanupUiStyle.button(activity, "기준 다시 고르기", false);
            reselectBtn.setOnClickListener(v -> {
                stepAnchorSelection = true;
                renderAnchorSelectionStep();
            });
            header.addView(reselectBtn, new LinearLayout.LayoutParams(-2, -2));
        }
        rootContainer.addView(header);

        nameInput = new EditText(activity);
        nameInput.setSingleLine(true);
        nameInput.setTextSize(15);
        nameInput.setHint("장소 이름 입력 (예: 우리집, 회사, 단골 카페)");
        nameInput.setText(previousName);
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        int inputPad = CleanupUiStyle.dp(activity, 12);
        nameInput.setPadding(inputPad, inputPad, inputPad, inputPad);
        nameInput.setBackground(CleanupUiStyle.card(activity, 0xfffaf7fd));
        rootContainer.addView(nameInput, new LinearLayout.LayoutParams(-1, -2));

        // Preset name chips ('우리집', '회사', '단골 카페', '자주 가는 곳')
        HorizontalScrollView presetScroll = new HorizontalScrollView(activity);
        presetScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout presetRow = new LinearLayout(activity);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setPadding(0, CleanupUiStyle.dp(activity, 6), 0, CleanupUiStyle.dp(activity, 6));
        for (String preset : PRESET_NAMES) {
            TextView chip = createChipView(preset, false, () -> {
                nameInput.setText(preset);
                nameInput.setSelection(preset.length());
                if ("회사".equals(preset) && selectedRadius <= PersonalPlace.RADIUS_DEFAULT) {
                    selectedRadius = PersonalPlace.RADIUS_CAMPUS;
                    bindRadiusChips();
                    recomputeCandidatesFromGps();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = CleanupUiStyle.dp(activity, 6);
            presetRow.addView(chip, lp);
        }
        presetScroll.addView(presetRow);
        rootContainer.addView(presetScroll, new LinearLayout.LayoutParams(-1, -2));

        // 3-tier Radius selector chips (좁게 50m / 보통 80m / 넓게 120m)
        radiusRow = new LinearLayout(activity);
        radiusRow.setOrientation(LinearLayout.HORIZONTAL);
        radiusRow.setGravity(Gravity.CENTER_VERTICAL);
        radiusRow.setPadding(0, CleanupUiStyle.dp(activity, 2), 0, CleanupUiStyle.dp(activity, 6));
        bindRadiusChips();
        rootContainer.addView(radiusRow, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CleanupUiStyle.PURPLE));
        progressBar.setVisibility(gpsLoading ? View.VISIBLE : View.GONE);
        rootContainer.addView(progressBar, new LinearLayout.LayoutParams(-1, CleanupUiStyle.dp(activity, 8)));

        statusText = CleanupUiStyle.text(activity, "근처 사진을 확인하고 있어요...", 12, false, CleanupUiStyle.MUTED);
        statusText.setPadding(0, CleanupUiStyle.dp(activity, 2), 0, CleanupUiStyle.dp(activity, 6));
        rootContainer.addView(statusText);

        modeToggleRow = new LinearLayout(activity);
        modeToggleRow.setOrientation(LinearLayout.HORIZONTAL);
        modeToggleRow.setGravity(Gravity.CENTER_VERTICAL);
        modeToggleRow.setPadding(0, 0, 0, CleanupUiStyle.dp(activity, 6));
        rootContainer.addView(modeToggleRow, new LinearLayout.LayoutParams(-1, -2));

        gridView = createPhotoGridView();
        gridAdapter = new PhotoGridAdapter(false);
        gridView.setAdapter(gridAdapter);
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (gpsLoading || position < 0 || position >= displayedRefs.size()) {
                return;
            }
            String uri = displayedRefs.get(position).sourceUri;
            if (checkedUris.contains(uri)) {
                checkedUris.remove(uri);
                manuallyToggledInUris.remove(uri);
                manuallyToggledOutUris.add(uri);
            } else {
                checkedUris.add(uri);
                manuallyToggledOutUris.remove(uri);
                manuallyToggledInUris.add(uri);
            }
            refreshPreviewCountsAndButtons();
            gridAdapter.notifyDataSetChanged();
        });
        rootContainer.addView(gridView, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout footer = new LinearLayout(activity);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, CleanupUiStyle.dp(activity, 10), 0, 0);
        Button cancel = MemorySelectionStyle.action(activity, "취소", false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, -2, 0.55f);
        cancelLp.rightMargin = CleanupUiStyle.dp(activity, 8);
        footer.addView(cancel, cancelLp);

        primaryButton = MemorySelectionStyle.action(activity, "개인 장소 저장", true);
        primaryButton.setOnClickListener(v -> savePersonalPlace());
        footer.addView(primaryButton, new LinearLayout.LayoutParams(0, -2, 1f));
        rootContainer.addView(footer);
    }

    private void bindRadiusChips() {
        if (radiusRow == null) {
            return;
        }
        radiusRow.removeAllViews();
        TextView label = CleanupUiStyle.text(activity, "범위", 12, true, CleanupUiStyle.MUTED);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
        labelLp.rightMargin = CleanupUiStyle.dp(activity, 8);
        radiusRow.addView(label, labelLp);

        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsContainer = new LinearLayout(activity);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipsContainer.setGravity(Gravity.CENTER_VERTICAL);

        String[] labels = {"50m", "80m", "150m", "300m", "500m", "1km"};
        for (int i = 0; i < RADIUS_OPTIONS.length; i++) {
            final int radius = RADIUS_OPTIONS[i];
            boolean active = selectedRadius == radius;
            TextView chip = createChipView(labels[i], active, () -> {
                if (selectedRadius != radius) {
                    selectedRadius = radius;
                    bindRadiusChips();
                    recomputeCandidatesFromGps();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = CleanupUiStyle.dp(activity, 6);
            chipsContainer.addView(chip, lp);
        }
        scroll.addView(chipsContainer);
        radiusRow.addView(scroll, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private void bindModeToggleRow(int candidateCount) {
        if (modeToggleRow == null) {
            return;
        }
        modeToggleRow.removeAllViews();
        TextView nearChip = createChipView(
                "근처 사진 (" + candidateCount + "장)",
                !showAllPhotosInPreview,
                () -> {
                    if (showAllPhotosInPreview) {
                        showAllPhotosInPreview = false;
                        recomputeDisplayedList();
                    }
                });
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(-2, -2);
        lp1.rightMargin = CleanupUiStyle.dp(activity, 6);
        modeToggleRow.addView(nearChip, lp1);

        TextView allChip = createChipView(
                "전체 기록에서 더 넣기 (" + allRefs.size() + "장)",
                showAllPhotosInPreview,
                () -> {
                    if (!showAllPhotosInPreview) {
                        showAllPhotosInPreview = true;
                        recomputeDisplayedList();
                    }
                });
        modeToggleRow.addView(allChip, new LinearLayout.LayoutParams(-2, -2));
    }

    private void startGpsResolutionAndPreview() {
        gpsLoading = true;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (statusText != null) {
            statusText.setText("사진의 위치 정보를 확인하고 있어요...");
        }
        worker.execute(() -> {
            PersonalPlaceGpsReader reader = new PersonalPlaceGpsReader(activity);
            Map<String, PersonalPlaceGpsPoint> resolved = reader.resolveForGroup(
                    allRefs,
                    (done, total) -> main.post(() -> {
                        if (!closed && statusText != null && gpsLoading) {
                            statusText.setText("근처 사진을 찾고 있어요 (" + done + " / " + total + "장)...");
                        }
                    }),
                    () -> closed);
            main.post(() -> {
                if (closed) {
                    return;
                }
                gpsLoading = false;
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                gpsByUri = resolved;
                recomputeCandidatesFromGps();
            });
        });
    }

    private void recomputeCandidatesFromGps() {
        ArrayList<PersonalPlaceGpsPoint> anchorPoints = new ArrayList<>();
        for (String uri : anchorUris) {
            PersonalPlaceGpsPoint pt = gpsByUri.get(uri);
            if (pt != null) {
                anchorPoints.add(pt);
            }
        }
        if (existingPlace != null && existingPlace.hasCenterCoordinates && anchorPoints.isEmpty()) {
            anchorAnalysis = new PersonalPlaceMatcher.AnchorAnalysis(
                    true, false, existingPlace.centerLat, existingPlace.centerLng, 0.0d, 1);
        } else {
            anchorAnalysis = PersonalPlaceMatcher.analyzeAnchors(anchorPoints);
        }

        if (anchorAnalysis.spreadTooWide) {
            statusText.setText("선택한 기준 사진들이 서로 멀리 떨어져 있어요(최대 "
                    + Math.round(anchorAnalysis.maxSpreadMeters)
                    + "m). 같은 장소의 사진만 기준으로 다시 골라 주세요.");
            statusText.setTextColor(0xffb42318);
        } else if (!anchorAnalysis.hasValidCoordinate) {
            statusText.setText("기준 사진에서 GPS 좌표를 읽지 못했어요. 직접 포함할 사진만 골라 저장하거나, 위치 정보가 있는 기준 사진을 다시 골라 주세요.");
            statusText.setTextColor(CleanupUiStyle.MUTED);
        } else {
            statusText.setTextColor(CleanupUiStyle.MUTED);
        }

        List<String> candidateUris = PersonalPlaceMatcher.findPreviewCandidateUris(
                anchorAnalysis.centerLat,
                anchorAnalysis.centerLng,
                anchorAnalysis.hasValidCoordinate && !anchorAnalysis.spreadTooWide,
                selectedRadius,
                allRefs,
                gpsByUri,
                anchorUris,
                manuallyToggledOutUris,
                lockedInSiblingPlaces);

        initialRadiusMatchedUris.clear();
        initialRadiusMatchedUris.addAll(candidateUris);

        checkedUris.clear();
        checkedUris.addAll(anchorUris);
        for (String uri : candidateUris) {
            if (!manuallyToggledOutUris.contains(uri)) {
                checkedUris.add(uri);
            }
        }
        checkedUris.addAll(manuallyToggledInUris);

        recomputeDisplayedList();
    }

    private void recomputeDisplayedList() {
        ArrayList<DiscoveryPhotoRef> nextDisplayed = new ArrayList<>();
        if (showAllPhotosInPreview) {
            for (DiscoveryPhotoRef ref : allRefs) {
                if (!lockedInSiblingPlaces.contains(ref.sourceUri)) {
                    nextDisplayed.add(ref);
                }
            }
        } else {
            LinkedHashSet<String> candidateSet = new LinkedHashSet<>(initialRadiusMatchedUris);
            candidateSet.addAll(checkedUris);
            for (DiscoveryPhotoRef ref : allRefs) {
                if (candidateSet.contains(ref.sourceUri)) {
                    nextDisplayed.add(ref);
                }
            }
        }
        displayedRefs = nextDisplayed;
        bindModeToggleRow(initialRadiusMatchedUris.size());
        refreshPreviewCountsAndButtons();
        if (gridAdapter != null) {
            gridAdapter.notifyDataSetChanged();
        }
    }

    private void refreshPreviewCountsAndButtons() {
        if (statusText != null && anchorAnalysis.hasValidCoordinate && !anchorAnalysis.spreadTooWide) {
            statusText.setText("선택한 범위(" + selectedRadius + "m) 근처 사진 "
                    + initialRadiusMatchedUris.size() + "장을 찾았어요. 눌러서 빼거나 더할 수 있어요. 앞으로 이 범위 안에서 찍는 새 사진도 자동으로 모아드려요.");
        }
        if (primaryButton != null) {
            boolean canSave = !gpsLoading && !anchorAnalysis.spreadTooWide && !checkedUris.isEmpty();
            primaryButton.setEnabled(canSave);
            primaryButton.setAlpha(canSave ? 1.0f : 0.45f);
            primaryButton.setText("저장하기 (" + checkedUris.size() + "장)");
        }
    }

    private void savePersonalPlace() {
        if (gpsLoading) {
            return;
        }
        if (anchorAnalysis.spreadTooWide) {
            Toast.makeText(activity, "서로 멀리 떨어진 기준 사진이 함께 선택되었어요. 기준 사진을 다시 골라 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = nameInput == null ? "" : nameInput.getText().toString().trim();
        if (name.isEmpty() || name.matches("(?s).*\\p{Cntrl}.*")) {
            if (nameInput != null) {
                nameInput.setError("장소 이름을 입력해 주세요 (예: 우리집, 회사)");
                nameInput.requestFocus();
            }
            return;
        }
        if (checkedUris.isEmpty()) {
            Toast.makeText(activity, "장소에 담을 사진을 1장 이상 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<DiscoveryPhotoRef> includedRefs = new ArrayList<>();
        ArrayList<PersonalPlaceGpsPoint> confirmedPoints = new ArrayList<>();
        for (String uri : checkedUris) {
            DiscoveryPhotoRef ref = refByUri.get(uri);
            if (ref != null) {
                includedRefs.add(ref);
            }
            PersonalPlaceGpsPoint pt = gpsByUri.get(uri);
            if (pt != null && pt.hasCoordinates()) {
                confirmedPoints.add(pt);
            }
        }

        ArrayList<DiscoveryPhotoRef> excludedRefs = new ArrayList<>();
        for (String uri : manuallyToggledOutUris) {
            DiscoveryPhotoRef ref = refByUri.get(uri);
            if (ref != null) {
                excludedRefs.add(ref);
            }
        }

        int effectiveRadius = anchorAnalysis.hasValidCoordinate
                ? PersonalPlaceMatcher.computeEffectiveRadius(
                selectedRadius, anchorAnalysis.centerLat, anchorAnalysis.centerLng, confirmedPoints)
                : selectedRadius;

        long now = System.currentTimeMillis();
        PersonalPlaceStore store = new PersonalPlaceStore(activity);
        PersonalPlace saved = store.savePlace(
                existingPlace == null ? "" : existingPlace.placeId,
                parentStableId,
                detail.record.memoryKey,
                name,
                anchorAnalysis.centerLat,
                anchorAnalysis.centerLng,
                anchorAnalysis.hasValidCoordinate,
                selectedRadius,
                effectiveRadius,
                includedRefs,
                excludedRefs,
                now);
        store.syncAutoMatchesForGroup(parentStableId, detail.record.memoryKey, allRefs, gpsByUri, now);
        PersonalPlace latest = store.findById(saved.placeId);
        dialog.dismiss();
        if (onSaved != null) {
            onSaved.accept(latest != null ? latest : saved);
        }
    }

    private GridView createPhotoGridView() {
        GridView grid = new GridView(activity);
        grid.setNumColumns(3);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        int spacing = CleanupUiStyle.dp(activity, 6);
        grid.setHorizontalSpacing(spacing);
        grid.setVerticalSpacing(spacing);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setClipToPadding(false);
        return grid;
    }

    private TextView createChipView(String text, boolean selected, Runnable onClick) {
        TextView chip = CleanupUiStyle.text(
                activity, text, 12, selected, selected ? Color.WHITE : CleanupUiStyle.PURPLE);
        int hPad = CleanupUiStyle.dp(activity, 12);
        int vPad = CleanupUiStyle.dp(activity, 7);
        chip.setPadding(hPad, vPad, hPad, vPad);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(CleanupUiStyle.dp(activity, 16));
        bg.setColor(selected ? CleanupUiStyle.PURPLE : 0xfff3effb);
        bg.setStroke(CleanupUiStyle.dp(activity, 1), selected ? CleanupUiStyle.PURPLE : 0xffdcd3f2);
        chip.setBackground(bg);
        chip.setOnClickListener(v -> onClick.run());
        return chip;
    }

    private void close() {
        if (closed) {
            return;
        }
        closed = true;
        worker.shutdownNow();
        thumbPool.shutdownNow();
        thumbCache.evictAll();
    }

    private final class PhotoGridAdapter extends BaseAdapter {
        private final boolean anchorStep;

        PhotoGridAdapter(boolean anchorStep) {
            this.anchorStep = anchorStep;
        }

        @Override
        public int getCount() {
            return displayedRefs.size();
        }

        @Override
        public Object getItem(int position) {
            return displayedRefs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MemorySelectionTile tile = convertView instanceof MemorySelectionTile
                    ? (MemorySelectionTile) convertView
                    : new MemorySelectionTile(activity);
            tile.setLayoutParams(new AbsListView.LayoutParams(-1, CleanupUiStyle.dp(activity, 96)));
            DiscoveryPhotoRef ref = displayedRefs.get(position);
            boolean isSelected = anchorStep
                    ? anchorUris.contains(ref.sourceUri)
                    : checkedUris.contains(ref.sourceUri);
            tile.bindState(isSelected, ref.mediaKind == MediaKind.VIDEO,
                    ref.displayName == null || ref.displayName.isEmpty() ? "사진" : ref.displayName);

            ImageView image = tile.image;
            image.setTag(ref.sourceUri);
            Bitmap cached = thumbCache.get(ref.sourceUri);
            image.setImageBitmap(cached);
            if (cached == null && !closed) {
                final String uriStr = ref.sourceUri;
                thumbPool.execute(() -> {
                    try {
                        Bitmap bmp = activity.getContentResolver().loadThumbnail(
                                Uri.parse(uriStr), new Size(240, 240), null);
                        if (bmp != null) {
                            thumbCache.put(uriStr, bmp);
                            main.post(() -> {
                                if (!closed && uriStr.equals(image.getTag())) {
                                    image.setImageBitmap(bmp);
                                }
                            });
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
            return tile;
        }
    }

    static List<DiscoveryPhotoRef> extractAllRefs(MemoryBrowserDetail detail) {
        if (detail == null) {
            return Collections.emptyList();
        }
        ArrayList<DiscoveryPhotoRef> combined = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        if (detail.record != null && detail.record.discoveryGroup != null
                && detail.record.discoveryGroup.photoRefs != null) {
            for (DiscoveryPhotoRef ref : detail.record.discoveryGroup.photoRefs) {
                if (ref != null && !ref.stale && ref.sourceUri != null && !ref.sourceUri.trim().isEmpty()
                        && seen.add(ref.sourceUri.trim())) {
                    combined.add(ref);
                }
            }
        }
        if (detail.photoSections != null) {
            for (MemoryPhotoSection section : detail.photoSections) {
                if (section == null || section.photos == null) {
                    continue;
                }
                for (MemoryPhotoItem item : section.photos) {
                    if (item != null && !item.sourceUri.isEmpty() && seen.add(item.sourceUri.trim())) {
                        combined.add(new DiscoveryPhotoRef(
                                item.sourceUri,
                                item.mediaStoreId,
                                item.mediaKind,
                                item.mimeType,
                                item.displayName,
                                item.takenAtMillis,
                                detail.record == null ? "" : detail.record.placeKey,
                                detail.item == null ? "" : detail.item.title,
                                detail.record == null ? "" : detail.record.countryCode,
                                detail.record == null ? "" : detail.record.countryName,
                                detail.record == null ? "" : detail.record.adminArea,
                                detail.record == null ? "" : detail.record.addressLine,
                                item.sourceRelativePath,
                                0L,
                                0L,
                                false,
                                item.manualAssigned));
                    }
                }
            }
        }
        return combined;
    }

    static String resolveStableId(Activity activity, String memoryKey) {
        try {
            return new MemoryIdentityRegistryStore(activity).findStableId(memoryKey);
        } catch (Exception ignored) {
            return "";
        }
    }
}
