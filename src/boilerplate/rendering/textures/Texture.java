package boilerplate.rendering.textures;

import boilerplate.utility.Logging;

import static org.lwjgl.opengl.GL45.*;

/**
 * Loads a texture from a file path or buffered image & binds to a slot
 */
public abstract class Texture {
    protected Integer textureId;
    protected Integer textureTarget;

    public int getId() {
        return textureId;
    }

    public void genId() {
        if (textureId != null) {
            Logging.warn("Attempting to re-generate already generated texture id, aborting");
            return;
        }
        textureId = glGenTextures();
    }

    public void bind() {
        glBindTexture(textureTarget, textureId);
    }

    public void bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(textureTarget, textureId);
    }

    public static void unbind(int textureType, int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(textureType, 0);
    }

    public void delete() {
        glDeleteTextures(textureId);
    }

    public void useNearestInterpolation() {
        setInterpolation(GL_NEAREST);
    }

    public void useLinearInterpolation() {
        setInterpolation(GL_LINEAR);
    }

    public void setInterpolation(int interpolation) {
        bind();
        glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, interpolation);
        glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, interpolation);
    }

    public void useClampEdgeWrap() {
        setWrap(GL_CLAMP_TO_EDGE);
    }

    public void useRepeatWrap() {
        setWrap(GL_REPEAT);
    }

    public void useMirroredRepeatWrap() {
        setWrap(GL_MIRRORED_REPEAT);
    }

    public void setWrap(int wrap) {
        bind();
        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, wrap);
        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, wrap);
        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_R, wrap);
    }
}
