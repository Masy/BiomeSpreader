package pw.masy.biomespreader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
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
        final Path configFile = Path.of("config/BiomeSpreader.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        if (!Files.exists(configFile)) {
            mapper.writeValue(configFile.toFile(), config);
            return;
        }

        config = mapper.readValue(configFile.toFile(), BiomeSpreaderConfig.class);
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
