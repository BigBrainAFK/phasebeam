precision lowp float;

attribute vec2 aPosition;
attribute vec3 aColor;

uniform float uXOffset;

varying vec3 vColor;

void main() {
    gl_Position = vec4(aPosition.x + uXOffset / 3.5, aPosition.y, 0.0, 1.0);
    vColor = aColor;
}