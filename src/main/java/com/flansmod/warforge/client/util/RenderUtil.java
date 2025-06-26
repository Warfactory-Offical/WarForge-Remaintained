package com.flansmod.warforge.client.util;

import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.world.World;

//Trying to move that crap out of tick handler
public class RenderUtil {

    public static void vertexAt(DimChunkPos chunkPos, World world, Tessellator tess, int x, int z, double groundLevelBlend, double playerHeight) {
        double topHeight = playerHeight + 128;

        double maxHeight = world.getHeight(chunkPos.x * 16 + x, chunkPos.z * 16 + z) + 8;
        if (maxHeight > playerHeight + 16) maxHeight = playerHeight + 16;

        double height = topHeight + (maxHeight - topHeight) * groundLevelBlend;

        tess.getBuffer().pos(x, height, z).tex(z / 16f, x / 16f).endVertex();
    }

    public static void drawTexturedModalRect(Tessellator tess, int x, int y, float u, float v, int w, int h) {
        float texScale = 1f / 256f;

        tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);

        tess.getBuffer().pos(x, y + h, -90d).tex(u * texScale, (v + h) * texScale).endVertex();
        tess.getBuffer().pos(x + w, y + h, -90d).tex((u + w) * texScale, (v + h) * texScale).endVertex();
        tess.getBuffer().pos(x + w, y, -90d).tex((u + w) * texScale, (v) * texScale).endVertex();
        tess.getBuffer().pos(x, y, -90d).tex(u * texScale, (v) * texScale).endVertex();

        tess.draw();
    }

    public static void renderZAlignedSquare(Tessellator tess, int x, int y, double z, int ori) {
        tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
        tess.getBuffer().pos(x, y, z).tex(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        tess.getBuffer().pos(x + 1, y, z).tex(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        tess.getBuffer().pos(x + 1, y + 1, z).tex(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        tess.getBuffer().pos(x, y + 1, z).tex(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
        tess.draw();
    }

    public static void renderZAlignedRecangle(Tessellator tess, double x, int y, double z, int ori, double width) {
        tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
        tess.getBuffer().pos(x + 0 - width, y, z).tex(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        tess.getBuffer().pos(x + 1, y, z).tex(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        tess.getBuffer().pos(x + 1, y + 1, z).tex(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        tess.getBuffer().pos(x + 0 - width, y + 1, z).tex(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
        tess.draw();
    }

    public static void renderXAlignedSquare(Tessellator tess, double x, int y, int z, int ori) {
        tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
        tess.getBuffer().pos(x, y, z).tex(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        tess.getBuffer().pos(x, y, z + 1).tex(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        tess.getBuffer().pos(x, y + 1, z + 1).tex(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        tess.getBuffer().pos(x, y + 1, z).tex(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
        tess.draw();
    }

    public static void renderXAlignedRecangle(Tessellator tess, double x, int y, double z, int ori, double width) {
        tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
        tess.getBuffer().pos(x, y, z + 0 - width).tex(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        tess.getBuffer().pos(x, y, z + 1).tex(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        tess.getBuffer().pos(x, y + 1, z + 1).tex(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        tess.getBuffer().pos(x, y + 1, z + 0 - width).tex(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
        tess.draw();
    }

    // Helper for rendering horizontal edges (along Z-axis)
    public static void renderZEdge(World world, Tessellator tess, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderZAlignedSquare(tess, x + 1, y, align, dir); // Entering air
        if (air0 && !air1) RenderUtil.renderZAlignedSquare(tess, x, y, align, 2 + dir); // Exiting air
    }

    // X-aligned horizontal edge
    public static void renderXEdge(World world, Tessellator tess, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderXAlignedSquare(tess, align, y, z + 1, dir);
        if (air0 && !air1) RenderUtil.renderXAlignedSquare(tess, align, y, z, 2 + dir);
    }

    // X-aligned vertical edge
    public static void renderXVerticalEdge(World world, Tessellator tess, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderXAlignedSquare(tess, align, y + 1, z, 3 + dir);
        if (air0 && !air1) RenderUtil.renderXAlignedSquare(tess, align, y, z, 1 + dir);
    }

    // X-aligned vertical corner
    public static void renderXVerticalCorner(World world, Tessellator tess, double x, int y, double z, boolean air0, boolean air1, int dir, double width) {
        if (!air0 && air1) RenderUtil.renderXAlignedRecangle(tess, x, y + 1, z, 3 + dir, width);
        if (air0 && !air1) RenderUtil.renderXAlignedRecangle(tess, x, y, z, 1 + dir, width);
    }

    // Z-aligned vertical corner
    public static void renderZVerticalCorner(World world, Tessellator tess, double x, int y, double z, boolean air0, boolean air1, int dir, double width) {
        if (!air0 && air1) RenderUtil.renderZAlignedRecangle(tess, x, y + 1, z, 3 + dir, width);
        if (air0 && !air1) RenderUtil.renderZAlignedRecangle(tess, x, y, z, 1 + dir, width);
    }

    // Z-aligned vertical edge
    public static void renderZVerticalEdge(World world, Tessellator tess, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderZAlignedSquare(tess, x, y + 1, align, 3 + dir); // Entering air upward
        if (air0 && !air1) RenderUtil.renderZAlignedSquare(tess, x, y, align, 1 + dir);     // Exiting air downward
    }
}