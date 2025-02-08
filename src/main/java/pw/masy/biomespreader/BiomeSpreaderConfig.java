package pw.masy.biomespreader;

import java.util.List;

/**
 * Config class to configure the mod.
 */
public class BiomeSpreaderConfig {

    /**
     * Enum describing how the biome is spread on the vertical axis.
     */
    public enum SpreadMode {
        /**
         * The biome will be spread as a half sphere
         */
        SPHERE,
        /**
         * The biome will be spread as a cylinder.
         */
        CYLINDER
    }

    /**
     * The radius of the spread biome.
     * <p>
     * The actual radius will most likely vary, because biomes are only stored for 4x4x4 cubes.
     * </p>
     */
    public int radius = 16;
    /**
     * The way the biomes are spread upwards.
     */
    public SpreadMode upSpreadMode = SpreadMode.CYLINDER;
    /**
     * The way the biomes are spread downwards.
     */
    public SpreadMode downSpreadMode = SpreadMode.CYLINDER;
    /**
     * Whether the data pack should be recreated when the server starts or not.
     */
    public boolean recreateDataPack = true;
    /**
     * Whether the fertilizers can be used in the overworld dimension or not.
     */
    public boolean allowInOverworld = true;
    /**
     * Whether the fertilizers can be used in the nether dimension or not.
     */
    public boolean allowInNether = false;
    /**
     * Whether the fertilizers can be used in the end dimension or not.
     */
    public boolean allowInEnd = false;
    /**
     * A blacklist of biomes that can't be overwritten by fertilizers.
     */
    public List<String> biomeBlacklist = List.of("minecraft:dripstone_caves", "minecraft:lush_caves");

}
