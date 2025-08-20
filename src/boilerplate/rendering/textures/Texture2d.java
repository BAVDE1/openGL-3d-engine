package boilerplate.rendering.textures;

import boilerplate.rendering.ShaderProgram;
import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;

public class Texture2d extends Texture {
    public static final int TYPE_NOTHING = 0;
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_HEIGHT = 2;
    public static final int TYPE_DIFFUSE = 3;
    public static final int TYPE_SPECULAR = 4;

    protected static final Map<Integer, Integer> boundSlots = new HashMap<>();  // key: slot, value: texId
    public static File outputFile = new File("image.png");  // for debugging

    public int textureType = TYPE_NOTHING;
    public int pixelDataType = GL_UNSIGNED_BYTE;
    public int storedFormat = GL45.GL_RGBA16F;

    public Dimension size = new Dimension();

    private String loadedPath = "";

    public Texture2d() {
        textureTarget = GL_TEXTURE_2D;
    }

    public Texture2d(Dimension size, boolean generateId) {
        this();
        this.size = size;
        if (generateId) genId();
    }

    public Texture2d(String path) {
        this();
        createTextureFromPath(path);
    }

    public Texture2d(BufferedImage buffImg) {
        this();
        createTextureFromImgBuff(buffImg);
    }

    public void createTextureFromPath(String path) {
        createTextureFromPath(path, 0);
    }

    public void createTextureFromPath(String path, int desiredChannels) {
        Image image = Image.loadImageFromPathSTB(path, desiredChannels);
        size = new Dimension(image.width, image.height);
        loadedPath = path;
        createTextureFromImage(image);
    }

    public void createTextureFromImgBuff(BufferedImage buffImg) {
        Image image = Image.bufferedImageToByteImage(buffImg);
        size = new Dimension(image.width, image.height);
        createTextureFromImage(image);
    }

    protected void createTextureFromImage(Image image) {
        image.buffer.flip();

        genId();
        bind();
        useNearestInterpolation();
        useClampEdgeWrap();
        createTexture2d(storedFormat, image.getImageFormat(), image.buffer);

        MemoryUtil.memFree(image.buffer);  // may want to keep for later though :shrug:
    }

    /**
     * internalFormat: format to be stored in
     * format: format of supplied image
     */
    public void createTexture2d(int storedFormat, int givenImgFormat, ByteBuffer buffer) {
        glTexImage2D(GL_TEXTURE_2D, 0, storedFormat, size.width, size.height, 0, givenImgFormat, pixelDataType, buffer);
    }

    /**
     * meant for use with a texture array
     */
    public void bindToSlot(int slot) {
        if (slot == 0) {
            Logging.danger("cannot bind texture to slot 0, use anything > 0.");
            return;
        }

        if (boundSlots.containsKey(slot)) {
            Logging.warn("Overriding already set texture slot '%s'", slot);
            boundSlots.remove(slot);
        }

        glBindTextureUnit(slot, textureId);
        boundSlots.put(slot, textureId);
    }

    public void bindToTexArray(int slot, ShaderProgram sh) {
        bindToSlot(slot);
        sh.uniform1iv("textures", boundSlots.keySet().stream().mapToInt(i -> i).toArray());
    }

    public static void unbind(int slot) {
        Texture.unbind(GL_TEXTURE_2D, slot);
    }

    public static void deleteAll() {
        for (int id : boundSlots.values()) glDeleteTextures(id);
        boundSlots.clear();
        glActiveTexture(GL_TEXTURE0);
    }

    /**
     * debug function writes given image to src/image.png
     */
    public static void writeToFile(BufferedImage img) {
        try {
            ImageIO.write(img, "png", outputFile);
        } catch (IOException e) {
            Logging.danger("Failed to write given image to file 'src/image.png'\nError message thrown:\n%s", e);
        }
    }

    @Override
    public String toString() {
        return "Texture2d(%s, %s, %s)".formatted(textureId, loadedPath, size);
    }
}
