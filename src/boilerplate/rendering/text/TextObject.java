package boilerplate.rendering.text;

import boilerplate.common.BoilerplateConstants;
import boilerplate.models.Mesh;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.builders.BufferBuilder2f;
import boilerplate.utility.Logging;
import org.joml.Vector2f;

import java.awt.*;
import java.util.Objects;

public class TextObject {
    public interface MeshBuilder {
        default void build(TextObject textObject) {
        }

        default void buildBackground(TextObject textObject, Vector2f linePos, float lineWidth, int yAddition) {
        }

        default void buildLine(TextObject textObject, String line, FontManager.LoadedFont font, Vector2f linePos) {
        }

        default void pushRect(TextObject textObject, Vector2f topLeft, Vector2f size, Vector2f texCoords, Vector2f texSize, Color colour, int zAddition) {
        }

        default void pushVertex(TextObject textObject, Vector2f pos, Vector2f texCoords, Color colour, int zAddition) {
        }
    }

    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_RIGHT = 1;
    public static final int ALIGN_MIDDLE = 2;

    private TextRenderer parent;
    private Color textColour = Color.WHITE;
    private float scale = 1;
    private int ySpacing = 0;
    private int alignment = ALIGN_LEFT;

    private int loadedFontId;
    private String string;
    private Vector2f pos;
    private float z = 1;

    private final BufferBuilder2f sb = new BufferBuilder2f(true, FontManager.textLayoutAdditionalVerts());
    private final BufferBuilder2f bgSb = new BufferBuilder2f(true, FontManager.textLayoutAdditionalVerts());
    private boolean hasChanged = true;
    private Mesh mesh;
    private int vertexCounter = 0;

    private Color bgCol = new Color(0, 0, 0, 0);
    private final Vector2f bgMargin = new Vector2f();
    private boolean seamlessBgLines = false;

    public TextObject(int loadedFontId, String string, Vector2f pos, float scale, int ySpacing) {
        this(loadedFontId, string, pos);
        setScale(scale);
        setYSpacing(ySpacing);
    }

    public TextObject(int loadedFontId, String string, Vector2f pos, Color textColour) {
        this(loadedFontId, string, pos);
        setTextColour(textColour);
    }

    public TextObject(int loadedFontId, String string, Vector2f pos, Color textColour, Color bgCol) {
        this(loadedFontId, string, pos, textColour);
        setBgCol(bgCol);
    }

    public TextObject(int loadedFontId, String string, Vector2f pos) {
        setLoadedFontId(loadedFontId);
        setString(string);
        setPos(pos);
    }

    public void addParent(TextRenderer parent) {
        if (this.parent != null) {
            Logging.danger("parent is already assigned to this text object, aborting");
            return;
        }
        this.parent = parent;
    }

    public void removeParent() {
        if (this.parent == null) return;
        this.parent = null;
    }

    public Mesh buildMesh() {
        if (!hasChanged) return mesh;
        if (mesh != null) mesh.clearData();

        vertexCounter = 0;
        mesh = new Mesh(parent.vertexLayout);
        mesh.allocateMemory(calcVerticesSize(), calcIndicesSize());
        parent.textObjectMeshBuilder.build(this);
        mesh.finalizeMesh();

        hasChanged = false;
        return mesh;
    }

    public String getFilteredString() {
        return string.replaceAll(" |\n", "");
    }

    public int getLinesCount() {
        int count = 0;
        for (String line : string.split("\n")) count += line.isEmpty() ? 0 : 1;
        return count;
    }

    public int calcVerticesSize() {
        boolean hasBg = bgCol.getAlpha() > BoilerplateConstants.EPSILON;
        int charsSize = getFilteredString().length() * parent.vertexLayout.stride * 4;
        int bgSize = hasBg ? getLinesCount() * parent.vertexLayout.stride * 4 : 0;
        return charsSize + bgSize;
    }

    public int calcIndicesSize() {
        boolean hasBg = bgCol.getAlpha() > BoilerplateConstants.EPSILON;
        int charsSize = getFilteredString().length() * 6 * Integer.BYTES;
        int bgSize = hasBg ? getLinesCount() * 6 * Integer.BYTES : 0;
        return charsSize + bgSize;
    }

    public String getString() {
        return string;
    }

    public void setString(String newString, Object... args) {
        setString(String.format(newString, args));
    }

    public void setString(String newString) {
        if (!Objects.equals(newString, string)) {
            string = newString;
            setHasChanged();
        }
    }

    public Vector2f getPos() {
        return new Vector2f(pos);
    }

    public void setPos(Vector2f newPos) {
        if (newPos != pos) {
            pos = newPos;
            setHasChanged();
        }
    }

    public int getLoadedFontId() {
        return loadedFontId;
    }

    public void setLoadedFontId(int newFontId) {
        if (newFontId != loadedFontId) {
            loadedFontId = newFontId;
            setHasChanged();
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float newScale) {
        if (newScale != scale) {
            scale = newScale;
            setHasChanged();
        }
    }

    public int getYSpacing() {
        return ySpacing;
    }

    public void setYSpacing(int newYSpacing) {
        if (newYSpacing != ySpacing) {
            ySpacing = newYSpacing;
            setHasChanged();
        }
    }

    public Color getTextColour() {
        return textColour;
    }

    public void setTextColour(Color newColour) {
        if (!newColour.equals(textColour)) {
            textColour = newColour;
            setHasChanged();
        }
    }

    public Color getBgCol() {
        return bgCol;
    }

    public void setBgCol(Color newBgColour) {
        if (newBgColour != bgCol) {
            bgCol = newBgColour;
            setHasChanged();
        }
    }

    public boolean getSeamlessBgLines() {
        return seamlessBgLines;
    }

    public void setSeamlessBgLines(boolean isSeamlessBg) {
        if (isSeamlessBg != seamlessBgLines) {
            seamlessBgLines = isSeamlessBg;
            setHasChanged();
        }
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int newAlignment) {
        if (newAlignment < ALIGN_LEFT || newAlignment > ALIGN_MIDDLE) {
            Logging.warn("Alignment '%s' is not valid", newAlignment);
            return;
        }

        if (newAlignment != alignment) {
            alignment = newAlignment;
            setHasChanged();
        }
    }

    public Vector2f getBgMargin() {
        return bgMargin;
    }

    public void setBgMargin(Vector2f newBgMargin) {
        if (!newBgMargin.equals(bgMargin)) {
            bgMargin.set(newBgMargin);
            setHasChanged();
        }
    }

    public float getZ() {
        return z;
    }

    public void setZ(float newZ) {
        if (z != newZ) {
            z = newZ;
            setHasChanged();
        }
    }

    public void setHasChanged() {
        hasChanged = true;
        if (parent != null) parent.setHasBeenModified(true);
    }

    public static TextObject.MeshBuilder getDefaultMeshBuilder() {
        return new TextObject.MeshBuilder() {
            @Override
            public void build(TextObject textObject) {
                FontManager.LoadedFont font = FontManager.getLoadedFont(textObject.loadedFontId);
                int genericHeight = (int) (font.getLineHeight() * textObject.scale);
                int yAddition = genericHeight + textObject.ySpacing;

                String[] lines = textObject.string.split("\n");

                int accumulatedY = 0;
                for (String line : lines) {
                    if (line.isEmpty()) {
                        accumulatedY -= genericHeight + textObject.ySpacing;
                        continue;
                    }

                    float lineWidth = font.findLineWidth(line) * textObject.scale;
                    Vector2f linePos = new Vector2f(textObject.alignment == 0 ? textObject.pos.x : textObject.pos.x + (lineWidth * (1f / textObject.alignment)), textObject.pos.y + accumulatedY);

                    // line background
                    if (textObject.bgCol.getAlpha() > BoilerplateConstants.EPSILON)
                        buildBackground(textObject, linePos, lineWidth, yAddition);

                    buildLine(textObject, line, font, linePos);
                    accumulatedY -= yAddition;
                }
            }

            @Override
            public void buildBackground(TextObject textObject, Vector2f linePos, float lineWidth, int yAddition) {
                Vector2f size = new Vector2f(lineWidth, yAddition);
                if (!textObject.seamlessBgLines) size.y -= textObject.ySpacing;
                pushRect(textObject, linePos.sub(textObject.bgMargin, new Vector2f()), size.add(textObject.bgMargin.mul(2, new Vector2f())), new Vector2f(-1), new Vector2f(), textObject.bgCol, 1);
            }

            @Override
            public void buildLine(TextObject textObject, String line, FontManager.LoadedFont font, Vector2f linePos) {
                System.out.println(line);
                int accumulatedX = 0;
                for (char c : line.toCharArray()) {
                    FontManager.Glyph glyph = font.getGlyph(c);
                    Vector2f size = glyph.getSize().mul(textObject.scale);

                    if (c == ' ') {
                        accumulatedX -= (int) size.x;
                        continue;
                    }

                    Vector2f topLeft = linePos.add(accumulatedX, 0, new Vector2f());
                    pushRect(textObject, topLeft, size, glyph.texTopLeft, glyph.texSize, textObject.textColour, 0);
                    accumulatedX -= (int) size.x;
                }
            }

            @Override
            public void pushRect(TextObject textObject, Vector2f topLeft, Vector2f size, Vector2f texCoords, Vector2f texSize, Color colour, int zAddition) {
                pushVertex(textObject, topLeft, texCoords, colour, zAddition);
                pushVertex(textObject, topLeft.sub(size.x, 0, new Vector2f()), texCoords.add(texSize.x, 0, new Vector2f()), colour, zAddition);
                pushVertex(textObject, topLeft.sub(size, new Vector2f()), texCoords.add(texSize, new Vector2f()), colour, zAddition);
                pushVertex(textObject, topLeft.sub(0, size.y, new Vector2f()), texCoords.add(0, texSize.y, new Vector2f()), colour, zAddition);
                textObject.mesh.pushIndice(textObject.vertexCounter);
                textObject.mesh.pushIndice(textObject.vertexCounter + 3);
                textObject.mesh.pushIndice(textObject.vertexCounter + 2);
                textObject.mesh.pushIndice(textObject.vertexCounter);
                textObject.mesh.pushIndice(textObject.vertexCounter + 2);
                textObject.mesh.pushIndice(textObject.vertexCounter + 1);
                textObject.mesh.indicesCount += 6;
                textObject.vertexCounter += 4;
            }

            @Override
            public void pushVertex(TextObject textObject, Vector2f pos, Vector2f texCoords, Color colour, int zAddition) {
                for (VertexLayout.Element element : textObject.parent.vertexLayout.elements) {
                    switch (element.hint) {
                        case (VertexLayout.HINT_POSITION) ->
                                textObject.mesh.pushFloats(pos.x, pos.y, textObject.z + zAddition);
                        case (VertexLayout.HINT_TEX_POS) -> textObject.mesh.pushFloats(texCoords.x, texCoords.y);
                        case (VertexLayout.HINT_COLOUR) ->
                                textObject.mesh.pushFloats(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha());
                    }
                }
            }
        };
    }
}
