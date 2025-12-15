#version 150

uniform float Time;

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // Pure red overlay
    fragColor = vec4(1.0, 0.0, 0.0, 0.5);

    // Or make it pulse with time:
    // float pulse = (sin(Time * 2.0) + 1.0) / 2.0;
    // fragColor = vec4(1.0, 0.0, 0.0, pulse * 0.5);
}