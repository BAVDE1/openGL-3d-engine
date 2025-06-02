package boilerplate.rendering.buffers;

import boilerplate.rendering.builders.BufferBuilder;
import org.lwjgl.opengl.GL45;
import boilerplate.utility.Logging;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL45.*;

public class VertexBuffer {
    protected Integer bufferType;
    protected Integer usage = GL45.GL_DYNAMIC_DRAW;
    protected Integer bufferId;

    public VertexBuffer() {
    }

    public VertexBuffer(boolean generateId) {
        if (generateId) genId();
    }

    public void setBufferType(int bufferType) {
        this.bufferType = bufferType;
    }

    public void setUsage(int newUsage) {
        this.usage = newUsage;
    }

    public void bind() {
        glBindBuffer(getBufferType(), bufferId);
    }

    public static void unbindTYpe(int bufferType) {
        glBindBuffer(bufferType, 0);
    }

    public void bufferData(float[] data) {
        bind();
        GL45.glBufferData(bufferType, data, usage);
    }

    public void bufferData(FloatBuffer data) {
        bind();
        GL45.glBufferData(bufferType, data, usage);
    }

    public void bufferData(int[] data) {
        bind();
        GL45.glBufferData(bufferType, data, usage);
    }

    public void bufferData(BufferBuilder bb) {
        bufferData(bb.getFloats());
    }

    public void bufferSize(int bytesSize) {
        bind();
        GL45.glBufferData(bufferType, bytesSize, usage);
    }

    public void bufferSubData(int bytesOffset, float[] data) {
        bind();
        GL45.glBufferSubData(bufferType, bytesOffset, data);
    }

    public void bufferSubData(int bytesOffset, FloatBuffer data) {
        bind();
        GL45.glBufferSubData(bufferType, bytesOffset, data);
    }

    public void genId() {
        if (bufferId != null) {
            Logging.warn("Attempting to re-generate already generated buffer id, aborting");
            return;
        }
        bufferId = GL45.glGenBuffers();
    }

    public void delete() {
        glDeleteBuffers(bufferId);
    }

    public int getId() {
        return bufferId;
    }

    public int getBufferType() {
        return bufferType;
    }

    public int getUsage() {
        return usage;
    }
}
