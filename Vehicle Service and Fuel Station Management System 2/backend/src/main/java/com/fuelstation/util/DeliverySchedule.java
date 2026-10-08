package com.fuelstation.util;

import java.time.LocalTime;
import java.util.Locale;
import java.util.regex.Pattern;

/** Supplier delivery windows recur in station time and finish on the same day. */
public final class DeliverySchedule {
    private DeliverySchedule() {}
    private static final Pattern WINDOW=Pattern.compile("^(Daily|Weekdays|Weekends|Every (?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)),\\s*(.*?)\\s*(?:–|—|-|\\bto\\b)\\s*(.*?)$",Pattern.CASE_INSENSITIVE);
    private static String time(String value) {
        String clean=value.trim().toLowerCase(Locale.ROOT);
        if(clean.equals("noon"))return "12:00";
        if(clean.equals("midnight"))return "00:00";
        if(clean.matches("(?:[01]\\d|2[0-3]):[0-5]\\d"))return clean;
        var match=Pattern.compile("^(\\d{1,2})(?::([0-5]\\d))?\\s*(am|pm)$").matcher(clean);
        if(!match.matches())return "";
        int hour=Integer.parseInt(match.group(1));if(hour<1||hour>12)return "";
        return String.format(Locale.ROOT,"%02d:%s",hour%12+(match.group(3).equals("pm")?12:0),match.group(2)==null?"00":match.group(2));
    }
    public static String normalize(String value) {
        if(value==null||value.isBlank())return value;
        var match=WINDOW.matcher(value.trim());if(!match.matches())return value;
        String start=time(match.group(2)),end=time(match.group(3));if(start.isEmpty()||end.isEmpty()||end.compareTo(start)<=0)return value;
        String frequency=match.group(1).toLowerCase(Locale.ROOT);
        frequency=frequency.startsWith("every ")?"Every "+Character.toUpperCase(frequency.charAt(6))+frequency.substring(7):Character.toUpperCase(frequency.charAt(0))+frequency.substring(1);
        return frequency+", "+start+"–"+end;
    }
    public static void validate(String raw) {
        String value=InputValidation.text(raw,"Delivery schedule",255,false,false);if(value==null||value.isEmpty())return;
        var match=WINDOW.matcher(value);if(!match.matches())throw new IllegalArgumentException("Choose a delivery frequency, start time and end time");
        String start=time(match.group(2)),end=time(match.group(3));
        if(start.isEmpty()||end.isEmpty())throw new IllegalArgumentException("Use real delivery times from 00:00 to 23:59");
        if(!LocalTime.parse(end).isAfter(LocalTime.parse(start)))throw new IllegalArgumentException("Delivery end time must be after the start time on the same day");
    }
}
