package boilerplate.rendering;

import boilerplate.common.BoilerplateShaders;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.buffers.VertexElementBuffer;
import boilerplate.rendering.builders.BufferBuilder3f;
import boilerplate.rendering.builders.Shape3d;
import boilerplate.rendering.textures.CubeMap;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.opengl.GL11.*;

public class SkyBox {
    public static String[] expectedImageNames = new String[]{"px", "nx", "py", "ny", "pz", "nz"};

    protected ShaderProgram sh = new ShaderProgram();
    protected VertexArray va = new VertexArray();
    protected VertexArrayBuffer vb = new VertexArrayBuffer();
    protected VertexElementBuffer veb = new VertexElementBuffer(VertexElementBuffer.ELEMENT_TYPE_INT);
    protected CubeMap skyBoxTexture = new CubeMap();

    protected Camera3d camera3d;

    public SkyBox() {

    }

    public void setupBuffers(Camera3d camera3d, String texturesDirectory, String imageExtension) {
        this.camera3d = camera3d;

        sh.genProgram();
        sh.attachShader(String.format(BoilerplateShaders.SkyBoxVertex, camera3d.uniformBlockName), GL45.GL_VERTEX_SHADER, "BoilerplateShaders class, SkyBoxVertex");
        sh.attachShader(BoilerplateShaders.SkyBoxFragment, GL45.GL_FRAGMENT_SHADER, "BoilerplateShaders class, SkyBoxFragment");
        sh.linkProgram();
        camera3d.bindShaderToUniformBlock(sh);

        va.genId();
        vb.genId();
        veb.genId();
        va.fastSetup(new int[]{3}, vb, veb);

        BufferBuilder3f bb = new BufferBuilder3f(true);
        Shape3d.Poly3d poly = Shape3d.createCubeE(new Vector3f(), 1);
        bb.pushPolygon(poly);
        vb.bufferData(bb);
        veb.bufferData(poly.elementIndex);

        String[] textureFileNames = new String[6];
        for (int i = 0; i < 6; i++) {
            textureFileNames[i] = String.format("%s/%s.%s", texturesDirectory, expectedImageNames[i], imageExtension);
        }
        skyBoxTexture.genId();
        skyBoxTexture.loadFaces(textureFileNames);
        skyBoxTexture.useLinearInterpolation();
        skyBoxTexture.useClampEdgeWrap();
        CubeMap.unbind(0);
    }

    public void bindSkyBoxTexture() {
        skyBoxTexture.bind();
    }

    public void draw() {
        sh.bind();
        bindSkyBoxTexture();

        GL45.glDepthFunc(GL_LEQUAL);  // since all skybox depth (z) values are exactly maximum (1.0)
        Renderer.cullFrontFace();
        Renderer.drawElements(GL_TRIANGLES, va, veb, 36);
        Renderer.cullBackFace();
        GL45.glDepthFunc(GL_LESS);
    }
}
