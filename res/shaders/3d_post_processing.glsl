//--- VERT
#version 450 core

layout(location = 0) in vec2 pos;

out vec2 v_texPos;

void main() {
    gl_Position = vec4(pos.x, pos.y, 0, 1);
    v_texPos = (pos * .5) + .5;
}

//--- FRAG
#version 450 core

uniform sampler2D screenTexture;
uniform sampler2D bloomTexture;

in vec2 v_texPos;

out vec4 colour;

void main() {
    vec4 screen = texture(screenTexture, v_texPos);
    vec4 bloom = texture(bloomTexture, v_texPos);
    colour = screen + bloom;
}