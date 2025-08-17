package boilerplate.rendering.builders;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.nio.FloatBuffer;

public class BufferBuilder2f extends BufferBuilder {
    public BufferBuilder2f() {this(DEFAULT_SIZE, false, 0);}
    public BufferBuilder2f(int size) {this(size, false, 0);}
    public BufferBuilder2f(boolean autoResize) {this(DEFAULT_SIZE, autoResize, 0);}
    public BufferBuilder2f(boolean autoResize, int additionalVertFloats) {this(DEFAULT_SIZE, autoResize, additionalVertFloats);}
    public BufferBuilder2f(int size, boolean autoResize, int additionalVertFloats){
        floats = new float[size];
        this.size = size;
        this.autoResize = autoResize;
        setAdditionalVertFloats(additionalVertFloats);
    }

    @Override
    public int getPosFloatCount() {
        return 2;
    }

    @Override
    public void setPosFloatCount(int count) throws IllegalStateException {
        throw new IllegalStateException("Cannot override the position float count for this BufferBuilder");
    }

    public void pushSeparatedPolygon(Shape2d.Poly2d p) {
        shouldNextBeSeparated = true;
        pushPolygon(p);
    }

    /** first, second, third, ... */
    public void pushPolygon(Shape2d.Poly2d p) {
        float[] verts = new float[p.points.size() * floatCountPerVert];
        for (int i = 0; i < p.points.size(); i++) {
            int inx = i * floatCountPerVert;
            Vector2f point = p.points.get(i);
            FloatBuffer pointPos = BufferUtils.createFloatBuffer(2);
            point.add(p.pos, new Vector2f()).get(pointPos);
            addPointsToArray(verts, inx, i, pointPos, p.mode);
        }
        pushRawFloats(verts);
    }

    public void pushSeparatedPolygonSorted(Shape2d.Poly2d p) {
        shouldNextBeSeparated = true;
        pushPolygonSorted(p);
    }

    /** first, last, first+1, last-1, ... */
    public void pushPolygonSorted(Shape2d.Poly2d p) {
        float[] verts = new float[p.points.size() * floatCountPerVert];
        for (int i = 0; i < p.points.size(); i++) {
            int inx = i * floatCountPerVert;
            int offset = (int) (i / 2f);  // floor

            // choose which point to add
            Vector2f point;
            if (i % 2 == 0) point = p.points.get(offset);  // front
            else point = p.points.get(p.points.size()-1 - offset);  // back
            FloatBuffer pointPos = BufferUtils.createFloatBuffer(2);
            point.add(p.pos, new Vector2f()).get(pointPos);
            addPointsToArray(verts, inx, i, pointPos, p.mode);
        }
        pushRawFloats(verts);
    }

    /** Circles should be rendered as instanced GL_TRIANGLES */
    public void pushCircle(Vector2f pos, float radius, Color col) {
        pushCircle(pos, radius, 0, col);
    }

    /** Circles should be rendered as instanced GL_TRIANGLES */
    public void pushCircle(Vector2f pos, float radius, float outline, Color col) {
        pushRawFloats(new float[] {
                pos.x, pos.y, radius, outline, col.getRed(), col.getGreen(), col.getBlue()
        });
    }
}
