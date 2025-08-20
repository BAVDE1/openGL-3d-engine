package boilerplate.rendering.builders;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Creates shapes to be passed to a BufferBuilder.
 */
public class Shape2d {
    public static class Poly2d {
        public ShapeMode mode = null;
        public List<Vector2f> points;
        public Vector2f pos = new Vector2f();

        public Poly2d(Vector2f... points) {
            this.points = List.of(points);
        }

        public Poly2d(ShapeMode mode, Vector2f... points) {
            this(points);
            this.mode = mode;
        }

        public Poly2d addPos(Vector2f newPos) {
            this.pos = newPos;
            return this;
        }

        public List<float[]> toArray() {
            List<float[]> l = new ArrayList<>(List.of());
            for (int i = 0; i < points.size(); i++) {
                l.add(i, new float[] {points.get(i).x, points.get(i).y});
            }
            return l;
        }

        @Override
        public String toString() {
            return "Poly2d(%s, %s)".formatted(pos, points);
        }
    }

    public static Poly2d createRect(Vector2f topLeft, Vector2f size) {
        return new Poly2d(
                topLeft.add(size, new Vector2f()),
                topLeft.add(0, size.y, new Vector2f()),
                topLeft.add(size.x, 0, new Vector2f()),
                topLeft
        );
    }

    public static Poly2d createRect(Vector2f topLeft, Vector2f size, ShapeMode mode) {
        Poly2d p = createRect(topLeft, size);
        p.mode = mode;
        return p;
    }

    public static Poly2d createLine(Vector2f point1, Vector2f point2, float thickness) {
        Vector2f perp = new Vector2f();
        point2.sub(point1, perp).normalize();
        perp.perpendicular().mul(thickness * .5f);
        return new Poly2d(
            point1.add(perp, new Vector2f()), point1.sub(perp, new Vector2f()),
            point2.add(perp, new Vector2f()), point2.sub(perp, new Vector2f())
        );
    }
    public static Poly2d createLine(Vector2f point1, Vector2f point2, float thickness, ShapeMode mode) {
        Poly2d p = createLine(point1, point2, thickness);
        p.mode = mode;
        return p;
    }

    public static Poly2d createRectOutline(Vector2f topLeft, Vector2f size, float thickness) {
        Vector2f pos = topLeft.add(size.div(2, new Vector2f()), new Vector2f());

        topLeft = topLeft.sub(pos, new Vector2f());
        Vector2f topRight = topLeft.add(size.x, 0, new Vector2f());
        Vector2f btmRight = topLeft.add(size, new Vector2f());
        Vector2f btmLeft = topLeft.add(0, size.y, new Vector2f());

        return new Poly2d(
                topLeft,  topLeft.add(thickness, thickness, new Vector2f()),
                topRight, topRight.add(-thickness, thickness, new Vector2f()),
                btmRight, btmRight.sub(thickness, thickness, new Vector2f()),
                btmLeft,  btmLeft.sub(-thickness, thickness, new Vector2f()),
                topLeft,  topLeft.add(thickness, thickness, new Vector2f())
        ).addPos(pos);
    }

    public static Poly2d createRectOutline(Vector2f topLeft, Vector2f size, float thickness, ShapeMode mode) {
        Poly2d p = createRectOutline(topLeft, size, thickness);
        p.mode = mode;
        return p;
    }

    public static void sortPoints(Poly2d p) {
        sortPoints(p, new Vector2f(1));
    }

    /** Sort points of a polygon so they are all listed in a clockwise direction */
    public static void sortPoints(Poly2d p, Vector2f center) {
        if (center.x == 0 && center.y == 0) center = new Vector2f(1);
        final int fidelity = 2;

        // functions
        interface Func {void call(ArrayList<ArrayList<Integer>> radixGroups);}
        Func buildRadixGroups = radixGroups -> {
            radixGroups.clear();
            for (int i = 0; i < 10; i++) {radixGroups.add(new ArrayList<>());}
        };

        interface Func2 {void call(int[] arr, ArrayList<ArrayList<Integer>> radixGroups);}
        Func2 reOrderIndexes = (arr, radixGroups) -> {
            int i = 0;
            for (ArrayList<Integer> group : radixGroups) {
                for (int index : group) arr[i++] = index;
            }
        };

        // pre calculate comparisons
        int[] indexOrder = new int[p.points.size()];
        String[] comparableItems = new String[p.points.size()];
        for(int i = 0; i < p.points.size(); i++) {
            Vector2f point = p.points.get(i);
            float onSide = (point.x * center.y) - (point.y * center.x);
            float dot = point.sub(center, new Vector2f()).normalize().dot(center.normalize());
            dot = onSide > 0 ? dot+3 : (-dot)+1;

            indexOrder[i] = i;
            comparableItems[i] = String.valueOf((int) Math.floor(dot * Math.pow(10, fidelity)));
        }

        // radix sort all the points
        ArrayList<ArrayList<Integer>> radixGroups = new ArrayList<>();
        buildRadixGroups.call(radixGroups);

        int on_digit = 0;
        int i = 0;
        while(i < p.points.size()) {
            int index = indexOrder[i];
            String comparison = comparableItems[index];

            int groupInto = 0;
            try {groupInto = comparison.charAt(fidelity - on_digit) - '0';}
            catch (IndexOutOfBoundsException _){}
            radixGroups.get(groupInto).add(index);

            // next digit
            if (++i == p.points.size()) {
                i = 0;
                on_digit++;
                reOrderIndexes.call(indexOrder, radixGroups);
                buildRadixGroups.call(radixGroups);
            }

            // finish
            if (fidelity - on_digit < 0) {
                Vector2f[] newPoints = new Vector2f[p.points.size()];
                for (int j = 0; j < p.points.size(); j++) {
                    newPoints[j] = p.points.get(indexOrder[j]);
                }
                p.points = List.of(newPoints);
                break;
            }
        }
    }

    /** Finds average of all points */
    public static Vector2f findCenter(List<Vector2f> points) {
        ListIterator<Vector2f> iterator = points.listIterator();
        Vector2f avg = new Vector2f(iterator.next());
        while(iterator.hasNext()) avg.add(iterator.next());
        return avg.div(points.size());
    }
}
