package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreviewCompletionPresenterTest {
    @Test public void zeroNewPlacesUsesQuietExistingRecordsNotice() {
        PreviewCompletionPresenter.present(0, () -> fail("Unexpected popup"), () -> { });
    }

    @Test public void newPlacesKeepDiscoveryResultAction() {
        PreviewCompletionPresenter.present(2, () -> { }, () -> fail("Missing discovery result"));
    }
}
