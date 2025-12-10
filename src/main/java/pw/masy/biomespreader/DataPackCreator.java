package pw.masy.biomespreader;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DataPackCreator {

    /**
     * Path to the datapack directory inside the mod jar.
     */
    public static final String DATAPACK_PATH_JAR = "assets/biome-spreader/datapacks/BiomeSpreader";

    /**
     * Copies the datapack inside the mod jar into the "datapacks" directory of the server world.
     *
     * @param server The instance of the minecraft server.
     */
    public static void createDataPack(MinecraftServer server) {
        final Path dataPackPath = server.getSavePath(WorldSavePath.DATAPACKS).resolve("BiomeSpreader");

        try {
            if (!Files.exists(dataPackPath))
                Files.createDirectories(dataPackPath);

            final URL resourceURL = DataPackCreator.class.getClassLoader().getResource(DATAPACK_PATH_JAR);
            if (resourceURL == null) {
                throw new IOException("Directory not found in JAR: " + DATAPACK_PATH_JAR);
            }

            if (resourceURL.getProtocol().equals("jar")) {
                // Running from inside a JAR file
                final String jarPath = resourceURL.getPath().substring(5, resourceURL.getPath().indexOf("!"));
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                    final Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        final JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(DATAPACK_PATH_JAR + "/") && !entry.isDirectory()) {
                            // Get relative path inside JAR
                            final String relativePath = entry.getName().substring(DATAPACK_PATH_JAR.length() + 1);
                            final Path outputPath = dataPackPath.resolve(relativePath);

                            // Ensure parent directories exist
                            Files.createDirectories(outputPath.getParent());

                            // Copy file
                            try (InputStream in = jar.getInputStream(entry)) {
                                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            } else {
                // Running from IDE (Extract from classpath)
                final Path sourcePath = Paths.get(resourceURL.toURI());
                Files.walk(sourcePath).forEach(source -> {
                    try {
                        final Path relative = sourcePath.relativize(source);
                        final Path destinationFile = dataPackPath.resolve(relative.toString());
                        if (!Files.isDirectory(source)) {
                            Files.createDirectories(destinationFile.getParent());
                            Files.copy(source, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            server.getCommandManager().parseAndExecute(server.getCommandSource(), "reload");
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        }
    }

}
