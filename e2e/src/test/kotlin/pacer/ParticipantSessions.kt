package pacer

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page

enum class NotificationPermissionMode {
    GRANTED,
    NOT_GRANTED,
}

object ParticipantSessions {
    private val contexts = mutableMapOf<String, BrowserContext>()
    private val pages = mutableMapOf<String, Page>()
    private val clockInstalledPages = mutableSetOf<Page>()
    private var sharedRoomCode: String? = null
    private var rememberedRemainingMmSs: String? = null
    private var rememberedRemainingAtMs: Long? = null
    private val rememberedEmojis = mutableMapOf<String, String>()

    private val notificationSpyScript =
        """
        (() => {
          window.__pacerNotifications = [];
          window.__pacerPermissionRequests = 0;
          const NativeNotification = window.Notification;
          if (!NativeNotification) return;
          function SpyNotification(title, options) {
            const body = options && options.body != null ? String(options.body) : null;
            window.__pacerNotifications.push({ title: String(title), body });
            try {
              return new NativeNotification(title, options);
            } catch (e) {
              return { title, close() {} };
            }
          }
          SpyNotification.prototype = NativeNotification.prototype;
          Object.defineProperty(SpyNotification, 'permission', {
            get() { return NativeNotification.permission; },
          });
          SpyNotification.requestPermission = async function (...args) {
            window.__pacerPermissionRequests += 1;
            return NativeNotification.requestPermission(...args);
          };
          window.Notification = SpyNotification;
        })();
        """.trimIndent()

    fun openHome(
        participant: String,
        browser: Browser,
        notificationPermission: NotificationPermissionMode = NotificationPermissionMode.NOT_GRANTED,
    ): Page {
        close(participant)
        val context = browser.newContext()
        context.addInitScript(notificationSpyScript)
        if (notificationPermission == NotificationPermissionMode.GRANTED) {
            context.grantPermissions(listOf("notifications"))
        }
        val page = context.newPage()
        contexts[participant] = context
        pages[participant] = page
        val url = config.target.url.toString()
        println("[STEP] participant=$participant open home url=$url notifications=$notificationPermission")
        page.navigate(url)
        page.waitForURL(java.util.regex.Pattern.compile(".*/room/[A-Za-z0-9]+/?$"))
        return page
    }

    fun openSharedRoom(participant: String, browser: Browser, roomCode: String): Page {
        close(participant)
        val context = browser.newContext()
        context.addInitScript(notificationSpyScript)
        val page = context.newPage()
        contexts[participant] = context
        pages[participant] = page
        val origin = config.target.url.toString().trimEnd('/')
        val url = "$origin/room/${roomCode.trim()}"
        println("[STEP] participant=$participant open shared room url=$url")
        page.navigate(url)
        page.waitForURL(java.util.regex.Pattern.compile(".*/room/[A-Za-z0-9]+/?$"))
        return page
    }

    fun page(participant: String): Page =
        pages[participant] ?: error("参加者 \"$participant\" のページがありません。先にホームを開いてください")

    fun ensureClockInstalled(page: Page) {
        if (page in clockInstalledPages) return
        page.clock().install()
        clockInstalledPages.add(page)
    }

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

    fun rememberEmoji(displayName: String, emoji: String) {
        rememberedEmojis[displayName] = emoji.trim()
    }

    fun rememberedEmoji(displayName: String): String =
        rememberedEmojis[displayName]?.takeIf { it.isNotEmpty() }
            ?: error("表示名 \"$displayName\" の絵文字がまだ記憶されていません")

    fun clear() {
        contexts.values.forEach { context ->
            runCatching { context.close() }
        }
        contexts.clear()
        pages.clear()
        clockInstalledPages.clear()
        sharedRoomCode = null
        rememberedRemainingMmSs = null
        rememberedRemainingAtMs = null
        rememberedEmojis.clear()
    }

    private fun close(participant: String) {
        pages.remove(participant)?.let { clockInstalledPages.remove(it) }
        contexts.remove(participant)?.let { runCatching { it.close() } }
    }
}
