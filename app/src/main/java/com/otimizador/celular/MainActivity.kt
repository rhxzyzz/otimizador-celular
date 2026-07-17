package com.otimizador.celular

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.provider.Settings
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvRamInfo: TextView
    private lateinit var progressRam: ProgressBar
    private lateinit var tvStorageInfo: TextView
    private lateinit var progressStorage: ProgressBar
    private lateinit var containerApps: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRamInfo = findViewById(R.id.tvRamInfo)
        progressRam = findViewById(R.id.progressRam)
        tvStorageInfo = findViewById(R.id.tvStorageInfo)
        progressStorage = findViewById(R.id.progressStorage)
        containerApps = findViewById(R.id.containerApps)

        findViewById<android.view.View>(R.id.btnLimparCache).setOnClickListener {
            limparCacheDoApp()
        }

        findViewById<android.view.View>(R.id.btnGerenciarApps).setOnClickListener {
            abrirTelaAppsEmSegundoPlano()
        }

        findViewById<android.view.View>(R.id.btnConfigArmazenamento).setOnClickListener {
            abrirConfiguracoesArmazenamento()
        }

        atualizarInfoRam()
        atualizarInfoArmazenamento()
        carregarListaDeApps()
    }

    override fun onResume() {
        super.onResume()
        // Atualiza os números sempre que o usuário volta para o app
        atualizarInfoRam()
        atualizarInfoArmazenamento()
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
            // Fallback: abre as configurações gerais de apps
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
        val pm = packageManager
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(0)
            .filter { app ->
                // Mostra só apps que têm ícone de lançamento (evita poluir com apps de sistema sem interface)
                pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val inflater = layoutInflater
        for (app in apps) {
            val view = inflater.inflate(R.layout.item_app, containerApps, false)
            val icon = view.findViewById<ImageView>(R.id.imgIcon)
            val name = view.findViewById<TextView>(R.id.tvAppName)

            icon.setImageDrawable(pm.getApplicationIcon(app))
            name.text = pm.getApplicationLabel(app).toString()

            view.setOnClickListener {
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
