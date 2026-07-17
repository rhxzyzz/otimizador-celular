package com.otimizador.celular

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvRamInfo: TextView
    private lateinit var progressRam: ProgressBar
    private lateinit var tvStorageInfo: TextView
    private lateinit var progressStorage: ProgressBar
    private lateinit var containerApps: LinearLayout
    private lateinit var barraModoLimpeza: LinearLayout
    private lateinit var btnModoJogo: MaterialButton

    // Estado do modo de limpeza de cache assistida
    private var modoLimpezaCache = false
    private var ultimoPacoteAberto: String? = null
    private val appsLimpos = mutableSetOf<String>()

    // Estado do Modo Jogo
    private var modoJogoAtivo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRamInfo = findViewById(R.id.tvRamInfo)
        progressRam = findViewById(R.id.progressRam)
        tvStorageInfo = findViewById(R.id.tvStorageInfo)
        progressStorage = findViewById(R.id.progressStorage)
        containerApps = findViewById(R.id.containerApps)
        barraModoLimpeza = findViewById(R.id.barraModoLimpeza)
        btnModoJogo = findViewById(R.id.btnModoJogo)

        findViewById<View>(R.id.btnLimparCache).setOnClickListener {
            limparCacheDoApp()
        }

        findViewById<View>(R.id.btnLimparCacheTodos).setOnClickListener {
            iniciarModoLimpezaCache()
        }

        findViewById<View>(R.id.btnConcluirLimpeza).setOnClickListener {
            encerrarModoLimpezaCache()
        }

        findViewById<View>(R.id.btnFecharSegundoPlano).setOnClickListener {
            fecharAppsEmSegundoPlano(mostrarToast = true)
        }

        findViewById<View>(R.id.btnModoJogo).setOnClickListener {
            alternarModoJogo()
        }

        findViewById<View>(R.id.btnGerenciarApps).setOnClickListener {
            abrirTelaAppsEmSegundoPlano()
        }

        findViewById<View>(R.id.btnConfigArmazenamento).setOnClickListener {
            abrirConfiguracoesArmazenamento()
        }

        atualizarInfoRam()
        atualizarInfoArmazenamento()
        carregarListaDeApps()
        atualizarEstadoModoJogo()
    }

    override fun onResume() {
        super.onResume()
        atualizarInfoRam()
        atualizarInfoArmazenamento()

        // Se voltamos de uma tela de detalhes de app durante o modo de limpeza,
        // marcamos aquele app como "limpo" na lista.
        if (modoLimpezaCache && ultimoPacoteAberto != null) {
            appsLimpos.add(ultimoPacoteAberto!!)
            ultimoPacoteAberto = null
            carregarListaDeApps()
        }

        atualizarEstadoModoJogo()
    }

    /** Mostra quanta RAM está livre/usada no aparelho (leitura, sem forçar nada). */
    private fun atualizarInfoRam() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)

        val totalMb = info.totalMem / (1024 * 1024)
        val disponivelMb = info.availMem / (1024 * 1024)
        val usadaMb = totalMb - disponivelMb
        val percentualUsado = if (totalMb > 0) ((usadaMb * 100) / totalMb).toInt() else 0

        tvRamInfo.text = String.format(
            Locale.getDefault(),
            "%d MB usados de %d MB (%d MB livres)",
            usadaMb, totalMb, disponivelMb
        )
        progressRam.progress = percentualUsado
    }

    /** Mostra o uso de armazenamento interno do aparelho. */
    private fun atualizarInfoArmazenamento() {
        val stat = StatFs(filesDir.path)
        val totalBytes = stat.totalBytes
        val disponivelBytes = stat.availableBytes
        val usadoBytes = totalBytes - disponivelBytes
        val percentualUsado = if (totalBytes > 0) ((usadoBytes * 100) / totalBytes).toInt() else 0

        tvStorageInfo.text = String.format(
            Locale.getDefault(),
            "%s usados de %s (%s livres)",
            formatarTamanho(usadoBytes),
            formatarTamanho(totalBytes),
            formatarTamanho(disponivelBytes)
        )
        progressStorage.progress = percentualUsado
    }

    private fun formatarTamanho(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.getDefault(), "%.1f GB", gb)
    }

    /**
     * Limpa o cache do PRÓPRIO app. Este é o único tipo de limpeza de cache
     * que um app comum tem permissão de fazer diretamente no Android.
     */
    private fun limparCacheDoApp() {
        try {
            cacheDir.deleteRecursively()
            externalCacheDir?.deleteRecursively()
            Toast.makeText(this, "Cache deste app limpo com sucesso", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível limpar o cache", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * O Android não permite que um app comum limpe o cache de outros apps
     * automaticamente. Este modo guia o usuário: ele toca em cada app da lista,
     * o sistema abre a tela nativa "Armazenamento e cache" daquele app, o usuário
     * toca em "Limpar cache" manualmente, e volta para continuar com o próximo.
     */
    private fun iniciarModoLimpezaCache() {
        modoLimpezaCache = true
        appsLimpos.clear()
        barraModoLimpeza.visibility = View.VISIBLE
        Toast.makeText(
            this,
            "Toque em cada app abaixo. Na tela que abrir, toque em 'Armazenamento e cache' > 'Limpar cache' e volte.",
            Toast.LENGTH_LONG
        ).show()
        carregarListaDeApps()
    }

    private fun encerrarModoLimpezaCache() {
        modoLimpezaCache = false
        barraModoLimpeza.visibility = View.GONE
        val total = appsLimpos.size
        appsLimpos.clear()
        Toast.makeText(this, "Modo de limpeza encerrado. $total apps marcados como limpos.", Toast.LENGTH_SHORT).show()
        carregarListaDeApps()
    }

    /**
     * Fecha processos em segundo plano de outros apps usando a API oficial
     * killBackgroundProcesses(). Funciona de verdade, mas o próprio Android
     * já faz esse gerenciamento sozinho e de forma mais eficiente — usar isso
     * com frequência pode até piorar a bateria, pois os apps precisam reabrir
     * do zero depois.
     */
    private fun fecharAppsEmSegundoPlano(mostrarToast: Boolean) {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = packageManager
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(0)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null && app.packageName != packageName
            }

        for (app in apps) {
            try {
                am.killBackgroundProcesses(app.packageName)
            } catch (e: Exception) {
                // Alguns apps do sistema não podem ser fechados; ignoramos e seguimos
            }
        }

        atualizarInfoRam()
        if (mostrarToast) {
            Toast.makeText(this, "Apps em segundo plano foram fechados", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Ativa/desativa o Modo Jogo: usa a permissão "Não perturbe" do Android
     * para bloquear notificações enquanto ativado, e libera RAM fechando
     * apps em segundo plano ao ativar.
     */
    private fun alternarModoJogo() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(
                this,
                "Autorize o acesso ao 'Não perturbe' na tela que vai abrir para usar o Modo Jogo",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }

        modoJogoAtivo = !modoJogoAtivo

        if (modoJogoAtivo) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            fecharAppsEmSegundoPlano(mostrarToast = false)
            Toast.makeText(this, "Modo Jogo ativado: notificações bloqueadas", Toast.LENGTH_SHORT).show()
        } else {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Toast.makeText(this, "Modo Jogo desativado: notificações normais", Toast.LENGTH_SHORT).show()
        }

        atualizarEstadoModoJogo()
    }

    private fun atualizarEstadoModoJogo() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ativoDeVerdade = nm.isNotificationPolicyAccessGranted &&
            nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        modoJogoAtivo = ativoDeVerdade
        btnModoJogo.text = if (modoJogoAtivo) {
            "Desativar Modo Jogo"
        } else {
            "Ativar Modo Jogo (bloquear notificações)"
        }
    }

    /**
     * Abre a tela do sistema com os apps recentes / em segundo plano,
     * onde o próprio Android permite ao usuário fechar apps manualmente.
     */
    private fun abrirTelaAppsEmSegundoPlano() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.component = android.content.ComponentName(
                "com.android.settings",
                "com.android.settings.Settings\$AppAndNotificationDashboardActivity"
            )
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }
    }

    private fun abrirConfiguracoesArmazenamento() {
        try {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Configurações de armazenamento indisponíveis neste aparelho", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Lista os apps instalados no aparelho. Ao tocar em um item,
     * abre a tela "Sobre o app" do sistema, onde o usuário pode
     * forçar parada, limpar cache/dados ou desinstalar manualmente.
     */
    private fun carregarListaDeApps() {
        containerApps.removeAllViews()

        val pm = packageManager
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(0)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val inflater = layoutInflater
        for (app in apps) {
            val view = inflater.inflate(R.layout.item_app, containerApps, false)
            val icon = view.findViewById<ImageView>(R.id.imgIcon)
            val name = view.findViewById<TextView>(R.id.tvAppName)
            val status = view.findViewById<TextView>(R.id.tvStatus)

            icon.setImageDrawable(pm.getApplicationIcon(app))
            name.text = pm.getApplicationLabel(app).toString()

            if (modoLimpezaCache) {
                if (appsLimpos.contains(app.packageName)) {
                    status.visibility = View.VISIBLE
                    status.text = "✓"
                } else {
                    status.visibility = View.VISIBLE
                    status.text = "○"
                }
            } else {
                status.visibility = View.GONE
            }

            view.setOnClickListener {
                if (modoLimpezaCache) {
                    ultimoPacoteAberto = app.packageName
                }
                abrirDetalhesDoApp(app.packageName)
            }

            containerApps.addView(view)
        }
    }

    /** Abre a tela nativa "Sobre o app" (Forçar parada / Limpar dados / Desinstalar). */
    private fun abrirDetalhesDoApp(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir os detalhes do app", Toast.LENGTH_SHORT).show()
        }
    }
}
