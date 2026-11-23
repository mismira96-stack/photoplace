package com.example.gallerysorter;

import java.util.function.BooleanSupplier;

/** A Back press cancels pending screen entry before applying the old screen's Back action. */
final class MemoryLoadNavigation {
    private final BooleanSupplier browserPending, detailPending;
    private final Runnable cancelBrowser, cancelDetail;
    private final BooleanSupplier countryPending;
    private final Runnable cancelCountry;

    MemoryLoadNavigation(BooleanSupplier browserPending, Runnable cancelBrowser,
                         BooleanSupplier detailPending, Runnable cancelDetail) {
        this(browserPending, cancelBrowser, detailPending, cancelDetail, () -> false, () -> { });
    }

    MemoryLoadNavigation(BooleanSupplier browserPending, Runnable cancelBrowser,
                         BooleanSupplier detailPending, Runnable cancelDetail,
                         BooleanSupplier countryPending, Runnable cancelCountry) {
        this.browserPending = browserPending; this.cancelBrowser = cancelBrowser;
        this.detailPending = detailPending; this.cancelDetail = cancelDetail;
        this.countryPending = countryPending; this.cancelCountry = cancelCountry;
    }

    void back(Runnable existingBack) {
        if (browserPending.getAsBoolean() || detailPending.getAsBoolean() || countryPending.getAsBoolean()) cancel();
        else existingBack.run();
    }

    void cancel() { cancelBrowser.run(); cancelDetail.run(); cancelCountry.run(); }

    void switchTab(int target, Runnable cancelBrowserUnlessPreloading) {
        if (target == 1) {
            cancelCountry.run();
            cancelDetail.run();
            cancelBrowserUnlessPreloading.run();
        } else {
            cancel();
        }
    }

    // An intent/result can request a detail before onResume. Do not replace its origin
    // with Home while the asynchronous detail loader is still binding that request.
    void whenIdle(Runnable action) {
        if(!browserPending.getAsBoolean()&&!detailPending.getAsBoolean()&&!countryPending.getAsBoolean())action.run();
    }
}
