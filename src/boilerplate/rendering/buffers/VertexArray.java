package boilerplate.rendering.buffers;

import boilerplate.utility.Logging;

import java.util.ArrayList;
import java.util.Arrays;

import static org.lwjgl.opengl.GL45.*;


public class VertexArray {
    private Integer arrayId;
    public int attribCount = 0;
    boolean bufferBound = false;

    public VertexArray() {
    }

    public VertexArray(boolean generateId) {
        if (generateId) genId();
    }

    public void genId() {
        if (arrayId != null) {
            Logging.warn("Attempting to re-generate already generated array id, aborting");
            return;
        }
        arrayId = glGenVertexArrays();
    }

    public void delete() {
        glDeleteVertexArrays(arrayId);
    }

    public void bind() {
        glBindVertexArray(arrayId);
    }

    public static void unbind() {
        glBindVertexArray(0);
    }

    public void bindBuffer(VertexBuffer... buffers) {
        bufferBound = true;
        bind();
        for (VertexBuffer vb : buffers) vb.bind();
    }

    public void fastSetup(int[] layoutFloats, VertexBuffer... buffersToBind) {
        fastSetup(new VertexLayout(layoutFloats), buffersToBind);
    }

    public void fastSetup(VertexLayout layout, VertexBuffer... buffersToBind) {
        bindBuffer(buffersToBind);
        pushLayout(layout);
    }

    public void pushLayout(VertexLayout layout) {
        pushLayout(layout, 0);
    }

    /**
     * Pushing multiple layouts adds onto last layout that was bound
     */
    public void pushLayout(VertexLayout layout, int divisor) {
        if (!bufferBound) Logging.warn("Bind buffers before pushing the layout pls");
        bind();

        int offset = 0;
        for (int i = 0; i < layout.elements.size(); i++) {
            VertexLayout.Element element = layout.elements.get(i);

            int attribInx = i + attribCount;  // adding onto the last
            glEnableVertexAttribArray(attribInx);

            if (element.isIntType()) glVertexAttribIPointer(attribInx, element.count, element.type, layout.stride, offset);
            else glVertexAttribPointer(attribInx, element.count, element.type, element.normalized, layout.stride, offset);

            if (divisor > 0) glVertexAttribDivisor(attribInx, divisor);

            offset += element.count * element.getByteSizeForType();
        }
        attribCount += layout.elements.size();
    }

    public int getId() {
        return arrayId;
    }

    public void drawArrays(int mode, int vertexCount) {
        bind();
        glDrawArrays(mode, 0, vertexCount);
    }

    public void drawInstanced(int mode, int vertsPerInstance, int instanceCount) {
        bind();
        glDrawArraysInstanced(mode, 0, vertsPerInstance, instanceCount);
    }

    public void drawElements(int mode, VertexElementBuffer veb, int vertexCount) {
        drawElements(mode, veb.getElementType(), vertexCount);
    }

    public void drawElementsBaseVertex(int mode, VertexElementBuffer veb, int vertexCount, int baseIndice, int baseVertex) {
        bind();
        glDrawElementsBaseVertex(mode, vertexCount, veb.getElementType(), (long) Integer.BYTES * baseIndice, baseVertex);
    }

    public void drawElements(int mode, int elementType, int vertexCount) {
        bind();
        glDrawElements(mode, vertexCount, elementType, 0);
    }
}
