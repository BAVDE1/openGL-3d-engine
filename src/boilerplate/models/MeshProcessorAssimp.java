package boilerplate.models;

import boilerplate.common.BoilerplateConstants;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class MeshProcessorAssimp {
    public static void processScene(Model model,  AIScene rootAiScene) {
        // materials
        model.materials = Arrays.asList(new Material[rootAiScene.mNumMaterials()]);
        processMaterials(model, rootAiScene);

        // node hierarchy & model
        model.meshes = Arrays.asList(new Mesh[rootAiScene.mNumMeshes()]);
        processNode(model, rootAiScene.mRootNode(), rootAiScene, model.rootNode);
        model.rootNode.transform.invert(model.rootNodeInvTrans);

        // animations
        model.animator.init(model.boneCounter, model.rootNode);
        processAnimations(model, rootAiScene);
    }

    private static void processMaterials(Model model, AIScene rootAiScene) {
        PointerBuffer allMaterials = rootAiScene.mMaterials();
        if (allMaterials == null) return;

        for (int mi = 0; mi < rootAiScene.mNumMaterials(); mi++) {
            try (AIMaterial material = AIMaterial.create(allMaterials.get(mi))) {
                model.materials.set(mi, processMaterial(model, material));
            }
        }
    }

    private static Material processMaterial(Model model, AIMaterial aiMaterial) {
        Material material = new Material();

        material.diffuseTexture = getMaterialTexture(model, aiMaterial, Assimp.aiTextureType_DIFFUSE);
        material.specularMap = getMaterialTexture(model, aiMaterial, Assimp.aiTextureType_SPECULAR);
        material.normalMap = getMaterialTexture(model, aiMaterial, Assimp.aiTextureType_NORMALS);

        material.ambient = getMaterialColour(aiMaterial, Assimp.AI_MATKEY_COLOR_AMBIENT);
        material.diffuse = getMaterialColour(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE);
        material.specular = getMaterialColour(aiMaterial, Assimp.AI_MATKEY_COLOR_SPECULAR);

        material.shininess = 32f;
        return material;
    }

    private static Texture2d getMaterialTexture(Model model, AIMaterial aiMaterial, int type) {
        AIString texPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, type, 0, texPath, (IntBuffer) null, null, null, null, null, null);
        if (texPath.dataString().isEmpty()) return null;

        String texturePath = texPath.dataString();
        if (texturePath.contains("\\")) {  // clean the path
            texturePath = texturePath.substring(texturePath.lastIndexOf("\\") + 1);
        }
        return new Texture2d(model.directory + "/" + texturePath);
    }

    private static Vector3f getMaterialColour(AIMaterial aiMaterial, String type) {
        AIColor4D colBuff = AIColor4D.create();
        int result = Assimp.aiGetMaterialColor(aiMaterial, type, Assimp.aiTextureType_NONE, 0, colBuff);
        if (result == 0) return new Vector3f(colBuff.r(), colBuff.g(), colBuff.b());
        return new Vector3f();
    }

    private static Float getMaterialFloat(AIMaterial aiMaterial, String type) {
        float[] f = new float[1];
        Assimp.aiGetMaterialFloatArray(aiMaterial, type, Assimp.aiTextureType_SHININESS, 0, f, new int[1]);
        return f[0];
    }

    /**
     * Recursively process a node and its children
     */
    private static void processNode(Model model, AINode aiNode, AIScene rootAiScene, Model.NodeData nodeDest) {
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
                    model.meshes.set(meshInx, processMesh(model, aiMesh));
                }
            }
        }

        // process children
        PointerBuffer children = aiNode.mChildren();
        if (children == null) return;  // no children :(

        for (int i = 0; i < aiNode.mNumChildren(); i++) {
            try (AINode child = AINode.create(children.get(i))) {
                Model.NodeData childNode = new Model.NodeData();
                processNode(model, child, rootAiScene, childNode);

                // assign bone parents
                if (model.boneMap.containsKey(nodeDest.name) && model.boneMap.containsKey(childNode.name)) {
                    model.boneMap.get(childNode.name).parent = model.boneMap.get(nodeDest.name);
                }

                nodeDest.children.add(childNode);
            }
        }
    }

    private static Mesh processMesh(Model model, AIMesh aiMesh) {
        Mesh mesh = new Mesh(model.vertexLayout);
        mesh.indicesCount = findIndicesCount(aiMesh);
        mesh.allocateMemory(calculateVertexDataBytes(model, aiMesh), mesh.indicesCount * Integer.BYTES);

        processBones(model, mesh, aiMesh);
        processVertices(model, mesh, aiMesh);
        processFaces(mesh, aiMesh);
        processMeshMaterial(model, mesh, aiMesh);

        mesh.finalizeMesh();
        return mesh;
    }

    private static int calculateVertexDataBytes(Model model, AIMesh aiMesh) {
        return aiMesh.mNumVertices() * model.vertexLayout.stride;
    }

    private static int findIndicesCount(AIMesh aiMesh) {
        int count = 0;
        for (int fi = 0; fi < aiMesh.mNumFaces(); fi++) {
            count += aiMesh.mFaces().get(fi).mNumIndices();
        }
        return count;
    }

    private static void processBones(Model model, Mesh mesh, AIMesh aiMesh) {
        PointerBuffer allBones = aiMesh.mBones();
        if (allBones == null) return;  // no bones

        model.hasBones = allBones.hasRemaining();
        while (allBones.hasRemaining()) {
            try (AIBone aiBone = AIBone.create(allBones.get())) {
                String boneName = aiBone.mName().dataString();
                Bone bone = model.boneMap.computeIfAbsent(boneName, _ -> new Bone(model.boneCounter++, aiBone));
                processBoneWeights(mesh, bone, aiBone);
            }
        }
    }

    private static void processBoneWeights(Mesh mesh, Bone bone, AIBone aiBone) {
        AIVertexWeight.Buffer weights = aiBone.mWeights();
        while (weights.hasRemaining()) {
            AIVertexWeight aiWeight = weights.get();
            int vertexId = aiWeight.mVertexId();
            float weight = aiWeight.mWeight();
            if (weight < BoilerplateConstants.EPSILON) continue;  // no need to even add the bone

            List<Model.VertexWeight> vwList = mesh.vertexWeights.computeIfAbsent(vertexId, _ -> new ArrayList<>());
            vwList.add(new Model.VertexWeight(bone.id, weight));
        }
    }

    /**
     * After process bones
     */
    private static void processVertices(Model model, Mesh mesh, AIMesh aiMesh) {
        AIVector3D.Buffer allVertices = aiMesh.mVertices();
        AIVector3D.Buffer allNormals = aiMesh.mNormals();
        AIVector3D.Buffer allTexPos = aiMesh.mTextureCoords(0);
        AIVector3D.Buffer allTangents = aiMesh.mTangents();
        AIVector3D.Buffer allBitangents = aiMesh.mTangents();

        // data checks
        if (allNormals == null && model.vertexLayout.hasElementWithHint(VertexLayout.HINT_NORMAL))
            throw new RuntimeException("Given vertex layout contains normals, but mesh data does not contain normals.");
        if (allTexPos == null && model.vertexLayout.hasElementWithHint(VertexLayout.HINT_TEX_POS))
            throw new RuntimeException("Given vertex layout contains texture coords, but mesh data does not contain texture coords.");

        // process
        for (int i = 0; i < aiMesh.mNumVertices(); i++) {
            model.processAssimpVertexFunc.call(model, mesh, i, allVertices, allNormals, allTexPos, allTangents, allBitangents);
        }
    }

    private static void processFaces(Mesh mesh, AIMesh aiMesh) {
        AIFace.Buffer allFaces = aiMesh.mFaces();

        while (allFaces.hasRemaining()) {
            IntBuffer indices = allFaces.get().mIndices();
            while (indices.hasRemaining()) mesh.pushIndice(indices.get());
        }
    }

    private static void processMeshMaterial(Model model, Mesh mesh, AIMesh aiMesh) {
        int matInx = aiMesh.mMaterialIndex();
        if (matInx >= 0 && matInx < model.materials.size()) {
            mesh.setMaterial(model.materials.get(matInx));
        }
    }

    /**
     * After process bones
     */
    private static void processAnimations(Model model, AIScene rootAIScene) {
        PointerBuffer allAnimations = rootAIScene.mAnimations();
        if (allAnimations == null) return;  // no animations

        while (allAnimations.hasRemaining()) {
            try (AIAnimation aiAnimation = AIAnimation.create(allAnimations.get())) {
                Animation animation = new Animation(aiAnimation, model);
                model.animator.addAnimation(animation);
            }
        }
    }
}
