import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID

class RoutesTest : FunSpec() {

    private val dummyAnswers = listOf(
        "This is the first test response.",
        "This is the second test response.",
        "This is the third test response."
    )

    init {
        context("POST /api/chat") {

            test("400 - message is missing") {
                testApplication {
                    testApp()

                    val response = client.post("/api/chat") {
                        contentType(ContentType.Application.Json)
                        setBody("""{}""")
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }

            test("400 - message contains only whitespace") {
                testApplication {
                    testApp()

                    val response = client.post("/api/chat") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"message":"   "}""")
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }

            test("200 - valid message returns a dummy response and a new session ID") {
                testApplication {
                    testApp()

                    val response = client.post("/api/chat") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"message":"Explain REST APIs."}""")
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val body = Json.decodeFromString<ChatResponse>(
                        response.bodyAsText()
                    )

                    dummyAnswers shouldContain body.response

                    UUID.fromString(body.sessionId).toString() shouldBe
                            body.sessionId
                }
            }

            test("200 - forwards the message and preserves the provided session ID") {
                testApplication {
                    var receivedMessage: String? = null
                    var receivedSessionId: String? = null

                    testApp { message, sessionId ->
                        receivedMessage = message
                        receivedSessionId = sessionId
                        dummyAnswers.first()
                    }

                    val response = client.post("/api/chat") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "message": "Explain Kotlin.",
                              "sessionId": "test-session-1"
                            }
                            """.trimIndent()
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val body = Json.decodeFromString<ChatResponse>(
                        response.bodyAsText()
                    )

                    body.response shouldBe dummyAnswers.first()
                    body.sessionId shouldBe "test-session-1"
                    receivedMessage shouldBe "Explain Kotlin."
                    receivedSessionId shouldBe "test-session-1"
                }
            }

            test("400 - session ID is empty") {
                testApplication {
                    testApp()

                    val response = client.post("/api/chat") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                              "message": "Hello",
                              "sessionId": ""
                            }
                            """.trimIndent()
                        )
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }

            test("502 - AI service throws an exception") {
                testApplication {
                    testApp { _, _ ->
                        error("Simulated AI service failure.")
                    }

                    val response = client.post("/api/chat") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"message":"Explain REST APIs."}""")
                    }

                    response.status shouldBe HttpStatusCode.BadGateway
                }
            }
        }
    }

    private fun ApplicationTestBuilder.testApp(
        sendMessage: suspend (String, String) -> String = { _, _ ->
            dummyAnswers.first()
        }
    ) {
        application {
            install(ContentNegotiation) {
                json()
            }

            configureErrorHandling()
            configureRoutes(sendMessage)
        }
    }
}