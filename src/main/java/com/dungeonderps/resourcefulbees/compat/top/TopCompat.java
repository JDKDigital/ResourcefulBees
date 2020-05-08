package com.dungeonderps.resourcefulbees.compat.top;

import com.dungeonderps.resourcefulbees.RegistryHandler;
import com.dungeonderps.resourcefulbees.tileentity.HoneycombBlockEntity;
import mcjty.theoneprobe.api.*;
import net.minecraft.entity.passive.CustomBeeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.function.Function;

import static com.dungeonderps.resourcefulbees.ResourcefulBees.LOGGER;

public class TopCompat implements Function<ITheOneProbe, Void>
{
    private final String formatting = TextFormatting.BLUE.toString() + TextFormatting.ITALIC.toString();

    @Nullable
    @Override
    public Void apply(ITheOneProbe theOneProbe)
    {
        theOneProbe.registerBlockDisplayOverride((mode, probeInfo, player, world, blockState, data) -> {
            if(world.getTileEntity(data.getPos()) instanceof HoneycombBlockEntity)
            {
                TileEntity honeyBlock = world.getTileEntity(data.getPos());
                final ItemStack honeyCombBlock = new ItemStack(RegistryHandler.HONEYCOMBBLOCKITEM.get());
                final CompoundNBT honeyCombBlockItemTag = honeyCombBlock.getOrCreateChildTag("ResourcefulBees");
                honeyCombBlockItemTag.putString("Color", honeyBlock.serializeNBT().getString("Color"));
                honeyCombBlockItemTag.putString("BeeType", honeyBlock.serializeNBT().getString("BeeType"));

                probeInfo.horizontal()
                        .item(honeyCombBlock)
                        .vertical()
                        .itemLabel(honeyCombBlock)
                        .text(formatting + "Resourceful Bees");
                return true;
            }
            if (blockState.getBlock().getRegistryName().getNamespace().equals("resourcefulbees")){
                probeInfo.horizontal()
                        .item(blockState.getBlock().asItem().getDefaultInstance())
                        .vertical()
                        .itemLabel(blockState.getBlock().asItem().getDefaultInstance())
                        .text(formatting + "Resourceful Bees");
                return true;
            }
            return false;
        });

        /*
        theOneProbe.registerEntityDisplayOverride((mode, probeInfo, player, world, entity, data) -> {
            if (entity instanceof CustomBeeEntity){
                CustomBeeEntity bee = new CustomBeeEntity(RegistryHandler.CUSTOM_BEE.get(), world);
                bee.selectBeeType(entity.serializeNBT().getString("BeeType"));
                probeInfo.horizontal()
                        .entity(bee)
                        .vertical()
                        .text(formatting + "Resourceful Bees");
                LOGGER.info(entity.serializeNBT());
                return true;
            }
            return false;
        });
         */
        return null;
    }
}