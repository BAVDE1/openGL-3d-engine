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

public class CameraOrtho {
    public boolean hasChangedView = true;
    public boolean hasChangedProjection = true;

    protected VertexUniformBuffer vub = new VertexUniformBuffer();
    public String uniformBlockName = "CameraView";

    // perspective
    public Dimension captureSize;
    protected float aspect;
    public float near = .05f;
    public float far = 50;

    // view
    public Vector3f pos = new Vector3f();
    public Vector3f worldUp = new Vector3f(0, 1, 0);
    protected Vector3f forward = new Vector3f(0, 0, 1);
    protected Vector3f up = new Vector3f(worldUp);

    // controls
    public float moveSpeed = 3;
    public float mouseSensitivity = .1f;
    public float scrollAmount = 1;

    private boolean isMouseDown = false;
    private Vector2f mousePosOnClick;
    private Vector2f prevMousePos;  // for wayland

    ArrayList<CameraKeyAction> keyMovementActions = new ArrayList<>(Arrays.asList(
            new CameraKeyAction(GLFW_KEY_W, speed -> pos.add(up.mul(speed, new Vector3f()))),
            new CameraKeyAction(GLFW_KEY_S, speed -> pos.sub(up.mul(speed, new Vector3f()))),
            new CameraKeyAction(GLFW_KEY_D, speed -> pos.sub(up.cross(forward, new Vector3f()).mul(speed))),
            new CameraKeyAction(GLFW_KEY_A, speed -> pos.add(up.cross(forward, new Vector3f()).mul(speed))),
            new CameraKeyAction(GLFW_KEY_E, speed -> up.rotateAxis(speed, forward.x, forward.y, forward.z)),
            new CameraKeyAction(GLFW_KEY_Q, speed -> up.rotateAxis(-speed, forward.x, forward.y, forward.z))
    ));
    ArrayList<CameraKeyAction> keyRotationActions = new ArrayList<>();

    public CameraOrtho(Dimension aspectSize) {
        this.captureSize = aspectSize;
    }

    public CameraOrtho(Dimension aspectSize, Vector3f initialPos) {
        this(aspectSize);
        pos = new Vector3f(initialPos);
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
        if (hasChangedProjection) {
            hasChangedProjection = false;
            vub.bufferSubData(0, MathUtils.matrixToBuff(generateOrthoMatrix()));
        }

        if (hasChangedView) {
            hasChangedView = false;
            vub.bufferSubData(MathUtils.MATRIX4F_BYTES_SIZE, MathUtils.matrixToBuff(generateViewMatrix()));
        }
    }

    public void processKeyInputs(Window window, double dt) {
        float speedMul = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) ? 3 : (window.isKeyPressed(GLFW_KEY_LEFT_ALT) ? .3f : 1);

        // movement
        float mSpeed = moveSpeed * speedMul * (float) dt;
        for (CameraKeyAction action : keyMovementActions) {
            if (window.isKeyPressed(action.key)) {
                action.callback.call(mSpeed);
                hasChangedView = true;
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
            hasChangedView = true;
        }
    }

    public void processMouseScroll(Window window, float xDelta, float yDelta) {
        if (yDelta != 0) {
            // todo
        }
    }

    private Matrix4f generateViewMatrix() {
        return new Matrix4f().lookAt(pos, pos.add(forward, new Vector3f()), up);
    }

    public Matrix4f generateOrthoMatrix() {
        return new Matrix4f().identity().ortho(-captureSize.width, captureSize.width, -captureSize.height, captureSize.height, near, far);
    }

    public Vector3f getForward() {
        return new Vector3f(forward);
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }
}
