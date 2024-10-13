package net.isger.util;

import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Dates {

    private static final String DATE_PATTERNS[] = { "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "yyyy-MM", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd", "yyyy/MM", "yyyyMMddHHmmss", "yyyyMMddHHmm", "yyyyMMdd", "yyyyMM", "yyyy", "HH:mm:ss", "HH:mm", "EEE MMM dd HH:mm:ss zzz yyyy", "yyyy-MM-dd'T'HH:mm:ssXXX", "MMM dd, yyyy hh:mm:ss a" };

    public static final int PATTERN_DEFAULT = 0;

    public static final int PATTERN_COMMON = 1;

    public static final int PATTERN_COMMON_MINUTE = 2;

    /** 通用日期格式（yyyy-MM-dd） */
    public static final int PATTERN_COMMON_DATE = 3;

    public static final int PATTERN_COMMON_MONTH = 4;

    public static final int PATTERN_NORMAL = 5;

    public static final int PATTERN_NORMAL_MINUTE = 6;

    public static final int PATTERN_NORMAL_DATE = 7;

    public static final int PATTERN_NORMAL_MONTH = 8;

    public static final int PATTERN_COMPACT = 9;

    public static final int PATTERN_COMPACT_MINUTE = 10;

    public static final int PATTERN_COMPACT_DATE = 11;

    public static final int PATTERN_COMPACT_MONTH = 12;

    public static final int PATTERN_YEAR = 13;

    public static final int PATTERN_TIME = 14;

    public static final int PATTERN_TIME_MINUTE = 15;

    public static final int PATTERN_RFC_822 = 16;

    public static final int PATTERN_RFC_3339 = 17;

    public static final int PATTERN_APPEAR = 18;

    private static final int UNITS[] = { 1, 1000, 60000, 3600000, 86400000 };

    public static final int UNIT_MILLIS = 0;

    public static final int UNIT_SECOND = 1;

    public static final int UNIT_MINUTE = 2;

    public static final int UNIT_HOUR = 3;

    public static final int UNIT_DAY = 4;

    private Dates() {
    }

    public static String getPattern(int type) {
        if (type < 0 && type > DATE_PATTERNS.length) type = 0;
        return DATE_PATTERNS[type];
    }

    public static Date toDate(Object value) {
        if (value == null) return null;
        Date date;
        if (value instanceof java.sql.Timestamp) {
            date = new Date(((Timestamp) value).getTime());
        } else if (value instanceof Date) {
            date = (Date) value;
        } else if (value instanceof Number) {
            date = new Date(((Number) value).longValue());
        } else {
            String source = String.valueOf(value);
            try {
                source = LocalDateTime.parse(source).format(DateTimeFormatter.ofPattern(DATE_PATTERNS[PATTERN_COMMON]));
            } catch (Exception e) {
            }
            source = source.replaceFirst("\\d+([-/]?\\d+)T", " ").replaceAll("[年月日]", "-").replaceAll("[时分]", ":").replaceAll("秒", "");
            date = parse(source, Locale.getDefault(Locale.Category.FORMAT));
            if (date == null && Locale.getDefault(Locale.Category.FORMAT) != Locale.ENGLISH) {
                date = parse(source, Locale.ENGLISH);
            }
        }
        return date;
    }

    private static Date parse(String source, Locale locale) {
        Date date = null;
        SimpleDateFormat parser = new SimpleDateFormat("", locale);
        parser.setLenient(true);
        ParsePosition pos = new ParsePosition(0);
        for (int i = 0; i < DATE_PATTERNS.length; i++) {
            parser.applyPattern(DATE_PATTERNS[i]);
            pos.setIndex(0);
            date = parser.parse(source, pos);
            if (date != null && pos.getIndex() == source.length()) break;
        }
        return date;
    }

    public static String toString(Object date) {
        return toString(date, PATTERN_DEFAULT);
    }

    public static String toString(Object date, int selector) {
        return toString(toDate(date), selector);
    }

    public static String toString(int selector) {
        return toString(null, selector);
    }

    public static String toString(Date date) {
        return toString(date, PATTERN_DEFAULT);
    }

    public static String toString(Date date, int selector) {
        if (date == null) date = new Date();
        if (selector < 0 || selector >= DATE_PATTERNS.length) selector = PATTERN_DEFAULT;
        return toString(date, DATE_PATTERNS[selector]);
    }

    public static String toString(Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }

    public static Date getDate() {
        return getDate(null, 0, 0);
    }

    public static Date getDate(long delay) {
        return getDate(null, delay, 0);
    }

    public static Date getDate(long delay, int unit) {
        return getDate(null, delay, unit);
    }

    public static Date getDate(Date startTime, long delay) {
        return getDate(startTime, delay, UNIT_MILLIS);
    }

    public static Date getDate(Date startTime, long delay, int unit) {
        if (unit < UNIT_MILLIS || unit > UNIT_DAY) unit = UNIT_DAY;
        if (startTime == null) startTime = new Date();
        if (delay != 0) startTime = new Date(startTime.getTime() + delay * UNITS[unit]);
        return startTime;
    }

    public static long getGap(Date startTime, Date endTime) {
        return getGap(startTime, endTime, UNIT_DAY);
    }

    public static long getGap(Date startTime) {
        return getGap(startTime, UNIT_DAY);
    }

    public static long getGap(Date startTime, int unit) {
        return getGap(startTime, new Date(), unit);
    }

    public static long getGap(Date startTime, Date endTime, int unit) {
        if (unit < 0 || unit > UNIT_DAY) unit = UNIT_DAY;
        Calendar sc = Calendar.getInstance();
        sc.setTime(startTime);
        Calendar ec = Calendar.getInstance();
        ec.setTime(endTime);
        switch (unit) {
        case UNIT_DAY:
            sc.set(Calendar.HOUR_OF_DAY, 0);
            ec.set(Calendar.HOUR_OF_DAY, 0);
        case UNIT_HOUR:
            sc.set(Calendar.MINUTE, 0);
            ec.set(Calendar.MINUTE, 0);
        case UNIT_MINUTE:
            ec.set(Calendar.SECOND, 0);
            sc.set(Calendar.SECOND, 0);
        case UNIT_SECOND:
            sc.set(Calendar.MILLISECOND, 0);
            ec.set(Calendar.MILLISECOND, 0);
        }
        return (ec.getTimeInMillis() - sc.getTimeInMillis()) / UNITS[unit];
    }

    public static Date getMin(Date source, int delay) {
        return getMin(source, delay, UNIT_DAY);
    }

    public static Date getMin(Date source, int delay, int unit) {
        return getMin(source, getDate(delay, unit));
    }

    public static Date getMin(Date source, Date target) {
        return source.getTime() >= target.getTime() ? target : source;
    }

    public static Date getMax(Date source, int delay) {
        return getMax(source, delay, UNIT_DAY);
    }

    public static Date getMax(Date source, int delay, int unit) {
        return getMax(source, getDate(delay, unit));
    }

    public static Date getMax(Date source, Date target) {
        return source.getTime() >= target.getTime() ? source : target;
    }

}
