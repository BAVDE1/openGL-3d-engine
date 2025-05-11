package boilerplate.example;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.GameBase;
import boilerplate.common.TimeStepper;
import boilerplate.common.Window;
import boilerplate.rendering.builders.BufferBuilder2f;
import boilerplate.rendering.builders.ShapeMode;
import org.joml.Options;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL45;
import boilerplate.rendering.builders.Shape2d;
import boilerplate.rendering.*;
import boilerplate.rendering.text.FontManager;
import boilerplate.rendering.text.TextRenderer;
import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;

import java.awt.*;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * Manages everything. Contains the main loop.
 */
public class Example2d extends GameBase {
    public Window window = new Window();
    final Dimension SCREEN_SIZE = new Dimension(900, 400);
    final float[] PROJECTION_MATRIX = BoilerplateConstants.create2dProjectionMatrix(SCREEN_SIZE);

    public static boolean debugMode = false;

    // main buffers
    ShaderHelper shMain = new ShaderHelper();
    VertexArray vaMain = new VertexArray();
    VertexBuffer vbMain = new VertexBuffer();
    BufferBuilder2f builderMain = new BufferBuilder2f();

    // circle buffers
    ShaderHelper shCircles = new ShaderHelper();
    VertexArray vaCircles = new VertexArray();
    VertexBuffer vbCircles = new VertexBuffer();
    BufferBuilder2f builderCircles = new BufferBuilder2f();

    // text
    TextRenderer.TextObject to1;
    TextRenderer.TextObject to2;
    TextRenderer textRenderer = new TextRenderer();

    Vector2f mousePos = new Vector2f();
    Vector2f mousePosOnClick = new Vector2f();

    Vector2f viewPos = new Vector2f();
    float viewScale = 1f;
    float scaleAddition = .1f;
    boolean forceUpdateView = false;

    boolean[] heldMouseKeys = new boolean[8];
    boolean[] heldKeys = new boolean[350];

    double timeStarted = 0;
    int secondsElapsed = 0;
    int frameCounter = 0;
    int fps = 0;

    public void start() {
        timeStarted = System.currentTimeMillis();
        TimeStepper.startTimeStepper(BoilerplateConstants.DT, this);
    }

    public void createCapabilitiesAndOpen() {
        Window.Options winOps = new Window.Options();
        winOps.title = "the 2d example";
        winOps.initWindowSize = SCREEN_SIZE;
        window.quickSetupAndShow(winOps);

//        FontManager.init();
//        FontManager.loadFont(Font.MONOSPACED, Font.BOLD, 14, true);
//        FontManager.generateAndBindAllFonts(SCREEN_SIZE, BoilerplateConstants.create2dProjectionMatrix(SCREEN_SIZE));

//        bindEvents();
        setupShaders();
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
                    case GLFW_KEY_E -> addToViewScale(-scaleAddition, false);
                    case GLFW_KEY_Q -> addToViewScale(scaleAddition, false);
                    case GLFW_KEY_R -> {
                        if (heldMouseKeys[GLFW_MOUSE_BUTTON_1]) break;
                        viewPos.set(0);
                        viewScale = 1;
                        forceUpdateView = true;
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

                if (button == GLFW_MOUSE_BUTTON_1) {
                    mousePosOnClick.set(mousePos.add(viewPos));
                }
            }

            if (action == GLFW_RELEASE) {
                heldMouseKeys[button] = false;
            }
        });

        glfwSetScrollCallback(window.handle, (window, xOffset, yOffset) -> {
            if (yOffset != 0.0) addToViewScale((float) (scaleAddition * Math.clamp(-yOffset, -1, 1)), true);
        });

        glfwSetCursorPosCallback(window.handle, (window, xPos, yPos) -> mousePos.set((float) xPos, (float) yPos));
    }

    public void setupBuffers() {
        // MAIN BUFFERS
        vbMain.genId(); vaMain.genId();

        builderMain.setAdditionalVertFloats(VertexArray.Layout.getDefaultLayoutAdditionalVerts());
//        builderMain.pushSeparatedPolygon(Shape2d.createRect(new Vector2f(50, 50), new Vector2f(500, 100), new ShapeMode.Demonstration(1, new Vector2f(), new Vector2f(1))));
//        builderMain.pushSeparatedPolygon(Shape2d.createRect(new Vector2f(200, 200), new Vector2f(700, 150), new ShapeMode.Demonstration(2, new Vector2f(), new Vector2f(1))));
        Shape2d.Poly p = Shape2d.createRect(new Vector2f(0, 20), new Vector2f(50, 20), new ShapeMode.Demonstration(3));
        System.out.println(p.points);
        builderMain.pushSeparatedPolygon(p);
//        builderMain.pushSeparatedPolygon(Shape2d.createRectOutline(new Vector2f(700, 100), new Vector2f(100, 50), 15, new ShapeMode.Demonstration(3)));

//        Shape2d.Poly p2 = new Shape2d.Poly(new ShapeMode.Demonstration(Color.RED), new Vector2f(-50, 50), new Vector2f(0, -50), new Vector2f(50, 50), new Vector2f(-50, 0), new Vector2f(50, 0));
//        p2.addPos(new Vector2f(100, 250));
//        Shape2d.sortPoints(p2);
//        builderMain.pushSeparatedPolygonSorted(p2);

        vbMain.bufferData(builderMain);
        vaMain.bindBuffer(vbMain);
        vaMain.pushLayout(VertexArray.Layout.createDefaultLayout());
        System.out.println(Arrays.toString(builderMain.getFloats()));

//        to1 = new TextRenderer.TextObject(1, "", new Vector2f(5), Color.CYAN, Color.BLACK);
//        to2 = new TextRenderer.TextObject(1, "", new Vector2f(5, 50), Color.WHITE, Color.BLACK);
//        to1.setBgMargin(new Vector2f(5));
//        to2.setBgMargin(new Vector2f(5));
//        textRenderer.setupBufferObjects();
//        textRenderer.pushTextObject(to1, to2);
//
//        // CIRCLE BUFFERS
//        vbCircles.genId(); vaCircles.genId();
//
//        builderCircles.setAdditionalVertFloats(5);
//        builderCircles.pushCircle(new Vector2f(200), 50, Color.BLUE);
//        builderCircles.pushCircle(new Vector2f(300), 34, 10, Color.GREEN);
//
//        VertexArray.Layout instanceLayout = new VertexArray.Layout();
//        instanceLayout.pushFloat(2);  // circle pos
//        instanceLayout.pushFloat(1);  // radius
//        instanceLayout.pushFloat(1);  // inner radius
//        instanceLayout.pushFloat(3);  // colour
//        vaCircles.bindBuffer(vbCircles);
//        vaCircles.pushLayout(instanceLayout, 1);
//        vbCircles.bufferData(builderCircles);
    }

    /** Must be called after window is visible */
    public void setupShaders() {
        shMain.useDemoShader();
//        shCircles.useCircleShader();

//        new Texture("textures/explosion.png").bindToTexArray(2, shMain);
//        new Texture("textures/closed.png").bindToTexArray(3, shMain);

        shMain.uniformResolutionData(SCREEN_SIZE, PROJECTION_MATRIX);
        shMain.uniform1f("viewScale", viewScale);
//        shCircles.uniformResolutionData(SCREEN_SIZE, PROJECTION_MATRIX);
//        shCircles.uniform1f("viewScale", viewScale);
    }

    public void updateFpsAndDebugText() {
        // updates every second
//        int newSeconds = (int) Math.floor(MathUtils.millisToSecond(System.currentTimeMillis()) - MathUtils.millisToSecond(timeStarted));
//        if (newSeconds != secondsElapsed) {
//            fps = frameCounter;
//            frameCounter = 0;
//            secondsElapsed = newSeconds;
//        }

        // debug string
//        BufferBuilder2f textBuff = textRenderer.getBufferBuilder();
//        to1.setString("FPS: %s, Elapsed: %s [debug (tab): %s]\nView [pos:%.0f,%.0f, scale:%.2f] (r)eset",
//                fps, secondsElapsed, debugMode,
//                viewPos.x, viewPos.y, viewScale
//        );
//        to2.setString("""
//                        Buffers:\
//
//                         - text [s:%s, v:%s, f:%s/%s (%.5f)]\
//
//                         - main [s:%s, v:%s, f:%s/%s (%.5f)]\
//
//                         - circles [s:%s, v:%s, f:%s/%s (%.5f)]""",
//                textBuff.getSeparationsCount(),  // note: cause we're using the text buffers own values in this text
//                textBuff.getVertexCount(),       // object it'll need to re-build itself a few extra times than normal
//                textBuff.getFloatCount(),
//                textBuff.getBufferSize(),
//                textBuff.getCurrentFullnessPercent(),
//                builderMain.getSeparationsCount(),
//                builderMain.getVertexCount(),
//                builderMain.getFloatCount(),
//                builderMain.getBufferSize(),
//                builderMain.getCurrentFullnessPercent(),
//                builderCircles.getSeparationsCount(),
//                builderCircles.getVertexCount(),
//                builderCircles.getFloatCount(),
//                builderCircles.getBufferSize(),
//                builderCircles.getCurrentFullnessPercent()
//        );
    }

    public void addToViewScale(float addition, boolean relativeToMouse) {
        // mouse or middle of screen
//        Vector2f relativeTo = relativeToMouse ? mousePos : new Vector2f(SCREEN_SIZE.width, SCREEN_SIZE.height).mul(.5f);
//        viewPos.add(relativeTo.mul(viewScale).sub(relativeTo.mul(viewScale+addition)));
//
//        viewScale += addition;
//        forceUpdateView = true;
    }

    public void updateViewPos(double dt) {
//        Vector2f addition = new Vector2f();

        // mouse prioritised over keys
//        if (heldMouseKeys[GLFW_MOUSE_BUTTON_1]) {
//            addition = mousePos.sub(mousePosOnClick).negate();
//            addition.sub(viewPos);
//        }
//
//        if (forceUpdateView || !addition.equals(new Vector2f())) {
//            viewPos.add(addition);
//            shMain.uniform2f("viewPos", viewPos.x, viewPos.y);
//            shMain.uniform1f("viewScale", viewScale);

//            shCircles.uniform2f("viewPos", viewPos.x, viewPos.y);
//            shCircles.uniform1f("viewScale", viewScale);
//            forceUpdateView = false;
//        }
    }

    public void toggleDebug() {
//        debugMode = !debugMode;
//        shMain.uniform1i("debugMode", debugMode ? 1:0);
//        shCircles.uniform1i("debugMode", debugMode ? 1:0);
    }

    public void render() {
        Renderer.clearScreen();

        // shape examples & textures
        shMain.bind();
        shMain.uniform1f("time", (float) glfwGetTime());
        Renderer.drawArrays(debugMode ? GL_LINE_STRIP : GL_TRIANGLE_STRIP, vaMain, builderMain.getVertexCount());

//        shCircles.bind();
//        Renderer.drawInstanced(GL_TRIANGLES, vaCircles, 3, builderCircles.getVertexCount());
//
//        Renderer.drawText(textRenderer);

        // FINISH
        Renderer.finish(window);
    }

    public void mainLoop(double dt) {
//        frameCounter++;

        glfwPollEvents();
//        updateViewPos(dt);
//        updateFpsAndDebugText();
        render();
    }
}
