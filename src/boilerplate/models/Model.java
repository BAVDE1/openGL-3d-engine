package boilerplate.models;

import boilerplate.common.BoilerplateShaders;
import boilerplate.rendering.camera.CameraPerspective;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.utility.Logging;
import org.joml.Matrix4f;
import org.lwjgl.assimp.*;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

public class Model {
    public static final int MAX_BONE_INFLUENCE = 4;
    private static final ShaderProgram boneShader = new ShaderProgram();

    private static final int BONE_ID_NULL = -1;
    private static final int VERTEX_WEIGHT_NULL = 0;

    public interface ProcessAssimpVertexFunc {
        default void call(Model model, Mesh mesh, int vertexInx, AIVector3D.Buffer allVertices, AIVector3D.Buffer allNormals, AIVector3D.Buffer allTexPos, AIVector3D.Buffer allTangents, AIVector3D.Buffer allBitangents) {
        }
    }

    public interface ProcessShapeVertexFunc {
        default void call(Model model, Mesh mesh, int vertexInx, int texCoordInx, FloatBuffer allPoints, FloatBuffer allNormals, FloatBuffer allTexCoords) {
        }
    }

    public static class NodeData {
        String name;
        Matrix4f transform;
        List<NodeData> children = new ArrayList<>();

        @Override
        public String toString() {
            return "NodeData(" +
                    "name='" + name + '\'' +
                    ", children=" + children +
                    ')';
        }
    }

    public static class VertexWeight {
        int boneId = BONE_ID_NULL;
        float weight = VERTEX_WEIGHT_NULL;

        public VertexWeight() {
        }

        public VertexWeight(int boneId, float weight) {
            this.boneId = boneId;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return "VertexWeight(%s, %s)".formatted(boneId, weight);
        }
    }

    private final int UNLOADED = 0;
    private final int LOADED_MODEL = 1;
    private final int LOADED_SHAPE = 2;
    private int loaded = UNLOADED;

    public String modelFile;
    public String directory;
    public VertexLayout vertexLayout = defaultAssimpVertexLayout();
    public ProcessAssimpVertexFunc processAssimpVertexFunc = defaultAssimpProcessVertexFunc();
    public ProcessShapeVertexFunc processShapeVertexFunc = defaultShapeProcessVertexFunc();
    public Animator animator = new Animator(this);

    public final NodeData rootNode = new NodeData();
    public final Matrix4f rootNodeInvTrans = new Matrix4f();

    public List<Mesh> meshes = new ArrayList<>();
    public List<Material> materials = new ArrayList<>();
    public int boneCounter = 0;
    public final HashMap<String, Bone> boneMap = new HashMap<>();

    public Matrix4f modelTransform = new Matrix4f().identity();

    public boolean hasBones = false;
    public boolean renderWireFrame = false;
    public boolean renderBones = false;
    public VertexArray boneVa;

    public String boneMatrixUniform = "finalBonesMatrices";
    public String modelUniform = "model";

    public Model() {
    }

    public Model(VertexLayout vertexLayout) {
        this.vertexLayout = vertexLayout;
    }

    public void loadModel(String filePath, boolean flipTextures) {
        if (loaded != UNLOADED) {
            Logging.warn("This model has already been loaded, aborting.");
            return;
        }

        Logging.debug("Attempting to load model '%s'", filePath);

        File file = new File(filePath);
        if (!file.isFile()) {
            Logging.danger("Given filePath is not a valid file '%s'", filePath);
            return;
        }

        modelFile = filePath;
        directory = file.getParent();

        // https://assimp-docs.readthedocs.io/en/latest/usage/use_the_lib.html
        try (AIScene aiScene = Assimp.aiImportFile(filePath,
                Assimp.aiProcess_Triangulate |  // handles concave polygons
                        Assimp.aiProcess_GenSmoothNormals |
                        (flipTextures ? Assimp.aiProcess_FlipUVs : 0) |  // make upper left corner 0, 0
                        Assimp.aiProcess_CalcTangentSpace |
                        Assimp.aiProcess_JoinIdenticalVertices)) {
            boolean failed = true;
            String failedMsg = "no message";
            if (aiScene == null) {
                failedMsg = "scene is null";
            } else if (aiScene.mFlags() == Assimp.AI_SCENE_FLAGS_INCOMPLETE) {
                failedMsg = "scene is flagged as incomplete";
            } else if (aiScene.mRootNode() == null) {
                failedMsg = "root node is missing";
            } else failed = false;
            if (failed) {
                Logging.danger("Failed to load scene, %s\n%s", failedMsg, Assimp.aiGetErrorString());
                return;
            }

            MeshProcessorAssimp.processScene(this, aiScene);
            loaded = LOADED_MODEL;
        }
    }

    public void loadShape(ParShapesMesh shapesMesh) {
        loadShape(shapesMesh, new Material(), true);
    }

    /**
    * Reuse already loaded materials
    */
    public void loadShape(ParShapesMesh shapesMesh, int materialIndex) {
        loadShape(shapesMesh, materials.get(materialIndex), true);
    }

    public void loadShape(ParShapesMesh shapesMesh, int materialIndex, boolean freeMesh) {
        loadShape(shapesMesh, materials.get(materialIndex), freeMesh);
    }

    public void loadShape(ParShapesMesh shapesMesh, Material material, boolean freeMesh) {
        if (loaded == LOADED_MODEL) {
            Logging.warn("This model has already been loaded from a file, aborting.");
            return;
        }

        MeshProcessorShapes.processShape(this, shapesMesh, material);
        if (freeMesh) ParShapes.par_shapes_free_mesh(shapesMesh);
        loaded = LOADED_SHAPE;
    }

    public void pushVertexBoneIds(Mesh mesh, int vertexInx) {
        List<VertexWeight> vwList = mesh.vertexWeights.get(vertexInx);
        for (int i = 0; i < MAX_BONE_INFLUENCE; i++) {
            if (vwList != null && i < vwList.size()) mesh.pushInt(vwList.get(i).boneId);
            else mesh.pushInt(BONE_ID_NULL);
        }
    }

    public void pushVertexBoneWeights(Mesh mesh, int vertexInx) {
        List<VertexWeight> vwList = mesh.vertexWeights.get(vertexInx);
        for (int i = 0; i < MAX_BONE_INFLUENCE; i++) {
            if (vwList != null && i < vwList.size()) mesh.pushFloat(vwList.get(i).weight);
            else mesh.pushFloat(VERTEX_WEIGHT_NULL);
        }
    }

    public void setupBoneRendering(CameraPerspective camera3d) {
        if (boneCounter == 0) return;

        if (!boneShader.isSetup()) {
            boneShader.genProgram();
            boneShader.attachShader(BoilerplateShaders.safeFormat(BoilerplateShaders.ModelBoneVertex, "% ", camera3d.uniformBlockName), GL45.GL_VERTEX_SHADER, "BoilerplateShaders.ModelBoneVertex");
            boneShader.attachShader(BoilerplateShaders.ModelBoneFragment, GL45.GL_FRAGMENT_SHADER, "BoilerplateShaders.ModelBoneFragment");
            boneShader.linkProgram();
            camera3d.bindShaderToUniformBlock(boneShader);
            boneShader.unbind();
        }

        boneVa = new VertexArray(true);
        VertexArrayBuffer boneVb = new VertexArrayBuffer(true);

        boneVa.bindBuffer(boneVb);
        boneVa.pushLayout(new VertexLayout(
                new VertexLayout.Element(VertexLayout.TYPE_INT, 1)
        ));

        ByteBuffer data = MemoryUtil.memAlloc(boneMap.size() * Integer.BYTES);
        for (Bone bone : boneMap.values()) data.putInt(bone.id);
        boneVb.bufferData(data);
    }

    public void updateAnimation(double dt) {
        animator.update((float) dt);
    }

    public void draw(ShaderProgram shaderProgram, int textureSlotStart) {
        draw(shaderProgram, textureSlotStart, modelTransform);
    }

    public void draw(ShaderProgram shaderProgram, int textureSlotStart, Matrix4f modelTransformOverride) {
        shaderProgram.bind();

        for (int i = 0; i < boneCounter; i++) {
            shaderProgram.uniformMatrix4f(boneMatrixUniform + "[%s]".formatted(i), animator.finalBoneMatrices[i]);
        }

        shaderProgram.uniformMatrix4f(modelUniform, modelTransformOverride);
        for (Mesh mesh : meshes) mesh.draw(shaderProgram, textureSlotStart);
        GL45.glActiveTexture(GL45.GL_TEXTURE0);  // reset
        if (renderBones) renderBones();
    }

    private void renderBones() {
        boneShader.uniformMatrix4f("model", modelTransform);
        for (int i = 0; i < boneCounter; i++) {
            boneShader.uniformMatrix4f("finalBonesMatrices[%s]".formatted(i), animator.finalBoneMatrices[i]);
        }

        GL45.glPointSize(10);
        boneShader.bind();
        GL45.glDepthFunc(GL45.GL_ALWAYS);
        boneVa.drawArrays(GL45.GL_POINTS, boneMap.size());
        GL45.glDepthFunc(GL45.GL_LESS);
        boneShader.unbind();
        GL45.glPointSize(1);
    }

    public void renderWireFrame(boolean val) {
        if (val == renderWireFrame) return;
        renderWireFrame = val;
        int mode = val ? GL45.GL_LINES : GL45.GL_TRIANGLES;
        for (Mesh mesh : meshes) mesh.renderMode = mode;
    }

    public void renderBones(boolean val) {
        if (!hasBones || val == renderBones) return;
        renderBones = val;
    }

    public Bone getBone(String boneName) {
        if (!boneMap.containsKey(boneName)) return null;
        return boneMap.get(boneName);
    }

    public String getModelFile() {
        return modelFile;
    }

    public String getModelDirectory() {
        return directory;
    }

    public Material getMaterial(int index) {
        return materials.get(index);
    }

    public static VertexLayout defaultAssimpVertexLayout() {
        return new VertexLayout(
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_POSITION),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_NORMAL),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_TANGENT),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 2, VertexLayout.HINT_TEX_POS),
                new VertexLayout.Element(VertexLayout.TYPE_INT, MAX_BONE_INFLUENCE, VertexLayout.HINT_BONE_IDS),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, MAX_BONE_INFLUENCE, VertexLayout.HINT_BONE_WEIGHTS, true),
                new VertexLayout.Element(VertexLayout.TYPE_INT, 1, VertexLayout.HINT_CUSTOM_0)
        );
    }

    public static VertexLayout defaultShapeVertexLayout() {
        return new VertexLayout(
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_POSITION),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_NORMAL)
        );
    }

    public static ProcessAssimpVertexFunc defaultAssimpProcessVertexFunc() {
        return new ProcessAssimpVertexFunc() {
            @Override
            public void call(Model model, Mesh mesh, int vertexInx, AIVector3D.Buffer allVertices, AIVector3D.Buffer allNormals, AIVector3D.Buffer allTexPos, AIVector3D.Buffer allTangents, AIVector3D.Buffer allBitangents) {
                for (VertexLayout.Element element : model.vertexLayout.elements) {
                    switch (element.hint) {
                        case (VertexLayout.HINT_POSITION) -> mesh.pushVector3D(allVertices.get(vertexInx));
                        case (VertexLayout.HINT_NORMAL) -> mesh.pushVector3D(allNormals.get(vertexInx));
                        case (VertexLayout.HINT_TANGENT) -> mesh.pushVector3D(allTangents.get(vertexInx));
                        case (VertexLayout.HINT_BITANGENT) -> mesh.pushVector3D(allBitangents.get(vertexInx));
                        case (VertexLayout.HINT_TEX_POS) -> mesh.pushVector2D(allTexPos.get(vertexInx));
                        case (VertexLayout.HINT_BONE_IDS) -> model.pushVertexBoneIds(mesh, vertexInx);
                        case (VertexLayout.HINT_BONE_WEIGHTS) -> model.pushVertexBoneWeights(mesh, vertexInx);
                        case (VertexLayout.HINT_CUSTOM_0) ->
                                mesh.pushInt(model.hasBones ? 0 : 1);  // it is static if no bones
                        default ->
                                throw new RuntimeException("Element from given VertexLayout is missing a hint value.");
                    }
                }
            }
        };
    }

    public static ProcessShapeVertexFunc defaultShapeProcessVertexFunc() {
        return new ProcessShapeVertexFunc() {
            @Override
            public void call(Model model, Mesh mesh, int vertexInx, int texCoordInx, FloatBuffer allPoints, FloatBuffer allNormals, FloatBuffer allTexCoords) {
                for (VertexLayout.Element element : model.vertexLayout.elements) {
                    switch (element.hint) {
                        case (VertexLayout.HINT_POSITION) -> mesh.pushFloats(allPoints.get(vertexInx), allPoints.get(vertexInx+1), allPoints.get(vertexInx+2));
                        case (VertexLayout.HINT_NORMAL) -> {
                            if (allNormals == null) {
                                Logging.danger("A Normal was expected in the layout, but there are no normals generated for the shape.");
                                continue;
                            }
                            mesh.pushFloats(allNormals.get(vertexInx), allNormals.get(vertexInx + 1), allNormals.get(vertexInx + 2));
                        }
                        case (VertexLayout.HINT_TEX_POS) -> {
                            if (allTexCoords == null) {
                                Logging.danger("A Tex Coord was expected in the layout, but there are no tex coords generated for the shape.");
                                continue;
                            }
                            mesh.pushFloats(allTexCoords.get(texCoordInx), allTexCoords.get(texCoordInx + 1));
                        }
                    }
                }
            }
        };
    }

    @Override
    public String toString() {
        return "Model(" +
                "modelFile='" + modelFile + '\'' +
                ')';
    }
}
