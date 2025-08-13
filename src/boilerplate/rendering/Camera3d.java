package boilerplate.rendering;

import boilerplate.common.Window;
import boilerplate.rendering.buffers.VertexUniformBuffer;
import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class Camera3d {
    public static final int MODE_FLY = 0;
    public static final int MODE_TARGET = 1;

    protected int mode;
    public boolean hasChangedView = true;
    public boolean hasChangedPerspective = true;

    protected VertexUniformBuffer vub = new VertexUniformBuffer();
    public String uniformBlockName = "CameraView";

    // perspective
    public Dimension captureSize;
    protected float aspect;
    public float fov = 80;
    public float near = .05f;
    public float far = 50;

    // view
    public Vector3f pos = new Vector3f();
    public Vector3f target = new Vector3f();
    public float targetRadius = 3;
    public float targetRadiusMin = .2f;
    public float targetRadiusMax = 120;

    public float pitch = 0;
    public float yaw = -90;  // (initial -90 to look along z axis)
    public float roll = 0;  // TODO: not implemented

    public Vector3f worldUp = new Vector3f(0, 1, 0);
    protected Vector3f forward = new Vector3f();
    protected Vector3f right = new Vector3f();
    protected Vector3f up = new Vector3f();

    // controls
    public float rotSpeed = 70;
    public float moveSpeed = 3;
    public float mouseSensitivity = .1f;
    public float scrollAmount = 1;

    private boolean isMouseDown = false;
    private Vector2f mousePosOnClick;
    private Vector2f prevMousePos;  // for wayland

    ArrayList<CameraAction> keyMovementActions = new ArrayList<>(Arrays.asList(
            new CameraAction(GLFW_KEY_W, speed -> pos.add(forward.mul(speed, new Vector3f()))),
            new CameraAction(GLFW_KEY_S, speed -> pos.sub(forward.mul(speed, new Vector3f()))),
            new CameraAction(GLFW_KEY_D, speed -> pos.add(right.mul(speed, new Vector3f()))),
            new CameraAction(GLFW_KEY_A, speed -> pos.sub(right.mul(speed, new Vector3f()))),
            new CameraAction(GLFW_KEY_E, speed -> pos.add(up.mul(speed, new Vector3f()))),
            new CameraAction(GLFW_KEY_Q, speed -> pos.sub(up.mul(speed, new Vector3f()))),
            new CameraAction(GLFW_KEY_SPACE, speed -> pos.add(0, speed, 0)),
            new CameraAction(GLFW_KEY_LEFT_CONTROL, speed -> pos.sub(0, speed, 0))
    ));
    ArrayList<CameraAction> keyRotationActions = new ArrayList<>(Arrays.asList(
            new CameraAction(GLFW_KEY_UP, speed -> pitch += speed),
            new CameraAction(GLFW_KEY_DOWN, speed -> pitch -= speed),
            new CameraAction(GLFW_KEY_RIGHT, speed -> yaw += speed),
            new CameraAction(GLFW_KEY_LEFT, speed -> yaw -= speed),
            new CameraAction(GLFW_KEY_P, speed -> roll += speed),
            new CameraAction(GLFW_KEY_O, speed -> roll -= speed)
    ));

    public Camera3d(Dimension aspectSize, int mode) {
        this.captureSize = aspectSize;
        this.mode = mode;
        calculateDirections();
    }

    public Camera3d(Dimension aspectSize, int mode, Vector3f initialPos) {
        this(aspectSize, mode);
        pos = new Vector3f(initialPos);
        calculateDirections();
    }

    public Camera3d(Dimension aspectSize, int mode, int initialRadius) {
        this(aspectSize, mode);
        targetRadius = initialRadius;
        calculateDirections();
    }

    public Camera3d(Dimension aspectSize, int mode, Vector3f initialPos, int initialRadius) {
        this(aspectSize, mode);
        targetRadius = initialRadius;
        pos = new Vector3f(initialPos);
        calculateDirections();
    }

    public void setupUniformBuffer(ShaderProgram... shadersToBind) {
        if (vub.getId() != -1) {
            Logging.danger("Uniform buffer has already been setup. Call `bindShaderToUniformBlock` to bind more shaders.");
            return;
        }
        vub.genId();
        vub.bufferSize(MathUtils.MATRIX4F_BYTES_SIZE * 2);
        bindShaderToUniformBlock(shadersToBind);
    }

    public void bindShaderToUniformBlock(ShaderProgram... shadersToBind) {
        if (vub.getId() == -1) {
            Logging.danger("Uniform buffer has not yet been setup. Aborting");
            return;
        }
        vub.bindUniformBlock(uniformBlockName, shadersToBind);
    }

    /** Updates the perspective and the view if they have changed */
    public void updateUniformBlock() {
        if (hasChangedPerspective) {
            hasChangedPerspective = false;
            vub.bufferSubData(0, MathUtils.matrixToBuff(generatePerspectiveMatrix()));
        }

        if (hasChangedView) {
            hasChangedView = false;
            vub.bufferSubData(MathUtils.MATRIX4F_BYTES_SIZE, MathUtils.matrixToBuff(generateViewMatrix()));
        }
    }

    public void processKeyInputs(Window window, double dt) {
        float speedMul = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) ? 3 : (window.isKeyPressed(GLFW_KEY_LEFT_ALT) ? .3f : 1);
        boolean rotUpdated = false;

        // rotation
        float rSpeed = rotSpeed * speedMul * (float) dt;
        for (CameraAction action : keyRotationActions) {
            if (window.isKeyPressed(action.key)) {
                action.callback.call(rSpeed);
                rotUpdated = true;
                hasChangedView = true;
            }
        }

        if (rotUpdated) calculateDirections();

        // movement
        if (mode == MODE_FLY) {
            float mSpeed = moveSpeed * speedMul * (float) dt;
            for (CameraAction action : keyMovementActions) {
                if (window.isKeyPressed(action.key)) {
                    action.callback.call(mSpeed);
                    hasChangedView = true;
                }
            }
        }
    }

    public void processMouseInputs(Window window) {
        isMouseDown = false;
        if (window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_2)) {
            isMouseDown = true;
            mousePosOnClick = window.getCursorPos();
            prevMousePos = window.getCursorPos();
            if (glfwGetPlatform() != GLFW_PLATFORM_WAYLAND) window.hideCursor();
        } else window.showCursor();
    }

    public void processMouseMovement(Window window, float xPos, float yPos) {
        if (isMouseDown) {
            Vector2f delta = new Vector2f(xPos, yPos).sub(window.isWaylandPlatform() ? prevMousePos : mousePosOnClick);
            if (delta.y + delta.x == 0) return;

            if (window.isWaylandPlatform()) prevMousePos.set(xPos, yPos);
            else window.setCursorPos(mousePosOnClick);

            pitch -= delta.y * mouseSensitivity;
            yaw += delta.x * mouseSensitivity;
            calculateDirections();
            hasChangedView = true;
        }
    }

    public void processMouseScroll(Window window, float xDelta, float yDelta) {
        if (yDelta != 0) {
            hasChangedView = true;
            if (mode == MODE_FLY) {
                pos.add(forward.mul(yDelta * scrollAmount));
                calculateDirections();
            } else {
                targetRadius = Math.clamp(targetRadius - yDelta, targetRadiusMin, targetRadiusMax);
            }
        }
    }

    /**
     * Returns the position of the camera in target mode
     */
    public Vector3f targetingCameraPos() {
        return forward.mul(-targetRadius, new Vector3f()).add(target);
    }

    public Vector3f getPos() {
        if (mode == MODE_TARGET) return targetingCameraPos();
        return pos;
    }

    private Matrix4f generateTargetViewMatrix() {
        return new Matrix4f().lookAt(targetingCameraPos(), target, worldUp);
    }

    private Matrix4f generateFlyViewMatrix() {
        return new Matrix4f().lookAt(pos, pos.add(forward, new Vector3f()), worldUp);
    }

    public Matrix4f generateViewMatrix() {
        return switch (mode) {
            case MODE_FLY -> generateFlyViewMatrix();
            case MODE_TARGET -> generateTargetViewMatrix();
            default -> {
                Logging.danger("This camera's mode is invalid! current mode: (%s)", mode);
                yield new Matrix4f().identity();
            }
        };
    }

    public Matrix4f generatePerspectiveMatrix() {
        aspect = (float) captureSize.width / (float) captureSize.height;
        return new Matrix4f().identity().perspective((float) Math.toRadians(fov), aspect, near, far);
    }

    public void forceDirectionUpdate() {
        calculateDirections();
    }

    private void calculateDirections() {
        clampPitch();
        float cPitch = (float) Math.cos(Math.toRadians(pitch));
        float sPitch = (float) Math.sin(Math.toRadians(pitch));
        float cYaw = (float) Math.cos(Math.toRadians(yaw));
        float sYaw = (float) Math.sin(Math.toRadians(yaw));
        forward.set(cYaw * cPitch, sPitch, sYaw * cPitch).normalize();

        forward.cross(worldUp, right).normalize();
        right.cross(forward, up);
    }

    private void clampPitch() {
        pitch = Math.clamp(pitch, -89, 89);
    }

    public void setMode(int newMode) {
        if (mode == newMode) return;
        mode = newMode;
        hasChangedView = true;
    }

    public int getMode() {
        return mode;
    }

    public Vector3f getForward() {
        return new Vector3f(forward);
    }

    public Vector3f getRight() {
        return new Vector3f(right);
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }
}
