package pacer

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright
import com.thoughtworks.gauge.AfterScenario
import com.thoughtworks.gauge.AfterSuite
import com.thoughtworks.gauge.BeforeSuite

class SetupAndTeardown {
    companion object {
        private var playwright: Playwright? = null
        private var browser: Browser? = null

        fun browser(): Browser =
            browser ?: error("Browser is not initialized")
    }

    @BeforeSuite
    fun setupSuite() {
        playwright = Playwright.create()
        browser = playwright!!.chromium().launch()
    }

    @AfterScenario
    fun teardownScenario() {
        ParticipantSessions.clear()
    }

    @AfterSuite
    fun teardownSuite() {
        runCatching { browser?.close() }
        browser = null
        playwright?.close()
        playwright = null
    }
}
