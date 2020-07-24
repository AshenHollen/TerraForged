package com.terraforged.mod.chunk.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.terraforged.mod.Log;
import com.terraforged.mod.util.nbt.NBTHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class SettingsHelper {

    public static final File SETTINGS_DIR = new File("config", "terraforged");
    public static final File DEFAULTS_FILE = new File(SETTINGS_DIR, "generator-defaults.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void clearDefaults() {
        if (DEFAULTS_FILE.exists() && DEFAULTS_FILE.delete()) {
            Log.info("Deleted generator defaults");
        }
    }

    public static TerraSettings loadSettings(File file) {
        TerraSettings settings = new TerraSettings();
        try (Reader reader = new BufferedReader(new FileReader(file))) {
            JsonElement data = new JsonParser().parse(reader);
            CompoundNBT nbt = NBTHelper.fromJson(data);
            if (NBTHelper.deserialize(nbt, settings)) {
                return settings;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Writer writer = new BufferedWriter(new FileWriter(file))) {
            CompoundNBT tag = NBTHelper.serializeCompact(settings);
            JsonElement json = NBTHelper.toJson(tag);
            GSON.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return settings;
    }

    public static void exportDefaults(TerraSettings settings) {
        CompoundNBT tag = NBTHelper.serializeCompact(settings);
        JsonElement json = NBTHelper.toJson(tag);
        try (Writer writer = new BufferedWriter(new FileWriter(DEFAULTS_FILE))) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CompoundNBT applyDefaults(CompoundNBT options, TerraSettings dest) {
        if (options.isEmpty()) {
            TerraSettings defaults = readDefaults();
            options = NBTHelper.serialize(defaults);
        }
        NBTHelper.deserialize(options, dest);
        return options;
    }

    public static TerraSettings readDefaults() {
        if (DEFAULTS_FILE.exists()) {
            return loadSettings(DEFAULTS_FILE);
        }
        return new TerraSettings();
    }

    public static TerraSettings getSettings(WorldInfo info) {
        if (info.getGeneratorOptions().isEmpty()) {
            return readDefaults();
        } else {
            Log.info("Loading generator settings from level.dat");
            TerraSettings settings = new TerraSettings();
            NBTHelper.deserialize(info.getGeneratorOptions(), settings);
            return settings;
        }
    }

    public static void init() {
        if (!DEFAULTS_FILE.exists()) {
            exportDefaults(new TerraSettings());
        }
    }
}
