package boilerplate.rendering.text;

import boilerplate.common.BoilerplateConstants;
import boilerplate.models.Model;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.builders.BufferBuilder2f;

import java.util.ArrayList;

/**
 * Renders (multiple) TentRenderer.TextObjects in its own auto-resizing buffer.
 * TextRenderer buffer is only rebuilt when one of its TextObjects has been modified
 */
public class TextRenderer {
    private final ArrayList<TextObject> textObjects = new ArrayList<>();

    private VertexArray va;
    private VertexArrayBuffer vb;
    private BufferBuilder2f sb;

    private final Model model = new Model();

    private boolean hasBeenModified = false;

    public VertexLayout vertexLayout = FontManager.getTextVertexLayout();
    public TextObject.MeshBuilder textObjectMeshBuilder = TextObject.getDefaultMeshBuilder();

    /**
     * after GL context created
     */
    public void setupBufferObjects() {
        va = new VertexArray(true);
        vb = new VertexArrayBuffer(true);
        sb = new BufferBuilder2f(true, FontManager.textLayoutAdditionalVerts());

        va.bindBuffer(vb);
        va.pushLayout(FontManager.getTextVertexLayout());
    }

    private void buildMeshes() {
        model.meshes.clear();

        for (TextObject to : textObjects) {
            if (to.getString().isEmpty() || to.getScale() < BoilerplateConstants.EPSILON) continue;
            model.meshes.add(to.buildMesh());
        }

        hasBeenModified = false;
    }

    public void delete() {
        if (va != null) va.delete();
        if (vb != null) vb.delete();
    }

    public void draw() {
        if (hasBeenModified) buildMeshes();

        if (!model.meshes.isEmpty()) {
            FontManager.bindText2dShader();
            model.draw(FontManager.textShader2d, 0);
        }
    }

    public ArrayList<TextObject> getTextObjects() {
        return textObjects;
    }

    public void pushTextObject(TextObject... tos) {
        for (TextObject to : tos) {
            to.addParent(this);
            textObjects.add(to);
            hasBeenModified = true;
        }
    }

    public void removeTextObject(TextObject to) {
        to.removeParent();
        textObjects.remove(to);
        hasBeenModified = true;
    }

    public void clearAllTextObjects() {
        for (TextObject to : textObjects) to.removeParent();
        textObjects.clear();
        hasBeenModified = true;
    }

    public void setHasBeenModified(boolean val) {
        hasBeenModified = val;
    }

    public BufferBuilder2f getBufferBuilder() {
        return sb;
    }
}
