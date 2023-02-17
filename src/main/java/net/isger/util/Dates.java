package net.isger.util;

import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Dates {

    private static final String DATE_PATTERNS[] = { "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "yyyy-MM", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd", "yyyy/MM", "yyyyMMddHHmmss", "yyyyMMddHHmm", "yyyyMMdd", "yyyyMM", "yyyy", "HH:mm:ss", "HH:mm", "MM", "dd", "HH", "mm", "ss" };

    public static final int PATTERN_DEFAULT = 0;

    public static final int PATTERN_COMMON = 1;

    public static final int PATTERN_COMMON_MINUTE = 2;

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

    public static final int PATTERN_MONTH = 16;

    public static final int PATTERN_DAY = 17;

    public static final int PATTERN_HOUR = 18;

    public static final int PATTERN_MINUTE = 19;

    public static final int PATTERN_SECOND = 20;

    private static final int UNITS[] = { 1, 1000, 60000, 3600000, 86400000 };

    public static final int UNIT_MILLIS = 0;

    public static final int UNIT_SECOND = 1;

    public static final int UNIT_MINUTE = 2;

    public static final int UNIT_HOUR = 3;

    public static final int UNIT_DAY = 4;

    private Dates() {
    }

    public static String getPattern(int type) {
        if (type < 0 && type > DATE_PATTERNS.length) {
            type = 0;
        }
        return DATE_PATTERNS[type];
    }

    public static Date toDate(Object value) {
        Date date = null;
        if (value != null) {
            parse: if (value instanceof java.sql.Timestamp) {
                date = new Date(((Timestamp) value).getTime());
            } else if (value instanceof Date) {
                date = (Date) value;
            } else if (value instanceof Number) {
                date = new Date(((Number) value).longValue());
            } else {
                String source = String.valueOf(value).replaceAll("[Tt]", " ").replaceAll("[年月日]", "-").replaceAll("[时分]", ":").replaceAll("秒", "");
                SimpleDateFormat parser = new SimpleDateFormat();
                parser.setLenient(true);
                ParsePosition pos = new ParsePosition(0);
                for (int i = 0; i < PATTERN_MONTH; i++) {
                    parser.applyPattern(DATE_PATTERNS[i]);
                    pos.setIndex(0);
                    date = parser.parse(source, pos);
                    if (date != null && pos.getIndex() == source.length()) {
                        break parse;
                    }
                }
                date = null;
            }
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
        if (date == null) {
            date = new Date();
        }
        if (selector < 0 || selector >= DATE_PATTERNS.length) {
            selector = PATTERN_DEFAULT;
        }
        SimpleDateFormat parser = new SimpleDateFormat(DATE_PATTERNS[selector]);
        return parser.format(date);
    }

    public static Date getDate() {
        return getDate(null, 0);
    }

    public static Date getDate(long delay) {
        return getDate(null, delay);
    }

    public static Date getDate(Date startTime, long delay) {
        if (startTime == null) {
            startTime = new Date();
        }
        if (delay != 0) {
            startTime = new Date(startTime.getTime() + delay);
        }
        return startTime;
    }

    public static long getGap(Date startTime, Date endTime) {
        return getGap(startTime, endTime, UNIT_DAY);
    }

    public static long getGap(Date startTime, Date endTime, int unit) {
        if (unit < 0 || unit > UNIT_DAY) {
            unit = UNIT_DAY;
        }
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
}
