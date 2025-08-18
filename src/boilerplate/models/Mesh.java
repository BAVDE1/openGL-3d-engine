package boilerplate.models;

import boilerplate.rendering.Renderer;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.buffers.VertexElementBuffer;
import boilerplate.rendering.buffers.VertexLayout;
import org.lwjgl.assimp.AIVector2D;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

public class Mesh {
    protected VertexArray va = new VertexArray();
    protected VertexArrayBuffer vb = new VertexArrayBuffer();
    protected VertexElementBuffer veb = new VertexElementBuffer(VertexElementBuffer.ELEMENT_TYPE_INT);

    VertexLayout vertexLayout;
    HashMap<Integer, List<Model.VertexWeight>> vertexWeights = new HashMap<>();

    public int indicesCount = 0;

    protected ByteBuffer data = MemoryUtil.memAlloc(0);
    protected ByteBuffer indices = MemoryUtil.memAlloc(0);
    protected Material material = new Material();

    public int renderMode = GL45.GL_TRIANGLES;

    private boolean hasUniformedValues = false;

    public Mesh(VertexLayout vertexLayout) {
        this.vertexLayout = vertexLayout;
    }

    public void allocateMemory(int verticesBytes, int indicesBytes) {
        data = MemoryUtil.memAlloc(verticesBytes);
        indices = MemoryUtil.memAlloc(indicesBytes);
    }

    public void pushIndice(int i) {
        indices.putInt(i);
    }

    public void pushInt(int i) {
        data.putInt(i);
    }

    public void pushInts(int x, int y) {
        data.putInt(x);
        data.putInt(y);
    }

    public void pushInts(int x, int y, int z) {
        pushInts(x, y);
        data.putInt(z);
    }

    public void pushInts(int x, int y, int z, int w) {
        pushInts(x, y, z);
        data.putInt(w);
    }

    public void push4Ints(int i) {
        pushInts(i, i, i, i);
    }

    public void pushFloat(float f) {
        data.putFloat(f);
    }

    public void pushFloats(float x, float y) {
        data.putFloat(x);
        data.putFloat(y);
    }

    public void pushFloats(float x, float y, float z) {
        pushFloats(x, y);
        data.putFloat(z);
    }

    public void pushFloats(float x, float y, float z, float w) {
        pushFloats(x, y, z);
        data.putFloat(w);
    }

    public void push4Floats(float f) {
        pushFloats(f, f, f, f);
    }

    public void pushVector2D(AIVector2D vector) {
        pushFloats(vector.x(), vector.y());
    }

    public void pushVector2D(AIVector3D vector) {
        pushFloats(vector.x(), vector.y());
    }

    public void pushVector3D(AIVector3D vector) {
        pushFloats(vector.x(), vector.y(), vector.z());
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void finalizeMesh() {
        va.genId();
        vb.genId();
        veb.genId();

        va.bindBuffer(vb, veb);
        va.pushLayout(vertexLayout);

        System.out.println(data);
        System.out.println(indices);
        vb.bufferData(data);
        veb.bufferData(indices);
    }

    public void draw(ShaderProgram sh, int textureSlotStart) {
        if (!hasUniformedValues) {
            material.uniformValues("material", sh);
            material.uniformAndBindTextures("material", sh, textureSlotStart);
        }
        va.drawElements(renderMode, veb, indicesCount);
    }
}
