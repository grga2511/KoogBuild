import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import io.ktor.client.HttpClient
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
class AgentFactory(
    private val config: AppConfig,
    private val httpClient: HttpClient
) {
    fun create() = run {
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

        val historyProvider = InMemoryChatHistoryProvider()

        AIAgent(
            promptExecutor = MultiLLMPromptExecutor(
                GoogleLLMClient(
                    apiKey = apiKey,
                    httpClientFactory = KtorKoogHttpClient.Factory(
                        baseClient = httpClient
                    )
                )
            ),
            llmModel = model,
            systemPrompt = config.systemPrompt
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
                windowSize(config.memoryWindowSize)
            }

            if (config.langfuseEnabled) {
                install(OpenTelemetry) {
                    addLangfuseExporter()
                }


        }
    }
}}