package boilerplate.rendering;

import boilerplate.rendering.buffers.VertexArray;
import boilerplate.rendering.buffers.VertexElementBuffer;
import org.lwjgl.opengl.GL;
import boilerplate.common.Window;
import boilerplate.rendering.text.TextRenderer;
import boilerplate.utility.Logging;
import org.lwjgl.opengl.GL45;

import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.opengl.GL45.*;

/**
 * State Machine
 * A bunch of common quick-access rendering utilities & toggles
 */
public class Renderer {
    /**
     * Do before anything GL related!
     */
    public static void setupGLContext(boolean setupDefaults) {
        GL.createCapabilities();

        if (setupDefaults) {
            enableDebugOutput();
            enableTexture2d();
            useDefaultBlend();
            setClearColour(0, 0, 0, 0);
        }

        Logging.debug("OpenGL capabilities created");
    }

    public static void setViewportSize(int size) {
        setViewportSize(size, size);
    }

    public static void setViewportSize(int width, int height) {
        glViewport(0, 0, width, height);
    }

    public static void enableDebugOutput() {
        glEnable(GL_DEBUG_OUTPUT);
    }

    public static void enableTexture2d() {
        glEnable(GL_TEXTURE_2D);
    }

    public static void enableDepthTest() {
        glEnable(GL_DEPTH_TEST);
    }

    public static void disableDepthTest() {
        glDisable(GL_DEPTH_TEST);
    }

    public static void enableStencilTest() {
        glEnable(GL_STENCIL_TEST);
    }

    public static void disableStencilTest() {
        glDisable(GL_STENCIL_TEST);
    }

    public static void setStencilOperation(int stencilFails, int stencilPassDepthFails, int stencilPassDepthPass) {
        glStencilOp(stencilFails, stencilPassDepthFails, stencilPassDepthPass);
    }

    public static void setStencilFunc(int function, int reference, boolean maskAllowAll) {
        glStencilFunc(function, reference, maskAllowAll ? 0xFF : 0x00);
    }

    public static void enableFramebufferGamma() {
        glEnable(GL_FRAMEBUFFER_SRGB);
    }

    public static void disableFramebufferGamma() {
        glDisable(GL_FRAMEBUFFER_SRGB);
    }

    public static void useDefaultFaceCulling() {
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);  // note: winding order is calculated after vertex shader
    }

    public static void enableFaceCulling() {
        glEnable(GL_CULL_FACE);
    }

    public static void enableFaceCulling(int face) {
        glEnable(GL_CULL_FACE);
        glCullFace(face);
    }

    public static void disableFaceCulling() {
        glDisable(GL_CULL_FACE);
    }

    public static void cullFrontFace() {
        enableFaceCulling(GL_FRONT);
    }

    public static void cullBackFace() {
        enableFaceCulling(GL_BACK);
    }

    public static void setWindingOrder(int order) {
        glFrontFace(order);
    }

    public static void enableStencilWriting() {
        glStencilMask(0xFF);  // bitwise && 1, allows everything
    }

    public static void disableStencilWriting() {
        glStencilMask(0x00);  // bitwise && 0, allows nothing
    }

    public static void enableMultiSampling() {
        glEnable(GL_MULTISAMPLE);
    }

    public static void disableMultiSampling() {
        glDisable(GL_MULTISAMPLE);
    }

    /**
     * logically behaving transparency
     */
    public static void useDefaultBlend() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void setClearColour(float r, float g, float b, float a) {
        glClearColor(r, g, b, a);
    }

    /**
     * Clear calls apply to the currently bound framebuffer
     */
    public static void clearCDS() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public static void clearC() {
        glClear(GL_COLOR_BUFFER_BIT);
    }

    public static void clearD() {
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public static void clearDS() {
        glClear(GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public static void clearCD() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public static void finish(Window window) {
        glfwSwapBuffers(window.handle);
    }
}
