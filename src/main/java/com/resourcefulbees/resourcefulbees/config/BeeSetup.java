package com.resourcefulbees.resourcefulbees.config;

import com.google.gson.Gson;
import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.api.beedata.CustomBeeData;
import com.resourcefulbees.resourcefulbees.registry.BeeRegistry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.resourcefulbees.resourcefulbees.ResourcefulBees.LOGGER;
import static com.resourcefulbees.resourcefulbees.config.Config.GENERATE_DEFAULTS;

public class BeeSetup {

    public static Path BEE_PATH;
    public static Path RESOURCE_PATH;

    public static void setupBees() {
        if (GENERATE_DEFAULTS.get()) setupDefaultBees();
        addBees();
        //TODO Fix when forge updates biome stuff
        //setupBeeSpawns();
    }

    private static void parseBee(File file) throws IOException {
        String name = file.getName();
        name = name.substring(0, name.indexOf('.'));

        Reader r = Files.newBufferedReader(file.toPath());

        parseBee(r, name);
    }

    private static void parseBee(ZipFile zf, ZipEntry zipEntry) throws IOException {
        String name = zipEntry.getName();
        name = name.substring(name.lastIndexOf("/") + 1, name.indexOf('.'));

        InputStream input = zf.getInputStream(zipEntry);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        parseBee(reader, name);
    }

    private static void parseBee(Reader reader, String name) {
        Gson gson = new Gson();
        CustomBeeData bee = gson.fromJson(reader, CustomBeeData.class);
        bee.setName(name);
        bee.shouldResourcefulBeesDoForgeRegistration = true;
        BeeRegistry.getRegistry().registerBee(name.toLowerCase(), bee);
/*        if (BeeValidator.validate(bee)) {
            if (BeeRegistry.registerBee(name.toLowerCase(), bee)) {
                if (bee.SpawnData.canSpawnInWorld())
                    BeeInfoUtils.parseBiomes(bee);
                if (bee.BreedData.isBreedable())
                    BeeInfoUtils.buildFamilyTree(bee);
                if (Config.SHOW_DEBUG_INFO.get())
                    LOGGER.info(name + " bee passed validation check!!");
            } else {
                LOGGER.warn("{} Bee already exists! {} Bee will not be registered.", name, name);
            }
        }*/
    }

    private static void addBees() {
        try {
            Files.walk(BEE_PATH)
                    .filter(f -> f.getFileName().toString().endsWith(".zip"))
                    .forEach(BeeSetup::addZippedBee);
            Files.walk(BEE_PATH)
                    .filter(f -> f.getFileName().toString().endsWith(".json"))
                    .forEach(BeeSetup::addBee);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addBee(Path file) {
        File f = file.toFile();
        try {
            parseBee(f);
        } catch (IOException e) {
            LOGGER.error("File not found when parsing bees");
        }
    }

    private static void addZippedBee(Path file) {
        try {
            ZipFile zf = new ZipFile(file.toString());
            zf.stream().forEach(zipEntry -> {
                if (zipEntry.getName().endsWith(".json")) {
                    try {
                        parseBee(zf, zipEntry);
                    } catch (IOException e) {
                        String name = zipEntry.getName();
                        name = name.substring(name.lastIndexOf("/") + 1, name.indexOf('.'));
                        LOGGER.error("Could not parse {} bee from ZipFile", name);
                    }
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not read ZipFile! ZipFile: " + file.getFileName());
        }
    }

    private static void setupDefaultBees() {

        ModFileInfo mod = ModList.get().getModFileById(ResourcefulBees.MOD_ID);
        Path source = mod.getFile().getFilePath();

        try {
            if (Files.isRegularFile(source)) {
                createFileSystem(source);
            } else if (Files.isDirectory(source)) {
                copyDefaultBees(Paths.get(source.toString(), "/data/resourcefulbees/default_bees"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createFileSystem(Path source) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(source, null)) {
            Path defaultBees = fileSystem.getPath("/data/resourcefulbees/default_bees");
            if (Files.exists(defaultBees)) {
                copyDefaultBees(defaultBees);
            }
        }
    }

    private static void copyDefaultBees(Path source) throws IOException {
        Files.walk(source)
                .filter(f -> f.getFileName().toString().endsWith(".json"))
                .forEach(path -> {
                    File targetFile = new File(String.valueOf(Paths.get(BEE_PATH.toString(),"/", path.getFileName().toString())));
                    try {
                        Files.copy(path, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

























































/*    private static void setupBeeSpawns() {
        for (Map.Entry<Biome, RandomCollection<String>> element : BeeInfo.SPAWNABLE_BIOMES.entrySet()) {
            Biome biome = element.getKey();
            if (Config.GENERATE_BEE_NESTS.get()) {
                addNestFeature(biome);
            }
            //int divisor = Config.GENERATE_BEE_NESTS.get() ? 2 : 1;  tweaked since it seems to make things worse than better.
            biome.getSpawns(EntityClassification.CREATURE).add(new Biome.SpawnListEntry(RegistryHandler.CUSTOM_BEE.get(),
                    Config.SPAWN_WEIGHT.get(),
                    Config.SPAWN_MIN_GROUP.get(),
                    Config.SPAWN_MAX_GROUP.get()));
        }

        EntitySpawnPlacementRegistry.register(RegistryHandler.CUSTOM_BEE.get(),
                EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                CustomBeeEntity::canBeeSpawn);
    }*/

/*    private static void addNestFeature(Biome biome) {
        Biome.Category category = biome.getCategory();
        if (category == Biome.Category.NETHER)
            biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION,
                    RegistryHandler.BEE_NEST_FEATURE.get()
                            .withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG)
                            .withPlacement(Placement.COUNT_CHANCE_HEIGHTMAP_DOUBLE
                                    .configure(new HeightWithChanceConfig(3, .125f))));
        else
            biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION,
                    RegistryHandler.BEE_NEST_FEATURE.get()
                            .withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG)
                            .withPlacement(Placement.CHANCE_TOP_SOLID_HEIGHTMAP
                                    .configure(new ChanceConfig(16))));
    }*/

}