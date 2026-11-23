package com.example.gallerysorter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Coordinates Personal Place chip filtering, creation entry, and management actions
 * inside the discovery detail screen without adding a single line to MainActivity.
 */
final class PersonalPlaceDetailPresenter {
    static final String FILTER_ALL = "__all__";
    static final String FILTER_REMAINING = "__remaining__";

    private static final Map<MemoryDetailSelection, PersonalPlaceDetailPresenter> ACTIVE_BY_SELECTION =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile String pendingMemoryKey = "";
    private static volatile String pendingPlaceId = "";

    private final Activity activity;
    private final MemoryBrowserDetail detail;
    private final MemoryDetailSelection selection;
    private final String parentStableId;
    private final LinearLayout filterHost;
    private final Runnable onFilterChanged;

    private String activeFilterId = FILTER_ALL;
    private List<PersonalPlace> places = Collections.emptyList();

    static void requestInitialPlace(String memoryKey, String placeId) {
        pendingMemoryKey = memoryKey == null ? "" : memoryKey.trim();
        pendingPlaceId = placeId == null ? "" : placeId.trim();
    }

    static View createHeaderAction(Activity activity,
                                   MemoryBrowserDetail detail,
                                   MemoryDetailSelection selection) {
        ImageView button = new ImageView(activity);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setImageDrawable(MediaVectorIconDrawable.discoveryAdd(
                CleanupUiStyle.PURPLE, CleanupUiStyle.dp(activity, 20), false));
        int inset = CleanupUiStyle.dp(activity, 8);
        button.setPadding(inset, inset, inset, inset);
        button.setContentDescription("개인 장소 만들기");
        button.setTooltipText("개인 장소 만들기");
        TypedValue selectable = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, selectable, true);
        button.setBackgroundResource(selectable.resourceId);
        button.setEnabled(!detail.sourceUris.isEmpty());
        button.setAlpha(button.isEnabled() ? 1f : 0.4f);
        button.setOnClickListener(v -> {
            PersonalPlaceDetailPresenter presenter = ACTIVE_BY_SELECTION.get(selection);
            if (presenter != null) {
                presenter.openCreationSheet(null);
            } else {
                PersonalPlaceCreationSheet.show(activity, detail, selection.selectedUris(), null, saved -> {
                });
            }
        });
        return button;
    }

    static PersonalPlaceDetailPresenter attach(Activity activity,
                                               LinearLayout parent,
                                               MemoryBrowserDetail detail,
                                               MemoryDetailSelection selection,
                                               Runnable onFilterChanged) {
        PersonalPlaceDetailPresenter presenter =
                new PersonalPlaceDetailPresenter(activity, parent, detail, selection, onFilterChanged);
        ACTIVE_BY_SELECTION.put(selection, presenter);
        selection.setSelectionListener(presenter::onSelectionChanged);
        presenter.reloadPlacesAndBind();
        return presenter;
    }

    private PersonalPlaceDetailPresenter(Activity activity,
                                         LinearLayout parent,
                                         MemoryBrowserDetail detail,
                                         MemoryDetailSelection selection,
                                         Runnable onFilterChanged) {
        this.activity = activity;
        this.detail = detail;
        this.selection = selection;
        this.parentStableId = PersonalPlaceCreationSheet.resolveStableId(activity, detail.record.memoryKey);
        this.onFilterChanged = onFilterChanged;
        this.filterHost = new LinearLayout(activity);
        this.filterHost.setOrientation(LinearLayout.VERTICAL);
        parent.addView(this.filterHost, new LinearLayout.LayoutParams(-1, -2));

        if (!pendingMemoryKey.isEmpty() && pendingMemoryKey.equals(detail.record.memoryKey)
                && !pendingPlaceId.isEmpty()) {
            this.activeFilterId = pendingPlaceId;
            pendingMemoryKey = "";
            pendingPlaceId = "";
        }
    }

    void openCreationSheet(PersonalPlace toEdit) {
        PersonalPlaceCreationSheet.show(
                activity,
                detail,
                selection.selectedUris(),
                toEdit,
                saved -> {
                    selection.clear();
                    if (saved != null) {
                        activeFilterId = saved.placeId;
                    }
                    reloadPlacesAndBind();
                    onFilterChanged.run();
                    if (saved != null) {
                        Toast.makeText(activity, "‘" + saved.name + "’ 장소를 저장했어요.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    List<MemoryPhotoSection> activePhotoSections() {
        if (FILTER_ALL.equals(activeFilterId) || places.isEmpty()) {
            return detail.photoSections;
        }
        if (FILTER_REMAINING.equals(activeFilterId)) {
            return PersonalPlaceMatcher.filterSectionsForRemaining(detail.photoSections, places);
        }
        PersonalPlace active = findActivePlace();
        if (active == null) {
            return detail.photoSections;
        }
        return PersonalPlaceMatcher.filterSectionsForPlace(detail.photoSections, active);
    }

    List<MemoryDateNote> activeNotes(List<MemoryPhotoSection> activeSections) {
        if (FILTER_ALL.equals(activeFilterId) || places.isEmpty()) {
            return detail.notes;
        }
        return PersonalPlaceMatcher.filterNotesForSections(detail.notes, activeSections);
    }

    private PersonalPlace findActivePlace() {
        for (PersonalPlace place : places) {
            if (place != null && place.placeId.equals(activeFilterId)) {
                return place;
            }
        }
        return null;
    }

    private void onSelectionChanged() {
        bindFilterBar();
    }

    private void reloadPlacesAndBind() {
        PersonalPlaceStore store = new PersonalPlaceStore(activity);
        List<DiscoveryPhotoRef> allRefs = PersonalPlaceCreationSheet.extractAllRefs(detail);
        Map<String, PersonalPlaceGpsPoint> cachedGps = new PersonalPlaceGpsCacheStore(activity).readAll();
        if (!store.listForParent(parentStableId, detail.record.memoryKey).isEmpty()) {
            places = store.syncAutoMatchesForGroup(
                    parentStableId, detail.record.memoryKey, allRefs, cachedGps, System.currentTimeMillis());
        } else {
            places = Collections.emptyList();
        }
        if (!FILTER_ALL.equals(activeFilterId)
                && !FILTER_REMAINING.equals(activeFilterId)
                && findActivePlace() == null) {
            activeFilterId = FILTER_ALL;
        }
        PersonalPlace activePlace = findActivePlace();
        selection.setStoryTitleOverride(activePlace != null ? activePlace.name : "");
        bindFilterBar();
    }

    private void bindFilterBar() {
        filterHost.removeAllViews();
        if (places.isEmpty() || detail.sourceUris.isEmpty()) {
            return;
        }
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, CleanupUiStyle.dp(activity, 4), 0, CleanupUiStyle.dp(activity, 8));

        int totalCount = MemoryPhotoPage.totalItemCount(detail.photoSections);
        addChip(row, "전체 " + formatCount(totalCount), FILTER_ALL.equals(activeFilterId), () -> selectFilter(FILTER_ALL));

        for (PersonalPlace place : places) {
            if (place == null) {
                continue;
            }
            int count = MemoryPhotoPage.totalItemCount(
                    PersonalPlaceMatcher.filterSectionsForPlace(detail.photoSections, place));
            boolean selected = place.placeId.equals(activeFilterId);
            String label = place.name + " " + formatCount(count);
            if (selected) {
                label += "  ⚙";
            }
            addChip(row, label,
                    selected,
                    () -> {
                        if (selected) {
                            showPlaceActions(place);
                        } else {
                            selectFilter(place.placeId);
                        }
                    });
        }

        if (!places.isEmpty()) {
            int remainingCount = MemoryPhotoPage.totalItemCount(
                    PersonalPlaceMatcher.filterSectionsForRemaining(detail.photoSections, places));
            addChip(row, "나머지 " + formatCount(remainingCount),
                    FILTER_REMAINING.equals(activeFilterId),
                    () -> selectFilter(FILTER_REMAINING));
        }

        addAddPlaceChip(row, () -> openCreationSheet(null));

        scroll.addView(row);
        filterHost.addView(scroll, new LinearLayout.LayoutParams(-1, -2));

        PersonalPlace activePlace = findActivePlace();
        if (activePlace != null) {
            View selectionAction = buildSelectionBannerIfAny(activePlace);
            if (selectionAction != null) {
                filterHost.addView(selectionAction, new LinearLayout.LayoutParams(-1, -2));
            }
        }
    }

    private void addAddPlaceChip(LinearLayout row, Runnable onClick) {
        TextView chip = CleanupUiStyle.text(activity, "+ 장소", 12, true, CleanupUiStyle.PURPLE);
        int hPad = CleanupUiStyle.dp(activity, 11);
        int vPad = CleanupUiStyle.dp(activity, 7);
        chip.setPadding(hPad, vPad, hPad, vPad);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(CleanupUiStyle.dp(activity, 18));
        bg.setColor(0xfff5f2ff);
        bg.setStroke(CleanupUiStyle.dp(activity, 1), 0xffdcd3f2);
        chip.setBackground(bg);
        chip.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = CleanupUiStyle.dp(activity, 6);
        row.addView(chip, lp);
    }

    private View buildSelectionBannerIfAny(PersonalPlace place) {
        List<String> selectedUris = selection.selectedUris();
        if (selectedUris.isEmpty()) {
            return null;
        }
        List<MemoryPhotoSection> placeSections =
                PersonalPlaceMatcher.filterSectionsForPlace(detail.photoSections, place);
        Map<String, MemoryPhotoItem> photoItemByUri = new LinkedHashMap<>();
        for (MemoryPhotoSection sec : placeSections) {
            for (MemoryPhotoItem item : sec.photos) {
                photoItemByUri.put(item.sourceUri, item);
            }
        }
        ArrayList<MemoryPhotoItem> selectedInThisPlace = new ArrayList<>();
        for (String uri : selectedUris) {
            MemoryPhotoItem item = photoItemByUri.get(uri);
            if (item != null) {
                selectedInThisPlace.add(item);
            }
        }
        if (selectedInThisPlace.isEmpty()) {
            return null;
        }

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = CleanupUiStyle.dp(activity, 8);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(CleanupUiStyle.card(activity, Color.WHITE));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = CleanupUiStyle.dp(activity, 8);
        card.setLayoutParams(cardLp);

        Button excludeBtn = MemorySelectionStyle.action(
                activity,
                "‘" + place.name + "’에서 빼기 (" + selectedInThisPlace.size() + "개)",
                true);
        excludeBtn.setOnClickListener(v -> {
            new PersonalPlaceStore(activity).excludePhotos(
                    place.placeId, selectedInThisPlace, System.currentTimeMillis());
            selection.clear();
            reloadPlacesAndBind();
            onFilterChanged.run();
            Toast.makeText(
                    activity,
                    "‘" + place.name + "’에서 " + selectedInThisPlace.size()
                            + "개를 제외했어요. 실제 파일과 상위 기록은 그대로 유지돼요.",
                    Toast.LENGTH_SHORT).show();
        });
        card.addView(excludeBtn, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    private void showPlaceActions(PersonalPlace place) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        int p = CleanupUiStyle.dp(activity, 20);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.setBackground(MemorySelectionStyle.surface(activity, 0xfffffbff, 24));

        TextView brand = CleanupUiStyle.text(activity, "PhotoPlace", 11, true, CleanupUiStyle.PURPLE);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand);

        TextView heading = CleanupUiStyle.text(activity, "‘" + place.name + "’ 장소 설정", 18, true, CleanupUiStyle.INK);
        heading.setPadding(0, p / 3, 0, p / 2);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        Button rangeBtn = CleanupUiStyle.button(activity, "사진·범위 조절", false);
        rangeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openCreationSheet(place);
        });
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(-1, -2);
        lp1.bottomMargin = CleanupUiStyle.dp(activity, 8);
        root.addView(rangeBtn, lp1);

        Button renameBtn = CleanupUiStyle.button(activity, "이름 변경", false);
        renameBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showRenameDialog(place);
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, -2);
        lp2.bottomMargin = CleanupUiStyle.dp(activity, 8);
        root.addView(renameBtn, lp2);

        Button deleteBtn = CleanupUiStyle.button(activity, "장소 삭제", false);
        deleteBtn.setTextColor(0xffd32f2f);
        deleteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteConfirmation(place);
        });
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(-1, -2);
        lp3.bottomMargin = CleanupUiStyle.dp(activity, 12);
        root.addView(deleteBtn, lp3);

        Button closeBtn = MemorySelectionStyle.action(activity, "닫기", true);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        root.addView(closeBtn, new LinearLayout.LayoutParams(-1, -2));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(Math.min(CleanupUiStyle.dp(activity, 340),
                    activity.getResources().getDisplayMetrics().widthPixels - CleanupUiStyle.dp(activity, 48)), -2);
        }
        dialog.show();
    }

    private void showRenameDialog(PersonalPlace place) {
        MemoryActionDialog.input(activity, newName -> {
            PersonalPlaceStore store = new PersonalPlaceStore(activity);
            PersonalPlace current = store.findById(place.placeId);
            if (current != null) {
                ArrayList<DiscoveryPhotoRef> incRefs = new ArrayList<>();
                ArrayList<DiscoveryPhotoRef> excRefs = new ArrayList<>();
                for (DiscoveryPhotoRef ref : PersonalPlaceCreationSheet.extractAllRefs(detail)) {
                    if (current.includedUris.contains(ref.sourceUri)) {
                        incRefs.add(ref);
                    } else if (current.excludedUris.contains(ref.sourceUri)) {
                        excRefs.add(ref);
                    }
                }
                store.savePlace(
                        current.placeId,
                        current.parentStableId,
                        current.parentMemoryKey,
                        newName,
                        current.centerLat,
                        current.centerLng,
                        current.hasCenterCoordinates,
                        current.selectedRadiusMeters,
                        current.effectiveRadiusMeters,
                        incRefs,
                        excRefs,
                        System.currentTimeMillis());
                reloadPlacesAndBind();
                onFilterChanged.run();
                Toast.makeText(activity, "장소 이름을 ‘" + newName + "’(으)로 변경했어요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(PersonalPlace place) {
        MemoryActionDialog.show(
                activity,
                "‘" + place.name + "’ 장소를 삭제할까요?",
                "앱 내부의 ‘" + place.name + "’ 개인 장소 분류만 삭제해요. 실제 사진 파일과 ‘"
                        + detail.item.title + "’ 기록, 날짜별 메모는 모두 그대로 유지돼요.",
                "삭제하기",
                () -> {
                    new PersonalPlaceStore(activity).deletePlace(place.placeId);
                    activeFilterId = FILTER_ALL;
                    selection.clear();
                    reloadPlacesAndBind();
                    onFilterChanged.run();
                    Toast.makeText(activity, "‘" + place.name + "’ 개인 장소를 삭제했어요.", Toast.LENGTH_SHORT).show();
                },
                "취소",
                () -> {
                });
    }

    private void selectFilter(String nextFilterId) {
        if (activeFilterId.equals(nextFilterId)) {
            return;
        }
        activeFilterId = nextFilterId;
        selection.clear();
        PersonalPlace activePlace = findActivePlace();
        selection.setStoryTitleOverride(activePlace != null ? activePlace.name : "");
        bindFilterBar();
        onFilterChanged.run();
    }

    private void addChip(LinearLayout row, String label, boolean selected, Runnable onClick) {
        TextView chip = CleanupUiStyle.text(
                activity, label, 13, selected, selected ? Color.WHITE : CleanupUiStyle.PURPLE);
        int hPad = CleanupUiStyle.dp(activity, 13);
        int vPad = CleanupUiStyle.dp(activity, 8);
        chip.setPadding(hPad, vPad, hPad, vPad);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(CleanupUiStyle.dp(activity, 18));
        bg.setColor(selected ? CleanupUiStyle.PURPLE : Color.WHITE);
        bg.setStroke(CleanupUiStyle.dp(activity, 1), selected ? CleanupUiStyle.PURPLE : 0xffdcd3f2);
        chip.setBackground(bg);
        chip.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = CleanupUiStyle.dp(activity, 6);
        row.addView(chip, lp);
    }

    static Function<String, List<PersonalPlace>> loadCardShortcuts(Context context) {
        if (PersonalPlaceStore.hasAnyPlaces(context)) {
            try {
                return new PersonalPlaceStore(context).snapshotByParent(new MemoryIdentityRegistryStore(context));
            } catch (Exception ignored) {
            }
        }
        return ignored -> Collections.emptyList();
    }

    /**
     * Renders compact personal place shortcut badges on outer discovery cards (e.g. '📍 우리집 · 회사')
     * so the user can jump straight into '우리집' with 1 tap from the main discovery grid!
     */
    static void bindCardShortcuts(Context context,
                                  LinearLayout titleRow,
                                  MemoryBrowserItem item,
                                  List<PersonalPlace> cardPlaces,
                                  MemoryBrowserGridRenderer.Listener listener) {
        if (context == null || titleRow == null || item == null || cardPlaces == null || cardPlaces.isEmpty()) {
            return;
        }
        try {
            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout badgeRow = new LinearLayout(context);
            badgeRow.setOrientation(LinearLayout.HORIZONTAL);
            badgeRow.setGravity(Gravity.CENTER_VERTICAL);
            int maxShow = Math.min(2, cardPlaces.size());
            for (int i = 0; i < maxShow; i++) {
                PersonalPlace place = cardPlaces.get(i);
                if (place == null) {
                    continue;
                }
                TextView pill = CleanupUiStyle.text(context, place.name, 11, true, CleanupUiStyle.PURPLE);
                int hp = CleanupUiStyle.dp(context, 6);
                int vp = CleanupUiStyle.dp(context, 2);
                pill.setPadding(hp, vp, hp, vp);
                pill.setSingleLine(true);
                pill.setEllipsize(TextUtils.TruncateAt.END);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(CleanupUiStyle.dp(context, 5));
                bg.setColor(0xfff5f2ff);
                bg.setStroke(CleanupUiStyle.dp(context, 1), 0xffe2daf8);
                pill.setBackground(bg);
                pill.setOnClickListener(v -> {
                    requestInitialPlace(item.memoryKey, place.placeId);
                    if (listener != null) {
                        listener.onMemorySelected(item);
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.rightMargin = CleanupUiStyle.dp(context, 4);
                badgeRow.addView(pill, lp);
            }
            scroll.addView(badgeRow);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            scrollLp.leftMargin = CleanupUiStyle.dp(context, 5);
            titleRow.addView(scroll, scrollLp);
        } catch (Exception ignored) {
        }
    }

    private static String formatCount(int n) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(Math.max(0, n));
    }
}
