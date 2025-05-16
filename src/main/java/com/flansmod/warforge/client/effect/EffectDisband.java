package com.flansmod.warforge.client.effect;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketEffect;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class EffectDisband implements IEffect {
    public static void composeEffect(int dim, double x, double y, double z, float radius) {
        PacketEffect packet = new PacketEffect();
        packet.x = x;
        packet.y = y;
        packet.z = z;
        packet.type = "disband";
        NBTTagCompound compound = new NBTTagCompound();
        packet.dataNBT = compound.toString();

        WarForgeMod.NETWORK.sendToAllAround(packet, x, y, z, radius, dim);

    }

    public static void composeEffect(int dim, BlockPos pos, float radius) {
        composeEffect(dim, pos.getX(), pos.getY(), pos.getZ(), radius);

    }

    @Override
    public void runEffect(World world, EntityPlayer player, TextureManager man, Random rand, double x, double y, double z, NBTTagCompound data) {

        world.playSound(x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 0.9F, false);
        world.playSound(x, y, z, SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 1.0F, 0.9F, false);

        for (int i = 0; i < 32; i++) {
            world.spawnParticle(
                    EnumParticleTypes.EXPLOSION_LARGE,
                    x + world.rand.nextFloat(),
                    y + (i % 2) + world.rand.nextFloat(),
                    z + world.rand.nextFloat(),
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }
}
