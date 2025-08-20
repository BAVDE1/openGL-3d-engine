package boilerplate.example;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.GameBase;
import boilerplate.common.TimeStepper;
import boilerplate.common.Window;
import boilerplate.rendering.Renderer;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.builders.BufferBuilder2f;
import boilerplate.rendering.builders.Shape2d;
import boilerplate.rendering.builders.ShapeMode;
import boilerplate.rendering.camera.CameraOrtho;
import boilerplate.rendering.text.FontManager;
import boilerplate.rendering.text.TextObject;
import boilerplate.rendering.text.TextRenderer;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL45;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL45.GL_TRIANGLE_STRIP;

/**
 * Manages everything. Contains the main loop.
 */
public class Example2d extends GameBase {
    /**
     * THIS TYPE IS FOR DEMONSTRATION PURPOSES
     * meant for use with the 2d_main shader
     */
    public static class Demonstration extends ShapeMode {
        java.util.List<Vector3f> vars;
        public int type;

        public Demonstration() {
            this(BoilerplateConstants.DEMO_MODE_NIL);
        }

        public Demonstration(int texSlot, Vector2f texTopLeft, Vector2f texSize) {
            this(BoilerplateConstants.DEMO_MODE_TEX);
            this.vars = java.util.List.of(new Vector3f[]{
                    new Vector3f(texTopLeft, texSlot),
                    new Vector3f(texTopLeft.add(texSize.x, 0, new Vector2f()), texSlot),
                    new Vector3f(texTopLeft.add(0, texSize.y, new Vector2f()), texSlot),
                    new Vector3f(texTopLeft.add(texSize, new Vector2f()), texSlot)
            });
        }

        public Demonstration(Color col) {
            this(BoilerplateConstants.DEMO_MODE_COL);
            this.vars = List.of(new Vector3f[]{new Vector3f(col.getRed(), col.getGreen(), col.getBlue())});
        }

        public Demonstration(int mode) {
            this.type = mode;
        }

        public Demonstration(int mode, Vector3f... modeVars) {
            this(mode);
            this.vars = Arrays.stream(modeVars).toList();
        }

        /**
         * Get Vector3f at inx, or last (or empty Vector3f if no vars exist)
         */
        public Vector3f getVar(int inx) {
            if (vars == null || vars.isEmpty()) return new Vector3f();
            if (inx >= vars.size()) return vars.getLast();
            return vars.get(inx);
        }
    }

    public Window window = new Window();
    final Vector2f SCREEN_SIZE = new Vector2f(900, 400);
    final Vector2f CAPTURE_UI_SIZE = new Vector2f(SCREEN_SIZE.x / 200, SCREEN_SIZE.y / 200);
    final Vector2f CAPTURE_SIZE = new Vector2f(SCREEN_SIZE.x / 200, SCREEN_SIZE.y / 200);
    CameraOrtho uiCamera = new CameraOrtho(CAPTURE_UI_SIZE);
    CameraOrtho camera = new CameraOrtho(CAPTURE_SIZE);

    float captureScale = 1;

    public static boolean debugMode = false;

    // main buffers
    ShaderProgram shMain = new ShaderProgram();
    VertexArray vaMain = new VertexArray();
    VertexArrayBuffer vbMain = new VertexArrayBuffer();
    BufferBuilder2f builderMain = new BufferBuilder2f();

    // text
    TextObject to1;
    TextObject to2;
    TextRenderer textRenderer = new TextRenderer();

    float scaleAddition = .1f;

    boolean[] heldMouseKeys = new boolean[8];
    boolean[] heldKeys = new boolean[350];

    double timeStarted = 0;
    int secondsElapsed = 0;
    int frameCounter = 0;
    int fps = 0;

    public void start() {
        timeStarted = System.currentTimeMillis();
        TimeStepper.startStaticTimeStepper(BoilerplateConstants.DT, this);
    }

    public void createCapabilitiesAndOpen() {
        Window.Options winOps = new Window.Options();
        winOps.title = "the 2d example";
        winOps.initWindowSize = new Dimension((int) SCREEN_SIZE.x, (int) SCREEN_SIZE.y);
        window.quickSetupAndShow(winOps);

        uiCamera.uniformBlockName = "UICameraView";
        camera.uniformBlockName = "CameraView";
        camera.setupUniformBuffer();
        uiCamera.setupUniformBuffer();
        FontManager.init();
        FontManager.loadFont(Font.MONOSPACED, Font.BOLD, 14, true);
        FontManager.generateAndBindAllFonts();

        shMain = new ShaderProgram();
        shMain.autoInitializeShadersMulti("shaders/2d_main.glsl");
        new Texture2d("res/textures/explosion.png").bindToTexArray(2, shMain);
        new Texture2d("res/textures/closed.png").bindToTexArray(3, shMain);

        bindEvents();
        setupBuffers();
    }

    public void close() {
        window.close();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window.handle);
    }

    public void bindEvents() {
        GL45.glDebugMessageCallback(Logging.debugCallback(), -1);

        // key inputs
        glfwSetKeyCallback(window.handle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                heldKeys[key] = true;

                switch (key) {
                    case GLFW_KEY_ESCAPE -> this.window.setToClose();
                    case GLFW_KEY_O -> addToCaptureScale(scaleAddition);
                    case GLFW_KEY_P -> addToCaptureScale(-scaleAddition);
                    case GLFW_KEY_R -> {
                        if (heldMouseKeys[GLFW_MOUSE_BUTTON_1]) break;
                        camera.pos = new Vector3f(0);
                        camera.captureSize = CAPTURE_SIZE;
                        camera.up = new Vector3f(camera.worldUp);
                        camera.hasChangedProjection = true;
                        camera.hasChangedView = true;
                    }

                    case GLFW_KEY_TAB -> toggleDebug();
                }
            }

            if (action == GLFW_RELEASE) {
                heldKeys[key] = false;
            }
        });

        glfwSetMouseButtonCallback(window.handle, (window, button, action, mode) -> {
            if (action == GLFW_PRESS) {
                heldMouseKeys[button] = true;
            }

            if (action == GLFW_RELEASE) {
                heldMouseKeys[button] = false;
            }
        });
    }

    public void setupBuffers() {
        // MAIN BUFFERS
        vbMain.genId(); vaMain.genId();

        builderMain.setAdditionalVertFloats(4);
        builderMain.pushSeparatedPolygon(Shape2d.createRect(new Vector2f(-1), new Vector2f(2), new Demonstration(0, new Vector2f(), new Vector2f(1))));
        builderMain.pushSeparatedPolygon(Shape2d.createRect(new Vector2f(2, -.5f), new Vector2f(1.5f), new Demonstration(1, new Vector2f(), new Vector2f(1))));
        builderMain.pushSeparatedPolygon(Shape2d.createLine(new Vector2f(-2, 0), new Vector2f(-3, -1), .2f, new Demonstration(3)));
        builderMain.pushSeparatedPolygon(Shape2d.createRectOutline(new Vector2f(-3.5f, .5f), new Vector2f(1.5f, 1), .2f, new Demonstration(3)));

        Shape2d.Poly2d p2 = new Shape2d.Poly2d(new Demonstration(Color.RED), new Vector2f(-1, 1), new Vector2f(0, -1), new Vector2f(1), new Vector2f(-1, 0), new Vector2f(1, 0));
        p2.addPos(new Vector2f(3.5f, -1));
        Shape2d.sortPoints(p2);
        builderMain.pushSeparatedPolygonSorted(p2);

        vbMain.bufferData(builderMain);
        vaMain.bindBuffer(vbMain);
        vaMain.fastSetup(new int[] {2, 1, 3}, vbMain);

        to1 = new TextObject(1, "", new Vector2f(0, -60), Color.CYAN, Color.BLACK);
        to2 = new TextObject(1, "", new Vector2f(0, 0), Color.WHITE, Color.BLACK);
        to1.setBgMargin(new Vector2f(5));
        to2.setBgMargin(new Vector2f(5));
        textRenderer.setupDefaultShader(uiCamera);
        textRenderer.pushTextObject(to1, to2);
        textRenderer.setModelTransform(new Matrix4f().identity().scale(1f/SCREEN_SIZE.x, 1f/SCREEN_SIZE.y, 1).translate(SCREEN_SIZE.x * CAPTURE_SIZE.x, SCREEN_SIZE.y * CAPTURE_SIZE.y, 0).scale(2*CAPTURE_SIZE.x, 2*CAPTURE_SIZE.y, 1));
        uiCamera.updateUniformBlock();  // ui never changes
    }

    public void updateFpsAndDebugText() {
        // updates every second
        int newSeconds = (int) Math.floor(MathUtils.millisToSecond(System.currentTimeMillis()) - MathUtils.millisToSecond(timeStarted));
        if (newSeconds != secondsElapsed) {
            fps = frameCounter;
            frameCounter = 0;
            secondsElapsed = newSeconds;
        }

        // debug string
        to1.setString("FPS: %s, Elapsed: %s [debug (tab): %s]\nView [pos:%.2f,%.2f, captureSize:%.2f,%.2f] (r)eset",
                fps, secondsElapsed, debugMode,
                camera.pos.x, camera.pos.y, camera.captureSize.x, camera.captureSize.y
        );
        to2.setString("""
                        Buffers:\

                         - main [s:%s, v:%s, f:%s/%s (%.5f)]""",
                builderMain.getSeparationsCount(),
                builderMain.getVertexCount(),
                builderMain.getFloatCount(),
                builderMain.getBufferSize(),
                builderMain.getCurrentFullnessPercent()
        );
    }

    public void addToCaptureScale(float addition) {
        captureScale += addition;
        camera.captureSize = CAPTURE_SIZE.mul(captureScale, new Vector2f());
        camera.hasChangedProjection = true;
    }

    public void toggleDebug() {
        debugMode = !debugMode;
        shMain.uniform1i("debugMode", debugMode ? 1:0);
    }

    public void update(double dt) {
        camera.processKeyInputs(window, dt);
        camera.updateUniformBlock();
    }

    public void render() {
        Renderer.clearCDS();
        Renderer.enableDepthTest();

        // shape examples & textures
        shMain.bind();
        shMain.uniform1f("time", (float) glfwGetTime());
        vaMain.drawArrays(debugMode ? GL_LINE_STRIP : GL_TRIANGLE_STRIP, builderMain.getVertexCount());

        textRenderer.draw();

        // FINISH
        Renderer.finish(window);
    }

    public void mainLoop(double dt) {
        frameCounter++;

        glfwPollEvents();
        update(dt);
        updateFpsAndDebugText();
        render();
    }
}
