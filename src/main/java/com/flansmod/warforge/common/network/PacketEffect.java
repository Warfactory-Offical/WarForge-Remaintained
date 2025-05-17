package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.effect.EffectRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketEffect extends PacketBase {

    public double x, y, z;
    public String type = "";
    public String dataNBT = "";


    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
        writeUTF(data, type);
        writeUTF(data, dataNBT);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.x = data.readDouble();
        this.y = data.readDouble();
        this.z = data.readDouble();
        this.type = readUTF(data);
        this.dataNBT = readUTF(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        WarForgeMod.LOGGER.error("Recieved effect packet on Server side!");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
        NBTTagCompound compound;
        try {
            compound = JsonToNBT.getTagFromJson(dataNBT);
        } catch (NBTException e) {
            WarForgeMod.LOGGER.error("Malformed effect data NBT for " + type);
            return;
        }
        if (EffectRegistry.EFFECT_REGISTRY.containsKey(type)) {
            EffectRegistry.EFFECT_REGISTRY.get(type).runEffect(
                    Minecraft.getMinecraft().world,
                    clientPlayer,
                    Minecraft.getMinecraft().renderEngine,
                    clientPlayer.world.rand,
                    x, y, z,
                    compound
            );

        }


    }

    @Override
    public boolean canUseCompression() {
        return true;
    }
}
