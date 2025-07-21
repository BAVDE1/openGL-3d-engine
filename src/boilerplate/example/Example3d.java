package boilerplate.example;

import boilerplate.common.BoilerplateConstants;
import boilerplate.common.GameBase;
import boilerplate.common.TimeStepper;
import boilerplate.common.Window;
import boilerplate.models.Model;
import boilerplate.rendering.Camera3d;
import boilerplate.rendering.Renderer;
import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.SkyBox;
import boilerplate.rendering.buffers.FrameBuffer;
import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexArrayBuffer;
import boilerplate.rendering.builders.*;
import boilerplate.rendering.light.DirectionalLight;
import boilerplate.rendering.light.Light;
import boilerplate.rendering.light.PointLight;
import boilerplate.rendering.light.SpotLight;
import boilerplate.rendering.textures.CubeMap;
import boilerplate.rendering.textures.Texture;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.rendering.textures.Texture2dMultisample;
import boilerplate.utility.Logging;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL45;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.glDebugMessageCallback;

public class Example3d extends GameBase {
    public boilerplate.common.Window window = new Window();
    public final static Dimension SCREEN_SIZE = new Dimension(800, 800);

    boolean renderWireFrame = false;

    Camera3d camera = new Camera3d(new Dimension(1, 1), Camera3d.MODE_TARGET, new Vector3f(0, 0, 5), 5);

    ShaderProgram shPost = new ShaderProgram();
    ShaderProgram shCubeMap = new ShaderProgram();
    ShaderProgram shReflect = new ShaderProgram();
    ShaderProgram shOutline = new ShaderProgram();
    ShaderProgram shLightSource = new ShaderProgram();
    VertexArray vaPost = new VertexArray();
    VertexArray vaCube = new VertexArray();
    VertexArrayBuffer vbPost = new VertexArrayBuffer();
    VertexArrayBuffer vbCube = new VertexArrayBuffer();
    CubeMap ballerCube = new CubeMap();
    SkyBox skyBox = new SkyBox();

    PointLight lightRed = new PointLight(new Vector3f(0));
    PointLight lightBlue = new PointLight(new Vector3f(0));
    Light.LightGroup lightGroup = new Light.LightGroup();
    DirectionalLight skyLight = new DirectionalLight(new Vector3f(0, -1, 1));
    SpotLight spotLight = new SpotLight(camera.getPos(), camera.getForward(), 10, 12);

    FrameBuffer fb = new FrameBuffer(SCREEN_SIZE);

    ShaderProgram modelShader = new ShaderProgram();
    Model model = new Model();
    Model model2 = new Model();
    Model model3 = new Model();
    Model model4 = new Model();
    Model modelFloor = new Model();
    Matrix4f modelFloorTrans1 = new Matrix4f().translate(-6, 1, 1).scale(4, 4, 10);
    Matrix4f modelFloorTrans2 = new Matrix4f().translate(6, 1, 1).scale(4, 4, 10);

    Dimension SHADOW_MAP_SIZE = new Dimension(SCREEN_SIZE.width * 2, SCREEN_SIZE.height * 2);
    Matrix4f lightSpaceMatrix;
    VertexArray vaDisplayShadowMap = new VertexArray();
    ShaderProgram displayShadowMapShader = new ShaderProgram();
    ShaderProgram shadowMapShader = new ShaderProgram();
    FrameBuffer shadowMap = new FrameBuffer(SCREEN_SIZE);
    Matrix4f displayShadowMatrixTrans = new Matrix4f();

    @Override
    public void start() {
        TimeStepper.startStaticTimeStepper(BoilerplateConstants.DT, this, false);
    }

    @Override
    public void createCapabilitiesAndOpen() {
        Window.Options winOps = new Window.Options();
        winOps.title = "the 3d example";
        winOps.initWindowSize = SCREEN_SIZE;
        window.quickSetupAndShow(winOps);

        Renderer.enableDepthTest();
        Renderer.enableStencilTest();
        Renderer.setStencilOperation(GL_KEEP, GL_KEEP, GL_REPLACE);
        Renderer.useDefaultFaceCulling();
//        Renderer.enableFramebufferGamma();
        Renderer.setViewportSize(SCREEN_SIZE.width, SCREEN_SIZE.height);

        bindEvents();
        setupBuffers();
    }

    public void bindEvents() {
        glDebugMessageCallback(Logging.debugCallback(), -1);

        glfwSetKeyCallback(window.handle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_ESCAPE) this.window.setToClose();
                if (key == GLFW_KEY_TAB) {
                    renderWireFrame = !renderWireFrame;
                    model.renderWireFrame(renderWireFrame);
                    model2.renderWireFrame(renderWireFrame);
                    model4.renderWireFrame(renderWireFrame);
                }
                if (key == GLFW_KEY_GRAVE_ACCENT) {
                    model.renderBones(!model.isRenderingBones());
                    model2.renderBones(!model2.isRenderingBones());
                }
                if (key == GLFW_KEY_F)
                    camera.setMode(camera.getMode() == Camera3d.MODE_FLY ? Camera3d.MODE_TARGET : Camera3d.MODE_FLY);
                if (key == GLFW_KEY_1) model.animator.playAnimation("R6Armature|WalkAnim");
                if (key == GLFW_KEY_2) model.animator.playAnimation("R6Armature|Climb");
                if (key == GLFW_KEY_3) model.animator.playAnimation("R6Armature|Idle2");
                if (key == GLFW_KEY_4) model.animator.playAnimation("R6Armature|Sit");
                if (key == GLFW_KEY_5) model.animator.playAnimation("R6Armature|Jump");
                if (key == GLFW_KEY_6) model.animator.playAnimation("R6Armature|Fall");
                if (key == GLFW_KEY_7) model2.animator.playAnimation("anim_0");
                if (key == GLFW_KEY_PERIOD) model.animator.animationSpeed += .1f;
                if (key == GLFW_KEY_COMMA) model.animator.animationSpeed -= .1f;
                if (key == GLFW_KEY_L) {
                    model.animator.stopPlayingAnimation(true);
                    model2.animator.stopPlayingAnimation(true);
                }
                if (key == GLFW_KEY_G) modelShader.uniform1f("flashLightStrength", 1);
                if (key == GLFW_KEY_H) modelShader.uniform1f("flashLightStrength", 0);
            }
        });

        glfwSetMouseButtonCallback(window.handle, (window, button, action, mods) -> {
            camera.processMouseInputs(this.window);
        });

        glfwSetCursorPosCallback(window.handle, (window, xPos, yPos) -> {
            camera.processMouseMovement(this.window, (float) xPos, (float) yPos);
        });

        glfwSetScrollCallback(window.handle, (window, xDelta, yDelta) -> {
            camera.processMouseScroll(this.window, (float) xDelta, (float) yDelta);
        });
    }

    public void setupBuffers() {
        ballerCube.genId();
        ballerCube.loadFaces("res/textures/baller.png");
        ballerCube.useNearestInterpolation();
        ballerCube.useClampEdgeWrap();
        CubeMap.unbind();
        vaCube.genId();
        vbCube.genId();

        shCubeMap.autoInitializeShadersMulti("shaders/3d_cube_map.glsl");
        shReflect.autoInitializeShadersMulti("shaders/3d_reflect.glsl");
        shOutline.autoInitializeShadersMulti("shaders/3d_outline.glsl");
        shLightSource.autoInitializeShadersMulti("shaders/3d_light_source.glsl");

        camera.setupUniformBuffer(shCubeMap, shReflect, shOutline, shLightSource);
        skyBox.setupBuffers(camera, "res/textures/space_skybox", "png");

        vaCube.fastSetup(new int[]{3, 3}, vbCube);
        BufferBuilder3f cubeData = new BufferBuilder3f(true, 3);
        Shape3d.Poly3d cube = Shape3d.createCube(new Vector3f(), 1);
        cube.mode = new ShapeMode.Unpack(Shape3d.defaultCubeNormals());
        cubeData.pushPolygon(cube);
        vbCube.bufferData(cubeData);

        shPost.autoInitializeShadersMulti("shaders/3d_post_processing.glsl");
        vaPost.genId();
        vbPost.genId();

        vaPost.fastSetup(new int[]{2}, vbPost);
        BufferBuilder2f rectData = new BufferBuilder2f(true);
        rectData.pushPolygon(Shape2d.createRect(new Vector2f(-1), new Vector2f(2)));
        vbPost.bufferData(rectData);

        fb.genId();
        fb.setupIntermediaryFB();
        Texture2dMultisample buff = new Texture2dMultisample(SCREEN_SIZE, true);
        buff.bind();
        buff.createTexture2d(FrameBuffer.defaultColourBuffFormat, 4);
        FrameBuffer.RenderBuffer rb = new FrameBuffer.RenderBuffer(true);
        rb.createBufferMultisample(SCREEN_SIZE, GL45.GL_DEPTH24_STENCIL8, GL45.GL_DEPTH_STENCIL_ATTACHMENT, 4);
        fb.attachColourBuffer(buff);
        fb.attachRenderBuffer(rb);
        fb.checkCompletionOrError();
        FrameBuffer.unbind();

        Matrix4f skyLightProjection = new Matrix4f().ortho(-8, 8, -8, 8, camera.near, camera.far);
        Matrix4f skyLightView = new Matrix4f().lookAt(skyLight.direction.negate(new Vector3f()).mul(3), new Vector3f(), new Vector3f(0, 1, 0));
        lightSpaceMatrix = skyLightProjection.mul(skyLightView);
        shadowMapShader.autoInitializeShadersMulti("shaders/3d_shadow_map.glsl");
        shadowMapShader.uniformMatrix4f("lightSpaceMatrix", lightSpaceMatrix);
        shadowMap.genId();
        Texture depthMap = FrameBuffer.setupDefaultDepthBuffer(SHADOW_MAP_SIZE);
        depthMap.useNearestInterpolation();
        depthMap.useRepeatWrap();
        shadowMap.attachDepthBuffer(depthMap);
        shadowMap.drawBufferNone();
        shadowMap.readBufferNone();
        shadowMap.checkCompletionOrError();
        FrameBuffer.unbind();

        VertexArrayBuffer vbShadowMap = new VertexArrayBuffer();
        displayShadowMapShader.autoInitializeShadersMulti("shaders/3d_shadow_display_map.glsl");
        vaDisplayShadowMap.genId();
        vbShadowMap.genId();
        vaDisplayShadowMap.fastSetup(new int[]{2}, vbShadowMap);
        rectData.clear();
        rectData.pushPolygon(Shape2d.createRect(new Vector2f(0), new Vector2f(1)));
        vbShadowMap.bufferData(rectData);
        displayShadowMatrixTrans.translate(.5f, .5f, 0).scale(.5f);

        modelShader.autoInitializeShadersMulti("shaders/3d_model.glsl");
        modelShader.uniformMatrix4f("lightSpaceMatrix", lightSpaceMatrix);
        modelShader.uniform1f("flashLightStrength", 1);
        camera.bindShaderToUniformBlock(modelShader);

        modelFloor.loadModel("res/models/crate/NEWCRATE.fbx", true);
        modelFloor.modelTransform.translate(0, -6, 1).scale(10);

        model.loadModel("res/models/roblox/scene.gltf", true);
        model.modelTransform.translate(-2, -1f, 1).rotateY(1);
        model.setupBoneRendering(camera);

        model2.loadModel("res/models/guard/scene.md5mesh", true);
        model2.modelTransform.scale(.03f).translate(0, -33, -40);
        model2.setupBoneRendering(camera);

        model3.loadModel("res/models/bloxycola/cola.obj", true);
        model3.modelTransform.translate(2, -.55f, 0).rotateY(2.1f);

        model4.loadModel("res/models/miku/miku_prefab.fbx", true);
        model4.modelTransform.translate(-3, -1, 2.5f).rotateY(1.2f);

        lightRed.setColourValues(new Vector3f(2, 0, 0), new Vector3f(.8f, 0, 0), new Vector3f());
        lightBlue.setColourValues(new Vector3f(0, 0, 2), new Vector3f(0, 0, .8f), new Vector3f());
        lightGroup.addLight(lightRed, lightBlue);

        skyLight.diffuse = new Vector3f(.4f);
        skyLight.specular = new Vector3f(.2f);
        skyLight.ambient = new Vector3f(.3f);
        skyLight.uniformValues("skyLight", modelShader);  // never changes

        spotLight.setColourValues(new Vector3f(1), new Vector3f(.6f), new Vector3f());
    }

    public void update(double dt) {
        camera.processKeyInputs(window, dt);
        model.updateAnimation(dt);
        model2.updateAnimation(dt);

        lightRed.position.z = 3 * (float) Math.sin(glfwGetTime());
        lightRed.position.x = 3 * (float) Math.cos(glfwGetTime());
        lightBlue.position.y = 3 * (float) Math.sin(glfwGetTime());
        lightBlue.position.z = 3 * (float) Math.cos(glfwGetTime());
        lightGroup.uniformValuesAsArray("lights", modelShader);
        spotLight.position = camera.getPos();
        spotLight.direction = camera.getForward();
        spotLight.uniformValues("spotLight", modelShader);

        model3.modelTransform.setRotationXYZ(0, 0, 0).rotateY((float) glfwGetTime());
        model4.modelTransform.setRotationXYZ(0, 0, 0).rotateY((float) glfwGetTime());
    }

    public void render() {
        float time = (float) glfwGetTime();

        Matrix4f matModel1 = new Matrix4f().identity().translate(10, 0, -10);
        Matrix4f matModel2 = new Matrix4f().identity().translate(10, 0, -10);
        matModel2.rotateX(time * (float) Math.toRadians(120));
        matModel2.rotateY(time * (float) Math.toRadians(70));
        matModel2.translate(0, 0, 1.2f);
        matModel2.scale(.8f, .5f, .5f);

        // --- SHADOW MAP --- //
        shadowMap.bind();
        Renderer.setViewportSize(SHADOW_MAP_SIZE.width, SHADOW_MAP_SIZE.height);
        Renderer.clearD();
        Renderer.enableDepthTest();
        Renderer.cullFrontFace();
        modelFloor.draw(shadowMapShader);
        modelFloor.draw(shadowMapShader, modelFloorTrans1);
        modelFloor.draw(shadowMapShader, modelFloorTrans2);
        Renderer.cullBackFace();
        model.draw(shadowMapShader);
        model2.draw(shadowMapShader);
        model3.draw(shadowMapShader);
        model4.draw(shadowMapShader);
        FrameBuffer.unbind();
        Renderer.setViewportSize(SCREEN_SIZE.width, SCREEN_SIZE.height);

        // --- 3D SPACE --- //
        fb.bind();
        camera.updateUniformBlock();
        Renderer.enableStencilTest();
        Renderer.cullBackFace();
        Renderer.setStencilFunc(GL_ALWAYS, 1, true);  // write 1 to all fragments that pass
        Renderer.enableStencilWriting();
        Renderer.clearCDS();

        // outline boxes
        shCubeMap.bind();
        ballerCube.bind();
        drawObjects(matModel1, matModel2, shCubeMap);
        Renderer.setStencilFunc(GL_NOTEQUAL, 1, true);  // only draw if fragment in stencil is NOT equal to 1
        Renderer.disableStencilWriting();
        Renderer.cullFrontFace();
        drawObjects(matModel1.scale(1.2f), matModel2.scale(1.2f), shOutline);
        Renderer.cullBackFace();
        Renderer.disableStencilTest();

        // sky box reflector
        skyBox.bindSkyBoxTexture();
        shReflect.bind();
        shReflect.uniform3f("camPos", camera.getPos());
        shReflect.uniformMatrix4f("model", matModel1.translate(2, 0, 0));
        Renderer.drawArrays(renderWireFrame ? GL_LINES : GL_TRIANGLES, vaCube, 36);

        // models
        modelShader.uniform3f("viewPos", camera.getPos());
        modelShader.uniformTexture("shadowMap", shadowMap.depthBuffer, 0);
        modelFloor.draw(modelShader);
        modelFloor.draw(modelShader, modelFloorTrans1);
        modelFloor.draw(modelShader, modelFloorTrans2);
        model.draw(modelShader);
        model2.draw(modelShader);
        model3.draw(modelShader);
        model4.draw(modelShader);

        // light sources
        shLightSource.bind();
        shLightSource.uniformMatrix4f("model", new Matrix4f().translate(lightRed.position).scale(.3f));
        shLightSource.uniform3f("lightColour", lightRed.diffuse);
        Renderer.drawArrays(GL_TRIANGLES, vaCube, 36);
        shLightSource.uniformMatrix4f("model", new Matrix4f().translate(lightBlue.position).scale(.3f));
        shLightSource.uniform3f("lightColour", lightBlue.diffuse);
        Renderer.drawArrays(GL_TRIANGLES, vaCube, 36);

        skyBox.draw();
        fb.blitIntoIntermediaryFB(GL_COLOR_BUFFER_BIT, GL_NEAREST);

        // --- POST PROCESSING --- //
        FrameBuffer.unbind();
        Renderer.clearC();
        Renderer.disableDepthTest();

        shPost.bind();
        fb.bindIntermediaryFBColorBuffer();
        Renderer.drawArrays(GL_TRIANGLE_STRIP, vaPost, 4);

        // debug shadow map
        displayShadowMapShader.bind();
        displayShadowMapShader.uniformMatrix4f("transform", displayShadowMatrixTrans);
        shadowMap.depthBuffer.bind();
        Renderer.drawArrays(GL_TRIANGLE_STRIP, vaDisplayShadowMap, 4);

        Renderer.finish(window);
    }

    private void drawObjects(Matrix4f model1, Matrix4f model2, ShaderProgram sh) {
        sh.uniformMatrix4f("model", model1);
        Renderer.drawArrays(renderWireFrame ? GL_LINES : GL_TRIANGLES, vaCube, 36);

        sh.uniformMatrix4f("model", model2);
        Renderer.drawArrays(renderWireFrame ? GL_LINES : GL_TRIANGLES, vaCube, 36);
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
    }
}
