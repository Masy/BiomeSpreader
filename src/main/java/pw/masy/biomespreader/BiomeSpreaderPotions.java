package pw.masy.biomespreader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
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
     * Creates a biome resolver with the given parameters.
     * <p>
     *     The biome resolver checks if the given biome can be spread at the biome coordinates
     *     that are passed to the getNoiseBiome() method of the resolver.
     * </p>
     *
     * @param counter A counter that is incremented if the biome was changed.
     * @param chunk The chunk in which the biome should be changed.
     * @param center The center of the spread as block coordinates.
     * @param radius The radius of the spread in blocks.
     * @param biome The registry entry of the biome that should be spread.
     * @param filter A filter that additionally determines if the biome at the given coordinates can be changed.
     * @return The biome resolver.
     */
    private static BiomeResolver createBiomeResolver(MutableInt counter, final ChunkAccess chunk, final BlockPos center, final int radius, final Holder<Biome> biome, Predicate<Holder<Biome>> filter) {
        return (x, y, z, sampler) -> {
            final int blockX = QuartPos.toBlock(x);
            final int blockY = QuartPos.toBlock(y);
            final int blockZ = QuartPos.toBlock(z);
            final int dX = blockX - center.getX();
            final int dZ = blockZ - center.getZ();
            final Holder<Biome> generatedBiome = chunk.getNoiseBiome(x, y, z);
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
     * @param level The level the biome is spread in.
     * @param center The block position from where the biome will be spread.
     * @param radius The radius of the spread in blocks.
     * @param biome The registry entry of the biome that will be spread.
     * @see BiomeSpreaderPotions#createBiomeResolver(MutableInt, ChunkAccess, BlockPos, int, Holder, Predicate)
     */
    public static void spreadBiome(ServerLevel level, final BlockPos center, final int radius, final Holder<Biome> biome) {
        List<ChunkAccess> chunkList = new ArrayList<>();

        for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += CHUNK_SIZE) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x += CHUNK_SIZE) {
                final int chunkX = SectionPos.blockToSectionCoord(x);
                final int chunkZ = SectionPos.blockToSectionCoord(z);
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null)
                    continue;

                chunkList.add(chunk);
            }
        }

        MutableInt counter = new MutableInt(0);
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();

        for (ChunkAccess chunk : chunkList) {
            counter.setValue(0);
            BiomeResolver resolver = createBiomeResolver(counter, chunk, center, radius, biome, (biomeHolder) -> !BiomeSpreader.config.biomeBlacklist.contains(biomeHolder.unwrapKey().map(key -> key.identifier().toString()).orElse("")));

            final int quartMinX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
            final int quartMinZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
            for (int sectionY = level.getMinSectionY(); sectionY <= level.getMaxSectionY(); sectionY++) {
                final LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                section.fillBiomesFromNoise(resolver, sampler, quartMinX, QuartPos.fromSection(sectionY), quartMinZ);
            }

            if (counter.intValue() > 0) {
                chunk.markUnsaved();
            }
        }

        level.getChunkSource().chunkMap.resendBiomesForChunks(chunkList);
    }

    /**
     * Creates a callback that is called when a splash potion collides with a block or entity.
     */
    public static void registerCallback() {
        SplashPotionCallback.EVENT.register((potion -> {
            if (potion.level().isClientSide())
                return InteractionResult.FAIL;

            final PotionContents potionContents = potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (potionContents.customName().isEmpty())
                return InteractionResult.FAIL;

            ServerLevel level = (ServerLevel) potion.level();
            if ((level.dimension() == ServerLevel.OVERWORLD && !BiomeSpreader.config.allowInOverworld)
                || (level.dimension() == ServerLevel.NETHER && !BiomeSpreader.config.allowInNether)
                || (level.dimension() == ServerLevel.END && !BiomeSpreader.config.allowInEnd))
                return InteractionResult.FAIL;

            final Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
            final BlockPos center = potion.blockPosition();

            final String name = potionContents.customName().get();
            final Optional<Holder.Reference<Biome>> biome = biomeRegistry.get(Identifier.withDefaultNamespace(name.substring(0, name.indexOf("_fertilizer"))));
            if (biome.isEmpty())
                return InteractionResult.FAIL;

            spreadBiome(level, center, BiomeSpreader.config.radius, biome.get());
            return InteractionResult.PASS;
        }));
    }
}
