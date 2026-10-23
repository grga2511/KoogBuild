import kotlinx.coroutines.runBlocking
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun main() {
    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?.takeIf { it.isNotBlank() }
        ?: error("GOOGLE_API_KEY not set.")

    val agent = AIAgent(
        promptExecutor = MultiLLMPromptExecutor(
            GoogleLLMClient(apiKey)
        ),
        llmModel = GoogleModels.Gemini2_5Pro
    )

    val agentMutex = Mutex()

    try {
        embeddedServer(Netty, host = "127.0.0.1", port = 8080) {
            install(ContentNegotiation) {
                json()
            }

            routing {
                post("/api/chat") {
                    val request = try {
                        call.receive<JsonObject>()
                    } catch (e: ContentTransformationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            buildJsonObject {
                                put("error", "Send correct JSON object.")
                            }
                        )
                        return@post
                    } catch (e: BadRequestException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            buildJsonObject {
                                put("error", "Send correct JSON object.")
                            }
                        )
                        return@post
                    }

                    val value = request["message"] as? JsonPrimitive
                    val message = value
                        ?.takeIf { it.isString }
                        ?.content
                        ?.trim()

                    if (message.isNullOrBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            buildJsonObject {
                                put("error", "Message field must not be empty.")
                            }
                        )
                        return@post
                    }

                    val response = try {
                        agentMutex.withLock {
                            agent.run(message)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadGateway,
                            buildJsonObject {
                                put("error", "AI call to service failed. Try again.")
                            }
                        )
                        return@post
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        buildJsonObject {
                            put("response", response)
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
}