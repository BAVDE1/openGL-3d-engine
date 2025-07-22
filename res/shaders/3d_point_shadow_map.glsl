//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texCoords;
layout(location = 3) in ivec4 boneIds;
layout(location = 4) in vec4 boneWeights;
layout(location = 5) in int isStatic;

const int MAX_BONES = 200;
const int MAX_BONE_INFLUENCE = 4;

uniform mat4 lightSpaceMatrix;

uniform mat4 model;
uniform mat4 finalBonesMatrices[MAX_BONES];

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
    gl_Position = lightSpaceMatrix * (model * finalPos);
}

//--- FRAG
#version 450 core

void main() {

}
