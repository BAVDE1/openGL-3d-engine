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

uniform bool horizontal;
uniform float weight[5] = float[] (0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec2 texOffset = 1.0 / textureSize(bloomTexture, 0);
    vec3 result = texture(bloomTexture, v_texPos).rgb * weight[0];

    if (horizontal) {
        for (int i = 1; i < 5; ++i){
            result += texture(bloomTexture, v_texPos + vec2(texOffset.x * i, 0.0)).rgb * weight[i];
            result += texture(bloomTexture, v_texPos - vec2(texOffset.x * i, 0.0)).rgb * weight[i];
        }
    }
    else {
        for (int i = 1; i < 5; ++i){
            result += texture(bloomTexture, v_texPos + vec2(0.0, texOffset.y * i)).rgb * weight[i];
            result += texture(bloomTexture, v_texPos - vec2(0.0, texOffset.y * i)).rgb * weight[i];
        }
    }
    colour = vec4(result, 1.0);
}