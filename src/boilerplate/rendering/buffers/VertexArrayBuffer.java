package boilerplate.rendering.buffers;

import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

public class VertexArrayBuffer extends Buffer {
    public VertexArrayBuffer() {
        this.bufferType = GL45.GL_ARRAY_BUFFER;
        this.usage = GL45.GL_DYNAMIC_DRAW;
    }

    public VertexArrayBuffer(boolean generateId) {
        this();
        if (generateId) genId();
    }

    public static void unbind() {
        unbindType(GL45.GL_ARRAY_BUFFER);
    }

    @Override
    public void setBufferType(int bufferType) {
        Logging.danger("Cannot set buffer type on this object. Use a VertexBuffer instead.");
    }
}
