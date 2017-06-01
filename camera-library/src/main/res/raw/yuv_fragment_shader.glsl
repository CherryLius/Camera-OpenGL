#extension GL_OES_EGL_image_external : require
varying highp vec2 v_TextureCoordinate;

uniform samplerExternalOES u_ImageTexture;

void main()
{
    gl_FragColor = texture2D(u_ImageTexture, v_TextureCoordinate);
}