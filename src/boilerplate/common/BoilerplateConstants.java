package boilerplate.common;

import boilerplate.utility.Logging;

import java.awt.*;
import java.io.File;
import java.net.URISyntaxException;

import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;

public class BoilerplateConstants {
    public static final String LOGGING_FILE_NAME = "latest.log";
    public static final int GLFW_VERSION_MAJOR = 4;
    public static final int GLFW_VERSION_MINOR = 5;
    public static final int GLFW_OPENGL_PROFILE = GLFW_OPENGL_CORE_PROFILE;

    public static final int BUFF_SIZE_SMALL    = 1024;
    public static final int BUFF_SIZE_MEDIUM   = BUFF_SIZE_SMALL   * 2;
    public static final int BUFF_SIZE_LARGE    = BUFF_SIZE_MEDIUM  * 2;
    public static int BUFF_SIZE_MAX            = BUFF_SIZE_LARGE * 16;
    public static int BUFF_SIZE_DEFAULT        = BUFF_SIZE_SMALL;

    public static final int DEMO_MODE_NIL = 0;
    public static final int DEMO_MODE_TEX = 1;
    public static final int DEMO_MODE_COL = 2;

    public static final int ERROR = -1;
    public static final double EPSILON = 0.0001;
    public static final float THREE_SQRT = 1.7321f;
    public static final double EPSILON_SQ = EPSILON * EPSILON;
    public static final int FPS = 60;
    public static final double DT = 1 / (double) FPS;

    public static final Dimension DEFAULT_SCREEN_SIZE = new Dimension(500, 500);

    public static final boolean OPTIMIZE_TIME_STEPPER = true;

    /** <a href="https://en.wikipedia.org/wiki/Orthographic_projection">projection matrix source</a> */
    @Deprecated
    public static float[] create2dProjectionMatrix(Dimension dimension) {
        return new float[] {
                2f/dimension.width, 0,                    0,  -1,
                0,                  2f/-dimension.height, 0,   1,
                0,                  0,                    1,   0,
                0,                  0,                    0,   1
        };
    }

    public static int findNextLargestBuffSize(int givenSize) {
        if (givenSize >= BUFF_SIZE_MAX) {
            Logging.danger("Maximum buffer size reached (max: %s)! Attempting to find a size greater than the given '%s', which doesn't currently exist.\n" +
                    "Optimise the buffer, or assign a greater maximum size.", BUFF_SIZE_MAX, givenSize);
            return ERROR;
        }

        // use set buffer sizes
        for (int size : new int[] {BUFF_SIZE_SMALL, BUFF_SIZE_MEDIUM, BUFF_SIZE_LARGE}) {
            if (size > givenSize) return size;
        }

        // calc new size
        int mul = 2;
        int newSize;
        do {
            newSize = BUFF_SIZE_LARGE * mul;
            mul++;
        } while (newSize < givenSize);
        return newSize;
    }

    public static String getJarFolder() {
        try {
            String path = new File(BoilerplateConstants.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            path = path.replace('/', File.separatorChar);

            int jarInx = path.indexOf(".jar");
            if (jarInx == -1) return null;
            path = path.substring(0, jarInx + 4);

            int driveInx = path.lastIndexOf(':');
            if (driveInx != -1) path = path.substring(driveInx - 1);
            return path.substring(0, path.lastIndexOf(File.separatorChar) + 1);
        } catch (URISyntaxException e) {
            return String.format("Error: %s", e);
        }
    }
}
