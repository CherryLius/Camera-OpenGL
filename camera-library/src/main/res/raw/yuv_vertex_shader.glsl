attribute vec4 a_Position;
attribute vec4 a_TextureCoordinate;
//attribute vec2 a_TextureCoordinate;

uniform mat4 u_TextureMatrix;
varying vec2 v_TextureCoordinate;

void main()
{
    v_TextureCoordinate = (u_TextureMatrix * a_TextureCoordinate).xy;
    //v_TextureCoordinate = a_TextureCoordinate;
    gl_Position = a_Position;
}