package com.koridor.app.gl

import android.opengl.GLES20

/** Yönlü ışık + ortam ışığı + hafif parlama yapan tek gölgelendirici. */
class Shader {
    val program: Int
    val aPosition: Int
    val aNormal: Int
    val uMvp: Int
    val uModel: Int
    val uColor: Int
    val uLightDir: Int
    val uEye: Int

    init {
        val vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] != 0) { "Gölgelendirici bağlanamadı: " + GLES20.glGetProgramInfoLog(program) }
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uModel = GLES20.glGetUniformLocation(program, "uModel")
        uColor = GLES20.glGetUniformLocation(program, "uColor")
        uLightDir = GLES20.glGetUniformLocation(program, "uLightDir")
        uEye = GLES20.glGetUniformLocation(program, "uEye")
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] != 0) { "Gölgelendirici derlenemedi: " + GLES20.glGetShaderInfoLog(shader) }
        return shader
    }

    private companion object {
        const val VERTEX = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vWorld;
            void main() {
                vec4 world = uModel * vec4(aPosition, 1.0);
                vWorld = world.xyz;
                vNormal = mat3(uModel) * aNormal;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        const val FRAGMENT = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightDir;
            uniform vec3 uEye;
            varying vec3 vNormal;
            varying vec3 vWorld;
            void main() {
                vec3 n = normalize(vNormal);
                vec3 l = normalize(uLightDir);
                float diffuse = max(dot(n, l), 0.0);
                vec3 viewDir = normalize(uEye - vWorld);
                vec3 h = normalize(l + viewDir);
                float spec = pow(max(dot(n, h), 0.0), 32.0) * 0.25;
                vec3 rgb = uColor.rgb * (0.38 + 0.62 * diffuse) + vec3(spec);
                gl_FragColor = vec4(rgb, uColor.a);
            }
        """
    }
}
