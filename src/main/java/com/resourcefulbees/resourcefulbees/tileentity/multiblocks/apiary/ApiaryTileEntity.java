package com.resourcefulbees.resourcefulbees.tileentity.multiblocks.apiary;


import com.resourcefulbees.resourcefulbees.api.ICustomBee;
import com.resourcefulbees.resourcefulbees.block.multiblocks.apiary.ApiaryBlock;
import com.resourcefulbees.resourcefulbees.block.multiblocks.apiary.ApiaryBreederBlock;
import com.resourcefulbees.resourcefulbees.block.multiblocks.apiary.ApiaryStorageBlock;
import com.resourcefulbees.resourcefulbees.config.Config;
import com.resourcefulbees.resourcefulbees.container.AutomationSensitiveItemStackHandler;
import com.resourcefulbees.resourcefulbees.container.UnvalidatedApiaryContainer;
import com.resourcefulbees.resourcefulbees.container.ValidatedApiaryContainer;
import com.resourcefulbees.resourcefulbees.item.BeeJar;
import com.resourcefulbees.resourcefulbees.lib.ApiaryTabs;
import com.resourcefulbees.resourcefulbees.lib.BeeConstants;
import com.resourcefulbees.resourcefulbees.lib.NBTConstants;
import com.resourcefulbees.resourcefulbees.network.NetPacketHandler;
import com.resourcefulbees.resourcefulbees.network.packets.UpdateClientApiaryMessage;
import com.resourcefulbees.resourcefulbees.registry.RegistryHandler;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.resourcefulbees.resourcefulbees.lib.BeeConstants.DEFAULT_ITEM_COLOR;
import static com.resourcefulbees.resourcefulbees.lib.BeeConstants.MIN_HIVE_TIME;

public class ApiaryTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider, IApiaryMultiblock {
    public static final int IMPORT = 0;
    public static final int EXPORT = 2;
    public static final int EMPTY_JAR = 1;
    public final LinkedHashMap<String, ApiaryBee> BEES = new LinkedHashMap<>();
    public final List<BlockPos> STRUCTURE_BLOCKS = new ArrayList<>();
    protected int TIER;
    private boolean isValidApiary;
    public boolean previewed;
    public AutomationSensitiveItemStackHandler h = new ApiaryTileEntity.TileStackHandler(4);
    public LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(() -> h);
    public int horizontalOffset = 0;
    public int verticalOffset = 0;
    public int numPlayersUsing;
    private int ticksSinceValidation = 290;
    private int ticksSinceSync;
    public BlockPos storagePos;
    public BlockPos breederPos;
    private ApiaryStorageTileEntity apiaryStorage;
    private ApiaryBreederTileEntity apiaryBreeder;


    public ApiaryTileEntity() {
        super(RegistryHandler.APIARY_TILE_ENTITY.get());
    }

    //region PLAYER SYNCING
    public static int calculatePlayersUsingSync(World world, ApiaryTileEntity apiaryTileEntity, int ticksSinceSync, int posX, int posY, int posZ, int numPlayersUsing) {
        if (!world.isRemote && numPlayersUsing != 0 && (ticksSinceSync + posX + posY + posZ) % 200 == 0) {
            numPlayersUsing = calculatePlayersUsing(world, apiaryTileEntity, posX, posY, posZ);
        }

        return numPlayersUsing;
    }

    public static int calculatePlayersUsing(World world, ApiaryTileEntity apiaryTileEntity, int posX, int posY, int posZ) {
        int i = 0;
        float f = 5.0F;

        for (PlayerEntity playerentity : world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(posX - f, posY - f, posZ - f, (posX + 1) + f, (posY + 1) + f, (posZ + 1) + f))) {
            if (playerentity.openContainer instanceof ValidatedApiaryContainer) {
                ApiaryTileEntity apiaryTileEntity1 = ((ValidatedApiaryContainer) playerentity.openContainer).apiaryTileEntity;
                if (apiaryTileEntity1 == apiaryTileEntity) {
                    ++i;
                }
            }
        }

        return i;
    }

    public static void syncApiaryToPlayersUsing(World world, BlockPos pos, CompoundNBT data) {
        NetPacketHandler.sendToAllLoaded(new UpdateClientApiaryMessage(pos, data), world, pos);
    }
    //endregion

    public boolean isValidApiary() {
        return isValidApiary;
    }

    public int getTier() {
        return TIER;
    }

    public int getMaxBees() {
        return Config.APIARY_MAX_BEES.get();
    }

    public int getBeeCount() {
        return this.BEES.size();
    }

    public ApiaryStorageTileEntity getApiaryStorage(){
        if (world != null && storagePos != null) {
            TileEntity tile = world.getTileEntity(storagePos);
            if (tile instanceof ApiaryStorageTileEntity) {
                return (ApiaryStorageTileEntity) tile;
            }
        }
        storagePos = null;
        return null;
    }

    public ApiaryBreederTileEntity getApiaryBreeder(){
        if (world != null && breederPos != null) {
            TileEntity tile = world.getTileEntity(breederPos);
            if (tile instanceof ApiaryBreederTileEntity) {
                return (ApiaryBreederTileEntity) tile;
            }
        }
        breederPos = null;
        return null;
    }

    public void setTier(int tier) {
        this.TIER = tier;
    }

    //region BEE HANDLING
    public boolean releaseBee(@Nonnull BlockState state, @Nonnull CompoundNBT nbt, @Nonnull State beehiveState, @Nullable BlockPos flowerPos, boolean exportBee) {
        BlockPos blockpos = this.getPos();
        Direction direction = state.get(BeehiveBlock.FACING);
        BlockPos blockpos1 = blockpos.offset(direction);
        if (world != null && this.world.getBlockState(blockpos1).getCollisionShape(this.world, blockpos1).isEmpty()) {
            nbt.remove("Passengers");
            nbt.remove("Leash");
            nbt.remove("UUID");
            Entity entity = EntityType.func_220335_a(nbt, this.world, entity1 -> entity1);
            if (entity != null) {
                float f = entity.getWidth();
                double d0 = 0.55D + f / 2.0F;
                double d1 = blockpos.getX() + 0.5D + d0 * direction.getXOffset();
                double d2 = blockpos.getY() + 0.5D - (entity.getHeight() / 2.0F);
                double d3 = blockpos.getZ() + 0.5D + d0 * direction.getZOffset();
                entity.setLocationAndAngles(d1, d2, d3, entity.rotationYaw, entity.rotationPitch);

                if (entity instanceof BeeEntity) {
                    BeeEntity vanillaBeeEntity = (BeeEntity) entity;

                    if (flowerPos != null && !vanillaBeeEntity.hasFlower() && this.world.rand.nextFloat() < 0.9F) {
                        vanillaBeeEntity.setFlowerPos(flowerPos);
                    }

                    if (beehiveState == State.HONEY_DELIVERED) {
                        vanillaBeeEntity.onHoneyDelivered();

                        if (!exportBee && isValidApiary()) {
                            getApiaryStorage().deliverHoneycomb((BeeEntity) entity, getTier());
                        }
                    }

                    vanillaBeeEntity.resetPollinationTicks();

                    if (exportBee) {
                        export(vanillaBeeEntity);
                    } else {
                        BlockPos hivePos = this.getPos();
                        this.world.playSound(null, hivePos.getX(), hivePos.getY(), hivePos.getZ(), SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        this.world.addEntity(entity);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean tryEnterHive(Entity bee, boolean hasNectar, boolean imported) {
        if (isValidApiary())
            return this.tryEnterHive(bee, hasNectar, 0, imported);
        return false;
    }

    public boolean tryEnterHive(@Nonnull Entity bee, boolean hasNectar, int ticksInHive, boolean imported) {
        if (this.world != null) {
            if (bee instanceof BeeEntity) {
                BeeEntity beeEntity = (BeeEntity) bee;
                String type = BeeConstants.VANILLA_BEE_TYPE;

                if (bee instanceof ICustomBee) {
                    type = ((ICustomBee) bee).getBeeType();
                }

                if (!this.BEES.containsKey(type) && this.BEES.size() < getMaxBees()) {
                    bee.removePassengers();
                    CompoundNBT nbt = new CompoundNBT();
                    bee.writeUnlessPassenger(nbt);

                    int maxTimeInHive = setMaxTimeInHive(BeeConstants.MAX_TIME_IN_HIVE);
                    if (bee instanceof ICustomBee) {
                        maxTimeInHive = setMaxTimeInHive(((ICustomBee) bee).getBeeData().getMaxTimeInHive());
                    }
                    int finalMaxTimeInHive = maxTimeInHive;

                    String finalType = type;
                    this.BEES.computeIfAbsent(finalType, k -> new ApiaryBee(nbt, ticksInHive, hasNectar ? finalMaxTimeInHive : MIN_HIVE_TIME, beeEntity.getFlowerPos(), finalType));
                    BlockPos pos = this.getPos();
                    this.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);

                    if (imported) {
                        this.BEES.get(type).isLocked = true;
                    }

                    if (this.numPlayersUsing > 0)
                        syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));

                    bee.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private int setMaxTimeInHive(int timeInput) {
        return this.getTier() != 1 ? this.getTier() == 0 ? (int) (timeInput * 1.05) : (int) (timeInput * (1 - getTier() * .05)) : timeInput;
    }

    @Override
    public void tick() {
        if (world != null) {
            BlockPos blockpos = this.getPos();
            int x = blockpos.getX();
            int y = blockpos.getY();
            int z = blockpos.getZ();
            ++ticksSinceSync;
            this.numPlayersUsing = calculatePlayersUsingSync(this.world, this, this.ticksSinceSync, x, y, z, this.numPlayersUsing);

            this.tickBees();

            if (!world.isRemote && isValidApiary()) {
                if (ticksSinceValidation >= 300)
                    runStructureValidation(null);
                else
                    ticksSinceValidation++;
                if (this.BEES.size() > 0 && this.world.getRandom().nextDouble() < 0.005D) {
                    double d0 = blockpos.getX() + 0.5D;
                    double d1 = blockpos.getY();
                    double d2 = blockpos.getZ() + 0.5D;
                    this.world.playSound(null, d0, d1, d2, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
            } else if (!world.isRemote && storagePos != null) {
                storagePos = null;
            }
        }
    }

    private void tickBees() {
        if (this.world != null) {
            Iterator<Map.Entry<String, ApiaryBee>> iterator = this.BEES.entrySet().iterator();
            BlockState blockstate = this.getBlockState();

            while (iterator.hasNext()) {
                Map.Entry<String, ApiaryBee> element = iterator.next();
                ApiaryBee apiaryBee = element.getValue();
                if (!apiaryBee.isLocked && apiaryBee.ticksInHive > apiaryBee.minOccupationTicks && !world.isRemote) {

                    CompoundNBT compoundnbt = apiaryBee.entityData;
                    State state = compoundnbt.getBoolean("HasNectar") ? State.HONEY_DELIVERED : State.BEE_RELEASED;
                    if (this.releaseBee(blockstate, compoundnbt, state, apiaryBee.savedFlowerPos, false)) {
                        iterator.remove();
                        if (this.numPlayersUsing > 0 && !this.world.isRemote)
                            syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));
                    }
                } else {
                    apiaryBee.ticksInHive++;
                }
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isFullOfBees() {
        return this.BEES.size() >= getMaxBees();
    }

    public boolean isAllowedBee() {
        Block hive = getBlockState().getBlock();
        return isValidApiary() && hive instanceof ApiaryBlock;
    }

    public void lockOrUnlockBee(String beeType) {
        this.BEES.get(beeType).isLocked = !this.BEES.get(beeType).isLocked;
        syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));
    }
    //endregion

    //region NBT HANDLING

    @Nonnull
    public ListNBT writeBees() {
        ListNBT listnbt = new ListNBT();

        this.BEES.forEach((key, apiaryBee) -> {
            apiaryBee.entityData.remove("UUID");
            CompoundNBT compoundnbt = new CompoundNBT();
            compoundnbt.put("EntityData", apiaryBee.entityData);
            compoundnbt.putInt("TicksInHive", apiaryBee.ticksInHive);
            compoundnbt.putInt("MinOccupationTicks", apiaryBee.minOccupationTicks);
            compoundnbt.putBoolean(NBTConstants.NBT_LOCKED, apiaryBee.isLocked);
            compoundnbt.putString(NBTConstants.NBT_BEE_TYPE, apiaryBee.beeType);
            if (apiaryBee.savedFlowerPos != null) {
                compoundnbt.put(NBTConstants.NBT_FLOWER_POS, NBTUtil.writeBlockPos(apiaryBee.savedFlowerPos));
            }
            listnbt.add(compoundnbt);
        });

        return listnbt;
    }

    @Override
    public void fromTag(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.fromTag(state, nbt);
        this.loadFromNBT(nbt);
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        return this.saveToNBT(nbt);
    }

    public void loadFromNBT(CompoundNBT nbt) {
        ListNBT listnbt = nbt.getList(NBTConstants.NBT_BEES, 10);

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundNBT data = listnbt.getCompound(i);

            BlockPos savedFlowerPos = data.contains(NBTConstants.NBT_FLOWER_POS) ? NBTUtil.readBlockPos(data.getCompound(NBTConstants.NBT_FLOWER_POS)) : null;

            String beeType = data.getString(NBTConstants.NBT_BEE_TYPE);

            this.BEES.computeIfAbsent(data.getString(NBTConstants.NBT_BEE_TYPE), k -> new ApiaryBee(
                    data.getCompound("EntityData"),
                    data.getInt("TicksInHive"),
                    data.getInt("MinOccupationTicks"),
                    savedFlowerPos,
                    beeType));

            this.BEES.get(beeType).isLocked = data.getBoolean(NBTConstants.NBT_LOCKED);
        }

        if (nbt.contains(NBTConstants.NBT_VALID_APIARY))
            this.isValidApiary = nbt.getBoolean(NBTConstants.NBT_VALID_APIARY);
        if (nbt.contains(NBTConstants.NBT_VERT_OFFSET))
            this.verticalOffset = nbt.getInt(NBTConstants.NBT_VERT_OFFSET);
        if (nbt.contains(NBTConstants.NBT_HOR_OFFSET))
            this.horizontalOffset = nbt.getInt(NBTConstants.NBT_HOR_OFFSET);
        CompoundNBT invTag = nbt.getCompound(NBTConstants.NBT_INVENTORY);
        h.deserializeNBT(invTag);

        if (nbt.contains(NBTConstants.NBT_STORAGE_POS))
            storagePos = NBTUtil.readBlockPos(nbt.getCompound(NBTConstants.NBT_STORAGE_POS));
        if (nbt.contains(NBTConstants.NBT_BREEDER_POS))
            breederPos = NBTUtil.readBlockPos(nbt.getCompound(NBTConstants.NBT_BREEDER_POS));
        if (nbt.contains(NBTConstants.NBT_TIER)) {
            setTier(nbt.getInt(NBTConstants.NBT_TIER));
        }
    }

    public CompoundNBT saveToNBT(CompoundNBT nbt) {
        CompoundNBT inv = this.h.serializeNBT();
        nbt.put(NBTConstants.NBT_INVENTORY, inv);
        nbt.put(NBTConstants.NBT_BEES, this.writeBees());
        nbt.putBoolean(NBTConstants.NBT_VALID_APIARY, isValidApiary());
        nbt.putInt(NBTConstants.NBT_VERT_OFFSET, verticalOffset);
        nbt.putInt(NBTConstants.NBT_HOR_OFFSET, horizontalOffset);
        if (storagePos != null)
            nbt.put(NBTConstants.NBT_STORAGE_POS, NBTUtil.writeBlockPos(storagePos));
        if (breederPos != null)
            nbt.put(NBTConstants.NBT_BREEDER_POS, NBTUtil.writeBlockPos(breederPos));
        nbt.putInt(NBTConstants.NBT_TIER, getTier());
        return nbt;
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(@Nonnull BlockState state, CompoundNBT tag) {
        this.fromTag(state, tag);
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return super.getUpdatePacket();
    }
    //endregion

    //region IMPORT/EXPORT
    public void importBee(ServerPlayerEntity player) {
        World world = this.world;
        boolean imported = false;

        if (world != null) {
            if (!this.h.getStackInSlot(IMPORT).isEmpty() && this.h.getStackInSlot(EMPTY_JAR).getCount() < 16) {
                ItemStack filledJar = this.h.getStackInSlot(IMPORT);
                ItemStack emptyJar = this.h.getStackInSlot(EMPTY_JAR);

                if (filledJar.getItem() instanceof BeeJar) {
                    BeeJar jarItem = (BeeJar) filledJar.getItem();
                    Entity entity = jarItem.getEntityFromStack(filledJar, world, true);

                    if (entity instanceof BeeEntity) {
                        BeeEntity beeEntity = (BeeEntity) entity;
                        imported = tryEnterHive(beeEntity, beeEntity.hasNectar(), true);

                        if (imported) {
                            filledJar.shrink(1);
                            if (emptyJar.isEmpty()) {
                                this.h.setStackInSlot(EMPTY_JAR, new ItemStack(jarItem));
                            } else {
                                emptyJar.grow(1);
                            }
                        }
                    }
                }
            }
        }

        player.sendStatusMessage(new TranslationTextComponent("gui.resourcefulbees.apiary.import." + imported), true);
    }

    public void exportBee(ServerPlayerEntity player, String beeType) {
        boolean exported = false;
        ApiaryBee bee = this.BEES.get(beeType);
        CompoundNBT data = bee.entityData;
        State state = data.getBoolean("HasNectar") ? State.HONEY_DELIVERED : State.BEE_RELEASED;

        if (bee.isLocked && h.getStackInSlot(EXPORT).isEmpty() && !h.getStackInSlot(EMPTY_JAR).isEmpty()) {
            exported = releaseBee(this.getBlockState(), data, state, bee.savedFlowerPos, true);
        }
        if (exported) {
            this.BEES.remove(beeType);
            this.h.getStackInSlot(EMPTY_JAR).shrink(1);
            if (this.numPlayersUsing > 0 && this.world != null && !this.world.isRemote)
                syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));
        }

        player.sendStatusMessage(new TranslationTextComponent("gui.resourcefulbees.apiary.export." + exported), true);
    }

    public void export(BeeEntity beeEntity) {
        ItemStack beeJar = new ItemStack(RegistryHandler.BEE_JAR.get());
        CompoundNBT data = new CompoundNBT();
        data.putString(NBTConstants.NBT_ENTITY, EntityType.getKey(beeEntity.getType()).toString());
        beeEntity.writeWithoutTypeId(data);
        String primaryColor = String.valueOf(DEFAULT_ITEM_COLOR);
        String beeType = BeeConstants.VANILLA_BEE_TYPE;
        if (beeEntity instanceof ICustomBee) {
            if (((ICustomBee)beeEntity).getBeeData().getColorData().hasPrimaryColor()) {
                primaryColor = ((ICustomBee) beeEntity).getBeeData().getColorData().getPrimaryColor();
            }
            beeType = ((ICustomBee) beeEntity).getBeeType();
        }
        data.putString(NBTConstants.NBT_COLOR, primaryColor);
        data.putString(NBTConstants.NBT_BEE_TYPE, beeType);
        beeJar.setTag(data);
        this.h.setStackInSlot(EXPORT, beeJar);
    }
    //endregion

    //region STRUCTURE VALIDATION
    public void runStructureValidation(@Nullable ServerPlayerEntity validatingPlayer) {
        if (this.world != null && !this.world.isRemote()) {
            if (!isValidApiary() || STRUCTURE_BLOCKS.isEmpty())
                buildStructureBlockList();
            this.isValidApiary = validateStructure(this.world, validatingPlayer);
            this.world.setBlockState(this.getPos(), getBlockState().with(ApiaryBlock.VALIDATED, isValidApiary()));
            if (validatingPlayer != null && isValidApiary()) {
                NetworkHooks.openGui(validatingPlayer, this, this.getPos());
            }
            this.ticksSinceValidation = 0;
        }
    }

    public boolean validateStructure(World worldIn, @Nullable ServerPlayerEntity validatingPlayer) {
        boolean isStructureValid = true;
        this.apiaryStorage = getApiaryStorage();
        this.apiaryBreeder = getApiaryBreeder();
        validateLinks();
        for (BlockPos pos : STRUCTURE_BLOCKS) {
            Block block = worldIn.getBlockState(pos).getBlock();
            if (block.isIn(BeeInfoUtils.getValidApiaryTag()) || block instanceof ApiaryBreederBlock || block instanceof ApiaryStorageBlock || block instanceof ApiaryBlock) {
                TileEntity tile = worldIn.getTileEntity(pos);
                linkStorageAndBreeder(tile);
            } else {
                isStructureValid = false;
                if (validatingPlayer != null) {
                    validatingPlayer.sendStatusMessage(new StringTextComponent(String.format("Block at position (X: %1$s Y: %2$s Z: %3$s) is invalid!", pos.getX(), pos.getY(), pos.getZ())), false);
                }
            }
        }

        if (apiaryStorage == null){
            isStructureValid = false;
            if (validatingPlayer != null) {
                validatingPlayer.sendStatusMessage(new StringTextComponent("Missing Apiary Storage Block!"), false);
            }
        }

        if (validatingPlayer != null) {
            validatingPlayer.sendStatusMessage(new TranslationTextComponent("gui.resourcefulbees.apiary.validated." + isStructureValid), true);
        }

        return isStructureValid;
    }

    public MutableBoundingBox buildStructureBounds(int horizontalOffset, int verticalOffset) {
        MutableBoundingBox box;
        int posX = this.getPos().getX();
        int posY = this.getPos().getY();
        int posZ = this.getPos().getZ();

        switch (this.getBlockState().get(ApiaryBlock.FACING)) {
            case NORTH:
                posX -= 3 + horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX + 6, posY + 5, posZ - 6);
                break;
            case EAST:
                posZ -= 3 + horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX + 6, posY + 5, posZ + 6);
                break;
            case SOUTH:
                posX -= 3 - horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX + 6, posY + 5, posZ + 6);
                break;
            default:
                posZ -= 3 - horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX - 6, posY + 5, posZ + 6);
        }
        return box;
    }

    private void buildStructureBlockList() {
        if (this.world != null) {
            MutableBoundingBox box = buildStructureBounds(this.horizontalOffset, this.verticalOffset);
            STRUCTURE_BLOCKS.clear();
            BlockPos.stream(box).forEach((blockPos -> {
                if (blockPos.getX() == box.minX || blockPos.getX() == box.maxX ||
                        blockPos.getY() == box.minY || blockPos.getY() == box.maxY ||
                        blockPos.getZ() == box.minZ || blockPos.getZ() == box.maxZ) {
                    BlockPos savedPos = new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    STRUCTURE_BLOCKS.add(savedPos);
                }
            }));
        }
    }

    public void runCreativeBuild(ServerPlayerEntity player) {
        if (this.world != null) {
            buildStructureBlockList();
            boolean addedStorage = false;
            for (BlockPos pos : STRUCTURE_BLOCKS) {
                Block block = this.world.getBlockState(pos).getBlock();
                if (!(block instanceof ApiaryBlock)) {
                    if (addedStorage) {
                        this.world.setBlockState(pos, Blocks.GLASS.getDefaultState());
                    }
                    else {
                        this.world.setBlockState(pos, RegistryHandler.APIARY_STORAGE_BLOCK.get().getDefaultState());
                        addedStorage = true;
                    }
                }
            }
            runStructureValidation(player);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean linkStorageAndBreeder(TileEntity tile){
        if (tile instanceof ApiaryStorageTileEntity) {
            if (apiaryStorage == null && ((ApiaryStorageTileEntity) tile).getApiaryPos() == null) {
                apiaryStorage = (ApiaryStorageTileEntity) tile;
                storagePos = apiaryStorage.getPos();
                apiaryStorage.setApiaryPos(this.pos);
                if (world != null) {
                    world.notifyBlockUpdate(storagePos, apiaryStorage.getBlockState(), apiaryStorage.getBlockState(), 2);
                }
                return true;
            }
        }

        if (tile instanceof ApiaryBreederTileEntity) {
            if (apiaryBreeder == null && ((ApiaryBreederTileEntity) tile).getApiaryPos() == null) {
                apiaryBreeder = (ApiaryBreederTileEntity) tile;
                breederPos = apiaryBreeder.getPos();
                apiaryBreeder.setApiaryPos(this.pos);
                if (world != null) {
                    world.notifyBlockUpdate(breederPos, apiaryBreeder.getBlockState(), apiaryBreeder.getBlockState(), 2);
                }
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean validateLinks() {
        boolean linksValid = true;
        if (apiaryStorage != null && apiaryStorage.getApiaryPos() == null) {
            apiaryStorage = null;
            storagePos = null;
            linksValid = false;
        }
        if (apiaryBreeder != null && apiaryBreeder.getApiaryPos() == null) {
            apiaryBreeder = null;
            breederPos = null;
            linksValid = false;
        }
        if (!linksValid) {
            markDirty();
        }
        return linksValid;
    }
    //endregion

    //region SCREEN HANDLING
    @Nullable
    @Override
    public Container createMenu(int i, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity playerEntity) {
        if (world != null) {
            numPlayersUsing++;
            if (isValidApiary()) {
                this.isValidApiary = validateStructure(world, (ServerPlayerEntity) playerEntity);
                if (isValidApiary()) {
                    return new ValidatedApiaryContainer(i, world, pos, playerInventory);
                }
            }
            return new UnvalidatedApiaryContainer(i, world, pos, playerInventory);
        }
        return null;
    }

    @Override
    public void switchTab(ServerPlayerEntity player, ApiaryTabs tab) {
        if (world != null) {
            if (tab == ApiaryTabs.STORAGE) {
                NetworkHooks.openGui(player, getApiaryStorage(), storagePos);
            }
            if (tab == ApiaryTabs.BREED) {
                NetworkHooks.openGui(player, getApiaryBreeder(), breederPos);
            }
        }
    }

    //endregion

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? lazyOptional.cast() :
                super.getCapability(cap, side);
    }

    public AutomationSensitiveItemStackHandler.IAcceptor getAcceptor() {
        return (slot, stack, automation) -> !automation || slot == 0 || slot == 1;
    }

    public AutomationSensitiveItemStackHandler.IRemover getRemover() {
        return (slot, automation) -> !automation || slot == 2 || slot == 3 || slot == 4;
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("gui.resourcefulbees.apiary");
    }

    public enum State {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY
    }

    public static class ApiaryBee {
        public final CompoundNBT entityData;
        public final int minOccupationTicks;
        public final BlockPos savedFlowerPos;
        public final String beeType;
        public int ticksInHive;
        public boolean isLocked = false;

        public ApiaryBee(CompoundNBT nbt, int ticksinhive, int minoccupationticks, @Nullable BlockPos flowerPos, String beeType) {
            nbt.remove("UUID");
            this.entityData = nbt;
            this.ticksInHive = ticksinhive;
            this.minOccupationTicks = minoccupationticks;
            this.savedFlowerPos = flowerPos;
            this.beeType = beeType;
        }
    }

    protected class TileStackHandler extends AutomationSensitiveItemStackHandler {

        protected TileStackHandler(int slots) {
            super(slots);
        }

        @Override
        public AutomationSensitiveItemStackHandler.IAcceptor getAcceptor() {
            return ApiaryTileEntity.this.getAcceptor();
        }

        @Override
        public AutomationSensitiveItemStackHandler.IRemover getRemover() {
            return ApiaryTileEntity.this.getRemover();
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markDirty();
        }
    }
}
