package com.github.smaugfm.power.tracker.interaction.telegram

object TelegramMarkdownV2Format {
    private fun escapeInternal(text: String, escapeChars: String): String {
        return text.replace(Regex("[$escapeChars\\\\]"), "\\$0")
    }

    fun escape(text: String): String {
        return text.replace(Regex("[_*\\[\\]()~`>#+\\-=|{}.!]"), "\\$0")
    }

    fun bold(text: String) = "*${text}*"
    fun italic(text: String) = "_${text}_"
    fun strikethrough(text: String) = "~${text}~"
    fun underline(text: String) = "__${text}__"
    fun spoiler(text: String) = "||${text}||"
    fun url(label: String, url: String) = "[$label](${escapeInternal(url, ")")})"
    fun userMention(label: String, userId: Long) = url(label, "tg://user?id=$userId")
    fun monospace(text: String) = "`${escapeInternal(text, "`")}`"
    fun monospaceBlock(text: String, language: String? = null): String {
        val result = StringBuilder()
        result.append("```")
        if (language != null)
            result.append(language)
        result.append("\n")
        result.append(escapeInternal(text, "`"))
        result.append("\n")
        result.append("```")
        return result.toString()
    }
}
