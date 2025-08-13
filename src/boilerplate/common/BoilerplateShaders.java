package boilerplate.common;

public class BoilerplateShaders {
    public static String safeFormat(String shaderCode, String ignore, Object... args) {
        String safeCode = shaderCode.replaceAll(ignore, "~~~~");
        safeCode = String.format(safeCode, args);
        return safeCode.replaceAll("~~~~", ignore);
    }

    public static String Text2DVertex = """
            #version 450 core
            
            layout(location = 0) in vec2 pos;
            layout(location = 1) in vec2 texturePos;
            layout(location = 2) in vec4 texColour;
            
            layout (std140) uniform %s {
                mat4 view;
            };
            
            out vec2 v_texturePos;
            out vec4 v_texColour;
            
            void main() {
                gl_Position = view * vec4(pos.xy, 1, 1);
                v_texturePos = texturePos;
                v_texColour = texColour;
            }
            """;

    public static String Text2DFragment = """
            #version 450 core
            
            uniform sampler2D fontTexture;
            
            in vec2 v_texturePos;
            in vec4 v_texColour;
            
            out vec4 colour;
            
            void main() {
                float alpha = v_texturePos.x > -1 ? texture(fontTexture, v_texturePos.xy).a : 1;
                colour = (v_texColour * alpha) / 255;
            }
            """;

    public static String SkyBoxVertex = """
            #version 450 core
            
            layout(location = 0) in vec3 pos;
            
            layout (std140) uniform %s {
                mat4 projection;
                mat4 view;
            };
            
            out vec3 v_texPos;
            
            void main() {
                gl_Position = (projection * mat4(mat3(view)) * vec4(pos, 1)).xyww;  // z values are always maximum (1.0) (w / w = 1.0)
                v_texPos = pos;
            }
            """;

    public static String SkyBoxFragment = """
            #version 450 core
            
            const float EXPOSURE = .8;
            
            uniform samplerCube skyBoxTexture;
            
            in vec3 v_texPos;
            
            layout (location = 0) out vec4 colour;
            layout (location = 1) out vec4 brightColour;
            
            void main() {
                colour = vec4(texture(skyBoxTexture, v_texPos).xyz, 1);
            
                float brightness = dot(colour.rgb, vec3(0.2126, 0.7152, 0.0722));
                if (brightness > 0.99) brightColour = vec4(colour.rgb, 1.0);
                else brightColour = vec4(0.0, 0.0, 0.0, 1.0);
            }
            """;

    public static String ModelBoneVertex = """
            #version 450 core
            
            layout(location = 0) in int boneId;
            
            layout (std140) uniform %s {
                mat4 projection;
                mat4 view;
            };
            
            const int MAX_BONES = 100;
            
            uniform mat4 model;
            uniform mat4 finalBonesMatrices[MAX_BONES];
            
            void main() {
                vec4 pos = finalBonesMatrices[boneId] * vec4(0, 0, 0, 1);
                gl_Position = projection * view * model * pos;
            }
            """;

    public static String ModelBoneFragment = """
            #version 450 core
            
            out vec4 colour;
            
            void main() {
                colour = vec4(1, 0, 0, 1);
            }
            """;
}
