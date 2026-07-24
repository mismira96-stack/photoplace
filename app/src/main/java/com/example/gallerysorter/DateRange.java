package com.example.gallerysorter;

import java.util.Date;

final class DateRange {
    Date end;
    Date start;

    void include(Date date) {
        if (date == null) {
            return;
        }
        Date currentStart = this.start;
        if (currentStart == null || date.before(currentStart)) {
            this.start = date;
        }
        Date currentEnd = this.end;
        if (currentEnd == null || date.after(currentEnd)) {
            this.end = date;
        }
    }
}
