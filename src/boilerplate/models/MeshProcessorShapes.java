package boilerplate.models;

import org.lwjgl.util.par.ParShapesMesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class MeshProcessorShapes {
    public static void processShape(Model model, ParShapesMesh shapesMesh, Material material) {
        Mesh mesh = new Mesh(model.vertexLayout);
        mesh.setMaterial(material);

        mesh.indicesCount = shapesMesh.ntriangles() * 3;
        mesh.allocateMemory(calculateVertexDataBytes(model, shapesMesh), mesh.indicesCount * Integer.BYTES);

        processIndices(mesh, shapesMesh);
        processVertices(model, mesh, shapesMesh);

        mesh.finalizeMesh();
        model.meshes = new Mesh[] {mesh};
        model.materials = new Material[] {material};
    }

    private static void processVertices(Model model, Mesh mesh, ParShapesMesh shapesMesh) {
        int numPoints = shapesMesh.npoints() * 3;
        FloatBuffer allPoints = shapesMesh.points(numPoints);
        FloatBuffer allNormals = shapesMesh.normals(numPoints);
        FloatBuffer allTexCoords = shapesMesh.tcoords(numPoints);

        for (int i = 0; i < shapesMesh.npoints(); i++) {
            model.processShapeVertexFunc.call(model, mesh, i * 3, i * 2, allPoints, allNormals, allTexCoords);
        }
    }

    private static void processIndices(Mesh mesh, ParShapesMesh shapesMesh) {
        IntBuffer iBuff = shapesMesh.triangles(mesh.indicesCount);
        for (int i = 0; i < mesh.indicesCount; i++) mesh.pushIndice(iBuff.get(i));
    }

    private static int calculateVertexDataBytes(Model model, ParShapesMesh shapesMesh) {
        return shapesMesh.npoints() * model.vertexLayout.stride;
    }
}
