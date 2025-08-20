package boilerplate.rendering.builders;

import java.util.List;

public abstract class ShapeMode {
    /**
     * Appends the given vars to end of each vertex
     */
    public static class Append extends ShapeMode {
        public float[] vars;

        public Append(float[] varsToAppend) {
            vars = varsToAppend;
        }
    }

    /**
     * Unpacks the given vars to the end of each vertex (wraps)
     */
    public static class Unpack extends ShapeMode {
        float[][] unpackVars;

        public Unpack(float[]... unpackVars) {
            this.unpackVars = unpackVars;
        }

        public Unpack(List<float[]> unpackVars) {
            this.unpackVars = unpackVars.toArray(new float[0][]);
        }
    }

    /**
     * Unpacks the unpackVars for each vertex in order (wraps when it reaches the end)
     * And then appends appendVars for each vertex
     */
    public static class UnpackAppend extends ShapeMode {
        Unpack unpack;
        Append append;

        public UnpackAppend(List<float[]> unpackVars, float[] appendVars) {
            unpack = new Unpack(unpackVars);
            append = new Append(appendVars);
        }
    }

    /**
     * same as UnpackAppend, but reversed
     */
    public static class AppendUnpack extends ShapeMode {
        Unpack unpack;
        Append append;

        public AppendUnpack(float[] appendVars, List<float[]> unpackVars) {
            unpack = new Unpack(unpackVars);
            append = new Append(appendVars);
        }
    }
}
