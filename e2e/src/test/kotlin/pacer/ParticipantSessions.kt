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
    private var lastConcurrentPhaseSuccessCount: Int? = null
    private var lastConcurrentPhaseConflictCount: Int? = null
    private val frozenRoomGetJson = mutableMapOf<String, String>()
    private var lastAlertFlashSeen: Boolean? = null
    private var lastConflictToastSeen: Boolean? = null

    private val notificationSpyScript =
        """
        (() => {
          window.__pacerNotifications = [];
          window.__pacerPermissionRequests = 0;
          const NativeNotification = window.Notification;
          if (!NativeNotification) return;
          let permissionOverride = null;
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
            get() {
              return permissionOverride != null
                ? permissionOverride
                : NativeNotification.permission;
            },
          });
          SpyNotification.requestPermission = async function (...args) {
            window.__pacerPermissionRequests += 1;
            const result = await NativeNotification.requestPermission(...args);
            permissionOverride = result;
            return result;
          };
          window.Notification = SpyNotification;
        })();
        """.trimIndent()

    private val chimeSpyScript =
        """
        (() => {
          window.__pacerChimePlays = [];
          const nativePlay = HTMLAudioElement.prototype.play;
          HTMLAudioElement.prototype.play = function (...args) {
            if (!this.muted && this.volume > 0) {
              window.__pacerChimePlays.push({
                src: String(this.src || ''),
                at: Date.now(),
              });
            }
            return nativePlay.apply(this, args).catch(() => undefined);
          };
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
        context.addInitScript(chimeSpyScript)
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
        context.addInitScript(chimeSpyScript)
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

    fun lastConcurrentPhaseSuccessCount(): Int =
        lastConcurrentPhaseSuccessCount
            ?: error("同時フェーズ操作の成功回数がまだ記録されていません")

    fun rememberConcurrentPhaseOutcome(successCount: Int, conflictCount: Int) {
        lastConcurrentPhaseSuccessCount = successCount
        lastConcurrentPhaseConflictCount = conflictCount
    }

    fun lastConcurrentPhaseConflictCount(): Int =
        lastConcurrentPhaseConflictCount
            ?: error("同時フェーズ操作の前提不一致回数がまだ記録されていません")

    fun rememberFrozenRoomGetJson(participant: String, json: String) {
        frozenRoomGetJson[participant] = json
    }

    fun frozenRoomGetJson(participant: String): String =
        frozenRoomGetJson[participant]?.takeIf { it.isNotEmpty() }
            ?: error("参加者 \"$participant\" の固定ルーム JSON がありません")

    fun clearFrozenRoomGetJson(participant: String) {
        frozenRoomGetJson.remove(participant)
    }

    fun rememberAlertFlashSeen(seen: Boolean) {
        lastAlertFlashSeen = seen
    }

    fun lastAlertFlashSeen(): Boolean =
        lastAlertFlashSeen
            ?: error("アラート点滅の観測結果がまだありません")

    fun rememberConflictToastSeen(seen: Boolean) {
        lastConflictToastSeen = seen
    }

    fun lastConflictToastSeen(): Boolean =
        lastConflictToastSeen
            ?: error("衝突トーストの観測結果がまだありません")

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
        lastConcurrentPhaseSuccessCount = null
        lastConcurrentPhaseConflictCount = null
        frozenRoomGetJson.clear()
        lastAlertFlashSeen = null
        lastConflictToastSeen = null
    }

    private fun close(participant: String) {
        pages.remove(participant)?.let { clockInstalledPages.remove(it) }
        contexts.remove(participant)?.let { runCatching { it.close() } }
    }
}
