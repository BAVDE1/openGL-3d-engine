package boilerplate.rendering;

import boilerplate.common.BoilerplateShaders;
import boilerplate.models.Model;
import boilerplate.rendering.buffers.VertexLayout;
import boilerplate.rendering.camera.CameraPerspective;
import boilerplate.rendering.textures.CubeMap;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import static org.lwjgl.opengl.GL11.*;

public class SkyBox {
    public static String[] expectedImageNames = new String[]{"px", "nx", "py", "ny", "pz", "nz"};

    protected ShaderProgram sh = new ShaderProgram();
    protected Model cubeModel;
    protected CubeMap skyBoxTexture = new CubeMap();

    protected CameraPerspective camera3d;

    public SkyBox() {

    }

    public void setupBuffers(CameraPerspective camera3d, String texturesDirectory, String imageExtension) {
        this.camera3d = camera3d;

        sh.genProgram();
        sh.attachShader(String.format(BoilerplateShaders.SkyBoxVertex, camera3d.getUniformBlockName()), GL45.GL_VERTEX_SHADER, "BoilerplateShaders.SkyBoxVertex");
        sh.attachShader(BoilerplateShaders.SkyBoxFragment, GL45.GL_FRAGMENT_SHADER, "BoilerplateShaders.SkyBoxFragment");
        sh.linkProgram();
        camera3d.bindShaderToUniformBlock(sh);

        cubeModel = new Model(new VertexLayout(
                new VertexLayout.Element(VertexLayout.TYPE_FLOAT, 3, VertexLayout.HINT_POSITION)
        ));
        ParShapesMesh cube = ParShapes.par_shapes_create_cube();
        assert cube != null;
        ParShapes.par_shapes_translate(cube, -.5f, -.5f, -.5f);
        cubeModel.loadShape(cube);

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
        cubeModel.draw(sh, 0);
        Renderer.cullBackFace();
        GL45.glDepthFunc(GL_LESS);
    }
}
