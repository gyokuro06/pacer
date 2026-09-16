package pacer

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import java.net.URI

private fun firstNonBlankEnv(vararg names: String): String? =
    names.asSequence()
        .mapNotNull { name -> System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() } }
        .firstOrNull()

private val loadedConfig: Config =
    ConfigLoaderBuilder.default()
        .addPropertySource(
            EnvironmentVariablesPropertySource(
                useUnderscoresAsSeparator = true,
                allowUppercaseNames = true,
            )
        )
        .addResourceSource("/config.toml")
        .build()
        .loadConfigOrThrow()

val config: Config =
    loadedConfig.copy(
        target =
            loadedConfig.target.copy(
                url =
                    firstNonBlankEnv("TARGET_URL")?.let(URI::create)
                        ?: loadedConfig.target.url,
            ),
    )

data class Target(val url: URI)

data class Config(
    val target: Target,
)
