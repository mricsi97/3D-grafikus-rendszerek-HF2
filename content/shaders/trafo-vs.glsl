#version 300 es

in vec4 vertexPosition;
in vec2 vertexTexCoord;

uniform struct{
	mat4 modelMatrix;
} gameObject;

uniform struct{
	mat4 viewProjMatrix;
} camera;

out vec2 tex;

void main(void) {
  gl_Position = vertexPosition * gameObject.modelMatrix * camera.viewProjMatrix;
  tex = vertexTexCoord;
}