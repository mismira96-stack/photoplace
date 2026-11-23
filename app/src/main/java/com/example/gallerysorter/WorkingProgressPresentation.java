package com.example.gallerysorter;

/** Presentation only; numbers come from the existing sort/analysis progress pipeline. */
final class WorkingProgressPresentation {
    final boolean visible, indeterminate;
    final String title, detail;
    final int current, total, percent;

    private WorkingProgressPresentation(boolean visible, String title, int current, int total) {
        this.visible = visible;
        this.title = title;
        this.total = Math.max(0, total);
        this.current = Math.min(Math.max(0, current), this.total);
        this.indeterminate = this.total == 0;
        this.percent = indeterminate ? 0 : (int) Math.round(this.current * 100.0 / this.total);
        this.detail = indeterminate ? "준비 중이에요. 백그라운드에서도 계속 진행됩니다."
                : this.current + " / " + this.total + " 완료 · " + percent + "%";
    }

    static WorkingProgressPresentation from(boolean working, String message, String label, int current, int total) {
        String title = label == null ? "" : label.trim();
        if (title.isEmpty()) title = message == null ? "" : message.trim();
        if (title.isEmpty()) title = "작업 진행 중";
        return new WorkingProgressPresentation(working, title, current, total);
    }
}
