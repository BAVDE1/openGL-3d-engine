package boilerplate.example;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.GameBase;
import boilerplate.common.TimeStepper;
import boilerplate.common.Window;
import boilerplate.models.Material;
import boilerplate.models.Model;
import boilerplate.rendering.Renderer;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.camera.CameraOrtho;
import boilerplate.rendering.text.FontManager;
import boilerplate.rendering.text.TextObject;
import boilerplate.rendering.text.TextRenderer;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.glDebugMessageCallback;

public class ExampleIndex extends GameBase {
    public boilerplate.common.Window window = new Window();
    final Vector2f SCREEN_SIZE = new Vector2f(500, 300);

    CameraOrtho camera = new CameraOrtho(new Vector2f(SCREEN_SIZE.x / 150, SCREEN_SIZE.y / 150));
    TextRenderer textRenderer = new TextRenderer();

    ShaderProgram modelShader = new ShaderProgram();
    Model cubeModel = new Model(Model.defaultShapeVertexLayout());

    boolean open2d = false;
    boolean open3d = false;

    @Override
    public void start() {
        TimeStepper.startStaticTimeStepper(BoilerplateConstants.DT, this);
    }

    @Override
    public void createCapabilitiesAndOpen() {
        Window.Options winOps = new Window.Options();
        winOps.title = "the example index";
        winOps.initWindowSize = new Dimension((int) SCREEN_SIZE.x, (int) SCREEN_SIZE.y);
        window.quickSetupAndShow(winOps);

        camera.setupUniformBuffer();
        FontManager.init();
        FontManager.loadFont(Font.MONOSPACED, Font.BOLD, 20, true);
        FontManager.generateAndBindAllFonts();

        modelShader.autoInitializeShadersMulti("res/shaders/2d_shapes.glsl");
        camera.bindShaderToUniformBlock(modelShader);

        ParShapesMesh cube = ParShapes.par_shapes_create_cube();
        assert cube != null;
        ParShapes.par_shapes_translate(cube, -.5f, -.5f, -.5f);
        ParShapes.par_shapes_unweld(cube, true);
        ParShapes.par_shapes_compute_normals(cube);
        cubeModel.loadShape(cube, new Material(), false);

        bindEvents();
        setupBuffers();
    }

    public void bindEvents() {
        glDebugMessageCallback(Logging.debugCallback(), -1);

        glfwSetKeyCallback(window.handle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_ESCAPE) this.window.setToClose();
                if (key == GLFW_KEY_O) {
                    open2d = true;
                    this.window.setToClose();
                }
                if (key == GLFW_KEY_P) {
                     open3d = true;
                     this.window.setToClose();
                }
            }
        });
    }

    public void setupBuffers() {
        TextObject to1 = new TextObject(1, "[p]\n3d example", new Vector2f(-80, 80), Color.YELLOW, Color.RED);
        TextObject to2 = new TextObject(1, "[o]\n2d example", new Vector2f(80, 80), Color.CYAN);
        to1.setBgMargin(new Vector2f(10, 5));
        to1.setAlignment(TextObject.ALIGN_MIDDLE);
        to2.setAlignment(TextObject.ALIGN_MIDDLE);
        textRenderer.setupDefaultShader(camera);
        textRenderer.pushTextObject(to1, to2);
        textRenderer.setModelTransform(new Matrix4f().identity().scale(1f/60f, 1f/60f, 1));
    }

    private void clearGlContext() {
        Logging.debug("Deleting GL values...");
        FontManager.deleteAll();
        Texture2d.deleteAll();
    }

    public void open2dExample() {
        clearGlContext();
        Logging.mystical("Opening 2d example");
        new Example2d().start();
    }

    public void open3dExample() {
        clearGlContext();
        Logging.mystical("Opening 3d example");
        new Example3d().start();
    }

    public void update(double dt) {
        camera.processKeyInputs(window, dt);
        camera.updateUniformBlock();
    }

    public void render() {
        Renderer.clearCDS();
        Renderer.enableDepthTest();
        Renderer.enableFaceCulling();

        cubeModel.modelTransform = new Matrix4f().identity().translate(-1.3f, -.5f, 2).rotateX(-.3f).rotateY((float) (Math.PI * glfwGetTime() * .6f));
        cubeModel.draw(modelShader, 0);
        cubeModel.modelTransform = new Matrix4f().identity().translate(1.3f, -.5f, 2).rotateY((float) (Math.PI * Math.cos(glfwGetTime()) * .5f));
        cubeModel.draw(modelShader, 0);

        textRenderer.draw();
        Renderer.finish(window);
    }

    @Override
    public void mainLoop(double dt) {
        glfwPollEvents();
        update(dt);
        render();
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(window.handle);
    }

    @Override
    public void close() {
        window.close();

        if (open2d) open2dExample();
        else if (open3d) open3dExample();
    }
}
