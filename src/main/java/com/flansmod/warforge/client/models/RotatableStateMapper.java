package com.flansmod.warforge.client.models;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class RotatableStateMapper extends StateMapperBase {
    private final String registryName;
    private PropertyDirection propertyDirection;

    public RotatableStateMapper(ResourceLocation blockName) {
        this.registryName = blockName.toString();
        this.propertyDirection = BlockHorizontal.FACING;
    }

    public RotatableStateMapper(ResourceLocation blockName, PropertyDirection propertyDirection) {
        this.registryName = blockName.toString();
        this.propertyDirection = propertyDirection;
    }

    @Override
    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
        EnumFacing facing = state.getValue(propertyDirection);
        return new ModelResourceLocation(registryName, "facing=" + facing.getName());
    }
}
