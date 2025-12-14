#version 150

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord);
    // Diagnostic: Make everything red
    fragColor = vec4(1.0, 0.0, 0.0, texColor.a) * vertexColor;
}