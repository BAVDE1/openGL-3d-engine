//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texCoords;

struct Light {
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform Light skyLight;
uniform mat4 model;
uniform vec3 viewPos;

layout (std140) uniform CameraView {
    mat4 projection;
    mat4 view;
};

out VS_OUT {
    vec3 fragPos;
    vec2 texCoords;
    vec3 tangentViewPos;
    vec3 tangentFragPos;
    vec3 skyLightDirection;
    mat3 TBN;
} vs_out;

void main() {
    gl_Position = projection * view * (model * vec4(pos, 1));

    vs_out.texCoords = texCoords;
    vs_out.fragPos = vec3(model * vec4(pos, 1));

    vec3 T = normalize(vec3(model * vec4(-1, 0, 0, 0)));
    vec3 B = normalize(vec3(model * vec4(0, 0, -1, 0)));
    vec3 N = normalize(vec3(model * vec4(0, 1, 0, 0)));
    vs_out.TBN = transpose(mat3(T, B, N));
    vs_out.tangentViewPos = vs_out.TBN * viewPos;
    vs_out.tangentFragPos = vs_out.TBN * vs_out.fragPos;
    vs_out.skyLightDirection = vs_out.TBN * -skyLight.direction;
}

//--- FRAG
#version 450 core

struct Light {
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform Light skyLight;
uniform sampler2D diffuseTexture;
uniform sampler2D normalMap;
uniform sampler2D heightMap;

in VS_OUT {
    vec3 fragPos;
    vec2 texCoords;
    vec3 tangentViewPos;
    vec3 tangentFragPos;
    vec3 skyLightDirection;
    mat3 TBN;
} fs_in;

out vec4 colour;

vec3 doBlinnPhong(vec3 viewDir, vec3 normal, vec3 textureCol) {
    float diff = max(dot(fs_in.skyLightDirection, normal), 0);
    vec3 halfwayDir = normalize(fs_in.skyLightDirection + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0), 32);

    vec3 ambient = skyLight.ambient * textureCol;
    vec3 diffuse = skyLight.diffuse * (diff * textureCol);
    vec3 specular = skyLight.specular * (spec * textureCol);
    return vec3(ambient + diffuse + specular);
}

vec2 parallaxMapping(vec3 viewDir) {
    float height = texture(heightMap, fs_in.texCoords).r;
    vec2 p = viewDir.xy / viewDir.z * (height * .05);
    return fs_in.texCoords - p;
}

void main() {
    vec3 viewDir = normalize(fs_in.tangentViewPos - fs_in.tangentFragPos);

    // parallax
    vec2 projectedCoords = parallaxMapping(viewDir);
    if(projectedCoords.x > 1.0 || projectedCoords.y > 1.0 || projectedCoords.x < 0.0 || projectedCoords.y < 0.0)
        discard;

    vec3 normal = texture(normalMap, projectedCoords).rgb;
    normal = normalize(normal * 2.0 - 1.0);

    vec3 textureCol = texture(diffuseTexture, projectedCoords).rgb;
    vec3 blinnPhong = doBlinnPhong(viewDir, normal, textureCol);

    colour = vec4(blinnPhong, 1);
}