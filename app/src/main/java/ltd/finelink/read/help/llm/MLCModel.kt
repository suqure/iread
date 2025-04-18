package ltd.finelink.read.help.llm

import ai.mlc.mlcllm.MLCEngine
import ai.mlc.mlcllm.OpenAIProtocol
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.entities.LLMConfig
import java.util.concurrent.Executors

class MLCModel @JvmOverloads constructor(
    private var modelConfig: LLMConfig
) : LLMModel {

    private var loadConifg: Boolean = false

    private val engine: MLCEngine = MLCEngine()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    private val executorService = Executors.newSingleThreadExecutor()
    private var modelChatState = mutableStateOf(ModelChatState.Ready)
        @Synchronized get
        @Synchronized set


    private fun switchToResetting() {
        modelChatState.value = ModelChatState.Resetting
    }

    private fun switchToGenerating() {
        modelChatState.value = ModelChatState.Generating
    }

    private fun switchToReloading() {
        modelChatState.value = ModelChatState.Reloading
    }

    private fun switchToReady() {
        modelChatState.value = ModelChatState.Ready
    }

    private fun switchToFailed() {
        modelChatState.value = ModelChatState.Falied
    }

    override fun chatable(): Boolean {
        if (loadConifg) {
            return modelChatState.value == ModelChatState.Ready
        } else {
            return false
        }

    }

    override fun interruptable(): Boolean {
        return modelChatState.value == ModelChatState.Ready
                || modelChatState.value == ModelChatState.Generating
                || modelChatState.value == ModelChatState.Falied
    }

    private fun interruptChat(prologue: () -> Unit, epilogue: () -> Unit) {
        require(interruptable())
        if (modelChatState.value == ModelChatState.Ready) {
            prologue()
            epilogue()
        } else if (modelChatState.value == ModelChatState.Generating) {
            prologue()
            executorService.submit {
                viewModelScope.launch { epilogue() }
            }
        } else {
            require(false)
        }
    }

    override fun requestResetChat() {
        require(interruptable())
        interruptChat(
            prologue = {
                switchToResetting()
            },
            epilogue = {
                mainResetChat()
            }
        )
    }

    override fun load() {
        if (!loadConifg) {
            loadConifg = true
            engine.unload()
            engine.reload(modelConfig.local!!, modelConfig.lib)
        }
    }

    override fun unload() {
        if (loadConifg) {
            loadConifg = false
            engine.unload()
        }
    }

    private fun mainResetChat() {
        executorService.submit {
            callBackend { engine.reset() }
            viewModelScope.launch {
                switchToReady()
            }
        }
    }

    override fun requestReload(modelConfig: LLMConfig) {
        if (this.modelConfig.lib == modelConfig.lib && this.modelConfig.local == modelConfig.local) {
            return
        }
        require(interruptable())
        interruptChat(
            prologue = {
                switchToReloading()
            },
            epilogue = {
                mainReloadChat(modelConfig)
            }
        )
    }

    override fun modelType(): Int {
        return modelConfig.type
    }

    private fun mainReloadChat(modelConfig: LLMConfig) {
        this.modelConfig = modelConfig
        executorService.submit {
            if (!callBackend {
                    engine.unload()
                    engine.reload(modelConfig.local!!, modelConfig.lib)
                }) return@submit
            viewModelScope.launch {
                switchToReady()
            }
        }
    }

    private fun callBackend(callback: () -> Unit): Boolean {
        try {
            callback()
        } catch (e: Exception) {
            viewModelScope.launch {
                AppLog.put(e.localizedMessage, e)
                switchToFailed()
            }
            return false
        }
        return true
    }

    private fun covertMessage(message: List<MessageData>): List<OpenAIProtocol.ChatCompletionMessage> {
        var result: MutableList<OpenAIProtocol.ChatCompletionMessage> = mutableListOf();
        for (m in message) {
            when (m.role) {
                MessageRole.System -> result.add(
                    OpenAIProtocol.ChatCompletionMessage(
                        role = OpenAIProtocol.ChatCompletionRole.system,
                        content = m.text
                    )
                )

                MessageRole.User -> result.add(
                    OpenAIProtocol.ChatCompletionMessage(
                        role = OpenAIProtocol.ChatCompletionRole.user,
                        content = m.text
                    )
                )

                MessageRole.Assistant -> result.add(
                    OpenAIProtocol.ChatCompletionMessage(
                        role = OpenAIProtocol.ChatCompletionRole.assistant,
                        content = m.text
                    )
                )
            }
        }
        return result;
    }

    override fun generate(
        message: List<MessageData>,
        stream: (response: String) -> Unit,
        finished: (response: String) -> Unit,
    ) {
        require(chatable())
        switchToGenerating()
        executorService.submit {
            viewModelScope.launch {
                val channel = engine.chat.completions.create(
                    messages = covertMessage(message),
                    temperature = 0.9f,
                    top_p = 0.8f,
                    max_tokens = 1024,
                    stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                )
                var texts = ""
                for (response in channel) {
                    if (!callBackend {
                            if (response.choices.isNotEmpty()) {
                                var text = response.choices[0].delta.content?.asText().orEmpty()
                                stream(text)
                                texts += text
                            }
                        });
                }
                finished(texts)
                if (modelChatState.value == ModelChatState.Generating) switchToReady()
            }
        }
    }

    override suspend fun generate(message: List<MessageData>): String {
        require(chatable())
        switchToGenerating()
        val channel = engine.chat.completions.create(
            messages = covertMessage(message),
            temperature = 0.9f,
            top_p = 0.8f,
            max_tokens = 1024,
            stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
        )
        var texts = ""
        for (response in channel) {
            if (!callBackend {
                    if (response.choices.isNotEmpty()) {
                        var text = response.choices[0].delta.content?.asText().orEmpty()
                        texts += text
                    }
                });
        }
        if (modelChatState.value == ModelChatState.Generating) switchToReady()
        return texts
    }

}


