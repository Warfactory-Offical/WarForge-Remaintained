package com.flansmod.warforge.client.effect;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class EffectUpgrade implements IEffect {
    @Override
    public void runEffect(World world, EntityPlayer player, TextureManager man, Random rand, double x, double y, double z, NBTTagCompound data) {
        double radius = data.getDouble("radius");
        int segments = data.getInteger("segments");


        Minecraft.getMinecraft().addScheduledTask(() -> {
            for (int i = 0; i < segments; i++) {
                double angle = 2 * Math.PI * i / segments;
                double offsetX = x + 0.5 + radius * Math.cos(angle);
                double offsetY = y + 0.5 + radius * Math.sin(angle);
                double offsetZ = z + 1.0;
                world.spawnParticle(EnumParticleTypes.SPELL, offsetX, offsetZ, offsetY, 0, 0, 0);
            }

        });
    }

    public static NBTTagCompound toNbtCompound(int segments, double radius) {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("segments", segments);
        compound.setDouble("radius", radius);


        return compound;
    }

    public static void composeEffect(int dim, double x, double y, double z, float radius, int segments, double circleRadius) {
        PacketEffect packet = new PacketEffect();
        packet.x = x;
        packet.y = y;
        packet.z = z;
        packet.type = "upgrade";
        packet.dataNBT = toNbtCompound(segments, circleRadius).toString();

        WarForgeMod.NETWORK.sendToAllAround(packet, x, y, z, radius, dim);

    }

    public static void composeEffect(int dim, BlockPos pos, float radius, int segments, double circleRadius) {
        composeEffect(dim, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius, segments, circleRadius);
    }

    public static void composeEffect( DimBlockPos pos, float radius, int segments, double circleRadius) {
        composeEffect(pos.dim, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius, segments, circleRadius);
    }
}
