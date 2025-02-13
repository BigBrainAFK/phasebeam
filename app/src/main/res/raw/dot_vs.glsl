precision highp float;

attribute vec3 aPosition;

uniform mat4 uMVPMatrix;
uniform float uScaleSize;
uniform float uXOffset;

varying float vAlpha;

void main() {
    vec4 objPos = vec4(aPosition, 1.0);
    float tmpPointSize = aPosition.z * 7.0;

    vAlpha = 0.5 - tmpPointSize / 1000.0;

    objPos.z = 0.0;
    objPos.x = objPos.x - uXOffset * tmpPointSize / 100.0;

    gl_Position = uMVPMatrix * objPos;
    gl_PointSize = tmpPointSize * uScaleSize;
}