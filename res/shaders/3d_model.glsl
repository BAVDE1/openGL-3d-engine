//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec3 tangent;
layout(location = 3) in vec3 bitangent;
layout(location = 4) in vec2 texCoords;
layout(location = 5) in ivec4 boneIds;
layout(location = 6) in vec4 boneWeights;
layout(location = 7) in int isStatic;

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
    vec3 fragPos;
    vec4 fragPosLightSpace;
    vec3 normal;
    vec2 texCoords;
    mat3 TBN;
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

    mat4 modelAnimTransformed = model * animTransformation;
    vec3 T = normalize(vec3(modelAnimTransformed * vec4(tangent, 0.0)));
    vec3 B = normalize(vec3(modelAnimTransformed * vec4(bitangent, 0.0)));
    vec3 N = normalize(vec3(modelAnimTransformed * vec4(normal, 0.0)));
    mat3 TBN = mat3(T, B, N);

    vs_out.fragPos = vec3(model * finalPos);
    vs_out.fragPosLightSpace = lightSpaceMatrix * vec4(vs_out.fragPos, 1);
    vs_out.normal = mat3(transpose(inverse(modelAnimTransformed))) * normal;
    vs_out.texCoords = texCoords;
    vs_out.TBN = TBN;
}

//--- FRAG
#version 450 core

struct Material {
    sampler2D diffuseTexture;
    sampler2D normalMap;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

struct Light {
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

struct BlinnPhong {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

vec3 gammaEncode(vec3 col);
vec3 calcPointLight(Light light, vec3 normal, vec3 diffuseTexture);
vec3 calcDirectionLighting(vec3 normal, vec3 diffuseTexture);
vec3 calcSpotLight(vec3 normal, vec3 diffuseTexture);
vec3 calcLightingWithShadows(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, Light light);
vec3 calcLighting(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, Light light);

const float GAMMA = 1.5;
const float EXPOSURE = .8;

const int LIGHT_COUNT = 2;
const float SHADOW_BIAS = .001;
const float POINT_SHADOW_BIAS = .05;
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

const vec3 POINT_SHADOW_MAP_OFFSETS[20] = vec3[](
    vec3( 1,  1,  1), vec3( 1, -1,  1), vec3(-1, -1,  1), vec3(-1,  1,  1),
    vec3( 1,  1, -1), vec3( 1, -1, -1), vec3(-1, -1, -1), vec3(-1,  1, -1),
    vec3( 1,  1,  0), vec3( 1, -1,  0), vec3(-1, -1,  0), vec3(-1,  1,  0),
    vec3( 1,  0,  1), vec3(-1,  0,  1), vec3( 1,  0, -1), vec3(-1,  0, -1),
    vec3( 0,  1,  1), vec3( 0, -1,  1), vec3( 0, -1, -1), vec3( 0,  1, -1)
);

uniform Material material;
uniform Light pointLights[LIGHT_COUNT];
uniform Light skyLight;
uniform Light spotLight;
uniform float flashLightStrength;
uniform vec3 viewPos;
uniform float farPlane;

uniform sampler2D shadowMap;
uniform samplerCube pointShadowMaps[LIGHT_COUNT];

in VS_OUT {
    vec3 fragPos;
    vec4 fragPosLightSpace;
    vec3 normal;
    vec2 texCoords;
    mat3 TBN;
} fs_in;

layout (location = 0) out vec4 colour;
layout (location = 1) out vec4 brightColour;

void main() {
//    if (material.normalMap != ERROR) {
        vec3 normal = texture(material.normalMap, fs_in.texCoords).rgb;
        normal = normalize(fs_in.TBN * (normal * 2.0 - 1.0));
//    } else {
//        vec3 normal = normalize(fs_in.normal);
//    }
    vec3 hdrCol = texture(material.diffuseTexture, fs_in.texCoords).xyz;
    vec3 mapped = vec3(1) - exp(-hdrCol * EXPOSURE);
    vec3 col = gammaEncode(mapped);

    vec3 finalCol = vec3(0);
    finalCol += calcDirectionLighting(normal, col);
    for (int i = 0; i < LIGHT_COUNT; i++) {
        finalCol += calcPointLight(pointLights[i], normal, col);
    }
    finalCol += calcSpotLight(normal, col) * flashLightStrength;
    colour = vec4(finalCol, 1);

    float brightness = dot(colour.rgb, vec3(0.2126, 0.7152, 0.0722));  // relative luminance
    if (brightness > 1.4) brightColour = vec4(colour.rgb, 1.0);
    else brightColour = vec4(0.0, 0.0, 0.0, 1.0);
}

vec3 gammaEncode(vec3 col) {
    return pow(col, vec3(1 / GAMMA));
}

float calcAttenuation(Light light) {
    float distance = length(light.position - fs_in.fragPos);
    return 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));
}

vec3 calcPointLight(Light light, vec3 normal, vec3 diffuseTexture) {
    float attenuation = calcAttenuation(light);
    vec3 viewDir = normalize(viewPos - fs_in.fragPos);
    vec3 lightDir = normalize(light.position - fs_in.fragPos);
    return calcLightingWithShadows(attenuation, viewDir, lightDir, normal, diffuseTexture, light);
}

vec3 calcDirectionLighting(vec3 normal, vec3 diffuseTexture) {
    vec3 viewDir = normalize(viewPos - fs_in.fragPos);
    vec3 lightDir = normalize(-skyLight.direction);
    return calcLightingWithShadows(1, viewDir, lightDir, normal, diffuseTexture, skyLight);
}

vec3 calcSpotLight(vec3 normal, vec3 diffuseTexture) {
    vec3 lightDir = normalize(spotLight.position - fs_in.fragPos);
    float theta = dot(lightDir, normalize(-spotLight.direction));
    if (theta > spotLight.outerCutoff) {  // cause of cosine: 0 degrees == cos 1, 90 degrees == cos 0
        float attenuation = calcAttenuation(spotLight);
        vec3 viewDir = normalize(viewPos - fs_in.fragPos);
        float intensity = clamp((theta - spotLight.outerCutoff) / (spotLight.cutoff - spotLight.outerCutoff), 0, 1);
        return calcLighting(attenuation * intensity, viewDir, lightDir, normal, diffuseTexture, spotLight);
    }
    return vec3(spotLight.ambient * diffuseTexture);
}

vec3 calcDirShadow() {
    vec3 shadow = vec3(0);
    vec3 projCoords = fs_in.fragPosLightSpace.xyz / fs_in.fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    for (int i = 0; i < 9; i++) {
        float pcfDepth = texture(shadowMap, projCoords.xy + SHADOW_MAP_OFFSETS[i]).r;
        shadow += projCoords.z - SHADOW_BIAS > pcfDepth ? .5 : 0;
    }
    return shadow / 9;
}

vec3 calcPointShadows() {
    vec3 shadow = vec3(0);
    for (int i = 0; i < LIGHT_COUNT; i++) {
        vec3 fragToLight = fs_in.fragPos - pointLights[i].position;
        float closestDepth = texture(pointShadowMaps[i], fragToLight).r * farPlane;
        float currentDepth = length(fragToLight);
        float attenuation = calcAttenuation(pointLights[i]);
        float addition = 0;
        for (int s = 0; s < 20; s++) {
            float closestDepth = texture(pointShadowMaps[i], fragToLight + POINT_SHADOW_MAP_OFFSETS[s] * .02).r * farPlane;
            addition += currentDepth - SHADOW_BIAS > closestDepth ? 1 : 0;
        }
        shadow += (pointLights[i].diffuse * attenuation) * addition / 20;
    }
    return shadow / LIGHT_COUNT;
}

vec3 calcShadows() {
    return (1 - calcDirShadow()) * (1 - calcPointShadows());
}

BlinnPhong blinnPhongComponents(vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, Light light) {
    float diff = max(dot(lightDir, normal), 0);

    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0), material.shininess);

    return BlinnPhong(
        light.ambient * diffuseTexture,
        light.diffuse * (diff * diffuseTexture),
        light.specular * (spec * material.specular)
    );
}

vec3 calcLightingWithShadows(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, Light light) {
    BlinnPhong bp = blinnPhongComponents(viewDir, lightDir, normal, diffuseTexture, light);
    return vec3(bp.ambient * attenuation + calcShadows() * (bp.diffuse * attenuation + bp.specular * attenuation));
}

vec3 calcLighting(float attenuation, vec3 viewDir, vec3 lightDir, vec3 normal, vec3 diffuseTexture, Light light) {
    BlinnPhong bp = blinnPhongComponents(viewDir, lightDir, normal, diffuseTexture, light);
    return vec3(bp.ambient * attenuation + bp.diffuse * attenuation + bp.specular * attenuation);
}
