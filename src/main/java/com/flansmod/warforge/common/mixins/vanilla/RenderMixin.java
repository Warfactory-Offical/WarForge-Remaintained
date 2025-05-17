package com.flansmod.warforge.common.mixins.vanilla;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static com.flansmod.warforge.client.util.LegacyColorUtil.getClosestLegacyColor;


@Mixin(Render.class)
public abstract class RenderMixin<T extends Entity>{

    @Shadow
    protected RenderManager renderManager;

    @Shadow
    protected abstract FontRenderer getFontRendererFromRenderManager();

    /**
     * @author
     * Fyrsti
     * @reason
     * adds ability to see player's faction in the nameplate
     */
    @Overwrite
    protected void renderLivingLabel(T entityIn, String str, double x, double y, double z, int maxDistance)
    {
        double d0 = entityIn.getDistanceSq(this.renderManager.renderViewEntity);

        if (d0 <= (double)(maxDistance * maxDistance))
        {
            boolean flag = entityIn.isSneaking();
            float f = this.renderManager.playerViewY;
            float f1 = this.renderManager.playerViewX;
            boolean flag1 = this.renderManager.options.thirdPersonView == 2;
            float f2 = entityIn.height + 0.5F - (flag ? 0.25F : 0.0F);
            EntityRenderer.drawNameplate(this.getFontRendererFromRenderManager(), str, (float)x, (float)y + f2, (float)z, 0, f, f1, flag1, flag);
            String faction = WarForgeMod.NAMETAG_CACHE.requestIfAbsent(str.replaceAll("§.", "")); //SOOO minecraft puts this symbol in player nicknames...? the fuck?
            if (faction != null){
                EntityRenderer.drawNameplate(this.getFontRendererFromRenderManager(), faction, (float)x, (float)y + f2 + 0.25f, (float)z, -10, f, f1, flag1, flag);
            }
        }
    }
}