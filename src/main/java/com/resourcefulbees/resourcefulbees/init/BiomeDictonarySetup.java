package com.resourcefulbees.resourcefulbees.init;

import com.google.gson.Gson;
import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.config.Config;
import com.resourcefulbees.resourcefulbees.registry.BiomeDictionary;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.resourcefulbees.resourcefulbees.ResourcefulBees.LOGGER;

public class BiomeDictonarySetup {

    public static Path DICTIONARY_PATH;

    public static void buildDictionary() {
        if (Config.GENERATE_BIOME_DICTIONARIES.get()) {
            setupDefaultTypes();
        }
        addBiomeTypes();
    }

    private static void parseType(File file) throws IOException {
        String name = file.getName();
        name = name.substring(0, name.indexOf('.'));

        Reader r = Files.newBufferedReader(file.toPath());

        parseType(r, name);
    }

    private static void parseType(ZipFile zf, ZipEntry zipEntry) throws IOException {
        String name = zipEntry.getName();
        name = name.substring(name.lastIndexOf("/") + 1, name.indexOf('.'));

        InputStream input = zf.getInputStream(zipEntry);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        parseType(reader, name);
    }

    private static void parseType(Reader reader, String name) {
        Gson gson = new Gson();
        BiomeDictionary.BiomeType biomeType = gson.fromJson(reader, BiomeDictionary.BiomeType.class);

        for (String biome: biomeType.biomes) {
            BiomeDictionary.TYPES.computeIfAbsent(name.toLowerCase(), k -> new HashSet<>()).add(new ResourceLocation(biome.toLowerCase()));
        }
    }

    private static void addBiomeTypes() {
        try {
            Files.walk(DICTIONARY_PATH)
                    .filter(f -> f.getFileName().toString().endsWith(".zip"))
                    .forEach(BiomeDictonarySetup::addZippedType);
            Files.walk(DICTIONARY_PATH)
                    .filter(f -> f.getFileName().toString().endsWith(".json"))
                    .forEach(BiomeDictonarySetup::addType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addType(Path file) {
        File f = file.toFile();
        try {
            parseType(f);
        } catch (IOException e) {
            LOGGER.error("File not found when parsing biome types");
        }
    }

    private static void addZippedType(Path file) {
        try {
            ZipFile zf = new ZipFile(file.toString());
            zf.stream().forEach(zipEntry -> {
                if (zipEntry.getName().endsWith(".json")) {
                    try {
                        parseType(zf, zipEntry);
                    } catch (IOException e) {
                        String name = zipEntry.getName();
                        name = name.substring(name.lastIndexOf("/") + 1, name.indexOf('.'));
                        LOGGER.error("Could not parse {} biome type from ZipFile", name);
                    }
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not read ZipFile! ZipFile: " + file.getFileName());
        }
    }

    private static void setupDefaultTypes() {

        ModFileInfo mod = ModList.get().getModFileById(ResourcefulBees.MOD_ID);
        Path source = mod.getFile().getFilePath();

        try {
            if (Files.isRegularFile(source)) {
                createFileSystem(source);
            } else if (Files.isDirectory(source)) {
                copyDefaultTypes(Paths.get(source.toString(), "/data/resourcefulbees/biome_dictionary"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createFileSystem(Path source) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(source, null)) {
            Path defaultBees = fileSystem.getPath("/data/resourcefulbees/biome_dictionary");
            if (Files.exists(defaultBees)) {
                copyDefaultTypes(defaultBees);
            }
        }
    }

    private static void copyDefaultTypes(Path source) throws IOException {
        Files.walk(source)
                .filter(f -> f.getFileName().toString().endsWith(".json"))
                .forEach(path -> {
                    File targetFile = new File(String.valueOf(Paths.get(DICTIONARY_PATH.toString(),"/", path.getFileName().toString())));
                    try {
                        Files.copy(path, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
