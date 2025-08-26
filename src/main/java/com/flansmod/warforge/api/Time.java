package com.flansmod.warforge.api;

import lombok.Getter;


/**
 * Object used to track and format time;
 *
 * @author MrNorwood
 */
public class Time {
    @Getter
    private long ms;               // duration in ms
    private int hours;
    private int minutes;
    private int seconds;
    private float secondsFloat;

    private boolean dirty = true;

    @Getter
    private long startTimestamp;   // epoch ms when tracking started
    @Getter
    private long endTimestamp;     // epoch ms when tracking ended (0 if ongoing)

    public Time(long ms) {
        setMs(ms);
        this.startTimestamp = System.currentTimeMillis();
    }

    public Time(long startTimestamp, long endTimestamp) {
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        setMs(endTimestamp - startTimestamp);
    }

    public void setMs(long ms) {
        this.ms = ms;
        this.dirty = true;
    }


    public void change(long ms) {
        this.ms += ms;
        this.dirty = true;
    }

    public void markStart() {
        this.startTimestamp = System.currentTimeMillis();
        this.endTimestamp = 0;
    }

    public void markEnd() {
        this.endTimestamp = System.currentTimeMillis();
    }

    private void updateIfDirty() {
        if (!dirty) return;

        this.seconds = (int) (ms / 1000) % 60;
        this.minutes = (int) ((ms / (1000 * 60)) % 60);
        this.hours = (int) (ms / (1000 * 60 * 60));
        this.secondsFloat = ms / 1000f;

        this.dirty = false;
    }

    public int getSeconds() {
        updateIfDirty();
        return seconds;
    }

    public int getMinutes() {
        updateIfDirty();
        return minutes;
    }

    public int getHours() {
        updateIfDirty();
        return hours;
    }

    public float getSecondsFloat() {
        updateIfDirty();
        return secondsFloat;
    }

    public int[] getFullTime() {
        updateIfDirty();
        return new int[]{hours, minutes, seconds};
    }

    public long getElapsedBetweenTimestamps() {
        if (endTimestamp == 0) {
            return System.currentTimeMillis() - startTimestamp;
        }
        return endTimestamp - startTimestamp;
    }

    public String getFormattedTime(TimeFormat format, Verbality verbal) {
        updateIfDirty();
        StringBuilder sb = new StringBuilder();

        switch (format) {
            case HOURS_MINUTES -> {
                sb.append(hours).append(getFormattingSeparator(verbal, "h"))
                        .append(minutes).append(getFormattingSeparator(verbal, "m"));
            }
            case HOURS_MINUTES_SECONDS -> {
                sb.append(hours).append(getFormattingSeparator(verbal, "h"))
                        .append(minutes).append(getFormattingSeparator(verbal, "m"))
                        .append(seconds).append(getFormattingSeparator(verbal, "s"));
            }
            case MINUTES_SECONDS -> {

                sb.append(minutes + hours * 60).append(getFormattingSeparator(verbal, "m"))
                        .append(seconds).append(getFormattingSeparator(verbal, "s"));
            }
            case SECONDS -> {
                sb.append(seconds + hours * 60 + minutes * 60).append(getFormattingSeparator(verbal, "s"));
            }
            case END_TIME -> {
                // Uses timestamp if set
                if (endTimestamp != 0) {
                    sb.append(new java.util.Date(endTimestamp));
                } else {
                    sb.append("N/A");
                }
            }
        }
        return sb.toString().trim();
    }

    private String getFormattingSeparator(Verbality verbality, String unit) {
        return switch (verbality) {
            case FULL -> switch (unit) {
                case "h" -> " hours ";
                case "m" -> " minutes ";
                case "s" -> " seconds ";
                default -> " ";
            };
            case SHORT -> switch (unit) {
                case "h" -> "h ";
                case "m" -> "m ";
                case "s" -> "s ";
                default -> " ";
            };
            case NONE -> switch (unit) {
                case "h", "m" -> ":";
                case "s" -> "";
                default -> "";
            };
        };
    }

    public enum TimeFormat {
        HOURS_MINUTES,
        HOURS_MINUTES_SECONDS,
        MINUTES_SECONDS,
        SECONDS,
        END_TIME,
    }

    public enum Verbality {
        FULL, SHORT, NONE
    }
}

