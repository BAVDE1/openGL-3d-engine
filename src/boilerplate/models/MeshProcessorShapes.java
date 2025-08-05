package boilerplate.models;

import org.lwjgl.util.par.ParShapesMesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class MeshProcessorShapes {
    public static void processShape(Model model, ParShapesMesh shapesMesh) {
        Mesh mesh = new Mesh(model.vertexLayout);

        mesh.indicesCount = shapesMesh.ntriangles() * 3;
        mesh.allocateMemory(calculateVertexDataBytes(model, shapesMesh), mesh.indicesCount * Integer.BYTES);

        processIndices(mesh, shapesMesh);
        processVertices(mesh, shapesMesh);

        model.meshes = new Mesh[] {mesh};
    }

    private static void processVertices(Mesh mesh, ParShapesMesh shapesMesh) {
        System.out.println(shapesMesh.ntriangles());
        System.out.println(shapesMesh.npoints());
//        FloatBuffer fb = shapesMesh.points(shapesMesh.npoints() * 3);
//        IntBuffer fb2 = shapesMesh.triangles(shapesMesh.ntriangles() * 3);
//        for (int i = 0; i < shapesMesh.npoints() * 3; i++) System.out.println(fb.get(i));
//        for (int i = 0; i < shapesMesh.ntriangles() * 3; i++) System.out.println(fb2.get(i));
    }

    private static void processIndices(Mesh mesh, ParShapesMesh shapesMesh) {
        IntBuffer iBuff = shapesMesh.triangles(mesh.indicesCount);
        for (int i = 0; i < mesh.indicesCount; i++) mesh.pushIndice(iBuff.get(i));
    }

    private static int calculateVertexDataBytes(Model model, ParShapesMesh shapesMesh) {
        return shapesMesh.npoints() * model.vertexLayout.stride;
    }
}
