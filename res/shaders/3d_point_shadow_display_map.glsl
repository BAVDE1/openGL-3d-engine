//--- VERT
#version 450 core

layout(location = 0) in vec2 pos;

layout (std140) uniform CameraView {
    mat4 projection;
    mat4 view;
};

uniform mat4 transform;

out vec3 v_texPos;

void main() {
    gl_Position = transform * vec4(pos, 0, 1);
    v_texPos = mat3(projection * mat4(mat3(view))) * vec3(pos, 1);
}

//--- FRAG
#version 450 core

uniform samplerCube depthCubeMap;

in vec3 v_texPos;

out vec4 colour;

void main() {
    float depth = texture(depthCubeMap, v_texPos).r;
    colour = vec4(depth, depth, depth, 1);;
}
