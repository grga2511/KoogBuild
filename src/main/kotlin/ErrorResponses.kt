import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AiServiceException(cause: Throwable) :
    RuntimeException("AI service call failed.", cause)

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String
) {
    respond(
        status,
        buildJsonObject {
            put("error", message)
        }
    )
}

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ContentTransformationException> { call, _ ->
            call.respondError(
                HttpStatusCode.BadRequest,
                "Send correct JSON object."
            )
        }

        exception<BadRequestException> { call, _ ->
            call.respondError(
                HttpStatusCode.BadRequest,
                "Send correct JSON object."
            )
        }

        exception<AiServiceException> { call, cause ->
            cause.printStackTrace()

            call.respondError(
                HttpStatusCode.BadGateway,
                "AI call to service failed. Try again."
            )
        }

        exception<Exception> { call, cause ->
            if (cause is CancellationException) {
                throw cause
            }

            cause.printStackTrace()

            call.respondError(
                HttpStatusCode.InternalServerError,
                "Internal server error."
            )
        }
    }
}