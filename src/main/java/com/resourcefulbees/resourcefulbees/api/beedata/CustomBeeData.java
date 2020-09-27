package com.resourcefulbees.resourcefulbees.api.beedata;

import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.config.Config;
import com.resourcefulbees.resourcefulbees.lib.BeeConstants;
import com.resourcefulbees.resourcefulbees.lib.MutationTypes;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.HashMap;
import java.util.function.Supplier;

public class CustomBeeData extends AbstractBeeData {

    private final String flower;
    private final String baseLayerTexture;
    private final int maxTimeInHive;
    private final Float attackDamage;
    private final float sizeModifier;
    private final String[] traits;
    private transient String name;
    private final boolean hasHoneycomb;
    public transient boolean shouldResourcefulBeesDoForgeRegistration;
    private final transient HashMap<String, AbstractBeeData> ADDITIONAL_DATA = new HashMap<>();
    private final BreedData BreedData;
    private final CentrifugeData CentrifugeData;
    private final ColorData ColorData;
    private final MutationData MutationData;
    private final SpawnData SpawnData;
    private TraitData TraitData;
    private transient Supplier<ItemStack> combSupplier;
    private transient Supplier<ItemStack> combBlockItemSupplier;
    private transient RegistryObject<Item> combRegistryObject;
    private transient RegistryObject<Block> combBlockRegistryObject;
    private transient RegistryObject<Item> combBlockItemRegistryObject;
    private transient RegistryObject<Item> spawnEggItemRegistryObject;
    private transient ResourceLocation entityTypeRegistryID;

    private CustomBeeData(String flower, String baseLayerTexture, int maxTimeInHive, Float attackDamage, float sizeModifier, String[] traits, String name, boolean hasHoneycomb, MutationData mutationData, ColorData colorData, CentrifugeData centrifugeData, BreedData breedData, SpawnData spawnData, TraitData traitData) {
        this.flower = flower;
        this.baseLayerTexture = baseLayerTexture;
        this.maxTimeInHive = maxTimeInHive;
        this.attackDamage = attackDamage;
        this.sizeModifier = sizeModifier;
        this.traits = traits;
        this.name = name;
        this.hasHoneycomb = hasHoneycomb;
        this.BreedData = breedData;
        this.CentrifugeData = centrifugeData;
        this.ColorData = colorData;
        this.MutationData = mutationData;
        this.SpawnData = spawnData;
        this.TraitData = traitData;
    }

    public String getFlower() { return flower == null ? BeeConstants.FLOWER_TAG_ALL : flower.toLowerCase(); }

    public boolean hasHoneycomb() { return hasHoneycomb; }

    public String getBaseLayerTexture() { return baseLayerTexture == null ? "/custom/bee" : baseLayerTexture.toLowerCase(); }

    public int getMaxTimeInHive() { return maxTimeInHive < BeeConstants.MIN_HIVE_TIME ? maxTimeInHive == 0 ? BeeConstants.MAX_TIME_IN_HIVE : BeeConstants.MIN_HIVE_TIME : maxTimeInHive; }

    public float getAttackDamage() { return attackDamage == null ? 1.0f : attackDamage; }

    public RegistryObject<Item> getCombRegistryObject() { return combRegistryObject; }

    public void setCombRegistryObject(RegistryObject<Item> combRegistryObject) { this.combRegistryObject = this.combRegistryObject == null ? combRegistryObject : this.combRegistryObject; }

    public RegistryObject<Block> getCombBlockRegistryObject() { return combBlockRegistryObject; }

    public void setCombBlockRegistryObject(RegistryObject<Block> combBlockRegistryObject) { this.combBlockRegistryObject = this.combBlockRegistryObject == null ? combBlockRegistryObject : this.combBlockRegistryObject; }

    public RegistryObject<Item> getCombBlockItemRegistryObject() { return combBlockItemRegistryObject; }

    public void setCombBlockItemRegistryObject(RegistryObject<Item> combBlockItemRegistryObject) { this.combBlockItemRegistryObject = this.combBlockItemRegistryObject == null ? combBlockItemRegistryObject : this.combBlockItemRegistryObject; }

    public ResourceLocation getEntityTypeRegistryID() { return entityTypeRegistryID; }

    public void setEntityTypeRegistryID(ResourceLocation entityTypeRegistryID) { this.entityTypeRegistryID = this.entityTypeRegistryID == null ? entityTypeRegistryID : this.entityTypeRegistryID; }

    public RegistryObject<Item> getSpawnEggItemRegistryObject() { return spawnEggItemRegistryObject; }

    public void setSpawnEggItemRegistryObject(RegistryObject<Item> spawnEggItemRegistryObject) { this.spawnEggItemRegistryObject = this.spawnEggItemRegistryObject == null ? spawnEggItemRegistryObject : this.spawnEggItemRegistryObject; }

    public void setName(String name) { this.name = this.name == null ? name.toLowerCase() : this.name; }

    public float getSizeModifier() { return sizeModifier != 0 ? sizeModifier : Config.BEE_SIZE_MODIFIER.get().floatValue(); }

    public String[] getTraitNames() { return traits; }

    public boolean hasTraitNames() { return traits != null && traits.length > 0;}

    public String getName() { return name.toLowerCase(); }

    public void addData(String key, AbstractBeeData data){
        if (data != null) {
            this.ADDITIONAL_DATA.put(key, data);
        }
    }

    public AbstractBeeData getData(String key) { return this.ADDITIONAL_DATA.get(key); }

    public boolean containsData(String key) { return this.ADDITIONAL_DATA.containsKey(key); }

    public BreedData getBreedData() { return this.BreedData != null ? this.BreedData : new BreedData.Builder(false).createBreedData(); }

    public CentrifugeData getCentrifugeData() { return this.CentrifugeData != null ? this.CentrifugeData : new CentrifugeData.Builder(false, "minecraft:stone").createCentrifugeData(); }

    public ColorData getColorData() { return this.ColorData != null ? this.ColorData : new ColorData.Builder(false).createColorData(); }

    public MutationData getMutationData() { return this.MutationData != null ? this.MutationData : new MutationData.Builder(false, MutationTypes.NONE).createMutationData(); }

    public SpawnData getSpawnData() { return this.SpawnData != null ? this.SpawnData : new SpawnData.Builder(false).createSpawnData(); }

    public TraitData getTraitData() { return this.TraitData != null ? this.TraitData : new TraitData(false); }

    public void setTraitData(TraitData traitData){ this.TraitData = this.TraitData != null ? this.TraitData : traitData; }

    public ItemStack getCombStack() {
        ResourcefulBees.LOGGER.info("getting comb stack " + combSupplier);
        if (combSupplier != null) {
            return combSupplier.get();
        }
        return new ItemStack(getCombRegistryObject().get());
    }

    public ItemStack getCombBlockItemStack() {
        ResourcefulBees.LOGGER.info("getting comb block stack " + combBlockItemSupplier);
        if (combSupplier != null) {
            return combBlockItemSupplier.get();
        }
        return new ItemStack(getCombBlockItemRegistryObject().get());
    }

    public void setCombSupplier(Supplier<ItemStack> combSupplier) {
        ResourcefulBees.LOGGER.info("setting comb supplier " + combSupplier.get());
        this.combSupplier = combSupplier;
    }

    public void setCombBlockItemSupplier(Supplier<ItemStack> combBlockItemSupplier) {
        ResourcefulBees.LOGGER.info("setting comb block supplier " + combBlockItemSupplier.get());
        this.combBlockItemSupplier = combBlockItemSupplier;
    }

    public static class Builder {
        private final String flower;
        private String baseLayerTexture;
        private int maxTimeInHive;
        private Float attackDamage;
        private float sizeModifier;
        private String[] traits;
        private final String name;
        private final boolean hasHoneycomb;
        private final MutationData mutationData;
        private final ColorData colorData;
        private final CentrifugeData centrifugeData;
        private final BreedData breedData;
        private final SpawnData spawnData;
        private final TraitData traitData;

        public Builder(String name, String flower, boolean hasHoneycomb, MutationData mutationData, ColorData colorData, CentrifugeData centrifugeData, BreedData breedData, SpawnData spawnData, TraitData traitData) {
            this.name = name;
            this.flower = flower;
            this.hasHoneycomb = hasHoneycomb;
            this.mutationData = mutationData;
            this.colorData = colorData;
            this.centrifugeData = centrifugeData;
            this.breedData = breedData;
            this.spawnData = spawnData;
            this.traitData = traitData;
        }

        public Builder setBaseLayerTexture(String baseLayerTexture) {
            this.baseLayerTexture = baseLayerTexture;
            return this;
        }

        public Builder setMaxTimeInHive(int maxTimeInHive) {
            this.maxTimeInHive = maxTimeInHive;
            return this;
        }

        public Builder setAttackDamage(float attackDamage) {
            this.attackDamage = attackDamage;
            return this;
        }

        public Builder setSizeModifier(float sizeModifier) {
            this.sizeModifier = sizeModifier;
            return this;
        }

        public Builder setTraits(String[] traits){
            this.traits = traits;
            return this;
        }

        public CustomBeeData createCustomBee() {
            return new CustomBeeData(flower, baseLayerTexture, maxTimeInHive, attackDamage, sizeModifier, traits, name, hasHoneycomb, mutationData, colorData, centrifugeData, breedData, spawnData, traitData);
        }
    }
}
