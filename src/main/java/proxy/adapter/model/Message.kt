package proxy.adapter.model

sealed class Message {
    /**
     * Represents a UTF-8 encoded text message.
     *
     * @property value The text data.
     */
    data class Text(val value: String) : Message()

    /**
     * Represents a binary message.
     *
     * @property value The binary data.
     */
    class Bytes(val value: ByteArray) : Message() {
        operator fun component1(): ByteArray = value
    }
}
