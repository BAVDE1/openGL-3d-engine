package boilerplate.rendering.text;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.BoilerplateShaders;
import boilerplate.models.Model;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.camera.Camera;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL45;

import java.util.ArrayList;

/**
 * Renders (multiple) TentRenderer.TextObjects in its own auto-resizing buffer.
 * TextRenderer buffer is only rebuilt when one of its TextObjects has been modified
 */
public class TextRenderer {
    private final ArrayList<TextObject> textObjects = new ArrayList<>();

    private final Model model = new Model();

    public TextObject.MeshBuilder textObjectMeshBuilder = TextObject.getDefaultMeshBuilder();
    public VertexLayout vertexLayout = FontManager.defaultVertexLayout();
    public ShaderProgram shader = new ShaderProgram();

    private boolean hasBeenModified = false;

    /**
     * after GL context created
     */
    public void setupDefaultShader(Camera camera) {
        shader.genProgram();
        shader.attachShader(String.format(BoilerplateShaders.Text2DVertex, camera.uniformBlockName), GL45.GL_VERTEX_SHADER, "BoilerplateShaders.Text2DVertex");
        shader.attachShader(BoilerplateShaders.Text2DFragment, GL45.GL_FRAGMENT_SHADER, "BoilerplateShaders.Text2DFragment");
        shader.linkProgram();
        camera.bindShaderToUniformBlock(shader);
        shader.uniformTexture("fontTexture", FontManager.getFinalTexture(), FontManager.FONT_TEXTURE_SLOT);
    }

    private void buildMeshes() {
        model.meshes.clear();

        for (TextObject to : textObjects) {
            if (to.getString().isEmpty() || to.getScale() < BoilerplateConstants.EPSILON) continue;
            model.meshes.add(to.buildMesh());
        }

        hasBeenModified = false;
    }

    public void draw() {
        if (hasBeenModified) buildMeshes();

        if (!model.meshes.isEmpty()) {
            shader.bind();
            model.draw(shader, 0);
        }
    }

    public Matrix4f getModelTransform() {
        return model.modelTransform;
    }

    public void setModelTransform(Matrix4f matrix4f) {
        model.modelTransform = matrix4f;
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
}
