import java.util.Properties

class AppConfig {
    private val properties = Properties()

    init {
        val stream = javaClass.classLoader
            .getResourceAsStream("application.properties")
            ?: error("application.properties nije pronaden.")

        stream.use {
            properties.load(it)
        }
    }

    private fun required(name: String): String =
        properties.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error("Nedostaje konfiguracijska postavka: $name")

    val serverHost = required("server.host")
    val serverPort = required("server.port").toInt()
    val provider = required("llm.provider")
    val modelName = required("llm.model")
    val proxyEnabled = required("proxy.enabled").toBooleanStrict()

    val proxyUrl: String? =
        if (proxyEnabled) required("proxy.url") else null


    val memoryWindowSize = required("chat.memory.window-size")
        .toInt()
        .also {
            require(it > 0) {
                "chat.memory.window-size mora biti veci od 0."
            }
        }
}