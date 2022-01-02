package fr.gartox.lulu.utils;

public class Utils {

    public static String formatTime(long millis) {
        long t = millis / 1000L;
        int sec = (int)(t % 60L);
        int min = (int)((t % 3600L) / 60);
        int hrs = (int)(t /3600);

        String timestamp;

        if (hrs != 0) {
            timestamp = forceTwoDigit(hrs) + ":" + forceTwoDigit(min) + ":" + forceTwoDigit(sec);
        } else {
            timestamp = forceTwoDigit(min) + ":" + forceTwoDigit(sec);
        }

        return timestamp;
    }

    private static String forceTwoDigit(int i) {
        return i < 10 ? "0" + i : Integer.toString(i);
    }

}
