package boilerplate.rendering.textures;

import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

/**
 * faces order:
 * 1: right  (+x)
 * 2: left  (-x)
 * 3: top  (+y)
 * 4: bottom  (-y)
 * 5: back  (+z)
 * 6: front  (-z)
 */
public class CubeMap extends Texture {
    public int pixelDataType = GL45.GL_UNSIGNED_BYTE;
    public int storedFormat = GL45.GL_RGBA16F;

    public CubeMap() {
        textureTarget = GL45.GL_TEXTURE_CUBE_MAP;
    }

    public CubeMap(boolean generateId) {
        this();
        if (generateId) genId();
    }

    public void loadFaces(String facePath) {
        Image img = Image.loadImageFromPathSTB(facePath);
        loadFaces(img);
        img.free();
    }

    public void loadFaces(String... facePaths) {
        Image[] images = new Image[facePaths.length];
        for (int i = 0; i < facePaths.length; i++) images[i] = Image.loadImageFromPathSTB(facePaths[i]);
        loadFaces(images);
        for (Image img : images) img.free();
    }

    public void loadFaces(Image face) {
        loadFaces(face, face, face, face, face, face);
    }

    public void loadFaces(Image... faces) {
        bind();
        if (faces.length != 6) {
            Logging.danger("Cube map must have 6 faces (%s were given). Aborting.", faces.length);
            return;
        }

        for (int i = 0; i < 6; i++) {
            Image image = faces[i];
            GL45.glTexImage2D(GL45.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, storedFormat, image.width, image.height, 0, image.getImageFormat(), pixelDataType, image.buffer);
        }
    }

    public static void unbind() {
        Texture.unbind(GL45.GL_TEXTURE_CUBE_MAP);
    }
}
