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
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL43.glDebugMessageCallback;

public class Example3d extends GameBase {
    public boilerplate.common.Window window = new Window();
    public final static Dimension SCREEN_SIZE = new Dimension(800, 800);
    public final static Dimension SHADOW_MAP_SIZE = new Dimension(SCREEN_SIZE.width * 2, SCREEN_SIZE.height * 2);

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

    PointLight lightRed = new PointLight(new Vector3f(0, 1, 0));
    PointLight lightBlue = new PointLight(new Vector3f(0, 2, 0));
    Light.LightGroup lightGroup = new Light.LightGroup();
    DirectionalLight skyLight = new DirectionalLight(new Vector3f(0, -1, 1));
    SpotLight spotLight = new SpotLight(camera.getPos(), camera.getForward(), 10, 12);

    FrameBuffer fb = new FrameBuffer(SCREEN_SIZE);
    FrameBuffer[] pingPongFbs = new FrameBuffer[]{new FrameBuffer(SCREEN_SIZE), new FrameBuffer(SCREEN_SIZE)};
    ShaderProgram gaussianBlurSh = new ShaderProgram();

    ShaderProgram modelShader = new ShaderProgram();
    Model model = new Model();
    Model model2 = new Model();
    Model model3 = new Model();
    Model model4 = new Model();
    Model modelFloor = new Model();
    Matrix4f modelFloorTrans1 = new Matrix4f().translate(-6, 1, 1).scale(4, 5, 10);
    Matrix4f modelFloorTrans2 = new Matrix4f().translate(6, 1, 1).scale(4, 5, 10);

    Matrix4f lightSpaceMatrix;
    VertexArray vaDisplayShadowMap = new VertexArray();
    ShaderProgram displayShadowMapShader = new ShaderProgram();
    ShaderProgram shadowMapShader = new ShaderProgram();
    FrameBuffer shadowMap = new FrameBuffer(SCREEN_SIZE);
    Matrix4f displayShadowMatrixTrans = new Matrix4f();

    Matrix4f pointShadowProjection;
    VertexArray vaDisplayPointShadowMap = new VertexArray();
    ShaderProgram displayPointShadowMapShader = new ShaderProgram();
    ShaderProgram pointShadowMapShader = new ShaderProgram();
    List<CubeMap> pointShadowTextures = Arrays.asList(new CubeMap(), new CubeMap());
    List<FrameBuffer> pointShadowMaps = Arrays.asList(new FrameBuffer(false), new FrameBuffer(false));

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
        FrameBuffer.RenderBuffer rb = new FrameBuffer.RenderBuffer(true);
        rb.createBufferMultisample(SCREEN_SIZE, GL45.GL_DEPTH24_STENCIL8, GL45.GL_DEPTH_STENCIL_ATTACHMENT, 4);
        fb.attachColourBuffer2D(fb.setupDefaultColourMultisampleBuffer(4));
        fb.attachColourBuffer2D(fb.setupDefaultColourMultisampleBuffer(4));
        fb.attachRenderBuffer(rb);
        fb.drawToMultipleColourBuffers(0, 1);
        fb.intermediaryFB = fb.createIntermediaryFB();
        fb.intermediaryFB.attachColourBuffer2D(fb.intermediaryFB.setupDefaultColourBuffer());
        fb.intermediaryFB.attachColourBuffer2D(fb.intermediaryFB.setupDefaultColourBuffer());
        fb.intermediaryFB.checkCompletionOrError();
        fb.checkCompletionOrError();

        for (FrameBuffer fb : pingPongFbs) {
            fb.genId();
            fb.attachColourBuffer2D(fb.setupDefaultColourBuffer());
            fb.checkCompletionOrError();
        }
        FrameBuffer.unbind();
        gaussianBlurSh.autoInitializeShadersMulti("shaders/3d_gaussian_blur.glsl");

        Matrix4f skyLightProjection = new Matrix4f().ortho(-8, 8, -8, 8, camera.near, camera.far);
        Matrix4f skyLightView = new Matrix4f().lookAt(skyLight.direction.negate(new Vector3f()).mul(3), new Vector3f(), new Vector3f(0, 1, 0));
        lightSpaceMatrix = skyLightProjection.mul(skyLightView);
        shadowMapShader.autoInitializeShadersMulti("shaders/3d_shadow_map.glsl");
        shadowMapShader.uniformMatrix4f("lightSpaceMatrix", lightSpaceMatrix);
        shadowMap.genId();
        Texture depthMap = FrameBuffer.setupDefaultDepthBuffer(SHADOW_MAP_SIZE);
        depthMap.useNearestInterpolation();
        depthMap.useRepeatWrap();
        shadowMap.attachDepthBuffer2D(depthMap);
        shadowMap.drawBufferNone();
        shadowMap.readBufferNone();
        shadowMap.checkCompletionOrError();
        FrameBuffer.unbind();

        VertexArrayBuffer vbShadowMap = new VertexArrayBuffer();
        displayShadowMapShader.autoInitializeShadersMulti("shaders/3d_shadow_display_map.glsl");
        displayShadowMapShader.uniformMatrix4f("transform", new Matrix4f().translate(.75f, .75f, 0).scale(.25f));
        vaDisplayShadowMap.genId();
        vbShadowMap.genId();
        vaDisplayShadowMap.fastSetup(new int[]{2}, vbShadowMap);
        rectData.clear();
        rectData.pushPolygon(Shape2d.createRect(new Vector2f(0), new Vector2f(1)));
        vbShadowMap.bufferData(rectData);

        modelShader.autoInitializeShadersMulti("shaders/3d_model.glsl");
        modelShader.uniformMatrix4f("lightSpaceMatrix", lightSpaceMatrix);
        modelShader.uniform1f("flashLightStrength", 1);
        modelShader.uniform1f("farPlane", camera.far);
        camera.bindShaderToUniformBlock(modelShader);

        modelFloor.loadModel("res/models/crate/NEWCRATE.fbx", true);
        modelFloor.modelTransform.translate(0, -6, 1).scale(10, 10, 10);

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

        displayPointShadowMapShader.autoInitializeShadersMulti("shaders/3d_point_shadow_display_map.glsl");
        camera.bindShaderToUniformBlock(displayPointShadowMapShader);
        VertexArrayBuffer vbPointShadowMap = new VertexArrayBuffer();
        vaDisplayPointShadowMap.genId();
        vbPointShadowMap.genId();
        vaDisplayPointShadowMap.fastSetup(new int[]{2}, vbPointShadowMap);
        rectData.clear();
        rectData.pushPolygon(Shape2d.createRect(new Vector2f(-1), new Vector2f(2)));
        vbPointShadowMap.bufferData(rectData);

        pointShadowMapShader.autoInitializeShadersMulti("shaders/3d_point_shadow_map.glsl");
        for (int i = 0; i < 2; i++) {
            CubeMap texture = pointShadowTextures.get(i);
            FrameBuffer fb = pointShadowMaps.get(i);
            texture.storedFormat = GL45.GL_DEPTH_COMPONENT;
            texture.pixelDataType = GL_FLOAT;
            texture.genId();
            texture.bind();
            for (int face = 0; face < 6; face++) texture.useCustomFace(face, SHADOW_MAP_SIZE);
            texture.useNearestInterpolation();
            texture.useClampEdgeWrap();
            fb.genId();
            fb.bind();
            fb.attachDepthBuffer(texture);
            fb.checkCompletionOrError();
        }
        FrameBuffer.unbind();
        pointShadowProjection = new Matrix4f().perspective((float) Math.toRadians(90), (float) SHADOW_MAP_SIZE.width / (float) SHADOW_MAP_SIZE.height, camera.near, camera.far);
    }

    public Matrix4f[] generatePointShadowTransformMatrices(PointLight light) {
        return new Matrix4f[] {
                pointShadowProjection.mul(new Matrix4f().lookAt(light.position, light.position.add(1, 0, 0, new Vector3f()), new Vector3f(0, -1, 0)), new Matrix4f()),
                pointShadowProjection.mul(new Matrix4f().lookAt(light.position, light.position.add(-1, 0, 0, new Vector3f()), new Vector3f(0, -1, 0)), new Matrix4f()),
                pointShadowProjection.mul(new Matrix4f().lookAt(light.position, light.position.add(0, 1, 0, new Vector3f()), new Vector3f(0, 0, 1)), new Matrix4f()),
                pointShadowProjection.mul(new Matrix4f().lookAt(light.position, light.position.add(0, -1, 0, new Vector3f()), new Vector3f(0, 0, -1)), new Matrix4f()),
                pointShadowProjection.mul(new Matrix4f().lookAt(light.position, light.position.add(0, 0, 1, new Vector3f()), new Vector3f(0, -1, 0)), new Matrix4f()),
                pointShadowProjection.mul(new Matrix4f().lookAt(light.position, light.position.add(0, 0, -1, new Vector3f()), new Vector3f(0, -1, 0)), new Matrix4f())
        };
    }

    public void update(double dt) {
        camera.processKeyInputs(window, dt);
        model.updateAnimation(dt);
        model2.updateAnimation(dt);
        double t = glfwGetTime() * .4;

        lightRed.position.z = 3 * (float) Math.sin(t);
        lightRed.position.x = 3 * (float) Math.cos(t);
//        lightBlue.position.y = 1 + 3 * (float) Math.abs(Math.sin(t));
//        lightBlue.position.z = 3 * (float) Math.cos(t);
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

        // --- SHADOW MAPS --- //
        shadowMap.bind();
        Renderer.setViewportSize(SHADOW_MAP_SIZE.width, SHADOW_MAP_SIZE.height);
        Renderer.clearD();
        Renderer.enableDepthTest();
        Renderer.cullFrontFace();
        modelFloor.draw(shadowMapShader, 0);
        modelFloor.draw(shadowMapShader, 0, modelFloorTrans1);
        modelFloor.draw(shadowMapShader, 0, modelFloorTrans2);
        Renderer.cullBackFace();
        model.draw(shadowMapShader, 0);
        model2.draw(shadowMapShader, 0);
        model3.draw(shadowMapShader, 0);
        model4.draw(shadowMapShader, 0);
        FrameBuffer.unbind();

        for (int i = 0; i < 2; i++) {
            FrameBuffer fb = pointShadowMaps.get(i);
            PointLight light = (PointLight) lightGroup.lights.get(i);
            fb.bind();
            Renderer.clearD();
            Renderer.enableDepthTest();
            Renderer.cullFrontFace();
            pointShadowMapShader.uniformMatrix4fArray("shadowMatrices", generatePointShadowTransformMatrices(light));
            pointShadowMapShader.uniform3f("lightPos", light.position);
            pointShadowMapShader.uniform1f("farPlane", camera.far);
            modelFloor.draw(pointShadowMapShader, 0);
            modelFloor.draw(pointShadowMapShader, 0, modelFloorTrans1);
            modelFloor.draw(pointShadowMapShader, 0, modelFloorTrans2);
            Renderer.cullBackFace();
            model.draw(pointShadowMapShader, 0);
            model2.draw(pointShadowMapShader, 0);
            model3.draw(pointShadowMapShader, 0);
            model4.draw(pointShadowMapShader, 0);
        }
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
        modelShader.uniformTexture("pointShadowMaps[0]", pointShadowMaps.getFirst().depthBuffer, 1);
        modelShader.uniformTexture("pointShadowMaps[1]", pointShadowMaps.get(1).depthBuffer, 2);
        modelFloor.draw(modelShader, 3);
        modelFloor.draw(modelShader, 3, modelFloorTrans1);
        modelFloor.draw(modelShader, 3, modelFloorTrans2);
        model.draw(modelShader, 3);
        model2.draw(modelShader, 3);
        model3.draw(modelShader, 3);
        model4.draw(modelShader, 3);

        // light sources
        shLightSource.bind();
        shLightSource.uniformMatrix4f("model", new Matrix4f().translate(lightRed.position).scale(.3f));
        shLightSource.uniform3f("lightColour", lightRed.diffuse);
        Renderer.drawArrays(GL_TRIANGLES, vaCube, 36);
        shLightSource.uniformMatrix4f("model", new Matrix4f().translate(lightBlue.position).scale(.3f));
        shLightSource.uniform3f("lightColour", lightBlue.diffuse);
        Renderer.drawArrays(GL_TRIANGLES, vaCube, 36);

        skyBox.draw();
        fb.blitIntoIntermediaryFB(GL_COLOR_BUFFER_BIT, GL_NEAREST, GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT0);
        fb.blitIntoIntermediaryFB(GL_COLOR_BUFFER_BIT, GL_NEAREST, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT1);

        // gaussian blur for bloom
        FrameBuffer.unbind();
        Renderer.clearC();
        boolean firstIter = true;
        int amount = 3;
        gaussianBlurSh.bind();
        for (int i = 0; i < amount; i++) {
            int inx = i % 2;
            pingPongFbs[inx].bind();
            gaussianBlurSh.uniform1i("horizontal", inx);
            if (firstIter) {
                fb.intermediaryFB.colourBuffers.get(1).bind();
                firstIter = false;
            } else pingPongFbs[1 - inx].colourBuffers.getFirst().bind();
            Renderer.drawArrays(GL_TRIANGLE_STRIP, vaPost, 4);
        }

        // --- POST PROCESSING --- //
        FrameBuffer.unbind();
        Renderer.clearC();
        Renderer.disableDepthTest();

        shPost.bind();
        shPost.uniformTexture("screenTexture", fb.intermediaryFB.colourBuffers.getFirst(), 0);
        shPost.uniformTexture("bloomTexture", pingPongFbs[1 - (amount % 2)].colourBuffers.getFirst(), 1);
        Renderer.drawArrays(GL_TRIANGLE_STRIP, vaPost, 4);
        GL45.glActiveTexture(GL45.GL_TEXTURE0);

        // debug shadow map
        displayShadowMapShader.bind();
        shadowMap.depthBuffer.bind();
        Renderer.drawArrays(GL_TRIANGLE_STRIP, vaDisplayShadowMap, 4);

        displayPointShadowMapShader.bind();
        displayPointShadowMapShader.uniformMatrix4f("transform", new Matrix4f().translate(.875f, .5f, 0).scale(.125f));
        pointShadowMaps.getFirst().depthBuffer.bind();
        Renderer.drawArrays(GL_TRIANGLE_STRIP, vaDisplayPointShadowMap, 4);

        displayPointShadowMapShader.uniformMatrix4f("transform", new Matrix4f().translate(.875f, .25f, 0).scale(.125f));
        pointShadowMaps.get(1).depthBuffer.bind();
        Renderer.drawArrays(GL_TRIANGLE_STRIP, vaDisplayPointShadowMap, 4);

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
