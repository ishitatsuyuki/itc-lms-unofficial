package net.accelf.itc_lms_unofficial.models

import net.accelf.itc_lms_unofficial.network.lmsHostUrl
import java.io.Serializable
import java.util.*

val MESSAGE_STATUS_REGEX = Regex("""(.+)\((.+)\)""")

fun String.getMessageId(): String {
    return lmsHostUrl.newBuilder(this)!!
        .build()
        .queryParameter("inquiryId")!!
}

data class Message(
    val id: String,
    val title: String,
    val createdAt: Date?,
    val status: MessageStatus,
    val actorName: String?,
    val actedAt: Date?
) : Serializable {
    enum class MessageStatus(private val texts: Set<String>) {
        WAITING_FOR_ANSWER(setOf("問い合わせ中", "Contacting")),
        HAS_ANSWER(setOf("回答あり", "Answered")),
        COMPLETED(setOf("完了", "Completed"));

        companion object {
            fun String.toMessageStatus(): MessageStatus {
                return values().first { this in it.texts }
            }
        }
    }
}
