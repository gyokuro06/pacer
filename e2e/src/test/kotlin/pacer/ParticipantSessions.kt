package pacer

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page

object ParticipantSessions {
    private val contexts = mutableMapOf<String, BrowserContext>()
    private val pages = mutableMapOf<String, Page>()
    private var sharedRoomCode: String? = null

    fun openHome(participant: String, browser: Browser): Page {
        close(participant)
        val context = browser.newContext()
        val page = context.newPage()
        contexts[participant] = context
        pages[participant] = page
        val url = config.target.url.toString()
        println("[STEP] participant=$participant open home url=$url")
        page.navigate(url)
        return page
    }

    fun page(participant: String): Page =
        pages[participant] ?: error("参加者 \"$participant\" のページがありません。先にホームを開いてください")

    fun rememberRoomCode(code: String) {
        sharedRoomCode = code.trim()
    }

    fun roomCode(): String =
        sharedRoomCode?.takeIf { it.isNotEmpty() }
            ?: error("共有ルームコードがまだありません")

    fun clear() {
        contexts.values.forEach { context ->
            runCatching { context.close() }
        }
        contexts.clear()
        pages.clear()
        sharedRoomCode = null
    }

    private fun close(participant: String) {
        contexts.remove(participant)?.let { runCatching { it.close() } }
        pages.remove(participant)
    }
}
