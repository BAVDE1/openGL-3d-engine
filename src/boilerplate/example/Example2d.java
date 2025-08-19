package boilerplate.example;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.GameBase;
import boilerplate.common.TimeStepper;
import boilerplate.common.Window;
import boilerplate.rendering.Renderer;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.buffers.VertexLayout;
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
import org.lwjgl.opengl.GL45;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL45.GL_TRIANGLE_STRIP;

/**
 * Manages everything. Contains the main loop.
 */
public class Example2d extends GameBase {
    public Window window = new Window();
    final Dimension SCREEN_SIZE = new Dimension(900, 400);
    final Dimension CAPTURE_SIZE = new Dimension(SCREEN_SIZE.width / 200, SCREEN_SIZE.height / 200);
    CameraOrtho camera = new CameraOrtho(CAPTURE_SIZE);

    public static boolean debugMode = false;

    // main buffers
    ShaderProgram shMain = new ShaderProgram();
    VertexArray vaMain = new VertexArray();
    VertexArrayBuffer vbMain = new VertexArrayBuffer();
    BufferBuilder2f builderMain = new BufferBuilder2f();

    // circle buffers
    ShaderProgram shCircles = new ShaderProgram();
    VertexArray vaCircles = new VertexArray();
    VertexArrayBuffer vbCircles = new VertexArrayBuffer();
    BufferBuilder2f builderCircles = new BufferBuilder2f();

    // text
    TextObject to1;
    TextObject to2;
    TextRenderer textRenderer = new TextRenderer();

//    Vector2f mousePos = new Vector2f();
//    Vector2f mousePosOnClick = new Vector2f();

//    Vector2f viewPos = new Vector2f();
//    float viewScale = 1f;
//    float scaleAddition = .1f;
//    boolean forceUpdateView = false;

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
        winOps.initWindowSize = SCREEN_SIZE;
        window.quickSetupAndShow(winOps);

        camera.setupUniformBuffer();
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
//                    case GLFW_KEY_E -> addToViewScale(-scaleAddition, false);
//                    case GLFW_KEY_Q -> addToViewScale(scaleAddition, false);
//                    case GLFW_KEY_R -> {
//                        if (heldMouseKeys[GLFW_MOUSE_BUTTON_1]) break;
//                        viewPos.set(0);
//                        viewScale = 1;
//                        forceUpdateView = true;
//                    }

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

//                if (button == GLFW_MOUSE_BUTTON_1) {
//                    mousePosOnClick.set(mousePos.add(viewPos, new Vector2f()));
//                }
            }

            if (action == GLFW_RELEASE) {
                heldMouseKeys[button] = false;
            }
        });

        glfwSetScrollCallback(window.handle, (window, xOffset, yOffset) -> {
//            if (yOffset != 0.0) addToViewScale((float) (scaleAddition * Math.clamp(-yOffset, -1, 1)), true);
        });

        glfwSetCursorPosCallback(window.handle, (window, xPos, yPos) -> {
//            mousePos.set((float) xPos, (float) yPos)
        });
    }

    public void setupBuffers() {
        // MAIN BUFFERS
        vbMain.genId(); vaMain.genId();

        builderMain.setAdditionalVertFloats(4);
        builderMain.pushSeparatedPolygon(Shape2d.createRect(new Vector2f(50, 50), new Vector2f(500, 100), new ShapeMode.Demonstration(1, new Vector2f(), new Vector2f(1))));
        builderMain.pushSeparatedPolygon(Shape2d.createRect(new Vector2f(200, 200), new Vector2f(700, 150), new ShapeMode.Demonstration(2, new Vector2f(), new Vector2f(1))));
        builderMain.pushSeparatedPolygon(Shape2d.createLine(new Vector2f(70, 20), new Vector2f(150, 150), 20, new ShapeMode.Demonstration(3)));
        builderMain.pushSeparatedPolygon(Shape2d.createRectOutline(new Vector2f(700, 100), new Vector2f(100, 50), 15, new ShapeMode.Demonstration(3)));

        Shape2d.Poly2d p2 = new Shape2d.Poly2d(new ShapeMode.Demonstration(Color.RED), new Vector2f(-50, 50), new Vector2f(0, -50), new Vector2f(50, 50), new Vector2f(-50, 0), new Vector2f(50, 0));
        p2.addPos(new Vector2f(100, 250));
        Shape2d.sortPoints(p2);
        builderMain.pushSeparatedPolygonSorted(p2);

        vbMain.bufferData(builderMain);
        vaMain.bindBuffer(vbMain);
        vaMain.fastSetup(new int[] {2, 1, 3}, vbMain);

        to1 = new TextObject(1, "", new Vector2f(5), Color.CYAN, Color.BLACK);
        to2 = new TextObject(1, "", new Vector2f(5, 50), Color.WHITE, Color.BLACK);
        to1.setBgMargin(new Vector2f(5));
        to2.setBgMargin(new Vector2f(5));
        textRenderer.setupDefaultShader(camera);
        textRenderer.pushTextObject(to1, to2);
        textRenderer.setModelTransform(new Matrix4f().identity().translate(CAPTURE_SIZE.width, CAPTURE_SIZE.height, 0).scale(1f/80, 1f/80, 1));

        // CIRCLE BUFFERS
        vbCircles.genId(); vaCircles.genId();

        builderCircles.setAdditionalVertFloats(5);
        builderCircles.pushCircle(new Vector2f(200), 50, Color.BLUE);
        builderCircles.pushCircle(new Vector2f(300), 34, 10, Color.GREEN);

        VertexLayout instanceLayout = new VertexLayout();
        instanceLayout.pushFloat(2);  // circle pos
        instanceLayout.pushFloat(1);  // radius
        instanceLayout.pushFloat(1);  // inner radius
        instanceLayout.pushFloat(3);  // colour
        vaCircles.bindBuffer(vbCircles);
        vaCircles.pushLayout(instanceLayout, 1);
        vbCircles.bufferData(builderCircles);
    }

//    /** Must be called after window is visible */
//    public void setupShaders() {
////        shMain.useDemoShader();
////        shCircles.useCircleShader();
//
//        new Texture2d("textures/explosion.png").bindToTexArray(2, shMain);
//        new Texture2d("textures/closed.png").bindToTexArray(3, shMain);
//
////        shMain.uniformResolutionData(SCREEN_SIZE, PROJECTION_MATRIX);
////        shMain.uniform1f("viewScale", viewScale);
////        shCircles.uniformResolutionData(SCREEN_SIZE, PROJECTION_MATRIX);
////        shCircles.uniform1f("viewScale", viewScale);
//    }

    public void updateFpsAndDebugText() {
        // updates every second
        int newSeconds = (int) Math.floor(MathUtils.millisToSecond(System.currentTimeMillis()) - MathUtils.millisToSecond(timeStarted));
        if (newSeconds != secondsElapsed) {
            fps = frameCounter;
            frameCounter = 0;
            secondsElapsed = newSeconds;
        }

        // debug string
        to1.setString("FPS: %s, Elapsed: %s [debug (tab): %s]\nView [pos:%.0f,%.0f, captureSize:%s] (r)eset",
                fps, secondsElapsed, debugMode,
                camera.pos.x, camera.pos.y, camera.captureSize
        );
        to2.setString("""
                        Buffers:\

                         - main [s:%s, v:%s, f:%s/%s (%.5f)]\

                         - circles [s:%s, v:%s, f:%s/%s (%.5f)]""",
                builderMain.getSeparationsCount(),
                builderMain.getVertexCount(),
                builderMain.getFloatCount(),
                builderMain.getBufferSize(),
                builderMain.getCurrentFullnessPercent(),
                builderCircles.getSeparationsCount(),
                builderCircles.getVertexCount(),
                builderCircles.getFloatCount(),
                builderCircles.getBufferSize(),
                builderCircles.getCurrentFullnessPercent()
        );
    }

//    public void addToViewScale(float addition, boolean relativeToMouse) {
//        // mouse or middle of screen
//        Vector2f relativeTo = relativeToMouse ? mousePos : new Vector2f(SCREEN_SIZE.width, SCREEN_SIZE.height).mul(.5f);
//        viewPos.add(relativeTo.mul(viewScale, new Vector2f()).sub(relativeTo.mul(viewScale+addition, new Vector2f())));
//
//        viewScale += addition;
//        forceUpdateView = true;
//    }

//    public void updateViewPos(double dt) {
//        Vector2f addition = new Vector2f();
//
//        // mouse prioritised over keys
//        if (heldMouseKeys[GLFW_MOUSE_BUTTON_1]) {
//            addition = mousePos.sub(mousePosOnClick, new Vector2f()).negate();
//            addition.sub(viewPos);
//        }
//
//        if (forceUpdateView || !addition.equals(new Vector2f())) {
//            viewPos.add(addition);
//            shMain.uniform2f("viewPos", viewPos.x, viewPos.y);
//            shMain.uniform1f("viewScale", viewScale);
//
//            shCircles.uniform2f("viewPos", viewPos.x, viewPos.y);
//            shCircles.uniform1f("viewScale", viewScale);
//            forceUpdateView = false;
//        }
//    }

    public void toggleDebug() {
        debugMode = !debugMode;
        shMain.uniform1i("debugMode", debugMode ? 1:0);
        shCircles.uniform1i("debugMode", debugMode ? 1:0);
    }

    public void update(double dt) {
        camera.processKeyInputs(window, dt);
        camera.updateUniformBlock();
    }

    public void render() {
        Renderer.clearCDS();

        // shape examples & textures
        shMain.bind();
        shMain.uniform1f("time", (float) glfwGetTime());
        vaMain.drawArrays(debugMode ? GL_LINE_STRIP : GL_TRIANGLE_STRIP, builderCircles.getVertexCount());

//        shCircles.bind();
//        vaCircles.drawInstanced(GL_TRIANGLES, 3, builderCircles.getVertexCount());

        textRenderer.draw();

        // FINISH
        Renderer.finish(window);
    }

    public void mainLoop(double dt) {
        frameCounter++;

        glfwPollEvents();
//        updateViewPos(dt);
        update(dt);
        updateFpsAndDebugText();
        render();
    }
}
