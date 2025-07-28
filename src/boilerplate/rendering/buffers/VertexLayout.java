package boilerplate.rendering.buffers;

import boilerplate.utility.Logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class VertexLayout {
    public static class Element {
        public int type;
        public int count;
        public int hint = HINT_NULL;
        public boolean normalized = false;

        public Element(int type, int count) {
            if (!TYPE_STRING_MAP.containsKey(type)) {
                throw new RuntimeException("Invalid type: %s\nExpected any: %s".formatted(type, TYPE_STRING_MAP));
            }
            this.type = type;
            this.count = count;
        }

        public Element(int type, int count, boolean normalized) {
            this(type, count);
            this.normalized = normalized;
        }

        public Element(int type, int count, int hint) {
            this(type, count);
            if (!HINT_STRING_MAP.containsKey(hint)) {
                throw new RuntimeException("Invalid hint: %s\nExpected any: %s".formatted(hint, HINT_STRING_MAP));
            }
            this.hint = hint;
        }

        public Element(int type, int count, int hint, boolean normalized) {
            this(type, count, hint);
            this.normalized = normalized;
        }

        public int getByteSizeForType() {
            return getByteSizeForType(type);
        }

        public static int getByteSizeForType(int type) {
            return switch (type) {
                case (GL_FLOAT) -> Float.BYTES;
                case (GL_INT), (GL_UNSIGNED_INT) -> Integer.BYTES;
                case (GL_BYTE), (GL_UNSIGNED_BYTE) -> 1;
                default -> {
                    Logging.danger("Can't find case for type '%s', no bytes returned", type);
                    yield 0;
                }
            };
        }

        public boolean isIntType() {
            return type == GL_INT || type == GL_UNSIGNED_INT || type == GL_BYTE || type == GL_UNSIGNED_BYTE;
        }
    }

    public static final int TYPE_FLOAT = GL_FLOAT;
    public static final int TYPE_INT = GL_INT;
    public static final int TYPE_UNSIGNED_INT = GL_UNSIGNED_INT;
    public static final int TYPE_BYTE = GL_BYTE;
    public static final int TYPE_UNSIGNED_BYTE = GL_UNSIGNED_BYTE;

    public static final int HINT_NULL = 0;
    public static final int HINT_POSITION = 1;
    public static final int HINT_NORMAL = 2;
    public static final int HINT_TANGENT = 3;
    public static final int HINT_BITANGENT = 4;
    public static final int HINT_TEX_POS = 5;
    public static final int HINT_BONE_IDS = 6;
    public static final int HINT_BONE_WEIGHTS = 7;
    public static final int HINT_CUSTOM_0 = 100;
    public static final int HINT_CUSTOM_1 = 101;
    public static final int HINT_CUSTOM_2 = 102;
    public static final int HINT_CUSTOM_3 = 103;
    public static final int HINT_CUSTOM_4 = 104;
    public static final int HINT_CUSTOM_5 = 105;
    public static final int HINT_CUSTOM_6 = 106;
    public static final int HINT_CUSTOM_7 = 107;
    public static final int HINT_CUSTOM_8 = 108;
    public static final int HINT_CUSTOM_9 = 109;

    public static final Map<Integer, String> TYPE_STRING_MAP = buildTypeStringMap();
    public static final Map<Integer, String> HINT_STRING_MAP = buildHintStringMap();

    public final ArrayList<Element> elements = new ArrayList<>();
    public int totalItems = 0;
    public int stride = 0;

    public VertexLayout() {

    }

    public VertexLayout(int[] pushFloats) {
        for (int f : pushFloats) pushFloat(f);
    }

    public VertexLayout(Element... elements) {
        for (Element element : elements) push(element);
    }

    private static Map<Integer, String> buildTypeStringMap() {
        Map<Integer, String> m = new HashMap<>();
        m.put(TYPE_FLOAT, "float");
        m.put(TYPE_INT, "int");
        m.put(TYPE_UNSIGNED_INT, "unsigned_int");
        m.put(TYPE_BYTE, "byte");
        m.put(TYPE_UNSIGNED_BYTE, "unsigned_byte");
        return m;
    }

    private static Map<Integer, String> buildHintStringMap() {
        Map<Integer, String> m = new HashMap<>();
        m.put(HINT_NULL, "no hint");
        m.put(HINT_POSITION, "position");
        m.put(HINT_NORMAL, "normal");
        m.put(HINT_TANGENT, "tangent");
        m.put(HINT_BITANGENT, "bitangent");
        m.put(HINT_TEX_POS, "tex_pos");
        m.put(HINT_BONE_IDS, "bone_ids");
        m.put(HINT_BONE_WEIGHTS, "bone_weights");
        m.put(HINT_CUSTOM_0, "custom 0");
        m.put(HINT_CUSTOM_1, "custom 1");
        m.put(HINT_CUSTOM_2, "custom 2");
        m.put(HINT_CUSTOM_3, "custom 3");
        m.put(HINT_CUSTOM_4, "custom 4");
        m.put(HINT_CUSTOM_5, "custom 5");
        m.put(HINT_CUSTOM_6, "custom 6");
        m.put(HINT_CUSTOM_7, "custom 7");
        m.put(HINT_CUSTOM_8, "custom 8");
        m.put(HINT_CUSTOM_9, "custom 9");
        return m;
    }

    private void push(Element element) {
        if (!List.of(1, 2, 3, 4).contains(element.count)) {
            throw new RuntimeException("An element must have a count value of 1, 2, 3, or 4. A count of %s was given. Use multiple elements if you must.".formatted(element.count));
        }
        elements.add(element);
        stride += element.count * Element.getByteSizeForType(element.type);
        totalItems += element.count;
    }

    public void pushFloat(int count) {
        push(new Element(GL_FLOAT, count, false));
    }

    public void pushInt(int count) {
        push(new Element(GL_INT, count, false));
    }

    public void pushUnsignedInt(int count) {
        push(new Element(GL_UNSIGNED_INT, count, false));
    }

    public void pushByte(int count) {
        push(new Element(GL_BYTE, count, false));
    }

    public void pushUnsignedByte(int count) {
        push(new Element(GL_UNSIGNED_BYTE, count, false));
    }

    public int getTotalItems() {
        return totalItems;
    }

    public boolean hasElementWithHint(int hint) {
        for (Element e : elements) {
            if (e.hint == hint) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder().append("VertexLayout(");
        for (int i = 0; i < elements.size(); i++) {
            if (i != 0) s.append(", ");
            Element elem = elements.get(i);
            s.append("%s %s %s".formatted(HINT_STRING_MAP.get(elem.hint), TYPE_STRING_MAP.get(elem.type), elem.count));
        }
        return s.append(")").toString();
    }
}
