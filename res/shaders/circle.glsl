//--- VERT
#version 450 core

// instanced
layout(location = 0) in vec2 circlePosition;
layout(location = 1) in float radius;
layout(location = 2) in float innerRadius;
layout(location = 3) in vec3 colour;

layout (std140) uniform CameraView {
    mat4 projection;
    mat4 view;
};

//uniform vec2 resolution;
//uniform mat4 projectionMatrix;
//
//uniform highp float time;
//uniform vec2 viewPos;
//uniform float viewScale;

out vec2 v_circlePos;
out float v_radius;
out float v_innerRadius;
out vec3 v_colour;

vec2 TRI_POSITIONS[3] = vec2[3] (
    vec2(0,       -2),
    vec2(1.7321,   1),
    vec2(-1.7321,  1)
);

void main() {
    vec2 circlePos = projection * view * circlePosition;
    float scaledRadius = abs(radius / viewScale);

    vec2 radiusMultiplier = TRI_POSITIONS[gl_VertexID % 3];
    vec2 pos = vec2(
        circlePos.x + (scaledRadius * radiusMultiplier.x),
        circlePos.y + (scaledRadius * radiusMultiplier.y)
    );
    gl_Position = vec4(pos, 1, 1) * projectionMatrix;

    v_circlePos = vec2(circlePos.x, circlePos.y);
    v_radius = scaledRadius;
    v_innerRadius = abs(innerRadius);
    v_colour = colour;
}

//--- FRAG
#version 450 core

uniform vec2 resolution;
uniform int debugMode;

const float EPSILON = 0.0001;
const int smoothness = 1;

in vec2 v_circlePos;
in float v_radius;
in float v_innerRadius;
in vec3 v_colour;

out vec4 colour;

vec2 invResolution = 1 / resolution;

void main() {
    float dist = length(gl_FragCoord.xy - v_circlePos);

    if (debugMode == 0 && dist > v_radius) discard;

    colour = vec4(v_colour, smoothstep(v_radius, v_radius - smoothness, dist));

    if (v_innerRadius > 0) {
        if (debugMode == 0 && dist < v_radius - v_innerRadius) discard;
        colour.a *= smoothstep(v_radius - v_innerRadius, v_radius - (v_innerRadius - smoothness), dist);
    }

    colour.a += .3 * debugMode;
}