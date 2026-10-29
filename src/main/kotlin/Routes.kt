import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import java.util.UUID

fun Application.configureRoutes(
    sendMessage: suspend (String, String) -> String
) {
    routing {
        post("/api/chat") {
            val request = call.receive<ChatRequest>()
            val message = request.message?.trim()

            if (message.isNullOrBlank()) {
                call.respondError(
                    HttpStatusCode.BadRequest,
                    "Message field must not be empty."
                )
                return@post
            }

            val providedId = request.sessionId?.trim()

            if (providedId != null && providedId.isBlank()) {
                call.respondError(
                    HttpStatusCode.BadRequest,
                    "sessionId must be a non-blank string."
                )
                return@post
            }

            val sessionId = providedId ?: UUID.randomUUID().toString()

            val response = try {
                sendMessage(message, sessionId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw AiServiceException(e)
            }

            call.respond(
                HttpStatusCode.OK,
                ChatResponse(
                    response = response,
                    sessionId = sessionId
                )
            )
        }
    }
}