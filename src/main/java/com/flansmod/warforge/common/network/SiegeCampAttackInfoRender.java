package com.flansmod.warforge.common.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

@AllArgsConstructor
public class SiegeCampAttackInfoRender extends SiegeCampAttackInfo {
    @Getter
    @Setter
    @Nullable
    public TextureAtlasSprite veinSprite = null;

    public SiegeCampAttackInfoRender(SiegeCampAttackInfo info) {
        super(info);
        retriveVeinSprite();
    }

    public void retriveVeinSprite() {
        if (mWarforgeVein == null) return ;
        TextureAtlasSprite sprite = null;

        ResourceLocation resLoc = mWarforgeVein.component_ids[0];
        Item item = ForgeRegistries.ITEMS.getValue(resLoc);

        if (item != null) {
            ItemStack stack = new ItemStack(item);


            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock) item).getBlock();
                IBlockState state = block.getDefaultState();
                IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);

                List<BakedQuad> quads = model.getQuads(state, EnumFacing.UP, 0);
                if (quads.isEmpty()) {
                    quads = model.getQuads(state, null, 0); // fallback
                }

                if (!quads.isEmpty()) {
                    sprite = quads.get(0).getSprite();
                }
            } else {
                IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, null, null);
                List<BakedQuad> quads = model.getQuads(null, null, 0);

                if (!quads.isEmpty()) {
                    sprite = quads.get(0).getSprite();
                } else {
                    sprite = model.getParticleTexture();
                }
            }


        }
        veinSprite = sprite;
    }
}
