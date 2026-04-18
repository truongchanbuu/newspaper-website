package com.fa.core.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class DateUtils {

    private DateUtils() {}

    public static final String DATE_WITH_NAME_PATTERN = "MMMM dd, yyyy";
    public static final String ISO_DATE_PATTERN = "yyyy-MM-dd";

    public static String computeTimeAgo(Calendar publishedDate) {
        if (publishedDate == null) {
            return "";
        }

        return computeTimeAgo(publishedDate.getTime());
    }

    public static String computeTimeAgo(Date publishedDate) {
        if (publishedDate == null) {
            return "";
        }

        long now = System.currentTimeMillis();
        long diffMillis = now - publishedDate.getTime();

        if (diffMillis < 0) {
            return "just now";
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

        if (minutes < 1) {
            return "just now";
        }

        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        if (days == 1) {
            return "yesterday";
        }

        if (days < 30) {
            return days + " days ago";
        }

        long months = days / 30;
        if (months < 12) {
            return months + (months == 1 ? " month ago" : " months ago");
        }

        long years = months / 12;
        return years + (years == 1 ? " year ago" : " years ago");
    }
}