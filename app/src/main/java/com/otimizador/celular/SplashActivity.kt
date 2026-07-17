package com.otimizador.celular

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Tela de abertura do app: mostra "RHX" por um instante, depois anima
 * a transformação em um buraco negro de partículas, e então abre a
 * tela principal.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvLogo = findViewById<TextView>(R.id.tvLogo)
        val blackHoleView = findViewById<BlackHoleView>(R.id.blackHoleView)

        tvLogo.alpha = 0f
        tvLogo.scaleX = 0.8f
        tvLogo.scaleY = 0.8f
        tvLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            tvLogo.animate()
                .alpha(0f)
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(400)
                .start()

            blackHoleView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

            blackHoleView.iniciarAnimacao {
                irParaTelaPrincipal()
            }
        }, 900)
    }

    private fun irParaTelaPrincipal() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
