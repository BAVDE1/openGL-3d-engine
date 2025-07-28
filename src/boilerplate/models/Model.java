package boilerplate.models;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.BoilerplateShaders;
import boilerplate.rendering.Camera3d;
import boilerplate.rendering.Renderer;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.processing.SupportedSourceVersion;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class Model {
    public static final int MAX_BONE_INFLUENCE = 4;
    private static final ShaderProgram boneShader = new ShaderProgram();

    private static final int BONE_ID_NULL = -1;
    private static final int VERTEX_WEIGHT_NULL = 0;

    public interface ProcessVertexFunc {
        default void call(Model model, Mesh mesh, int vertexInx, AIVector3D.Buffer allVertices, AIVector3D.Buffer allNormals, AIVector3D.Buffer allTexPos, AIVector3D.Buffer allTangents, AIVector3D.Buffer allBitangents) {
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
                    default -> throw new RuntimeException("Element from given VertexLayout is missing a hint value.");
                }
            }
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

    private String modelFile;
    private String directory;
    public VertexLayout vertexLayout = defaultVertexLayout();
    public ProcessVertexFunc processVertexFunc = defaultProcessVertexFunc();
    public Animator animator = new Animator(this);

    private final NodeData rootNode = new NodeData();
    public final Matrix4f rootNodeInvTrans = new Matrix4f();

    private Mesh[] meshes;
    private Material[] materials;
    private int boneCounter = 0;
    private final HashMap<String, Bone> boneMap = new HashMap<>();

    public Matrix4f modelTransform = new Matrix4f().identity();

    private boolean hasBones = false;
    private boolean renderWireFrame = false;
    private boolean renderBones = false;
    private VertexArray boneVa;

    public String boneMatrixUniform = "finalBonesMatrices";
    public String modelUniform = "model";

    public Model() {
    }

    public Model(VertexLayout vertexLayout) {
        this.vertexLayout = vertexLayout;
    }

    public void loadModel(String filePath, boolean flipTextures) {
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
                        Assimp.aiProcess_CalcTangentSpace)) {
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

            processScene(aiScene);
        }
    }

    private void processScene(AIScene rootAiScene) {
        // materials
        materials = new Material[rootAiScene.mNumMaterials()];
        processMaterials(rootAiScene);

        // node hierarchy & model
        meshes = new Mesh[rootAiScene.mNumMeshes()];
        processNode(rootAiScene.mRootNode(), rootAiScene, rootNode);
        rootNode.transform.invert(rootNodeInvTrans);

        // animations
        animator.init(boneCounter, rootNode);
        processAnimations(rootAiScene);
    }

    /**
     * Recursively process a node and its children
     */
    private void processNode(AINode aiNode, AIScene rootAiScene, NodeData nodeDest) {
        if (aiNode == null) {
            Logging.warn("Node is null, scene: %s", rootAiScene);
            return;
        }

        // node hierarchy
        nodeDest.name = aiNode.mName().dataString();
        nodeDest.transform = MathUtils.AIMatrixToMatrix(aiNode.mTransformation());

        // meshes
        PointerBuffer allMeshes = rootAiScene.mMeshes();
        IntBuffer nodeMeshes = aiNode.mMeshes();  // indexes of scene's meshes
        if (allMeshes != null && nodeMeshes != null) {
            while (nodeMeshes.hasRemaining()) {
                int meshInx = nodeMeshes.get();
                try (AIMesh aiMesh = AIMesh.create(allMeshes.get(meshInx))) {
                    meshes[meshInx] = processMesh(aiMesh);
                }
            }
        }

        // process children
        PointerBuffer children = aiNode.mChildren();
        if (children == null) return;  // no children :(

        for (int i = 0; i < aiNode.mNumChildren(); i++) {
            try (AINode child = AINode.create(children.get(i))) {
                NodeData childNode = new NodeData();
                processNode(child, rootAiScene, childNode);

                // assign bone parents
                if (boneMap.containsKey(nodeDest.name) && boneMap.containsKey(childNode.name)) {
                    boneMap.get(childNode.name).parent = boneMap.get(nodeDest.name);
                }

                nodeDest.children.add(childNode);
            }
        }
    }

    private Mesh processMesh(AIMesh aiMesh) {
        Mesh mesh = new Mesh(vertexLayout);
        mesh.indicesCount = findIndicesCount(aiMesh);
        mesh.allocateMemory(calculateVertexDataBytes(aiMesh), mesh.indicesCount * Integer.BYTES);

        processBones(mesh, aiMesh);
        processVertices(mesh, aiMesh);
        processFaces(mesh, aiMesh);
        processMeshMaterial(mesh, aiMesh);

        mesh.finalizeMesh();
        return mesh;
    }

    private int calculateVertexDataBytes(AIMesh aiMesh) {
        return aiMesh.mNumVertices() * vertexLayout.stride;
    }

    private int findIndicesCount(AIMesh aiMesh) {
        int count = 0;
        for (int fi = 0; fi < aiMesh.mNumFaces(); fi++) {
            count += aiMesh.mFaces().get(fi).mNumIndices();
        }
        return count;
    }

    /**
     * After process bones
     */
    private void processVertices(Mesh mesh, AIMesh aiMesh) {
        AIVector3D.Buffer allVertices = aiMesh.mVertices();
        AIVector3D.Buffer allNormals = aiMesh.mNormals();
        AIVector3D.Buffer allTexPos = aiMesh.mTextureCoords(0);
        AIVector3D.Buffer allTangents = aiMesh.mTangents();
        AIVector3D.Buffer allBitangents = aiMesh.mTangents();

        // data checks
        if (allNormals == null && vertexLayout.hasElementWithHint(VertexLayout.HINT_NORMAL))
            throw new RuntimeException("Given vertex layout contains normals, but mesh data does not contain normals.");
        if (allTexPos == null && vertexLayout.hasElementWithHint(VertexLayout.HINT_TEX_POS))
            throw new RuntimeException("Given vertex layout contains texture coords, but mesh data does not contain texture coords.");

        // process
        for (int i = 0; i < aiMesh.mNumVertices(); i++) {
            processVertexFunc.call(this, mesh, i, allVertices, allNormals, allTexPos, allTangents, allBitangents);
        }
    }

    private void processFaces(Mesh mesh, AIMesh aiMesh) {
        AIFace.Buffer allFaces = aiMesh.mFaces();

        while (allFaces.hasRemaining()) {
            IntBuffer indices = allFaces.get().mIndices();
            while (indices.hasRemaining()) mesh.pushIndice(indices.get());
        }
    }

    private void processBones(Mesh mesh, AIMesh aiMesh) {
        PointerBuffer allBones = aiMesh.mBones();
        if (allBones == null) return;  // no bones

        hasBones = allBones.hasRemaining();
        while (allBones.hasRemaining()) {
            try (AIBone aiBone = AIBone.create(allBones.get())) {
                String boneName = aiBone.mName().dataString();
                Bone bone = boneMap.computeIfAbsent(boneName, _ -> new Bone(boneCounter++, aiBone));
                processBoneWeights(mesh, bone, aiBone);
            }
        }
    }

    private void processBoneWeights(Mesh mesh, Bone bone, AIBone aiBone) {
        AIVertexWeight.Buffer weights = aiBone.mWeights();
        while (weights.hasRemaining()) {
            AIVertexWeight aiWeight = weights.get();
            int vertexId = aiWeight.mVertexId();
            float weight = aiWeight.mWeight();
            if (weight < BoilerplateConstants.EPSILON) continue;  // no need to even add the bone

            List<VertexWeight> vwList = mesh.vertexWeights.computeIfAbsent(vertexId, _ -> new ArrayList<>());
            vwList.add(new VertexWeight(bone.id, weight));
        }
    }

    /**
     * After process bones
     */
    private void processAnimations(AIScene rootAIScene) {
        PointerBuffer allAnimations = rootAIScene.mAnimations();
        if (allAnimations == null) return;  // no animations

        while (allAnimations.hasRemaining()) {
            try (AIAnimation aiAnimation = AIAnimation.create(allAnimations.get())) {
                Animation animation = new Animation(aiAnimation, this);
                animator.addAnimation(animation);
            }
        }
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

    private void processMaterials(AIScene rootAiScene) {
        PointerBuffer allMaterials = rootAiScene.mMaterials();
        if (allMaterials == null) return;

        for (int mi = 0; mi < rootAiScene.mNumMaterials(); mi++) {
            try (AIMaterial material = AIMaterial.create(allMaterials.get(mi))) {
                materials[mi] = processMaterial(material);
            }
        }
    }

    private Material processMaterial(AIMaterial aiMaterial) {
        Material material = new Material();

        material.diffuseTexture = getMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE);
        material.specularMap = getMaterialTexture(aiMaterial, Assimp.aiTextureType_SPECULAR);
        material.normalMap = getMaterialTexture(aiMaterial, Assimp.aiTextureType_NORMALS);

        material.ambient = getMaterialColour(aiMaterial, Assimp.AI_MATKEY_COLOR_AMBIENT);
        material.diffuse = getMaterialColour(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE);
        material.specular = getMaterialColour(aiMaterial, Assimp.AI_MATKEY_COLOR_SPECULAR);

        material.shininess = 32f;
        return material;
    }

    private Texture2d getMaterialTexture(AIMaterial aiMaterial, int type) {
        AIString texPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, type, 0, texPath, (IntBuffer) null, null, null, null, null, null);
        if (texPath.dataString().isEmpty()) return null;

        String texturePath = texPath.dataString();
        if (texturePath.contains("\\")) {  // clean the path
            texturePath = texturePath.substring(texturePath.lastIndexOf("\\") + 1);
        }
        return new Texture2d(directory + "/" + texturePath);
    }

    private Vector3f getMaterialColour(AIMaterial aiMaterial, String type) {
        AIColor4D colBuff = AIColor4D.create();
        int result = Assimp.aiGetMaterialColor(aiMaterial, type, Assimp.aiTextureType_NONE, 0, colBuff);
        if (result == 0) return new Vector3f(colBuff.r(), colBuff.g(), colBuff.b());
        return new Vector3f();
    }

    private Float getMaterialFloat(AIMaterial aiMaterial, String type) {
        float[] f = new float[1];
        Assimp.aiGetMaterialFloatArray(aiMaterial, type, Assimp.aiTextureType_SHININESS, 0, f, new int[1]);
        System.out.println(Arrays.toString(f));
        return f[0];
    }

    private void processMeshMaterial(Mesh mesh, AIMesh aiMesh) {
        int matInx = aiMesh.mMaterialIndex();
        if (matInx >= 0 && matInx < materials.length) {
            mesh.setMaterial(materials[matInx]);
        }
    }

    public void setupBoneRendering(Camera3d camera3d) {
        if (boneCounter == 0) return;

        if (!boneShader.isSetup()) {
            boneShader.genProgram();
            boneShader.attachShader(BoilerplateShaders.safeFormat(BoilerplateShaders.ModelBoneVertex, "% ", camera3d.uniformBlockName), GL45.GL_VERTEX_SHADER, "BoilerplateShaders class, ModelBoneVertex");
            boneShader.attachShader(BoilerplateShaders.ModelBoneFragment, GL45.GL_FRAGMENT_SHADER, "BoilerplateShaders class, ModelBoneFragment");
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
        GL45.glActiveTexture(GL45.GL_TEXTURE0);
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
        Renderer.drawArrays(GL45.GL_POINTS, boneVa, boneMap.size());
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

    public boolean isRenderingBones() {
        return renderBones;
    }

    public boolean isRenderingWireframe() {
        return renderWireFrame;
    }

    public boolean hasBones() {
        return hasBones;
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
        return materials[index];
    }

    public static VertexLayout defaultVertexLayout() {
        return new VertexLayout(
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_POSITION),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_NORMAL),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_TANGENT),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_BITANGENT),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 2, VertexLayout.HINT_TEX_POS),
                new VertexLayout.Element(VertexLayout.TYPE_INT, MAX_BONE_INFLUENCE, VertexLayout.HINT_BONE_IDS),
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, MAX_BONE_INFLUENCE, VertexLayout.HINT_BONE_WEIGHTS, true),
                new VertexLayout.Element(VertexLayout.TYPE_INT, 1, VertexLayout.HINT_CUSTOM_0)
        );
    }

    public static ProcessVertexFunc defaultProcessVertexFunc() {
        return new ProcessVertexFunc() {
            @Override
            public void call(Model model, Mesh mesh, int vertexInx, AIVector3D.Buffer allVertices, AIVector3D.Buffer allNormals, AIVector3D.Buffer allTexPos, AIVector3D.Buffer allTangents, AIVector3D.Buffer allBitangents) {
                ProcessVertexFunc.super.call(model, mesh, vertexInx, allVertices, allNormals, allTexPos, allTangents, allBitangents);
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
