package com.dungeonderps.resourcefulbees.block;

import com.dungeonderps.resourcefulbees.tileentity.AcaciaBeeNest;
import com.dungeonderps.resourcefulbees.tileentity.PrismarineBeeNest;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class PrismarineBeeNestBlock extends BeeNestBlock{
    public PrismarineBeeNestBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new PrismarineBeeNest();
    }

}