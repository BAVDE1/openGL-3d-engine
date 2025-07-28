//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec3 tangent;
layout(location = 3) in vec2 texCoords;
layout(location = 4) in ivec4 boneIds;
layout(location = 5) in vec4 boneWeights;
layout(location = 6) in int isStatic;

const int MAX_BONES = 200;
const int MAX_BONE_INFLUENCE = 4;

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
    gl_Position = model * finalPos;
}

//--- GEOM
#version 450 core
layout (triangles) in;
layout (triangle_strip, max_vertices=18) out;

uniform mat4 shadowMatrices[6];

out vec4 v_fragPos;

void main() {
    for (int face = 0; face < 6; ++face) {
        gl_Layer = face;  // built-in variable that specifies to which face of the cubemap we render.
        for (int i = 0; i < 3; ++i) {
            v_fragPos = gl_in[i].gl_Position;
            gl_Position = shadowMatrices[face] * v_fragPos;
            EmitVertex();
        }
        EndPrimitive();
    }
}

//--- FRAG
#version 450 core

uniform vec3 lightPos;
uniform float farPlane;

in vec4 v_fragPos;

void main() {
    // map to [0;1] range
    float lightDistance = length(v_fragPos.xyz - lightPos);
    gl_FragDepth = lightDistance / farPlane;
}
