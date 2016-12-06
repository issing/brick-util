package net.isger.util;

import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Dates {

    private static final String DATE_PATTERNS[] = { "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd",
            "yyyyMMddHHmmss", "yyyyMMddHHmm", "yyyyMMdd", "HH:mm:ss", "HH:mm" };

    public static final int PATTERN_NORMAL = 0;

    public static final int PATTERN_COMMON = 1;

    public static final int PATTERN_COMPACT = 7;

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
                String source = String.valueOf(value);
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

    public static String toString(Date date) {
        return toString(date, PATTERN_NORMAL);
    }

    public static String toString(Date date, int pattern) {
        if (date == null) {
            return null;
        }
        if (pattern < 0 || pattern >= DATE_PATTERNS.length) {
            pattern = PATTERN_NORMAL;
        }
        SimpleDateFormat parser = new SimpleDateFormat(DATE_PATTERNS[pattern]);
        return parser.format(date);
    }
    
    public static Date getDate() {
        return getDate(null, 0);
    }

    public static Date getDate(int delay) {
        return getDate(null, delay);
    }

    public static Date getDate(Date startTime, int delay) {
        if (startTime == null) {
            startTime = new Date();
        }
        if (delay > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(Calendar.MILLISECOND, delay);
            startTime = calendar.getTime();
        }
        return startTime;
    }

    public static long getGap(Date startTime, Date endTime) {
        return getGap(startTime, endTime, 86400000);
    }

    public static long getGap(Date startTime, Date endTime, long unitMillis) {
        Calendar sc = Calendar.getInstance();
        sc.setTime(startTime);
        sc.set(Calendar.HOUR_OF_DAY, 0);
        sc.set(Calendar.MINUTE, 0);
        sc.set(Calendar.SECOND, 0);
        sc.set(Calendar.MILLISECOND, 0);
        Calendar ec = Calendar.getInstance();
        ec.setTime(endTime);
        ec.set(Calendar.HOUR_OF_DAY, 0);
        ec.set(Calendar.MINUTE, 0);
        ec.set(Calendar.SECOND, 0);
        ec.set(Calendar.MILLISECOND, 0);
        return (ec.getTimeInMillis() - sc.getTimeInMillis()) / unitMillis;
    }
}
