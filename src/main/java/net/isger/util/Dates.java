package net.isger.util;

import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Dates {

    private static final String DATE_PATTERNS[] = { "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "yyyy-MM", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd", "yyyy/MM", "yyyyMMddHHmmss",
            "yyyyMMddHHmm", "yyyyMMdd", "yyyyMM", "yyyy", "HH:mm:ss", "HH:mm" };

    public static final int PATTERN_NORMAL = 0;

    public static final int PATTERN_COMMON = 1;

    public static final int PATTERN_COMPACT = 9;

    private static final int UNITS[] = { 1, 1000, 60000, 3600000, 86400000 };

    public static final int UNIT_MILLIS = 0;

    public static final int UNIT_SECOND = 1;

    public static final int UNIT_MINUTE = 2;

    public static final int UNIT_HOUR = 3;

    public static final int UNIT_DAY = 4;

    private Dates() {
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
                String source = String.valueOf(value).replaceAll("[Tt]", " ");
                SimpleDateFormat parser = new SimpleDateFormat();
                parser.setLenient(true);
                ParsePosition pos = new ParsePosition(0);
                for (String pattern : DATE_PATTERNS) {
                    parser.applyPattern(pattern);
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
        return toString(date, PATTERN_NORMAL);
    }

    public static String toString(Object date, int selector) {
        return toString(toDate(date), selector);
    }

    public static String toString(int selector) {
        return toString(null, selector);
    }

    public static String toString(Date date) {
        return toString(date, PATTERN_NORMAL);
    }

    public static String toString(Date date, int selector) {
        if (date == null) {
            date = new Date();
        }
        if (selector < 0 || selector >= DATE_PATTERNS.length) {
            selector = PATTERN_NORMAL;
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
