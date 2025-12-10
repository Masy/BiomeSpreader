package pw.masy.biomespreader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class BiomeSpreader implements ModInitializer {

    /**
     * The configuration of the mod.
     */
    public static BiomeSpreaderConfig config = new BiomeSpreaderConfig();

    /**
     * Loads the config file.
     *
     * @throws IOException when the config file could not be opened or saved.
     */
    public static void loadConfig() throws IOException {
        final File configFile = Path.of("config", "BiomeSpreader.json").toFile();
        if (!configFile.getParentFile().mkdirs())
            throw new RuntimeException("Failed to create config dir");

        Gson gson = new GsonBuilder().create();

        try {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    config = gson.fromJson(reader, BiomeSpreaderConfig.class);
                    return;
                }
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load BiomeSpreader config");
        }
    }

    /**
     * Method which is called when the mod is initialized.
     *
     * @see BiomeSpreader#loadConfig()
     * @see BiomeSpreaderPotions#registerCallback()
     * @see DataPackCreator#createDataPack(MinecraftServer)
     */
    @Override
    public void onInitialize() {
        try {
            loadConfig();
        } catch (IOException ex) {
            System.err.println("failed to load config of biome spreader");
            ex.printStackTrace();
        }
        BiomeSpreaderPotions.registerCallback();
        if (config.recreateDataPack) {
            ServerLifecycleEvents.SERVER_STARTED.register(DataPackCreator::createDataPack);
        }
    }
}
