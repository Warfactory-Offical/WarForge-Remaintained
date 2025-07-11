package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimBlockPos;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class SiegeCampProgressInfo {
    public DimBlockPos defendingPos;
    public DimBlockPos attackingPos;
    public int attackingColour;
    public int defendingColour;
    public String attackingName;
    public String defendingName;


    public int completionPoint = 5;
    public int mPreviousProgress = 0;
    public int progress = 0;
    public long maxTime = 0;
    public long timeProgress = 0;

    public int expiredTicks = 0;

    public static SiegeCampProgressInfo getDebugInfo() {
        SiegeCampProgressInfo info = new SiegeCampProgressInfo();
        info.defendingPos = new DimBlockPos(0, 100, 64, 100);
        info.attackingPos = new DimBlockPos(0, 120, 64, 100);
        info.attackingColour = 0xFF0000;
        info.defendingColour = 0x0000FF;
        info.attackingName = "Red Team";
        info.defendingName = "Blue Team";
        info.completionPoint = 10;
        info.progress = 6;
        info.mPreviousProgress = 4;
        info.expiredTicks = 20;
        info.maxTime = 1800;
        info.timeProgress = info.maxTime/2;

        return info;
    }


    public void ClientTick() {
        if (progress <= -5 || progress >= completionPoint) {
            expiredTicks++;
        }
    }

    public boolean Completed() {
        return expiredTicks >= 100;
    }
}
