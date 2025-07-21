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

out vec3 v_fragPos;
out vec4 v_fragPosLightSpace;
out vec3 v_normal;
out vec2 v_texCoords;

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

    v_fragPos = vec3(model * finalPos);
    v_fragPosLightSpace = lightSpaceMatrix * vec4(v_fragPos, 1);
    v_normal = mat3(transpose(inverse(model * animTransformation))) * normal;
    v_texCoords = texCoords;
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
vec3 calcLighting(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, vec3 lightAmbient, vec3 lightDiffuse, vec3 lightSpecular);

const float GAMMA = 1.5;
const float EXPOSURE = .8;
const int LIGHT_COUNT = 2;
const float SHADOW_BIAS = .001;
const float SHADOW_MAP_TEXEL_SIZE = 1.0 / (1000.0 * 2);
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

in vec3 v_fragPos;
in vec4 v_fragPosLightSpace;
in vec3 v_normal;
in vec2 v_texCoords;

out vec4 colour;

void main() {
    vec3 normal = normalize(v_normal);
    vec3 hdrCol = texture(material.diffuseTexture, v_texCoords).xyz;
    vec3 mapped = vec3(1) - exp(-hdrCol * EXPOSURE);
    vec3 gammaCol = gammaSpace(mapped);

    vec3 finalCol = vec3(0);
    finalCol += calcDirectionLighting(normal, gammaCol);
    for (int i = 0; i < LIGHT_COUNT; i++) {
        finalCol += calcPointLight(lights[i], normal, gammaCol);
    }
    finalCol += calcSpotLight(normal, gammaCol) * flashLightStrength;

    colour = vec4(finalCol, 1);
}

vec3 gammaSpace(vec3 linCol) {
    return pow(linCol, vec3(1 / GAMMA));
}

float calcAttenuation(vec3 lightPos, float c, float l, float q) {
    float distance = length(lightPos - v_fragPos);
    return 1.0 / (c + l * distance + q * (distance * distance));
}

vec3 calcPointLight(PointLight light, vec3 normal, vec3 diffuseTexture) {
    float attenuation = calcAttenuation(light.position, light.constant, light.linear, light.quadratic);
    vec3 viewDir = normalize(viewPos - v_fragPos);
    vec3 lightDir = normalize(light.position - v_fragPos);
    return calcLighting(attenuation, viewDir, lightDir, normal, diffuseTexture, light.ambient, light.diffuse, light.specular);
}

vec3 calcDirectionLighting(vec3 normal, vec3 diffuseTexture) {
    vec3 viewDir = normalize(viewPos - v_fragPos);
    vec3 lightDir = normalize(-skyLight.direction);
    return calcLighting(1, viewDir, lightDir, normal, diffuseTexture, skyLight.ambient, skyLight.diffuse, skyLight.specular);
}

vec3 calcSpotLight(vec3 normal, vec3 diffuseTexture) {
    vec3 lightDir = normalize(spotLight.position - v_fragPos);
    float theta = dot(lightDir, normalize(-spotLight.direction));
    if (theta > spotLight.outerCutoff) {  // cause of cosine: 0 degrees == cos 1, 90 degrees == cos 0
        float attenuation = calcAttenuation(spotLight.position, spotLight.constant, spotLight.linear, spotLight.quadratic);
        vec3 viewDir = normalize(viewPos - v_fragPos);
        float intensity = clamp((theta - spotLight.outerCutoff) / (spotLight.cutoff - spotLight.outerCutoff), 0, 1);
        return calcLighting(attenuation * intensity, viewDir, lightDir, normal, diffuseTexture, spotLight.ambient, spotLight.diffuse, spotLight.specular);
    }
    return vec3(spotLight.ambient * diffuseTexture);
}

float calcShadow(vec3 normal, vec3 lightDir) {
    float shadow = 0;
    vec3 projCoords = v_fragPosLightSpace.xyz / v_fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    for (int i = 0; i < 9; i++) {
        float pcfDepth = texture(shadowMap, projCoords.xy + SHADOW_MAP_OFFSETS[i]).r;
        shadow += projCoords.z - SHADOW_BIAS > pcfDepth ? .6 : 0;
    }
    return shadow / 9;
}

vec3 calcLighting(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, vec3 lightAmbient, vec3 lightDiffuse, vec3 lightSpecular) {
    float diff = max(dot(lightDir, normal), 0);

    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0), material.shininess);

    vec3 ambient = lightAmbient * diffuseTexture;
    vec3 diffuse = lightDiffuse * (diff * diffuseTexture);
    vec3 specular = lightSpecular * (spec * material.specular);
    return vec3(ambient * attenuation + (1 - calcShadow(normal, lightDir)) * (diffuse * attenuation + specular * attenuation));
}
