//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texCoords;
layout(location = 3) in ivec4 boneIds;
layout(location = 4) in vec4 boneWeights;
layout(location = 5) in int isStatic;

layout (std140) uniform CameraView {
    mat4 projection;
    mat4 view;
};

const int MAX_BONES = 200;
const int MAX_BONE_INFLUENCE = 4;

uniform mat4 model;
uniform mat4 finalBonesMatrices[MAX_BONES];
uniform mat4 lightSpaceMatrix;

out VS_OUT {
    vec3 v_fragPos;
    vec4 v_fragPosLightSpace;
    vec3 v_normal;
    vec2 v_texCoords;
} vs_out;

void main() {
    mat4 animTransformation = mat4(
    1, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1, 0,
    0, 0, 0, 1
    ) * isStatic;// only use identity if its not static (ie if its not animated)
    for (int i = 0; i < MAX_BONE_INFLUENCE; i++) {
        if (boneIds[i] == -1) continue;
        if (boneIds[i] >= MAX_BONES) break;
        animTransformation += finalBonesMatrices[boneIds[i]] * boneWeights[i];
    }

    vec4 finalPos = animTransformation * vec4(pos, 1);
    gl_Position = projection * view * (model * finalPos);

    vs_out.v_fragPos = vec3(model * finalPos);
    vs_out.v_fragPosLightSpace = lightSpaceMatrix * vec4(vs_out.v_fragPos, 1);
    vs_out.v_normal = mat3(transpose(inverse(model * animTransformation))) * normal;
    vs_out.v_texCoords = texCoords;
}

//--- GEOM
#version 450 core
layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

in VS_OUT {
    vec3 v_fragPos;
    vec4 v_fragPosLightSpace;
    vec3 v_normal;
    vec2 v_texCoords;
} gs_in[];

out GS_OUT {
    vec3 v_fragPos;
    vec4 v_fragPosLightSpace;
    vec3 v_normal;
    vec2 v_texCoords;
} gs_out;

void vert(int index) {
    gl_Position = gl_in[index].gl_Position;
    gs_out.v_fragPos = gs_in[index].v_fragPos;
    gs_out.v_fragPosLightSpace = gs_in[index].v_fragPosLightSpace;
    gs_out.v_normal = gs_in[index].v_normal;
    gs_out.v_texCoords = gs_in[index].v_texCoords;
    EmitVertex();
}

void main() {
    vert(0);
    vert(1);
    vert(2);
    EndPrimitive();
}

//--- FRAG
#version 450 core

struct Material {
    sampler2D diffuseTexture;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

struct PointLight {
    vec3 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
};

struct DirectionalLight {
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

struct SpotLight {
    vec3 position;
    vec3 direction;
    float cutoff;
    float outerCutoff;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
};

vec3 gammaSpace(vec3 linCol);
vec3 calcPointLight(PointLight light, vec3 normal, vec3 diffuseTexture);
vec3 calcDirectionLighting(vec3 normal, vec3 diffuseTexture);
vec3 calcSpotLight(vec3 normal, vec3 diffuseTexture);
vec3 calcLightingShadow(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, vec3 lightAmbient, vec3 lightDiffuse, vec3 lightSpecular);
vec3 calcLighting(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, vec3 lightAmbient, vec3 lightDiffuse, vec3 lightSpecular);

const float GAMMA = 1.5;
const float EXPOSURE = .8;

const int LIGHT_COUNT = 2;
const float SHADOW_BIAS = .001;
const float SHADOW_MAP_TEXEL_SIZE = 1.0 / (800.0 * 2);
const vec2 SHADOW_MAP_OFFSETS[9] = vec2[](
    vec2(-SHADOW_MAP_TEXEL_SIZE,  SHADOW_MAP_TEXEL_SIZE), // top-left
    vec2( 0.0f,       SHADOW_MAP_TEXEL_SIZE), // top-center
    vec2( SHADOW_MAP_TEXEL_SIZE,  SHADOW_MAP_TEXEL_SIZE), // top-right
    vec2(-SHADOW_MAP_TEXEL_SIZE,  0.0f),   // center-left
    vec2( 0.0f,       0.0f),   // center-center
    vec2( SHADOW_MAP_TEXEL_SIZE,  0.0f),   // center-right
    vec2(-SHADOW_MAP_TEXEL_SIZE, -SHADOW_MAP_TEXEL_SIZE), // bottom-left
    vec2( 0.0f,      -SHADOW_MAP_TEXEL_SIZE), // bottom-center
    vec2( SHADOW_MAP_TEXEL_SIZE, -SHADOW_MAP_TEXEL_SIZE)  // bottom-right
);

uniform Material material;
uniform PointLight lights[LIGHT_COUNT];
uniform DirectionalLight skyLight;
uniform SpotLight spotLight;
uniform float flashLightStrength;
uniform vec3 viewPos;
uniform sampler2D shadowMap;

in GS_OUT {
    vec3 v_fragPos;
    vec4 v_fragPosLightSpace;
    vec3 v_normal;
    vec2 v_texCoords;
} gs_in;

layout (location = 0) out vec4 colour;
layout (location = 1) out vec4 brightColour;

void main() {
    vec3 normal = normalize(gs_in.v_normal);
    vec3 hdrCol = texture(material.diffuseTexture, gs_in.v_texCoords).xyz;
    vec3 mapped = vec3(1) - exp(-hdrCol * EXPOSURE);
    vec3 gammaCol = gammaSpace(mapped);

    vec3 finalCol = vec3(0);
    finalCol += calcDirectionLighting(normal, gammaCol);
    for (int i = 0; i < LIGHT_COUNT; i++) {
        finalCol += calcPointLight(lights[i], normal, gammaCol);
    }
    finalCol += calcSpotLight(normal, gammaCol) * flashLightStrength;

    colour = vec4(finalCol, 1);

    float brightness = dot(colour.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 1.3) brightColour = vec4(colour.rgb, 1.0);
    else brightColour = vec4(0.0, 0.0, 0.0, 1.0);
}

vec3 gammaSpace(vec3 linCol) {
    return pow(linCol, vec3(1 / GAMMA));
}

float calcAttenuation(vec3 lightPos, float c, float l, float q) {
    float distance = length(lightPos - gs_in.v_fragPos);
    return 1.0 / (c + l * distance + q * (distance * distance));
}

vec3 calcPointLight(PointLight light, vec3 normal, vec3 diffuseTexture) {
    float attenuation = calcAttenuation(light.position, light.constant, light.linear, light.quadratic);
    vec3 viewDir = normalize(viewPos - gs_in.v_fragPos);
    vec3 lightDir = normalize(light.position - gs_in.v_fragPos);
    return calcLightingShadow(attenuation, viewDir, lightDir, normal, diffuseTexture, light.ambient, light.diffuse, light.specular);
}

vec3 calcDirectionLighting(vec3 normal, vec3 diffuseTexture) {
    vec3 viewDir = normalize(viewPos - gs_in.v_fragPos);
    vec3 lightDir = normalize(-skyLight.direction);
    return calcLightingShadow(1, viewDir, lightDir, normal, diffuseTexture, skyLight.ambient, skyLight.diffuse, skyLight.specular);
}

vec3 calcSpotLight(vec3 normal, vec3 diffuseTexture) {
    vec3 lightDir = normalize(spotLight.position - gs_in.v_fragPos);
    float theta = dot(lightDir, normalize(-spotLight.direction));
    if (theta > spotLight.outerCutoff) {  // cause of cosine: 0 degrees == cos 1, 90 degrees == cos 0
        float attenuation = calcAttenuation(spotLight.position, spotLight.constant, spotLight.linear, spotLight.quadratic);
        vec3 viewDir = normalize(viewPos - gs_in.v_fragPos);
        float intensity = clamp((theta - spotLight.outerCutoff) / (spotLight.cutoff - spotLight.outerCutoff), 0, 1);
        return calcLighting(attenuation * intensity, viewDir, lightDir, normal, diffuseTexture, spotLight.ambient, spotLight.diffuse, spotLight.specular);
    }
    return vec3(spotLight.ambient * diffuseTexture);
}

float calcShadow(vec3 normal, vec3 lightDir) {
    float shadow = 0;
    vec3 projCoords = gs_in.v_fragPosLightSpace.xyz / gs_in.v_fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    for (int i = 0; i < 9; i++) {
        float pcfDepth = texture(shadowMap, projCoords.xy + SHADOW_MAP_OFFSETS[i]).r;
        shadow += projCoords.z - SHADOW_BIAS > pcfDepth ? .6 : 0;
    }
    return shadow / 9;
}

vec3[3] blinnPhongComponents(vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, vec3 lightAmbient, vec3 lightDiffuse, vec3 lightSpecular) {
    float diff = max(dot(lightDir, normal), 0);

    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0), material.shininess);

    vec3 ambient = lightAmbient * diffuseTexture;
    vec3 diffuse = lightDiffuse * (diff * diffuseTexture);
    vec3 specular = lightSpecular * (spec * material.specular);
    return vec3[3] (ambient, diffuse, specular);
}

vec3 calcLightingShadow(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, vec3 lightAmbient, vec3 lightDiffuse, vec3 lightSpecular) {
    vec3 components[3] = blinnPhongComponents(viewDir, lightDir, normal, diffuseTexture, lightAmbient, lightDiffuse, lightSpecular);
    return vec3(components[0] * attenuation + (1 - calcShadow(normal, lightDir)) * (components[1] * attenuation + components[2] * attenuation));
}

vec3 calcLighting(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, vec3 lightAmbient, vec3 lightDiffuse, vec3 lightSpecular) {
    vec3 components[3] = blinnPhongComponents(viewDir, lightDir, normal, diffuseTexture, lightAmbient, lightDiffuse, lightSpecular);
    return vec3(components[0] * attenuation + components[1] * attenuation + components[2] * attenuation);
}
