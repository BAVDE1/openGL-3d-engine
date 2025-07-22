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

uniform sampler2D bloomTexture;

in vec2 v_texPos;

out vec4 colour;

uniform int horizontal;

const int WEIGHTS_COUNT = 6;
const float WEIGHTS[WEIGHTS_COUNT] = float[] (0.227027, 0.1945946, 0.1216216, 0.054054, 0.03, 0.016216);

void main() {
    vec2 texOffset = 1.0 / textureSize(bloomTexture, 0);
    vec3 result = texture(bloomTexture, v_texPos).rgb * WEIGHTS[0];

    for (int i = 1; i < WEIGHTS_COUNT; ++i){
        float x = texOffset.x * i * horizontal;
        float y = texOffset.y * i * (1-horizontal);
        result += texture(bloomTexture, v_texPos + vec2(x, y)).rgb * WEIGHTS[i];
        result += texture(bloomTexture, v_texPos - vec2(x, y)).rgb * WEIGHTS[i];
    }

    colour = vec4(result, 1.0);
}