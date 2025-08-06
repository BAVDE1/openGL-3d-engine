//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;

uniform mat4 model;

out vec3 v_normal;

layout (std140) uniform CameraView {
    mat4 projection;
    mat4 view;
};

void main() {
    gl_Position = projection * view * (model * vec4(pos, 1));
    v_normal = abs(normal);
}

//--- FRAG
#version 450 core

in vec3 v_normal;

out vec4 colour;

void main() {
    colour = vec4(v_normal, 1);
}