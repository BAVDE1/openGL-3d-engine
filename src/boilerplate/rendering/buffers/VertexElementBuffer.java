package boilerplate.rendering.buffers;

import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

/**
 * Element Array Buffer.
 */
public class VertexElementBuffer extends Buffer {
    public static final int ELEMENT_TYPE_BYTE = GL45.GL_UNSIGNED_BYTE;
    public static final int ELEMENT_TYPE_SHORT = GL45.GL_UNSIGNED_SHORT;
    public static final int ELEMENT_TYPE_INT = GL45.GL_UNSIGNED_INT;

    private final int elementType;

    public VertexElementBuffer(int elementType){
        this.bufferType = GL45.GL_ELEMENT_ARRAY_BUFFER;
        this.elementType = elementType;
    }

    public VertexElementBuffer(int elementType, boolean generateId) {
        this(elementType);
        if (generateId) genId();
    }

    public int getElementType() {
        return elementType;
    }

    public static void unbind() {
        unbindType(GL45.GL_ELEMENT_ARRAY_BUFFER);
    }

    @Override
    public void setBufferType(int bufferType) {
        Logging.danger("Cannot set buffer type on this object. Use a VertexBuffer instead.");
    }
}
