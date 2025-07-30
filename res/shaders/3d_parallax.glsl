//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texCoords;

uniform mat4 model;

layout (std140) uniform CameraView {
    mat4 projection;
    mat4 view;
};

out vec2 v_texCoords;
out vec3 v_fragPos;
out mat3 v_TBN;

void main() {
    gl_Position = projection * view * (model * vec4(pos, 1));

    v_texCoords = texCoords;
    v_fragPos = vec3(model * vec4(pos, 1));

    vec3 T = normalize(vec3(model * vec4(0, 0, 1, 0)));
    vec3 B = normalize(vec3(model * vec4(1, 0, 0, 0)));
    vec3 N = normalize(vec3(model * vec4(0, 1, 0, 0)));
//    T = normalize(T - dot(T, N) * N);
//    vec3 B = cross(N, T);
    v_TBN = mat3(T, B, N);
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
uniform vec3 viewPos;
uniform sampler2D diffuseTexture;
uniform sampler2D normalMap;
uniform sampler2D heightMap;

in vec2 v_texCoords;
in vec3 v_fragPos;
in mat3 v_TBN;

out vec4 colour;

vec3 doBlinnPhong(vec3 lightDir, vec3 viewDir, vec3 normal, vec3 textureCol) {
    float diff = max(dot(lightDir, normal), 0);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0), 32);

    vec3 ambient = skyLight.ambient * textureCol;
    vec3 diffuse = skyLight.diffuse * (diff * textureCol);
    vec3 specular = skyLight.specular * (spec * textureCol);
    return vec3(ambient + diffuse + specular);
}

void main() {
    vec3 normal = texture(normalMap, v_texCoords).rgb;
    normal = normalize(v_TBN * (normal * 2.0 - 1.0));

    vec3 textureCol = texture(diffuseTexture, v_texCoords).rgb;

    vec3 lightDir = normalize(-skyLight.direction);
    vec3 viewDir = normalize(viewPos - v_fragPos);

    vec3 blinnPhong = doBlinnPhong(lightDir, viewDir, normal, textureCol);

    colour = vec4(blinnPhong, 1);
}