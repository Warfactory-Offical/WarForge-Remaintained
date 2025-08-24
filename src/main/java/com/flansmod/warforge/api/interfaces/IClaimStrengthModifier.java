package com.flansmod.warforge.api.interfaces;

import net.minecraft.util.math.Vec3i;

public interface IClaimStrengthModifier {

    public int getClaimContribution();
    public Vec3i[] getEffectArea();
    public boolean isActive();
    public default boolean canStack(){
        return false;
    };
}
