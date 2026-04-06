package shipwrights.dataplanets.texturing;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fml.loading.FMLPaths;
import shipwrights.dataplanets.DataplanetsClientConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main entry point for the planet texture generation system.
 *
 * Usage:
 * <pre>
 *   BufferedImage input = ImageIO.read(new File("planet_seed.png")); // 8–32 px
 *   PlanetTextureGenerator gen = new PlanetTextureGenerator();
 *   BufferedImage[] faces = gen.generate(input);
 *   // faces[0..5] are 256×256 cube map textures
 * </pre>
 */
public class PlanetTextureGenerator {

    // -----------------------------------------------------------------------
    // Configuration (can be overridden before calling generate())
    // -----------------------------------------------------------------------

    /** Output resolution per face (square). */
    private int resolution = 256;

    /** Palette size (number of dominant colours to extract). */
    private int paletteSize = 16;

    /** Whether to apply dithering during palette remapping. */
    private boolean dither = true;

    /** Random seed for noise generation. */
    private long noiseSeed = 12345L;

    public static void genPlanetTexture(String name, BlockState base) throws IOException {


       NativeImage image = Minecraft.getInstance().getBlockRenderer().getBlockModel(base)
               .getQuads(base, Direction.UP,Minecraft.getInstance().level.getRandom())
               .get(0)
               .getSprite()
               .contents().getOriginalImage();


       ByteArrayInputStream iss = new ByteArrayInputStream(image.asByteArray());
       BufferedImage seedImage = ImageIO.read(iss);


        PlanetTextureGenerator gen = new PlanetTextureGenerator()
                .resolution(256)
                .paletteSize(16)
                .dither(true)
                .seed(99887766L);

        BufferedImage[] faces = gen.generate(seedImage, name);
        BufferedImage strip = makeStrip(faces);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(strip,"png",os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        Minecraft.getInstance().getTextureManager().register(ResourceLocation.fromNamespaceAndPath("genesis","textures/planets/dataplanets/"+name+".png"),new DynamicTexture(NativeImage.read(is)));

        // export textures if configured to do so
        if (DataplanetsClientConfig.shouldExportTextures()) {
            Path exportDir = FMLPaths.CONFIGDIR.get().resolve("dataplanets").resolve("textures");
            Files.createDirectories(exportDir);
            ImageIO.write(strip, "png", exportDir.resolve(name + ".png").toFile());
        }
    }

    /** Creates a 3×2 horizontal strip of all six faces for easy inspection. */
    private static BufferedImage makeStrip(BufferedImage[] faces) {
        BufferedImage strip = new BufferedImage(256 * 3, 256 * 2, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = strip.createGraphics();
        // Layout:
        // Top row: North(-Z), West(-X), South(+Z)
        // Bottom row: East(+X), Down(-Y), Up(+Y)
        int[] order = {5, 1, 4, 0, 3, 2};
        for (int i = 0; i < order.length; i++) {
            int col = i % 3;
            int row = i / 3;
            g2d.drawImage(faces[order[i]], col * 256, row * 256, null);
        }
        g2d.dispose();
        return strip;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generate six seamless cube-face textures from a small input texture,
     * using the planet name to deterministically seed the noise offset so
     * that different planets look distinct even with the same generator type.
     *
     * @param inputTexture a 8–32 px RGB image
     * @param planetName   any stable identifier (filename, registry key, etc.)
     */
    public BufferedImage[] generate(BufferedImage inputTexture, String planetName) {
        // Combine the manual seed with the name's hashcode so:
        //   - Same name + same seed → identical output (deterministic)
        //   - Different names → different noise offset even with same seed
        long combinedSeed = noiseSeed ^ ((long) planetName.hashCode() * 0x9e3779b97f4a7c15L);
        return generateWithSeed(inputTexture, combinedSeed);
    }

    /**
     * Generate using only the manual seed (no name). Kept for compatibility.
     */
    public BufferedImage[] generate(BufferedImage inputTexture) {
        return generateWithSeed(inputTexture, noiseSeed);
    }

    private BufferedImage[] generateWithSeed(BufferedImage inputTexture, long seed) {
        long start = System.currentTimeMillis();

        // -----------------------------------------------------------------
        // 1. Palette extraction
        // -----------------------------------------------------------------
        log("Step 1: Palette extraction...");
        PaletteExtractor extractor = new PaletteExtractor();
        List<PaletteColor> palette = extractor.extract(inputTexture, paletteSize);
        log("  Extracted " + palette.size() + " palette colours.");

        // -----------------------------------------------------------------
        // 2. Pattern analysis
        // -----------------------------------------------------------------
        log("Step 2: Pattern analysis...");
        PatternAnalyzer analyzer = new PatternAnalyzer();
        PatternAnalyzer.PatternDescriptor pattern = analyzer.analyze(inputTexture);
        log("  " + pattern);

        // -----------------------------------------------------------------
        // 3. Generator selection
        // -----------------------------------------------------------------
        GeneratorType genType = pattern.recommended;
        log("Step 3: Selected generator: " + genType);

        // -----------------------------------------------------------------
        // 4. Build patch library
        // -----------------------------------------------------------------
        log("Step 4: Building patch library...");
        PatchLibrary patchLibrary = new PatchLibrary(inputTexture);
        log("  Patch library contains " + patchLibrary.size() + " patches.");

        // -----------------------------------------------------------------
        // 5. Synthesize six cube faces
        // -----------------------------------------------------------------
        log("Step 5: Synthesizing " + resolution + "x" + resolution + " faces...");
        CubeMapper   cubeMapper = new CubeMapper(resolution);
        NoiseModel   noiseModel = new NoiseModel(seed);
        TextureSynthesizer synth = new TextureSynthesizer(cubeMapper, noiseModel, patchLibrary, genType);

        BufferedImage[] faces = synth.synthesize();

        // -----------------------------------------------------------------
        // 6. Palette remapping
        // -----------------------------------------------------------------
        log("Step 6: Palette remapping...");
        PaletteMapper mapper = new PaletteMapper(palette, dither);
        mapper.remap(faces);

        // -----------------------------------------------------------------
        // 7. Seam fixing
        // -----------------------------------------------------------------
        log("Step 7: Fixing seams...");
        SeamFixer seamFixer = new SeamFixer();
        seamFixer.fix(faces);

        long elapsed = System.currentTimeMillis() - start;
        log("Done! Total time: " + elapsed + " ms");
        return faces;
    }

    // -----------------------------------------------------------------------
    // Fluent configuration setters
    // -----------------------------------------------------------------------

    public PlanetTextureGenerator resolution(int res) {
        this.resolution = res;
        return this;
    }

    public PlanetTextureGenerator paletteSize(int size) {
        this.paletteSize = size;
        return this;
    }

    public PlanetTextureGenerator dither(boolean enabled) {
        this.dither = enabled;
        return this;
    }

    public PlanetTextureGenerator seed(long seed) {
        this.noiseSeed = seed;
        return this;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void log(String msg) {
        System.out.println("[PlanetGen] " + msg);
    }
}