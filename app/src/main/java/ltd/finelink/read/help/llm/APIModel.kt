package ltd.finelink.read.help.llm

import ltd.finelink.read.data.entities.LLMConfig

class APIModel@JvmOverloads constructor(
    private var modelConfig: LLMConfig
) : LLMModel  {
    override fun generate(
        message: List<MessageData>,
        stream: (response: String) -> Unit,
        finished: (response: String) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun generate(message: List<MessageData>): String {
        TODO("Not yet implemented")
    }

    override fun chatable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun interruptable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun requestResetChat() {
        TODO("Not yet implemented")
    }

    override fun load() {
        TODO("Not yet implemented")
    }

    override fun unload() {
        TODO("Not yet implemented")
    }

    override fun requestReload(modelConfig: LLMConfig) {
        this.modelConfig = modelConfig
    }

    override fun modelType(): Int {
        return modelConfig.type
    }
}