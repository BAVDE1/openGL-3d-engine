package boilerplate.rendering.textures;

import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.stb.STBImage.stbi_failure_reason;

public class Image {
    public static final int BPP = 4;  // bytes per pixel

    public ByteBuffer buffer;
    public int width, height, channels;

    public Image() {

    }

    public Image(ByteBuffer i, int w, int h) {
        buffer = i;
        width = w;
        height = h;
    }

    public Image(ByteBuffer i, int w, int h, int ch) {
        buffer = i;
        width = w;
        height = h;
        channels = ch;
    }

    public void free() {
        STBImage.stbi_image_free(buffer);
    }

    public int getImageFormat() {
        return switch (channels) {
            case 3 -> GL45.GL_RGB;
            case 4 -> GL45.GL_RGBA;
            default -> {
                Logging.warn("The format for this images' channels is not defined, you'll have to provide your own gl format");
                yield 0;
            }
        };
    }

    public static void flipOnSTBLoad() {
        STBImage.stbi_set_flip_vertically_on_load(true);
    }

    public static void noFlipOnSTBLoad() {
        STBImage.stbi_set_flip_vertically_on_load(false);
    }

    public static Image loadImageFromPathSTB(String path) {
        int[] width = new int[1];
        int[] height = new int[1];
        int[] channels = new int[1];
        ByteBuffer data = STBImage.stbi_load(path, width, height, channels, 0);
        if (data == null) Logging.danger("An error occurred when attempting to load image from '%s'.\n%s", path, stbi_failure_reason());
        return new Image(data, width[0], height[0], channels[0]);
    }

    public static Image bufferedImageToByteImage(BufferedImage bufferedImage) {
        Image image = new Image();
        image.width = bufferedImage.getWidth();
        image.height = bufferedImage.getHeight();
        image.channels = 4;

        // get all pixels
        int[] pixels = new int[image.width * image.height];
        bufferedImage.getRGB(0, 0, image.width, image.height, pixels, 0, image.width);

        image.buffer = MemoryUtil.memAlloc(image.width * image.height * BPP);
        for (int y = 0; y < image.height; y++) {
            for (int x = 0; x < image.width; x++) {
                int pixel = pixels[y * image.width + x];
                image.buffer.put((byte) ((pixel >> 16) & 0xFF));  // Red component
                image.buffer.put((byte) ((pixel >> 8) & 0xFF));   // Green component
                image.buffer.put((byte) (pixel & 0xFF));          // Blue component
                image.buffer.put((byte) ((pixel >> 24) & 0xFF));  // Alpha component
            }
        }
        return image;
    }
}
