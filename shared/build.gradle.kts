import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

// ---------------------------------------------------------------------------------------------
// `.env` -> `app.moviematch.config.Secrets` (shared Kotlin source for Android and iOS).
//
// Only the *path* of the env file is evaluated here; values are read later, inside the task
// action, so no secret ever reaches the configuration phase or the build logs.
// ---------------------------------------------------------------------------------------------

/** Names always emitted as `const val` in [app.moviematch.config.Secrets]. */
val secretsToGenerate = listOf("SUPABASE_URL", "SUPABASE_ANON_KEY", "TMDB_API_KEY", "WEB_CLIENT_ID")

// `-Pmoviematch.envFile=...` overrides the default; relative paths resolve against the root dir.
val secretsEnvFile = providers.gradleProperty("moviematch.envFile")
    .map { configuredPath ->
        val file = File(configuredPath)
        if (file.isAbsolute) file else File(rootDir, configuredPath)
    }
    .orElse(File(rootDir, ".env"))

val generateSecrets = tasks.register<GenerateSecretsTask>("generateSecrets") {
    group = "build"
    description = "Generates app.moviematch.config.Secrets from the root `.env` file."
    envFiles.setFrom(secretsEnvFile)
    secretNames.set(secretsToGenerate)
    outputDirectory.set(layout.buildDirectory.dir("generated/secrets/commonMain/kotlin"))
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            // The name is used by `import Shared` in the Swift code (`iosApp`).
            baseName = "Shared"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "app.moviematch.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }

        // Habilita os testes de `commonTest` no host (JVM), permitindo rodá-los no Linux/CI
        // sem depender de macOS (os targets iOS não compilam/rodam fora do Mac).
        withHostTest { }
    }

    sourceSets {
        // Wires the generated directory into `commonMain` for every target (Android, iOS and the
        // metadata compilations). `srcDir(task)` carries the task dependency, so `generateSecrets`
        // runs before any Kotlin compilation that consumes this source set.
        getByName("commonMain").kotlin.srcDir(generateSecrets)

        commonMain.dependencies {
            // Compose Multiplatform
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.material.icons.core)

            // Navigation (multiplatform)
            implementation(libs.navigation.compose)

            // Lifecycle / ViewModel (multiplatform)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodel)

            // Dependency injection
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)

            // kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Images
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Key/value storage
            implementation(libs.multiplatform.settings)

            // Backdrop blur (floating bottom bar)
            implementation(libs.haze)
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)

            // Android specific Koin bindings (`androidContext()`), needed to reach the
            // `Context` that `createSettings()` resolves for the Supabase session store.
            implementation(libs.koin.android)

            // Google Sign-In through the system Credential Manager (see `GoogleSignIn.android.kt`).
            // `credentials-play-services-auth` covers Android 13 and below.
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

/**
 * Reads a dotenv file and writes a `Secrets.kt` object that exposes every requested name as a
 * `const val` string. Missing names (and a missing file) become empty strings so a clean clone or
 * a CI machine without a `.env` still compiles.
 *
 * The task is configuration-cache and build-cache friendly: all it touches is its own declared
 * inputs/outputs, never the [org.gradle.api.Project] instance.
 */
@CacheableTask
abstract class GenerateSecretsTask : DefaultTask() {

    /** Optional `.env` file. When it does not exist every secret is generated empty. */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val envFiles: ConfigurableFileCollection

    /** Names emitted as constants, in declaration order. */
    @get:Input
    abstract val secretNames: ListProperty<String>

    /** Root of the generated source tree (the Kotlin package folders are created below it). */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val envFile = envFiles.files.firstOrNull { it.isFile }
        val values = envFile?.let { readEnvFile(it) } ?: emptyMap()

        val packageDir = outputDirectory.get().asFile.resolve("app/moviematch/config")
        packageDir.mkdirs()
        val target = packageDir.resolve("Secrets.kt")
        target.writeText(buildSource(values))

        val filled = secretNames.get().filter { !values[it].isNullOrEmpty() }
        logger.lifecycle(
            "generateSecrets: wrote ${target.path} from " +
                (envFile?.absolutePath ?: "<missing .env>") +
                "; populated: " + filled.ifEmpty { listOf("none") }.joinToString(),
        )
    }

    /** Minimal dotenv parser: no escape interpretation, values are kept verbatim. */
    private fun readEnvFile(file: File): Map<String, String> {
        val values = linkedMapOf<String, String>()
        file.readLines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            val declaration = line
                .removePrefix("export")
                .takeIf { it.length != line.length && it.firstOrNull()?.isWhitespace() == true }
                ?.trimStart()
                ?: line

            val separator = declaration.indexOf('=')
            if (separator <= 0) return@forEach

            val name = declaration.substring(0, separator).trim()
            if (name.isEmpty()) return@forEach

            values[name] = unquote(declaration.substring(separator + 1).trim())
        }
        return values
    }

    /** Strips one pair of surrounding single or double quotes, if present. */
    private fun unquote(value: String): String {
        if (value.length < 2) return value
        val isQuoted = (value.startsWith('"') && value.endsWith('"')) ||
            (value.startsWith('\'') && value.endsWith('\''))
        return if (isQuoted) value.substring(1, value.length - 1) else value
    }

    /** Escapes a raw value so it is a valid Kotlin double-quoted string literal. */
    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append('\\').append('$')
                else -> append(character)
            }
        }
    }

    private fun buildSource(values: Map<String, String>): String = buildString {
        appendLine("// Generated by the `generateSecrets` Gradle task — do not edit manually.")
        appendLine("package app.moviematch.config")
        appendLine()
        appendLine("/**")
        appendLine(" * Build-time configuration for MovieMatch, shared by Android and iOS.")
        appendLine(" *")
        appendLine(" * The values are read from the `.env` file at the repository root and re-generated on")
        appendLine(" * every build that needs them, so **this file must never be edited by hand** — any")
        appendLine(" * change is overwritten. The `.env` is not versioned (see `.gitignore`); the versioned")
        appendLine(" * template is `.env.example`.")
        appendLine(" *")
        appendLine(" * A variable that is missing from the `.env` is emitted as an empty string, which is")
        appendLine(" * what a clean clone or a CI build without secrets produces.")
        appendLine(" */")
        appendLine("public object Secrets {")
        secretNames.get().forEachIndexed { index, name ->
            if (index > 0) appendLine()
            appendLine("    /** `$name` in the root `.env`; empty when it is not configured. */")
            appendLine("    public const val $name: String = \"${escape(values[name].orEmpty())}\"")
        }
        appendLine()
        appendLine("    /** `true` when both `SUPABASE_URL` and `SUPABASE_ANON_KEY` are configured. */")
        appendLine("    public val hasSupabaseConfig: Boolean")
        appendLine("        get() = SUPABASE_URL.isNotBlank() && SUPABASE_ANON_KEY.isNotBlank()")
        appendLine()
        appendLine("    /** `true` when `WEB_CLIENT_ID` is configured. */")
        appendLine("    public val hasGoogleSignInConfig: Boolean")
        appendLine("        get() = WEB_CLIENT_ID.isNotBlank()")
        appendLine("}")
    }
}
