package pacer.pages

import com.microsoft.playwright.Page
import pacer.config

abstract class BasePage(protected val playwrightPage: Page) {
    companion object {
        fun homeUrl(): String = config.target.url.toString()
    }
}
