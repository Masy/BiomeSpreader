package pw.masy.biomespreader;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class BiomeSpreaderPotions {

    /**
     * The size of a chunk on the X and Z axes in blocks.
     */
    private static final int CHUNK_SIZE = 16;

    /**
     * Creates a biome supplier with the given parameters.
     * <p>
     *     The biome supplier checks if the given biome can be spread at the biome coordinates
     *     that are passed to the getBiome() method of the supplier.
     * </p>
     *
     * @param counter A counter that is incremented if the biome was changed.
     * @param chunk The chunk in which the biome should be changed.
     * @param center The center of the spread as block coordinates.
     * @param radius The radius of the spread in blocks.
     * @param biome The registry entry of the biome that should be spread.
     * @param filter A filter that additionally determines if the biome at the given coordinates can be changed.
     * @return The biome supplier.
     */
    private static BiomeSupplier createBiomeSupplier(MutableInt counter, final Chunk chunk, final BlockPos center, final int radius, final RegistryEntry<Biome> biome, Predicate<RegistryEntry<Biome>> filter) {
        return (x, y, z, noise) -> {
            final int blockX = BiomeCoords.toBlock(x);
            final int blockY = BiomeCoords.toBlock(y);
            final int blockZ = BiomeCoords.toBlock(z);
            final int dX = blockX - center.getX();
            final int dZ = blockZ - center.getZ();
            final RegistryEntry<Biome> generatedBiome = chunk.getBiomeForNoiseGen(x, y, z);
            if (!filter.test(generatedBiome))
                return generatedBiome;

            if ((dX * dX) + (dZ * dZ) > (radius * radius))
                return generatedBiome;

            boolean spread = true;
            if (blockY > center.getY() && (BiomeSpreader.config.upSpreadMode == BiomeSpreaderConfig.SpreadMode.SPHERE)) {
                spread = blockY - center.getY() < radius;
            } else if (blockY <= center.getY() && (BiomeSpreader.config.downSpreadMode == BiomeSpreaderConfig.SpreadMode.SPHERE)) {
                spread = center.getY() - blockY  < radius;
            }

            if (!spread)
                return generatedBiome;

            counter.increment();
            return biome;
        };
    }

    /**
     * Tries to spread the given biome at the center coordinate.
     *
     * @param world The world the biome is spread in.
     * @param center The block position from where the biome will be spread.
     * @param radius The radius of the spread in blocks.
     * @param biome The registry entry of the biome that will be spread.
     * @see BiomeSpreaderPotions#createBiomeSupplier(MutableInt, Chunk, BlockPos, int, RegistryEntry, Predicate)
     */
    public static void spreadBiome(ServerWorld world, final BlockPos center, final int radius, final RegistryEntry<Biome> biome) {
        List<Chunk> chunkList = new ArrayList<>();

        for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += CHUNK_SIZE) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x += CHUNK_SIZE) {
                final int chunkX = ChunkSectionPos.getSectionCoord(x);
                final int chunkZ = ChunkSectionPos.getSectionCoord(z);
                Chunk chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null)
                    continue;

                chunkList.add(chunk);
            }
        }

        MutableInt counter = new MutableInt(0);

        for (Chunk chunk : chunkList) {
            counter.setValue(0);
            chunk.populateBiomes(createBiomeSupplier(counter, chunk, center, radius, biome, (biomex) -> !BiomeSpreader.config.biomeBlacklist.contains(biomex.getIdAsString())), world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
            if (counter.getValue() > 0) {
                chunk.markNeedsSaving();
            }
        }
        world.getChunkManager().chunkLoadingManager.sendChunkBiomePackets(chunkList);
    }

    /**
     * Creates a callback that is called when a splash potion collides with a block or entity.
     */
    public static void registerCallback() {
        SplashPotionCallback.EVENT.register((potion -> {
            if (potion.getWorld().isClient())
                return ActionResult.FAIL;

            final PotionContentsComponent potionContentsComponent = potion.getStack().getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            if (potionContentsComponent.customName().isEmpty())
                return ActionResult.FAIL;

            ServerWorld world = (ServerWorld) potion.getWorld();
            if ((world.getRegistryKey() == ServerWorld.OVERWORLD && !BiomeSpreader.config.allowInOverworld)
                || (world.getRegistryKey() == ServerWorld.NETHER && !BiomeSpreader.config.allowInNether)
                || (world.getRegistryKey() == ServerWorld.END && !BiomeSpreader.config.allowInEnd))
                return ActionResult.FAIL;

            final Registry<Biome> biomeRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
            final BlockPos center = potion.getBlockPos();

            final String name = potionContentsComponent.customName().get();
            final Optional<RegistryEntry.Reference<Biome>> biome = biomeRegistry.getEntry(Identifier.ofVanilla(name.substring(0, name.indexOf("_fertilizer"))));
            if (biome.isEmpty())
                return ActionResult.FAIL;

            spreadBiome(world, center, BiomeSpreader.config.radius, biome.get());
            return ActionResult.PASS;
        }));
    }
}
