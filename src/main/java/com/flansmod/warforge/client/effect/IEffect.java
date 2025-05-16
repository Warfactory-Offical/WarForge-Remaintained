package com.flansmod.warforge.client.effect;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public interface IEffect {

    @SideOnly(Side.CLIENT)
    public void runEffect(World world, EntityPlayer player, TextureManager man, Random rand, double x, double y, double z, NBTTagCompound data);




}
