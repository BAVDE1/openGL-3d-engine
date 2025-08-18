package boilerplate.rendering.text;

import boilerplate.common.BoilerplateConstants;
import boilerplate.models.Mesh;
import boilerplate.models.Model;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.builders.BufferBuilder2f;
import boilerplate.rendering.builders.Shape2d;
import boilerplate.rendering.builders.ShapeMode;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import org.joml.Vector2f;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL45.*;

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

//    private void buildBuffer() {
//        sb.clear();
//
//        for (TextObject to : textObjects) {
//            if (to.getString().isEmpty() || to.getScale() < BoilerplateConstants.EPSILON) continue;
//            sb.pushRawSeparatedFloats(to.buildStrip());
//        }
//
//        vb.bufferData(sb);
//        hasBeenModified = false;
//    }

    public void delete() {
        if (va != null) va.delete();
        if (vb != null) vb.delete();
    }

    public void draw() {
        if (hasBeenModified) buildMeshes();

        if (!model.meshes.isEmpty()) {
            FontManager.bindText2dShader();
            model.draw(FontManager.textShader2d, 0);
//            va.drawArrays(GL_LINE_STRIP, sb.getVertexCount());
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

//    public static void pushTextToBuilder(BufferBuilder2f sb, String text, FontManager.LoadedFont font, Vector2f pos, float[] appendFloats) {
//        pushTextToBuilder(sb, text, font, pos, appendFloats, 1);
//    }

    /**
     * Pushes all chars into the buffer
     * Assumes that the VA looks like: `posX, posY, texturePosX, texturePosY, ...`
     */
//    public static void pushTextToBuilder(BufferBuilder2f sb, String text, FontManager.LoadedFont font, Vector2f pos, float[] appendFloats, float scale) {
//        int accumulatedX = 0;
//        boolean separate = true;
//        for (char c : text.toCharArray()) {
//            FontManager.Glyph glyph = font.getGlyph(c);
//            Vector2f size = glyph.getSize().mul(scale);
//
//            if (c == ' ') {
//                accumulatedX += (int) size.x;
//                separate = true;
//                continue;
//            }
//
//            Vector2f topLeft = pos.add(accumulatedX, 0, new Vector2f());
//            Shape2d.Poly2d texturePoints = Shape2d.createRect(glyph.texTopLeft, glyph.texSize);
//            ShapeMode.UnpackAppend mode = new ShapeMode.UnpackAppend(texturePoints.toArray(), appendFloats);
//            Shape2d.Poly2d p = Shape2d.createRect(topLeft, size, mode);
//
//            if (separate) {
//                sb.pushSeparatedPolygon(p);
//                System.out.println("sep");
//                separate = false;
//            } else {
//                sb.pushPolygon(p);
//                System.out.println("cont");
//            }
//            accumulatedX += (int) size.x;
//        }
//    }
}
