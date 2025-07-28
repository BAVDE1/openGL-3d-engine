package boilerplate.utility;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIQuaternion;
import org.lwjgl.assimp.AIVector2D;
import org.lwjgl.assimp.AIVector3D;

import java.nio.FloatBuffer;

public class MathUtils {
    public static double INV_PI = 1 / Math.PI;
    public static double INV_180 = 1 / 180d;
    public static int MATRIX4F_BYTES_SIZE = Float.BYTES * 4 * 4;

    public static double nanoToSecond(double nanoSecs) {
        return nanoSecs * 1E-9f;
    }

    public static double secondToNano(double seconds) {
        return seconds * 1E+9f;
    }

    public static double millisToSecond(double milliseconds) {
        return milliseconds * 1E-3f;
    }

    public static double secondToMillis(double seconds) {
        return seconds * 1E+3f;
    }

    public static double nanoToMillis(double nanoSecs) {
        return nanoSecs * 1E-6f;
    }

    public static double millisToNano(double nanoSecs) {
        return nanoSecs * 1E+6f;
    }

    public static float roundToPlace(float num, int place) {
        return (float) Math.round(num * place) / place;
    }

    public static FloatBuffer matrixToBuff(Matrix4f m) {
        return m.get(BufferUtils.createFloatBuffer(16));
    }

    public static Vector3f AIVectorToVector(AIVector3D aiVector3D) {
        return new Vector3f(aiVector3D.x(), aiVector3D.y(), aiVector3D.z());
    }

    public static Vector2f AIVectorToVector(AIVector2D aiVector2D) {
        return new Vector2f(aiVector2D.x(), aiVector2D.y());
    }

    public static Quaternionf AIQuatToQuat(AIQuaternion aiQuaternion) {
        return new Quaternionf(aiQuaternion.x(), aiQuaternion.y(), aiQuaternion.z(), aiQuaternion.w());
    }

    public static Matrix4f AIMatrixToMatrix(AIMatrix4x4 aiMatrix4x4) {
        Matrix4f m = new Matrix4f();
        m.m00(aiMatrix4x4.a1());
        m.m10(aiMatrix4x4.a2());
        m.m20(aiMatrix4x4.a3());
        m.m30(aiMatrix4x4.a4());
        m.m01(aiMatrix4x4.b1());
        m.m11(aiMatrix4x4.b2());
        m.m21(aiMatrix4x4.b3());
        m.m31(aiMatrix4x4.b4());
        m.m02(aiMatrix4x4.c1());
        m.m12(aiMatrix4x4.c2());
        m.m22(aiMatrix4x4.c3());
        m.m32(aiMatrix4x4.c4());
        m.m03(aiMatrix4x4.d1());
        m.m13(aiMatrix4x4.d2());
        m.m23(aiMatrix4x4.d3());
        m.m33(aiMatrix4x4.d4());
        return m;
    }
}
