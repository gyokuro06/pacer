package pacer

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page

object ParticipantSessions {
    private val contexts = mutableMapOf<String, BrowserContext>()
    private val pages = mutableMapOf<String, Page>()
    private var sharedRoomCode: String? = null
    private var rememberedRemainingMmSs: String? = null
    private var rememberedRemainingAtMs: Long? = null

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

    fun rememberRemainingTime(mmSs: String) {
        rememberedRemainingMmSs = mmSs.trim()
        rememberedRemainingAtMs = System.currentTimeMillis()
    }

    fun rememberedRemainingMmSs(): String =
        rememberedRemainingMmSs?.takeIf { it.isNotEmpty() }
            ?: error("残り時間がまだ記憶されていません")

    fun rememberedRemainingAtMs(): Long =
        rememberedRemainingAtMs
            ?: error("残り時間がまだ記憶されていません")

    fun clear() {
        contexts.values.forEach { context ->
            runCatching { context.close() }
        }
        contexts.clear()
        pages.clear()
        sharedRoomCode = null
        rememberedRemainingMmSs = null
        rememberedRemainingAtMs = null
    }

    private fun close(participant: String) {
        contexts.remove(participant)?.let { runCatching { it.close() } }
        pages.remove(participant)
    }
}
