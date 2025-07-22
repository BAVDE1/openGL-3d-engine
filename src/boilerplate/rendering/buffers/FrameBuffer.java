package boilerplate.rendering.buffers;

import boilerplate.rendering.textures.Texture;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.rendering.textures.Texture2dMultisample;
import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

import java.awt.*;
import java.util.ArrayList;

/**
 * Frame Buffer is a section of the GPUs memory that holds buffers (colour buffer, depth buffer, stencil buffer).
 * <p>
 * A default frame buffer is created by GLFW.
 * Custom Frame Buffers can be used for post-processing or mirrors and the like.
 * <p>
 * Render buffer objects are write only, cannot be read (cannot be sampled), which makes them fast.
 * If you're not sampling the values in your shader, use render buffer attachment, otherwise use a texture attachment.
 * <p>
 * Intermediary framebuffer can be used to blit a multisample framebuffer (this) into a normal framebuffer (this.intermediaryFB). For post-processing or whatever.
 * The default setup for the intermediary framebuffer only attaches one normal color buffer with the same size as this framebuffer.
 */
public class FrameBuffer {
    public static class RenderBuffer {
        private Integer id;
        public Integer attachment;
        private boolean hasMultisampling = false;

        public RenderBuffer() {

        }

        public RenderBuffer(boolean generateId) {
            if (generateId) genId();
        }

        public void createBuffer(Dimension size, int format, int attachment) {
            if (hasMultisampling) {
                Logging.warn("Cannot create normal render buffer for a render buffer that uses multisampling. Aborting.");
                return;
            }
            this.attachment = attachment;
            bind();
            GL45.glRenderbufferStorage(GL45.GL_RENDERBUFFER, format, size.width, size.height);
        }

        public void createBufferMultisample(Dimension size, int format, int attachment, int samples) {
            this.hasMultisampling = true;
            this.attachment = attachment;
            bind();
            GL45.glRenderbufferStorageMultisample(GL45.GL_RENDERBUFFER, samples, format, size.width, size.height);
        }

        public void bind() {
            GL45.glBindRenderbuffer(GL45.GL_RENDERBUFFER, id);
        }

        public static void unbind() {
            GL45.glBindRenderbuffer(GL45.GL_RENDERBUFFER, 0);
        }

        public void genId() {
            if (id != null) {
                Logging.warn("Attempting to re-generate already generated render buffer id, aborting");
                return;
            }
            id = GL45.glGenRenderbuffers();
        }

        public int getId() {
            return id;
        }
    }

    public static int defaultColourBuffStoredFormat = GL45.GL_RGBA16F;
    public static int defaultColourBuffGivenFormat = GL45.GL_RGBA;

    protected Integer bufferId;

    public Dimension size = new Dimension(128, 128);

    public ArrayList<Texture> colourBuffers = new ArrayList<>();
    public Texture depthBuffer;
    public Texture stencilBuffer;
    public Texture depthStencilBuffer;
    public RenderBuffer renderBuffer;

    public FrameBuffer intermediaryFB = null;

    public FrameBuffer(boolean generateId) {
        if (generateId) genId();
    }

    public FrameBuffer(Dimension bufferSize) {
        this.size = bufferSize;
    }

    public FrameBuffer(boolean generateId, Dimension bufferSize) {
        this(generateId);
        this.size = bufferSize;
    }

    public FrameBuffer createIntermediaryFB() {
        return new FrameBuffer(true, size);
    }

    public void blitIntoIntermediaryFB(int mask, int interpolation, int readBuffer, int drawBuffer) {
        blitIntoFrameBuffer(intermediaryFB, mask, interpolation, readBuffer, drawBuffer);
    }

    public void blitIntoFrameBuffer(FrameBuffer frameBuffer, int mask, int interpolation, int readBuffer, int drawBuffer) {
        bindToRead();
        frameBuffer.bindToDraw();
        GL45.glReadBuffer(readBuffer);
        GL45.glDrawBuffer(drawBuffer);
        GL45.glBlitFramebuffer(0, 0, size.width, size.height, 0, 0, frameBuffer.size.width, frameBuffer.size.height, mask, interpolation);
    }

    public void blitIntoDefaultFrameBuffer(Dimension destinationSize, int mask, int interpolation, int readBuffer, int drawBuffer) {
        bindToRead();
        unbindFromDraw();
        GL45.glReadBuffer(readBuffer);
        GL45.glDrawBuffer(drawBuffer);
        GL45.glBlitFramebuffer(0, 0, size.width, size.height, 0, 0, destinationSize.width, destinationSize.height, mask, interpolation);
    }

    public void attachColourBuffer2D(Texture colourBuff) {
        boolean multisample = colourBuff instanceof Texture2dMultisample;
        bind();
        GL45.glFramebufferTexture2D(GL45.GL_FRAMEBUFFER, GL45.GL_COLOR_ATTACHMENT0 + colourBuffers.size(), multisample ? GL45.GL_TEXTURE_2D_MULTISAMPLE : GL45.GL_TEXTURE_2D, colourBuff.getId(), 0);
        colourBuffers.add(colourBuff);
        Logging.debug("Attached colour buffer 2D to frame buffer (fb id: %s), (texture id: %s, col buff index: %s)", getId(), colourBuff.getId(), colourBuffers.size()-1);
    }

    public void attachDepthBuffer2D(Texture depthBuff) {
        bind();
        this.depthBuffer = depthBuff;
        GL45.glFramebufferTexture2D(GL45.GL_FRAMEBUFFER, GL45.GL_DEPTH_ATTACHMENT, GL45.GL_TEXTURE_2D, depthBuff.getId(), 0);
        Logging.debug("Attached depth buffer 2D to frame buffer (fb id: %s), (texture id: %s)", getId(), depthBuff.getId());
    }

    public void attachDepthBuffer(Texture depthBuff) {
        bind();
        this.depthBuffer = depthBuff;
        GL45.glFramebufferTexture(GL45.GL_FRAMEBUFFER, GL45.GL_DEPTH_ATTACHMENT, depthBuff.getId(), 0);
        Logging.debug("Attached depth buffer to frame buffer (fb id: %s), (texture id: %s)", getId(), depthBuff.getId());
    }

    public void attachStencilBuffer2D(Texture stencilBuff) {
        bind();
        this.stencilBuffer = stencilBuff;
        GL45.glFramebufferTexture2D(GL45.GL_FRAMEBUFFER, GL45.GL_STENCIL_ATTACHMENT, GL45.GL_TEXTURE_2D, stencilBuff.getId(), 0);
        Logging.debug("Attached stencil buffer 2D to frame buffer (fb id: %s), (texture id: %s)", getId(), stencilBuff.getId());
    }

    public void attachDepthStencilBuffer2D(Texture depthStencilBuff) {
        bind();
        this.depthStencilBuffer = depthStencilBuff;
        GL45.glFramebufferTexture2D(GL45.GL_FRAMEBUFFER, GL45.GL_DEPTH_STENCIL_ATTACHMENT, GL45.GL_TEXTURE_2D, depthStencilBuff.getId(), 0);
        Logging.debug("Attached depth / stencil buffer 2D to frame buffer (fb id: %s), (texture id: %s)", getId(), depthStencilBuff.getId());
    }

    public void attachRenderBuffer(RenderBuffer renderBuff) {
        bind();
        this.renderBuffer = renderBuff;
        GL45.glFramebufferRenderbuffer(GL45.GL_FRAMEBUFFER, renderBuff.attachment, GL45.GL_RENDERBUFFER, renderBuff.getId());
        Logging.debug("Attached render buffer to frame buffer (fb id: %s), (texture id: %s)", getId(), renderBuff.getId());
    }

    public Texture setupDefaultColourBuffer() {
        return setupDefaultColourBuffer(size);
    }

    public static Texture setupDefaultColourBuffer(Dimension size) {
        Texture t = setupTextureBuffer(size, defaultColourBuffStoredFormat, defaultColourBuffGivenFormat, GL45.GL_UNSIGNED_BYTE);
        t.useNearestInterpolation();
        t.useClampEdgeWrap();
        return t;
    }

    public Texture setupDefaultColourMultisampleBuffer(int samples) {
        return setupDefaultColourMultisampleBuffer(size, samples);
    }

    public static Texture setupDefaultColourMultisampleBuffer(Dimension size, int samples) {
        Texture2dMultisample t = new Texture2dMultisample(size, true);
        t.bind();
        t.createTexture2d(FrameBuffer.defaultColourBuffStoredFormat, samples);
        return t;
    }

    public Texture setupDefaultDepthBuffer() {
        return setupDefaultDepthBuffer(size);
    }

    public static Texture setupDefaultDepthBuffer(Dimension size) {
        return setupTextureBuffer(size, GL45.GL_DEPTH_COMPONENT);
    }

    public Texture setupDefaultStencilBuffer() {
        return setupDefaultStencilBuffer(size);
    }

    public static Texture setupDefaultStencilBuffer(Dimension size) {
        return setupTextureBuffer(size, GL45.GL_STENCIL_INDEX);
    }

    public Texture setupDefaultDepthStencilBuffer() {
        return setupDefaultDepthStencilBuffer(size);
    }

    public static Texture setupDefaultDepthStencilBuffer(Dimension size) {
        return setupTextureBuffer(size, GL45.GL_DEPTH24_STENCIL8, GL45.GL_DEPTH_STENCIL, GL45.GL_UNSIGNED_INT_24_8);
    }

    public static Texture setupTextureBuffer(Dimension size, int format) {
        return setupTextureBuffer(size, format, format, GL45.GL_UNSIGNED_BYTE);
    }

    public static Texture setupTextureBuffer(Dimension size, int storedFormat, int givenFormat, int pixelDataType) {
        Texture2d buff = new Texture2d(size, true);
        buff.pixelDataType = pixelDataType;
        buff.bind();
        buff.createTexture2d(storedFormat, givenFormat, null);
        return buff;
    }

    public RenderBuffer setupDefaultRenderBuffer() {
        return setupDefaultRenderBuffer(size);
    }

    public static RenderBuffer setupDefaultRenderBuffer(Dimension size) {
        RenderBuffer rb = new RenderBuffer();
        rb.genId();
        rb.createBuffer(size, GL45.GL_DEPTH24_STENCIL8, GL45.GL_DEPTH_STENCIL_ATTACHMENT);
        return rb;
    }

    public void drawToMultipleColourBuffers(int... attachmentIndex) {
        for (int i = 0; i < attachmentIndex.length; i++) attachmentIndex[i] = GL45.GL_COLOR_ATTACHMENT0 + attachmentIndex[i];
        GL45.glDrawBuffers(attachmentIndex);
    }

    public void genId() {
        if (bufferId != null) {
            Logging.warn("Attempting to re-generate already generated frame buffer id, aborting");
            return;
        }
        bufferId = GL45.glGenFramebuffers();
    }

    public int getId() {
        return bufferId;
    }

    public void drawBufferNone() {
        GL45.glDrawBuffer(GL45.GL_NONE);
    }

    public void readBufferNone() {
        GL45.glReadBuffer(GL45.GL_NONE);
    }

    public void bind() {
        GL45.glBindFramebuffer(GL45.GL_FRAMEBUFFER, bufferId);
    }

    public void bindToRead() {
        GL45.glBindFramebuffer(GL45.GL_READ_FRAMEBUFFER, bufferId);
    }

    public void bindToDraw() {
        GL45.glBindFramebuffer(GL45.GL_DRAW_FRAMEBUFFER, bufferId);
    }

    /**
     * 0 reverts to use the default frame buffer, set by the windowing system (GLFW)
     */
    public static void unbind() {
        GL45.glBindFramebuffer(GL45.GL_FRAMEBUFFER, 0);
    }

    public static void unbindFromRead() {
        GL45.glBindFramebuffer(GL45.GL_READ_FRAMEBUFFER, 0);
    }

    public static void unbindFromDraw() {
        GL45.glBindFramebuffer(GL45.GL_DRAW_FRAMEBUFFER, 0);
    }

    public void delete() {
        unbind();
        GL45.glDeleteFramebuffers(bufferId);
    }

    public int getFrameBufferStatus() {
        bind();
        return GL45.glCheckFramebufferStatus(GL45.GL_FRAMEBUFFER);
    }

    public boolean isCompletelyBuilt() {
        return getFrameBufferStatus() == GL45.GL_FRAMEBUFFER_COMPLETE;
    }

    public void checkCompletionOrError() {
        int status = getFrameBufferStatus();
        if (status != GL45.GL_FRAMEBUFFER_COMPLETE) {
            Logging.warn("The frame buffer is not complete.");
            switch (status) {
                case GL45.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> Logging.warn("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
                case GL45.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER ->
                        Logging.warn("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
                case GL45.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS ->
                        Logging.warn("GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS");
                case GL45.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT ->
                        Logging.warn("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
                case GL45.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE ->
                        Logging.warn("GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE");
                case GL45.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER ->
                        Logging.warn("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
                default -> Logging.warn("UNKNOWN STATUS: %s", status);
            }
        }
    }
}
