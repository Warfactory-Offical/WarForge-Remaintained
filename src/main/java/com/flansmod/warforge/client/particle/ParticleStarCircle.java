package com.flansmod.warforge.client.particle;

import net.minecraft.client.particle.ParticleFirework;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.world.World;

public class ParticleStarCircle extends ParticleFirework.Spark {

    public ParticleStarCircle(World world, double x, double y, double z, ParticleManager particleManager, float r, float g, float b) {
        super(world, x, y, z, 0, 0, 0, particleManager);
        this.particleRed = r;
        this.particleBlue = b;
        this.particleGreen = g;
    }

}
