//--- VERT
#version 450 core

layout(location = 0) in vec3 pos;
layout(location = 1) in vec3 normal;

layout (std140) uniform CameraView {
    mat4 projection;
    mat4 view;
};

uniform mat4 model;

void main() {
    gl_Position = projection * view * model * vec4(pos, 1);
}

//--- FRAG
#version 450 core

uniform vec3 lightColour;

out vec4 colour;

void main() {
    colour = vec4(lightColour, 1);
}