package boilerplate.example;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.GameBase;
import boilerplate.common.TimeStepper;
import boilerplate.common.Window;
import boilerplate.rendering.Camera2d;
import boilerplate.rendering.Renderer;
import boilerplate.rendering.text.FontManager;
import boilerplate.rendering.text.TextRenderer;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import org.joml.Vector2f;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.glDebugMessageCallback;

public class ExampleIndex extends GameBase {
    public boilerplate.common.Window window = new Window();
    final Dimension SCREEN_SIZE = new Dimension(500, 250);

    Camera2d camera = new Camera2d(new Dimension(2, 1));
    TextRenderer textRenderer = new TextRenderer();

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
        winOps.initWindowSize = SCREEN_SIZE;
        window.quickSetupAndShow(winOps);

        camera.setupUniformBuffer();
        FontManager.init();
        FontManager.loadFont(Font.MONOSPACED, Font.BOLD, 20, true);
        FontManager.generateAndBindAllFonts(camera);

        bindEvents();
        setupBuffers();
    }

    public void bindEvents() {
        glDebugMessageCallback(Logging.debugCallback(), -1);

        glfwSetKeyCallback(window.handle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_ESCAPE) this.window.setToClose();
                if (key == GLFW_KEY_Q) {
                    open2d = true;
                    this.window.setToClose();
                }
                if (key == GLFW_KEY_E) {
                     open3d = true;
                     this.window.setToClose();
                }
            }
        });
    }

    public void setupBuffers() {
        TextRenderer.TextObject to1 = new TextRenderer.TextObject(1, "[q]\n\n2d example", new Vector2f(120, 60), Color.YELLOW, Color.BLACK);
        TextRenderer.TextObject to2 = new TextRenderer.TextObject(1, "[e]\n\n3d example", new Vector2f(SCREEN_SIZE.width-120, 60), Color.CYAN, Color.BLACK);
        to1.setAlignment(TextRenderer.TextObject.ALIGN_MIDDLE);
        to2.setAlignment(TextRenderer.TextObject.ALIGN_MIDDLE);
        textRenderer.setupBufferObjects();
        textRenderer.pushTextObject(to1, to2);
    }

    private void clearGlContext() {
        Logging.debug("Deleting GL values...");
        textRenderer.delete();
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

    public void render() {
        Renderer.clearCDS();
        textRenderer.draw();
        Renderer.finish(window);
    }

    @Override
    public void mainLoop(double dt) {
        glfwPollEvents();
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
