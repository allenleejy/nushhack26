package com.nushhack26.app.nearby

object TapApdu {
    val select: ByteArray get() = byteArrayOf(0, 0xA4.toByte(), 4, 0, 7, 0xF0.toByte(), 0x54, 0x45, 0x4E, 0x45, 0x54, 1)
    private val ok = byteArrayOf(0x90.toByte(), 0)
    private val missing = byteArrayOf(0x6A, 0x82.toByte())
    private val unsupported = byteArrayOf(0x6D, 0)

    fun answer(command: ByteArray, payload: ByteArray?): ByteArray {
        if (payload == null) return missing.copyOf()
        if (command.size >= 2 && command[1] == 0xA4.toByte()) {
            return if (command.contentEquals(select) || command.contentEquals(select + byteArrayOf(0))) {
                byteArrayOf((payload.size ushr 8).toByte(), payload.size.toByte()) + ok
            } else missing.copyOf()
        }
        if (command.size == 5 && command[0] == 0.toByte() && command[1] == 0xB0.toByte() && command[4] == 0xE0.toByte()) {
            val offset = ((command[2].toInt() and 255) shl 8) or (command[3].toInt() and 255)
            if (offset < payload.size) return payload.copyOfRange(offset, minOf(offset + 224, payload.size)) + ok
        }
        return unsupported.copyOf()
    }

    fun isLastRead(command: ByteArray, payload: ByteArray): Boolean =
        command.size == 5 && command[1] == 0xB0.toByte() &&
            answer(command, payload).let { it.size > 2 && (((command[2].toInt() and 255) shl 8) or (command[3].toInt() and 255)) + it.size - 2 == payload.size }

    fun read(transceive: (ByteArray) -> ByteArray): String {
        fun body(command: ByteArray): ByteArray {
            val response = transceive(command)
            require(response.size >= 2 && response.takeLast(2).toByteArray().contentEquals(ok)) { "NFC command failed" }
            return response.copyOf(response.size - 2)
        }
        val header = body(select)
        require(header.size == 2)
        val length = ((header[0].toInt() and 255) shl 8) or (header[1].toInt() and 255)
        require(length in 1..5_600)
        val output = java.io.ByteArrayOutputStream()
        while (output.size() < length) {
            val offset = output.size()
            val chunk = body(byteArrayOf(0, 0xB0.toByte(), (offset ushr 8).toByte(), offset.toByte(), 0xE0.toByte()))
            require(chunk.isNotEmpty() && chunk.size <= 224 && output.size() + chunk.size <= length)
            output.write(chunk)
        }
        return output.toByteArray().toString(Charsets.UTF_8)
    }
}
