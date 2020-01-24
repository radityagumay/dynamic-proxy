package proxy.adapter

import proxy.adapter.model.Message

class TextMessageAdapter : MessageAdapter<String> {
    override fun fromMessage(message: Message): String = when (message) {
        is Message.Text -> message.value
        else -> throw IllegalArgumentException("This Message Adapter only supports text Messages")
    }

    override fun toMessage(data: String): Message = Message.Text(data)
}