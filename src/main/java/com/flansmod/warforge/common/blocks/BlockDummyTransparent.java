package com.flansmod.warforge.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;

public class BlockDummyTransparent extends BlockDummy {

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }
}
