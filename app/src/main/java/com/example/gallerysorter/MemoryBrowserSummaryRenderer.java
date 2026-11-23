package com.example.gallerysorter;

import android.content.Context;
import android.view.View;

import java.util.List;
import java.util.Locale;

final class MemoryBrowserSummaryRenderer {
    private final Context context;

    MemoryBrowserSummaryRenderer(Context context) {
        this.context = context;
    }

    View render(List<MemoryRecord> records) {
        return render(records, null, null);
    }

    View render(List<MemoryRecord> records, View addPlaceAction, View collectAction) {
        return render(MemoryBrowserSummary.from(records), addPlaceAction, collectAction);
    }

    View render(MemoryBrowserEntry entry, View addPlaceAction, View collectAction) {
        return render(entry.summary, addPlaceAction, collectAction);
    }

    private View render(MemoryBrowserSummary summary, View addPlaceAction, View collectAction) {
        return MemoryListChrome.summary(context, "장소 " + summary.placeCount + "곳",
                String.format(Locale.KOREA, "사진·동영상 %,d개 · %s", summary.photoCount, summary.yearRange),
                addPlaceAction, collectAction);
    }
}
