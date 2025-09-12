package boilerplate.rendering.buffers;

import boilerplate.rendering.ShaderProgram;
import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ShaderStorageBuffer extends Buffer {
    public ShaderStorageBuffer() {
        this.bufferType = GL45.GL_SHADER_STORAGE_BUFFER;
    }

    public ShaderStorageBuffer(boolean generateId) {
        this();
        if (generateId) genId();
    }

    public void bindShaderToBlock(int binding, ShaderProgram... programs) {
        for (ShaderProgram sh : programs) {
            sh.bind();
            GL45.glBindBufferBase(GL45.GL_SHADER_STORAGE_BUFFER, binding, getId());
        }
    }

    public ByteBuffer getData(int size) {
        return getSubData(0, size);
    }

    public ByteBuffer getSubData(int offset, int size) {
        bind();
        ByteBuffer data = MemoryUtil.memAlloc(size);
        GL45.glGetBufferSubData(GL45.GL_SHADER_STORAGE_BUFFER, offset, data);
        return data;
    }

    public static void unbind() {
        unbindType(GL45.GL_SHADER_STORAGE_BUFFER);
    }

    @Override
    public void setBufferType(int bufferType) {
        Logging.danger("Cannot set buffer type on this object. Use a VertexBuffer instead.");
    }
}
