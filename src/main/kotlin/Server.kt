import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun main() {
    val config = AppConfig()

    val httpClient = HttpClient {
        if (config.proxyEnabled) {
            engine {
                proxy = ProxyBuilder.http(
                    Url(requireNotNull(config.proxyUrl))
                )
            }
        }
    }

    try {
        val agent = AgentFactory(config, httpClient).create()
        val agentMutex = Mutex()

        try {
            embeddedServer(
                Netty,
                host = config.serverHost,
                port = config.serverPort
            ) {
                install(ContentNegotiation) {
                    json()
                }

                configureErrorHandling()

                configureRoutes { message, sessionId ->
                    agentMutex.withLock {
                        agent.run(message, sessionId)
                    }
                }
            }.start(wait = true)
        } finally {
            runBlocking {
                agent.close()
            }
        }
    } finally {
        httpClient.close()
    }
}