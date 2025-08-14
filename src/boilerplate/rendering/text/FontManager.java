package boilerplate.rendering.text;

import boilerplate.common.BoilerplateShaders;
import boilerplate.rendering.CameraOrtho;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL45;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * State Machine
 * Loads (default and then given) fonts once to be used by TextRenderers
 */
public class FontManager {
    public static class Glyph {
        public final int width, height;
        public final int x, y;

        // texture coordinates
        public final Vector2f texTopLeft;
        public final Vector2f texSize;

        public Glyph(int x, int y, int width, int height) {
            this.width = width; this.height = height;
            this.x = x; this.y = y;

            this.texTopLeft = new Vector2f(
                    (float) x / fullWidth,
                    (float) y / fullHeight
            );
            this.texSize = new Vector2f(
                    (float) width / fullWidth,
                    (float) height / fullHeight
            );
        }

        public Vector2f getSize() {
            return new Vector2f(width, height);
        }

        @Override
        public String toString() {
            return String.format("(x=%s, y=%s, w=%s, h=%s)", x, y, width, height);
        }
    }

    public static class LoadedFont {
        private final boolean aa;  // anti-aliasing
        public final HashMap<Character, Glyph> glyphMap = new HashMap<>();

        private final String name;
        private Font font;

        public int imgWidth = 0, imgHeight = 0;

        public LoadedFont(String fontName, int fontStyle, int fontSize, boolean antiAlias) {
            aa = antiAlias;
            name = String.format("%s_%s_%s_%s", fontName, fontStyle, fontSize, aa);

            font = new Font(fontName, fontStyle, fontSize);
            findImageDimensions();

            if (!font.getFamily().equalsIgnoreCase(fontName)) {
                Logging.warn("Font '%s' could not be found, using '%s' instead", fontName, font.getFamily());
            }
        }

        public LoadedFont(int fontEnum, int fontStyle, int fontSize, boolean antiAlias) {
            aa = antiAlias;
            name = String.format("%s_%s_%s_%s", fontEnum, fontStyle, fontSize, aa);

            String resourcePath = getCustomFontResourcePath(fontEnum);
            if (resourcePath == null) {
                Logging.danger("Font with enum value '%s' does not exist or has not been registered.", fontEnum);
                return;
            }

            Logging.debug("Attempting to load font from resource path: %s", resourcePath);
            try {
                InputStream customFontStream = ClassLoader.getSystemResourceAsStream(resourcePath);
                if (customFontStream == null) {
                    Logging.danger("Error, could not load custom font resource: %s", resourcePath);
                    return;
                }

                font = Font.createFont(Font.TRUETYPE_FONT, customFontStream);
                font = font.deriveFont(fontStyle, fontSize);
            } catch (IOException | FontFormatException e) {
                Logging.danger("Failed to load the font resource '%s'\nError message: %s", resourcePath, e);
                return;
            }

            findImageDimensions();
        }

        private String getCustomFontResourcePath(int fontEnum) {
            return switch (fontEnum) {
                case FONT_NOVA -> "fonts/Bona_Nova_SC/BonaNovaSC-Regular.ttf";
                case FONT_JACQUARD -> "fonts/Jacquard_24/Jacquard24-Regular.ttf";
                case FONT_TINY -> "fonts/Tiny5/Tiny5-Regular.ttf";
                case FONT_CASTORO -> "fonts/Castoro_Titling/CastoroTitling-Regular.ttf";
                case FONT_LUGRASIMO -> "fonts/Lugrasimo/Lugrasimo-Regular.ttf";
                case FONT_KINGS -> "fonts/Kings/Kings-Regular.ttf";
                default -> null;
            };
        }

        /** Find the dimensions of the image */
        private void findImageDimensions() {
            for (int i = AsciiFrom; i < AsciiTo; i++) {
                CharFondler.CharMetrics charSize = CharFondler.getCharSize(font, (char) i, aa);
                imgWidth += charSize.width;
                imgHeight = Math.max(imgHeight, charSize.height);
            }
        }

        /** Draw the fonts glyphs at the y offset onto the given image */
        public void drawOntoFinalImage(Graphics2D graphics, int yOffset) {
            int x = 0;
            for (int i = AsciiFrom; i < AsciiTo; i++) {
                BufferedImage charImage = CharFondler.createCharImage(font, (char) i, aa);
                if (charImage == null) continue;

                int charWidth = charImage.getWidth();
                int charHeight = charImage.getHeight();
                graphics.drawImage(charImage, x, yOffset, null);

                glyphMap.put((char) i, new Glyph(x, yOffset, charWidth, charHeight));
                x += charWidth;
            }
        }

        public int findLineWidth(String line) {
            int accumulatedWidth = 0;
            for (char c : line.toCharArray()) accumulatedWidth += getGlyph(c).width;
            return accumulatedWidth;
        }

        public int getLineHeight() {
            return glyphMap.get(' ').height;
        }

        /** Retrieve a glyph with error catching */
        public Glyph getGlyph(char c) {
            FontManager.Glyph glyph = glyphMap.get(c);

            if (glyph == null) {
                Logging.warn("Character '%s' does not exist in the currently loaded font. Using '0' instead.", c);
                glyph = glyphMap.get('0');
            }
            return glyph;
        }
    }

    public static final int FONT_TEXTURE_SLOT = 1;

    public static final int FONT_NOVA = 0;
    public static final int FONT_JACQUARD = 1;  // recommended size: 42
    public static final int FONT_TINY = 2;      // recommended size: 24
    public static final int FONT_CASTORO = 3;
    public static final int FONT_LUGRASIMO = 4;
    public static final int FONT_KINGS = 5;

    private static final int AsciiFrom = 32;
    private static final int AsciiTo = 256;

    private final static ArrayList<LoadedFont> allLoadedFonts = new ArrayList<>();
    private final static HashMap<String, Integer> loadedFontUids = new HashMap<>();

    private static VertexLayout textVertexLayout = new VertexLayout();
    private static ShaderProgram textShader2d = new ShaderProgram();

    public static int fullWidth = 0, fullHeight = 0;
    private static Texture2d finalTexture;
    private static boolean initialized = false;

    /** writes to Texture's outputFile when fonts are generated and bound */
    public static Boolean writeFontsToFile = false;

    /** Loads the default font first */
    public static void init() {
        initialized = true;
        loadFont(Font.DIALOG, Font.PLAIN, 32, true);
    }

    /** Returns -1 if there was an error */
    private static int runLoadFontChecks() {
        if (!initialized) {
            Logging.danger("FontManager has not been initialized! Aborting");
            return -1;
        }

        if (finalTexture != null) {
            Logging.danger("The font image atlas has already been generated, and so can't be appended to. Aborting.");
            return -1;
        }
        return 1;
    }

    public static int loadFont(int font, int style, int size, boolean antiAlias) {
        return loadFont("", font, style, size, antiAlias, false);
    }

    public static int loadFont(String font, int style, int size, boolean antiAlias) {
        return loadFont(font, -1, style, size, antiAlias, true);
    }

    /** Load given font. Returns the loaded font id / index */
    private static int loadFont(String fontStr, int fontInt, int style, int size, boolean antiAlias, boolean useString) {
        if (runLoadFontChecks() == -1) return -1;

        LoadedFont newFont = useString ? new LoadedFont(fontStr, style, size, antiAlias) : new LoadedFont(fontInt, style, size, antiAlias);
        String fn = newFont.name;
        if (loadedFontUids.containsKey(fn)) {
            Logging.warn("The font '%s' has already been loaded with uid '%s'. Aborting loading duplicate font", fn, loadedFontUids.get(fn));
            return -1;
        }

        fullWidth = Math.max(newFont.imgWidth, fullWidth);
        fullHeight += newFont.imgHeight;

        int id = allLoadedFonts.size();
        allLoadedFonts.add(newFont);
        loadedFontUids.put(fn, id);
        return id;
    }

    /** generate all font images onto one universal image atlas at their y offsets */
    public static void generateAndBindAllFonts(CameraOrtho camera2d) {
        BufferedImage fullImage = new BufferedImage(fullWidth, fullHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fullImage.createGraphics();

        int yOffset = 0;
        for (LoadedFont lFont : allLoadedFonts) {
            lFont.drawOntoFinalImage(graphics, yOffset);
            yOffset += lFont.imgHeight;
        }

        graphics.dispose();
        finalTexture = new Texture2d(fullImage);
        finalTexture.bindToSlot(FONT_TEXTURE_SLOT);
        Logging.debug("%s fonts generated, texture bound to slot %s", allLoadedFonts.size(), FONT_TEXTURE_SLOT);

        setupTextShader2d(camera2d);
        setupTextLayout();

        if (writeFontsToFile) Texture2d.writeToFile(fullImage);
    }

    private static void setupTextShader2d(CameraOrtho camera2d) {
        textShader2d.genProgram();
        textShader2d.attachShader(String.format(BoilerplateShaders.Text2DVertex, camera2d.uniformBlockName), GL45.GL_VERTEX_SHADER, "BoilerplateShaders.Text2DVertex");
        textShader2d.attachShader(BoilerplateShaders.Text2DFragment, GL45.GL_FRAGMENT_SHADER, "BoilerplateShaders.Text2DFragment");
        textShader2d.linkProgram();
        camera2d.bindShaderToUniformBlock(textShader2d);
        textShader2d.uniform1i("fontTexture", FONT_TEXTURE_SLOT);
    }

    private static void setupTextLayout() {
        textVertexLayout.pushFloat(2);  // vertex pos
        textVertexLayout.pushFloat(2);  // tex coord (x,y) [or is bg colour (-1,-1)]
        textVertexLayout.pushFloat(4);  // colour (text colour or bg colour)
    }

    public static int textLayoutAdditionalVerts() {return 6;}
    public static VertexLayout getTextVertexLayout() {
        return textVertexLayout;
    }

    public static void bindText2dShader() {
        textShader2d.bind();
    }

    public static LoadedFont getLoadedFont(int loadedFontId) {
        if (loadedFontId < 0 || loadedFontId > allLoadedFonts.size() - 1) {
            Logging.danger("Font with id '%s' does not exist. Returning default font.", loadedFontId);
            return allLoadedFonts.getFirst();
        }
        return allLoadedFonts.get(loadedFontId);
    }

    public static int getLoadedId(String loadedFontName) {
        if (!loadedFontUids.containsKey(loadedFontName)) {
            Logging.danger("Font with name '%s' does not exist. Returning default font id.", loadedFontName);
            return 0;
        }
        return loadedFontUids.get(loadedFontName);
    }

    public static void deleteAll() {
        finalTexture.delete();
        textShader2d.delete();

        allLoadedFonts.clear();
        loadedFontUids.clear();

        textVertexLayout = new VertexLayout();
        textShader2d = new ShaderProgram();

        fullWidth = 0;
        fullHeight = 0;
        finalTexture = null;
        initialized = false;
    }
}
