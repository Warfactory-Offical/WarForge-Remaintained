package com.flansmod.warforge.common.util;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;

public class TimeHelper {
    public TimeHelper() {
    }

    public static long getSiegeDayLengthMS() {
        return (long) (
                WarForgeConfig.SIEGE_DAY_LENGTH // In hours
                        * 60f // In minutes
                        * 60f // In seconds
                        * 1000f); // In milliseconds
    }

    public static long getYieldDayLengthMs() {
        return (long) (
                WarForgeConfig.YIELD_DAY_LENGTH // In hours
                        * 60f // In minutes
                        * 60f // In seconds
                        * 1000f); // In milliseconds
    }

    public static long getCooldownIntoTicks(float cooldown) {
        // From Minutes
        return (long) (
                cooldown
                        * 60L // Minutes -> Seconds
                        * 20L // Seconds -> Ticks
        );
    }

    public static String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        int hours = ((int) minutes) / 60;
        int days = hours / 24;

        // ensure entire time left is not represented in each format, but rather only leftover amounts for that unit

        seconds -= minutes * 60;
        minutes -= hours * 60;
        hours -= days * 24;

        StringBuilder timeBuilder = new StringBuilder();
        if (days > 0) timeBuilder.append(days).append("d ");
        if (hours > 0) timeBuilder.append(hours).append("h ");
        if (minutes > 0) timeBuilder.append(minutes).append("min ");
        if (seconds > 0) timeBuilder.append(seconds).append("s ");
        timeBuilder.append(ms % 1000).append("ms");

        return timeBuilder.toString();
    }

    public long getCooldownRemainingSeconds(float cooldown, long startOfCooldown) {
        long ticks = (long) cooldown; // why did you convert it into ticks if its already in ticks
        long elapsed = startOfCooldown - ticks;

        return (WarForgeMod.serverTick - elapsed) * 20;
    }

    public int getCooldownRemainingMinutes(float cooldown, long startOfCooldown) {
        long ticks = (long) cooldown;
        long elapsed = startOfCooldown - ticks;

        return (int) (
                (WarForgeMod.serverTick - elapsed) * 20 / 60
        );
    }

    public int getCooldownRemainingHours(float cooldown, long startOfCooldown) {
        long ticks = (long) cooldown;
        long elapsed = startOfCooldown - ticks;

        return (int) (
                (WarForgeMod.serverTick - elapsed) * 20 / 60 / 60
        );
    }

    public long getTimeToNextSiegeAdvanceMs() {
        long elapsedMS = System.currentTimeMillis() - WarForgeMod.timestampOfFirstDay;
        long todayElapsedMS = elapsedMS % getSiegeDayLengthMS();

        return getSiegeDayLengthMS() - todayElapsedMS;
    }

    public long getTimeToNextYieldMs() {
        long elapsedMS = System.currentTimeMillis() - WarForgeMod.timestampOfFirstDay;
        long todayElapsedMS = elapsedMS % getYieldDayLengthMs();

        return getYieldDayLengthMs() - todayElapsedMS;
    }
}