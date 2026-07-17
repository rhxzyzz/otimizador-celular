# Otimizador Celular

Projeto Android (Kotlin) pronto para abrir no Android Studio.

## O que o app faz

- Mostra uso de memória RAM e armazenamento em tempo real
- Limpa o cache do próprio app (única limpeza de cache que um app comum tem permissão de fazer no Android)
- Lista os apps instalados; ao tocar em um, abre a tela nativa do Android onde dá para forçar parada, limpar dados ou desinstalar
- Atalhos para as telas de configurações do sistema (apps em segundo plano e armazenamento)

## Como gerar o APK pelo computador (Android Studio)

1. Baixe e instale o [Android Studio](https://developer.android.com/studio) (gratuito).
2. Abra o Android Studio → **Open** → selecione a pasta `OtimizadorCelular`.
3. Aguarde o Gradle sincronizar (primeira vez pode demorar alguns minutos, precisa de internet).
4. No menu superior: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. O APK gerado aparece em `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfira esse arquivo para o celular e instale (é preciso permitir "instalar de fontes desconhecidas" nas configurações do Android).

## Como gerar o APK 100% pelo celular (sem PC)

Este projeto já vem com um arquivo `.github/workflows/build.yml` configurado para compilar o APK automaticamente na nuvem, usando o GitHub Actions (gratuito). Você só precisa subir os arquivos e baixar o resultado.

**Passo a passo (tudo pelo navegador do celular, ex: Chrome):**

1. Crie uma conta gratuita em [github.com](https://github.com) (se ainda não tiver).
2. Toque no **+** no canto superior direito → **New repository**.
3. Dê um nome (ex: `otimizador-celular`), deixe como **Public**, e crie o repositório.
4. Dentro do repositório vazio, toque em **"uploading an existing file"** (ou no ícone de upload).
5. Extraia o zip que te enviei (o próprio gerenciador de arquivos do Android já consegue "Extrair") e envie **todo o conteúdo da pasta `OtimizadorCelular`** (incluindo a pasta oculta `.github`) para esse upload. Se o site não deixar arrastar pastas, envie os arquivos e recrie a estrutura de pastas pelo próprio GitHub (ele permite criar arquivos em subpastas digitando o caminho completo, ex: `app/src/main/AndroidManifest.xml`).
6. Toque em **Commit changes** para confirmar o envio.
7. Vá até a aba **Actions** do repositório. Um processo chamado "Gerar APK" deve iniciar sozinho (ou toque em **Run workflow** para iniciar manualmente).
8. Aguarde a barra ficar verde (leva de 2 a 5 minutos).
9. Toque no processo concluído → role até **Artifacts** → baixe o arquivo **OtimizadorCelular-APK** (vem como `.zip`, e dentro dele está o `app-debug.apk`).
10. Extraia o `.zip` baixado, toque no `app-debug.apk` e instale (permita "instalar de fontes desconhecidas" quando o Android pedir).

Esse método usa apenas navegador + gerenciador de arquivos do próprio celular — não precisa instalar nenhum app pesado de programação.

**Alternativas menos recomendadas para editar/compilar direto no celular:**
- **Termux** (app de terminal Linux para Android): dá para instalar Java e o Android SDK por linha de comando, mas ocupa muito espaço, é lento e exige familiaridade com terminal.
- **AIDE** (IDE Android para Android): app pago que tenta rodar um ambiente parecido com o Android Studio no próprio celular, mas costuma ter dificuldade com projetos mais novos como este (Gradle 8 / AGP 8).

Por isso o caminho do GitHub Actions acima é o mais confiável para compilar 100% pelo celular.

## Para gerar um APK assinado (release), pronto para distribuir

No Android Studio: **Build → Generate Signed Bundle / APK** → escolha APK → crie ou selecione um keystore → siga o assistente.

## Requisitos mínimos

- Android 7.0 (API 24) ou superior no celular onde o app será instalado
- Android Studio Hedgehog ou mais recente, com JDK 17

## Por que o app não "mata" outros apps sozinho

A partir do Android 5, por segurança, nenhum app comum consegue forçar o encerramento de outros aplicativos sem que o próprio usuário confirme manualmente na tela do sistema. Por isso este app abre a tela oficial do Android para essa ação — é o comportamento correto e seguro, mesmo em apps "otimizadores" populares da Play Store.
