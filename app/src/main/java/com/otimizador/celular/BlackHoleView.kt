package com.otimizador.celular

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Desenha uma animação de partículas sendo puxadas para um buraco negro
 * central, usado como efeito visual da tela de abertura do app.
 * Tudo é desenhado por código (Canvas), sem depender de imagens ou vídeos.
 *
 * A animação roda quadro a quadro (via postOnAnimation) e só termina de
 * verdade quando todas as partículas já foram "engolidas" — não em um
 * tempo fixo, para evitar cortar a animação com partículas ainda na tela.
 * Existe um teto de segurança para não travar caso alguma partícula nunca
 * chegue ao centro.
 */
class BlackHoleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particula(
        var angulo: Float,
        var distancia: Float,
        var velocidadeAngular: Float,
        var tamanho: Float,
        var alpha: Int
    )

    private val particulas = mutableListOf<Particula>()

    private val paintParticula = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val paintBuraco = Paint(Paint.ANTI_ALIAS_FLAG)

    private var raioBuraco = 0f
    private var raioMaximoBuraco = 0f

    private var tempoDecorridoMs = 0L
    private var ultimoTempoMs = 0L
    private var animando = false
    private var callbackFinal: (() -> Unit)? = null

    private val duracaoCrescimentoMs = 900L
    private val duracaoMaximaMs = 4500L

    private val loop = object : Runnable {
        override fun run() {
            if (!animando) return

            val agora = System.currentTimeMillis()
            val deltaMs = (agora - ultimoTempoMs).coerceAtLeast(1)
            ultimoTempoMs = agora
            tempoDecorridoMs += deltaMs

            raioBuraco = raioMaximoBuraco * (tempoDecorridoMs.toFloat() / duracaoCrescimentoMs).coerceIn(0f, 1f)
            atualizarParticulas(deltaMs / 1000f)
            invalidate()

            val terminouDeVerdade = particulas.isEmpty() && tempoDecorridoMs >= duracaoCrescimentoMs
            val estourouTempoMaximo = tempoDecorridoMs >= duracaoMaximaMs

            if (terminouDeVerdade || estourouTempoMaximo) {
                animando = false
                callbackFinal?.invoke()
            } else {
                postOnAnimation(this)
            }
        }
    }

    /** Começa a animação. [aoFinalizar] é chamado quando ela termina de verdade. */
    fun iniciarAnimacao(aoFinalizar: () -> Unit) {
        callbackFinal = aoFinalizar
        post {
            criarParticulas()
            tempoDecorridoMs = 0L
            ultimoTempoMs = System.currentTimeMillis()
            animando = true
            postOnAnimation(loop)
        }
    }

    private fun criarParticulas() {
        particulas.clear()
        raioMaximoBuraco = min(width, height) * 0.16f

        repeat(180) {
            val angulo = Random.nextFloat() * (Math.PI * 2).toFloat()
            val distancia = raioMaximoBuraco + Random.nextFloat() * (min(width, height) * 0.55f)
            particulas.add(
                Particula(
                    angulo = angulo,
                    distancia = distancia,
                    velocidadeAngular = 0.5f + Random.nextFloat() * 1.5f,
                    tamanho = 3f + Random.nextFloat() * 5f,
                    alpha = 255
                )
            )
        }
    }

    private fun atualizarParticulas(deltaSegundos: Float) {
        val iterador = particulas.iterator()
        while (iterador.hasNext()) {
            val p = iterador.next()

            val fatorProximidade = (1f - (p.distancia / (raioMaximoBuraco * 4f))).coerceIn(0.1f, 3f)
            p.velocidadeAngular += fatorProximidade * 0.6f * deltaSegundos
            p.angulo += p.velocidadeAngular * deltaSegundos
            p.distancia -= (40f + fatorProximidade * 220f) * deltaSegundos

            if (p.distancia <= raioBuraco + 2f) {
                iterador.remove()
                continue
            }

            p.alpha = (255 * (p.distancia / (raioMaximoBuraco * 3f))).toInt().coerceIn(40, 255)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f

        if (raioBuraco > 1f) {
            val brilho = RadialGradient(
                cx, cy, raioBuraco * 2.2f,
                intArrayOf(Color.argb(160, 120, 80, 255), Color.argb(0, 0, 0, 0)),
                floatArrayOf(0.3f, 1f),
                Shader.TileMode.CLAMP
            )
            paintBuraco.shader = brilho
            canvas.drawCircle(cx, cy, raioBuraco * 2.2f, paintBuraco)
        }

        for (p in particulas) {
            val x = cx + p.distancia * cos(p.angulo)
            val y = cy + p.distancia * sin(p.angulo)
            paintParticula.alpha = p.alpha
            canvas.drawCircle(x, y, p.tamanho, paintParticula)
        }

        if (raioBuraco > 0f) {
            paintBuraco.shader = null
            paintBuraco.color = Color.BLACK
            canvas.drawCircle(cx, cy, raioBuraco, paintBuraco)
        }
    }
}
