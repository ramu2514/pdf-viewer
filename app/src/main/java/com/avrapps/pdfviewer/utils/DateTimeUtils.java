package com.avrapps.pdfviewer.utils;

import android.content.Context;

import com.avrapps.pdfviewer.R;

import java.util.Date;

public class DateTimeUtils {
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String getTimeAgo(long time, Context c) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = new Date().getTime();
        if (time > now || time <= 0) {
            return new Date(time).toLocaleString();
            //return "in the future";
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return c.getString(R.string.moments_Ago);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return c.getString(R.string.minute_ago);
        } else if (diff < 50 * MINUTE_MILLIS) {
            return c.getString(R.string.minutes_ago, diff / MINUTE_MILLIS);
        } else if (diff < 90 * MINUTE_MILLIS) {
            return c.getString(R.string.hour_ago);
        } else if (diff < 24 * HOUR_MILLIS) {
            return c.getString(R.string.hours_ago, diff / HOUR_MILLIS);
        } else if (diff < 48 * HOUR_MILLIS) {
            return c.getString(R.string.yesterday);
        } else {
            return c.getString(R.string.days_ago, diff / DAY_MILLIS);
        }
    }
}
