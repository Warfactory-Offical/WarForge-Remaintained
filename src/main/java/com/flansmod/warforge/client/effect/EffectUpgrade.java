package com.flansmod.warforge.client.effect;

import com.flansmod.warforge.client.particle.ParticleStarCircle;
import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketEffect;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class EffectUpgrade implements IEffect {
    public static NBTTagCompound toNbtCompound(int segments, double radius, double speed, int color, int count) {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("segments", segments);
        compound.setDouble("radius", radius);
        compound.setDouble("speed", speed);
        compound.setInteger("color", color);
        compound.setInteger("count", count);


        return compound;
    }

    public static void composeEffect(int dim, double x, double y, double z, float radius, int segments, double circleRadius, double speed, int color, int count) {
        PacketEffect packet = new PacketEffect();
        packet.x = x;
        packet.y = y;
        packet.z = z;
        packet.type = "upgrade";
        packet.dataNBT = toNbtCompound(segments, circleRadius, speed, color, count).toString();

        WarForgeMod.NETWORK.sendToAllAround(packet, x, y, z, radius, dim);

    }

    public static void composeEffect(int dim, BlockPos pos, float radius, int segments, double circleRadius, double speed, int color, int count) {
        composeEffect(dim, pos.getX(), pos.getY(), pos.getZ(), radius, segments, circleRadius, speed, color, count);
    }

    public static void composeEffect(DimBlockPos pos, float radius, int segments, double circleRadius, double speed, int color, int count) {
        composeEffect(pos.dim, pos.getX(), pos.getY(), pos.getZ(), radius, segments, circleRadius, speed, color, count);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void runEffect(World world, EntityPlayer player, TextureManager man, Random rand, double x, double y, double z, NBTTagCompound data) {
        double radius = data.getDouble("radius");
        int segments = data.getInteger("segments");
        double speed = data.getDouble("speed");
        int color = data.getInteger("color");
        int effectCount = data.getInteger("count");
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        float r = red / 255f;
        float g = green / 255f;
        float b = blue / 255f;


        for (int j = 0; j <= effectCount; j++) {
            EffectUpgradeContext context = new EffectUpgradeContext(new BlockPos(x, y, z), j, radius, segments, speed, r, g, b);
            AnimatedEffectHandler.add(new EffectAnimated<>(context, 20, (EffectUpgradeContext ctx, Integer i) -> {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    WorldClient world1 = Minecraft.getMinecraft().world;
                    double angle = (2 * Math.PI * i / ctx.segments) + context.angleOffset;

                    double rotatedAngle = angle + ctx.rotation;

                    double px = ctx.pos.getX() + 0.5 + ctx.radius * Math.cos(rotatedAngle);
                    double pz = ctx.pos.getZ() + 0.5 + ctx.radius * Math.sin(rotatedAngle);
                    double py = ctx.pos.getY() + world1.rand.nextFloat();

                    ParticleManager particleManager = Minecraft.getMinecraft().effectRenderer;
                    particleManager.addEffect(new ParticleStarCircle(world1, px, py, pz, particleManager, r, g, b));

                });
                ctx.tickRotation(context.speed);

            }));
        }


    }

    @RequiredArgsConstructor
    static class EffectUpgradeContext {
        public final BlockPos pos;
        public final double angleOffset;
        public final double radius;
        public final int segments;
        public final double speed;
        public final double r, g, b;
        public double rotation = 0;

        public void tickRotation(double delta) {
            rotation += delta;
            if (rotation > 2 * Math.PI) rotation -= 2 * Math.PI;
        }

    }
}



