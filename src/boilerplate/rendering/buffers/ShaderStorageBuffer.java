package boilerplate.rendering.buffers;

import boilerplate.rendering.ShaderProgram;
import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

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

    public static void unbind() {
        unbindType(GL45.GL_SHADER_STORAGE_BUFFER);
    }

    @Override
    public void setBufferType(int bufferType) {
        Logging.danger("Cannot set buffer type on this object. Use a VertexBuffer instead.");
    }
}
