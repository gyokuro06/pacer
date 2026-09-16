package pacer

import com.microsoft.playwright.Page
import com.thoughtworks.gauge.screenshot.ICustomScreenshotGrabber
import java.util.Base64

class PlaywrightScreenshotGrabber : ICustomScreenshotGrabber {
    override fun takeScreenshot(): ByteArray {
        val currentPage =
            runCatching { ParticipantSessions.page("Alice") }.getOrNull()
                ?: runCatching { ParticipantSessions.page("Bob") }.getOrNull()
                ?: return Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WnR2r8AAAAASUVORK5CYII=",
                )
        return currentPage.screenshot(Page.ScreenshotOptions().setFullPage(true))
    }
}
