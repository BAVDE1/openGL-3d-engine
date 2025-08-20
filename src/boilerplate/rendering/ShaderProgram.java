package boilerplate.rendering;

import boilerplate.rendering.textures.Texture;
import boilerplate.rendering.textures.Texture2d;
import boilerplate.utility.Logging;
import boilerplate.utility.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL45;

import java.awt.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL45.*;

/**
 * Loads, compiles and links shaders from file / file paths to its own shader program.
 */
public class ShaderProgram {
    public static class Shader {
        int id, type;
        String resourcePath;

        Shader(int id, int type, String resourcePath) {
            this.id = id;
            this.type = type;
            this.resourcePath = resourcePath;
        }

        @Override
        public String toString() {
            return String.format("Shader(%s, %s, %s)", id, getShaderTypeString(type), resourcePath);
        }
    }

    public final HashMap<String, Integer> uniformCache = new HashMap<>();
    public final HashMap<String, Integer> uniformBlockCache = new HashMap<>();
    public final ArrayList<Shader> attachedShaders = new ArrayList<>();

    private Integer program;
    private boolean linked = false;

    public static boolean displayLocationWarnings = false;

    /**
     * Gen program, attach shader multi, & then link program
     */
    public void autoInitializeShadersMulti(String path) {
        genProgram();
        attachShaderMulti(path);
        linkProgram();
    }

    public void genProgram() {
        if (program != null) {
            Logging.warn("Attempting to re-generate already generated program, aborting");
            return;
        }
        program = glCreateProgram();
    }

    /**
     * Attach different shaders that are in the same file. MUST be separated with a "//--- SHADER_TYPE" line
     */
    public void attachShaderMulti(String path) {
        int currentShaderType = -1;

        Scanner scanner;
        try {
            scanner = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            Logging.danger("'%s' could not be read. Aborting", path);
            return;
        }

        StringBuilder charSequence = new StringBuilder();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.startsWith("//---")) {
                // finish last shader
                if (!charSequence.isEmpty()) {
                    attachShader(charSequence.toString(), currentShaderType, path);
                }

                // start next shader
                line = line.replace(" ", "").toLowerCase();
                currentShaderType = getShaderType(line.split("//---")[1]);
                charSequence = new StringBuilder();
                continue;
            }

            charSequence.append("\n").append(line);
        }

        // finish
        attachShader(charSequence.toString(), currentShaderType, path);
    }

    /**
     * adds new GL shader from given file (if applicable)
     * <a href="https://docs.gl/gl2/glAttachShader">Shader setup example</a>
     */
    public void attachShader(String path) {
        int shaderType = getShaderType(path);
        if (shaderType < 0) return;  // not a shader file

        Scanner scanner;
        try {
            scanner = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            Logging.danger("'%s' could not be read. Aborting", path);
            return;
        }

        String charSequence;
        StringBuilder fileContents = new StringBuilder();
        while (scanner.hasNextLine()) {
            fileContents.append("\n").append(scanner.nextLine());
        }
        charSequence = fileContents.toString();

        attachShader(charSequence, shaderType, path);
    }

    public void attachShader(String shaderCode, int shaderType, String filePath) {
        if (program == null || program < 0) {
            Logging.danger("No program set or program failed to be created.");
            return;
        }

        // compile shader
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, shaderCode);

        glCompileShader(shader);
        int[] shaderCompiled = new int[1];  // only needs size of 1
        glGetShaderiv(shader, GL_COMPILE_STATUS, shaderCompiled);
        if (shaderCompiled[0] != GL_TRUE) {
            throw new RuntimeException("Shader Compile Error (%s): %s".formatted(filePath, glGetShaderInfoLog(shader, 1024)));
        }

        glAttachShader(program, shader);
        attachedShaders.add(new Shader(shader, shaderType, filePath));
    }

    public void linkProgram() {
        if (linked) return;

        linked = true;
        glLinkProgram(program);
        int[] programLinked = new int[1];

        glGetProgramiv(program, GL_LINK_STATUS, programLinked);
        if (programLinked[0] != GL_TRUE) {
            throw new RuntimeException("Shader Linking Error: %s".formatted(glGetProgramInfoLog(program, 1024)));
        }
        Logging.debug("Shaders linked to program %s: %s", program, attachedShaders);
    }

    /**
     * <a href="https://www.khronos.org/opengl/wiki/Shader">OpenGL shaders</a>
     */
    private static int getShaderType(File file) {
        String[] splitName = file.getName().split("\\.");
        return getShaderType(splitName[splitName.length - 1]);  // file extension
    }

    private static int getShaderType(String typeString) {
        return switch (typeString) {
            case "vert", "vertex" -> GL_VERTEX_SHADER;
            case "tesc", "tessellation_control" -> GL_TESS_CONTROL_SHADER;
            case "tese", "tessellation_evaluation" -> GL_TESS_EVALUATION_SHADER;
            case "geom", "geometry" -> GL_GEOMETRY_SHADER;
            case "frag", "fragment" -> GL_FRAGMENT_SHADER;
            default -> -1;
        };
    }

    private static String getShaderTypeString(int shaderType) {
        return switch (shaderType) {
            case GL_VERTEX_SHADER -> "VERTEX";
            case GL_TESS_CONTROL_SHADER -> "TESS_CONTROL";
            case GL_TESS_EVALUATION_SHADER -> "TESS_EVALUATION";
            case GL_GEOMETRY_SHADER -> "GEOMETRY";
            case GL_FRAGMENT_SHADER -> "FRAGMENT";
            default -> "UNKNOWN";
        };
    }

    public void bind() {
        glUseProgram(program);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public int getProgram() {
        return program;
    }

    public void delete() {
        for (Shader shader : attachedShaders) glDeleteShader(shader.id);
        glDeleteProgram(program);
    }

    private int getCachedUniformOrSet(boolean isBlock, String uniformName) {
        if (!linked) {
            Logging.danger("Shader has not been linked!");
            return -1;
        }

        HashMap<String, Integer> cache = isBlock ? uniformBlockCache : uniformCache;
        if (!cache.containsKey(uniformName)) {
            int location = isBlock ? glGetUniformBlockIndex(program, uniformName) : glGetUniformLocation(program, uniformName);
            if (displayLocationWarnings && location == -1) {
                Logging.warn("The uniform for '%s' could not be found in the attached shaders '%s'", uniformName, attachedShaders);
            }
            cache.put(uniformName, location);
        }
        return cache.get(uniformName);
    }

    public int getUniformBlockLocation(String uniformBlock) {
        bind();
        return getCachedUniformOrSet(true, uniformBlock);
    }

    public int getUniformLocation(String uniform) {
        bind();
        return getCachedUniformOrSet(false, uniform);
    }

    public void uniform1i(String uniform, int i) {
        glUniform1i(getUniformLocation(uniform), i);
    }

    public void uniform1iv(String uniform, int[] intArray) {
        glUniform1iv(getUniformLocation(uniform), intArray);
    }

    public void uniform1f(String uniform, float f) {
        glUniform1f(getUniformLocation(uniform), f);
    }

    public void uniform2f(String uniform, Vector2f v) {
        uniform2f(uniform, v.x, v.y);
    }

    public void uniform2f(String uniform, float f1, float f2) {
        glUniform2f(getUniformLocation(uniform), f1, f2);
    }

    public void uniform3f(String uniform, Vector3f v) {
        uniform3f(uniform, v.x, v.y, v.z);
    }

    public void uniform3f(String uniform, float f1, float f2, float f3) {
        glUniform3f(getUniformLocation(uniform), f1, f2, f3);
    }

    public void uniform4f(String uniform, Vector4f v) {
        uniform4f(uniform, v.x, v.y, v.z, v.w);
    }

    public void uniform4f(String uniform, float f1, float f2, float f3, float f4) {
        glUniform4f(getUniformLocation(uniform), f1, f2, f3, f4);
    }

    public void uniformMatrix4f(String uniform, Matrix4f m) {
        uniformMatrix4f(uniform, MathUtils.matrixToBuff(m));
    }

    public void uniformMatrix4fArray(String uniform, Matrix4f... matrices) {
        for (int i = 0; i < matrices.length; i++) {
            uniformMatrix4f(uniform + "[%s]".formatted(i), matrices[i]);
        }
    }

    public void uniformMatrix4f(String uniform, FloatBuffer buffer) {
        glUniformMatrix4fv(getUniformLocation(uniform), false, buffer);
    }

    public void uniformMatrix4f(String uniform, float[] matFloats) {
        glUniformMatrix4fv(getUniformLocation(uniform), false, matFloats);
    }

    public void uniformTexture(String uniform, Texture texture, int slot) {
        uniform1i(uniform, slot);
        texture.bind(slot);
    }

    public void uniformBool(String uniform, boolean val) {
        glUniform1i(getUniformLocation(uniform), val ? 1 : 0);
    }

    public boolean isSetup() {
        return program != null;
    }
}
