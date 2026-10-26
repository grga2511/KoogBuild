import ai.koog.agents.core.agent.AIAgent
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

fun main() {
    val config = AppConfig()

    require(config.provider == "google") {
        "Google provider support only."
    }

    val model = when (config.modelName) {
        "gemini-2.5-pro" -> GoogleModels.Gemini2_5Pro
        "gemini-2.5-flash" -> GoogleModels.Gemini2_5Flash
        else -> error("Unsupported model: ${config.modelName}")
    }

    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: error("GOOGLE_API_KEY not set.")

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
        val agent = AIAgent(
            promptExecutor = MultiLLMPromptExecutor(
                GoogleLLMClient(
                    apiKey = apiKey,
                    httpClientFactory = KtorKoogHttpClient.Factory(
                        baseClient = httpClient
                    )
                )
            ),
            llmModel = model
        )

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

                routing {
                    post("/api/chat") {
                        val request = call.receive<JsonObject>()

                        val value = request["message"] as? JsonPrimitive
                        val message = value
                            ?.takeIf { it.isString }
                            ?.content
                            ?.trim()

                        if (message.isNullOrBlank()) {
                            call.respondError(
                                HttpStatusCode.BadRequest,
                                "Message field must not be empty."
                            )
                            return@post
                        }

                        val sessionValue = request["sessionId"]

                        val sessionId = if (sessionValue == null) {
                            UUID.randomUUID().toString()
                        } else {
                            val providedId = (sessionValue as? JsonPrimitive)
                                ?.takeIf { it.isString }
                                ?.content
                                ?.trim()

                            if (providedId.isNullOrBlank()) {
                                call.respondError(
                                    HttpStatusCode.BadRequest,
                                    "sessionId must be a non-blank string."
                                )
                                return@post
                            }

                            providedId
                        }

                        val response = try {
                            agentMutex.withLock {
                                agent.run(message, sessionId)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            throw AiServiceException(e)
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            buildJsonObject {
                                put("response", response)
                                put("sessionId", sessionId)
                            }
                        )
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