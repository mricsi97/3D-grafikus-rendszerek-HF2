#version 300 es

precision highp float;

uniform struct {
	vec4 solidColor;
} material;

out vec4 fragmentColor;

void main(void) {
fragmentColor = material.solidColor;
}
