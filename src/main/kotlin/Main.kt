import ai.koog.agents.core.agent.AIAgent
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?: error("GOOGLE_API_KEY not set.")

    val httpClient = HttpClient {
        engine {
            proxy = ProxyBuilder.http(
                Url("http://192.168.255.60:8080")
            )
        }
    }

    val agent = AIAgent(
        promptExecutor = MultiLLMPromptExecutor(
            GoogleLLMClient(
                apiKey = apiKey,
                httpClientFactory = KtorKoogHttpClient.Factory(
                    baseClient = httpClient
                )
            )
        ),
        llmModel = GoogleModels.Gemini2_5Pro
    )

    try {
        println("For exit write /end.")

        while (true) {
            print("\nYou: ")
            val question = readlnOrNull()?.trim() ?: break

            if (question == "/end") break
            if (question.isBlank()) continue

            try {
                val result = agent.run(question)
                println("Gemini: $result")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    } finally {
        agent.close()
        httpClient.close()
    }
}