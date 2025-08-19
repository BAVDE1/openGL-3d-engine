package boilerplate.rendering.camera;

import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.buffers.VertexUniformBuffer;
import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.*;

public abstract class Camera {
    public abstract Matrix4f generateProjectionMatrix();
    public abstract Matrix4f generateViewMatrix();

    public boolean hasChangedView = true;
    public boolean hasChangedProjection = true;

    protected VertexUniformBuffer vub = new VertexUniformBuffer();
    public String uniformBlockName = "CameraView";

    // projection
    public Vector2f captureSize;
    protected float aspect;
    public float near = .05f;
    public float far = 50;

    // view
    public Vector3f pos = new Vector3f();
    public Vector3f worldUp = new Vector3f(0, 1, 0);

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
            vub.bufferSubData(0, MathUtils.matrixToBuff(generateProjectionMatrix()));
        }

        if (hasChangedView) {
            hasChangedView = false;
            vub.bufferSubData(MathUtils.MATRIX4F_BYTES_SIZE, MathUtils.matrixToBuff(generateViewMatrix()));
        }
    }
}
