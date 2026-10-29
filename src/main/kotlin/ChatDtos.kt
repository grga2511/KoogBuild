import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val message: String? = null,
    val sessionId: String? = null
)

@Serializable
data class ChatResponse(
    val response: String,
    val sessionId: String
)