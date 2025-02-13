precision highp float;

uniform sampler2D uTexture;

varying float vAlpha;

void main()  {
    vec4 texColor = texture2D(uTexture, gl_PointCoord);
    texColor.a = vAlpha;

    gl_FragColor = texColor;
}