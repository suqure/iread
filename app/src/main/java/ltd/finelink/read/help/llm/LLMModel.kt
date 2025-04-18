package ltd.finelink.read.help.llm

import ltd.finelink.read.data.entities.LLMConfig
import java.util.UUID

interface LLMModel {

    fun generate(message: List<MessageData>,stream: (response:String) -> Unit,finished:(response:String)->Unit)

    suspend fun generate(message:List<MessageData>):String

    fun chatable(): Boolean

    fun interruptable(): Boolean

    fun requestResetChat()

    fun load()

    fun unload()

    fun requestReload(modelConfig: LLMConfig)

    fun modelType():Int


}
enum class MessageRole {
    System,
    Assistant,
    User
}
data class MessageData(val role: MessageRole, val text: String, val id: UUID = UUID.randomUUID())
enum class ModelChatState {
    Generating,
    Resetting,
    Reloading,
    Ready,
    Falied
}