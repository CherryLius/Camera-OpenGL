precision mediump float;
uniform sampler2D u_ImageTexture;
varying highp vec2 v_TextureCoordinate;

void main()
{
    gl_FragColor = texture2D(u_ImageTexture, v_TextureCoordinate);
}