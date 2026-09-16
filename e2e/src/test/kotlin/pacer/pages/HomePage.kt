package pacer.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole

class HomePage(page: Page) : BasePage(page) {
    private val main = playwrightPage.getByRole(AriaRole.MAIN)

    fun createRoom(workMinutes: String, breakMinutes: String) {
        workMinutesInput().fill(workMinutes)
        breakMinutesInput().fill(breakMinutes)
        createRoomButton().click()
    }

    fun joinRoom(roomCode: String, displayName: String) {
        roomCodeInput().fill(roomCode)
        displayNameInput().fill(displayName)
        joinRoomButton().click()
    }

    private fun workMinutesInput(): Locator =
        main.getByRole(AriaRole.SPINBUTTON, Locator.GetByRoleOptions().setName("作業（分）"))

    private fun breakMinutesInput(): Locator =
        main.getByRole(AriaRole.SPINBUTTON, Locator.GetByRoleOptions().setName("休憩（分）"))

    private fun createRoomButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("ルームを作成"))

    private fun roomCodeInput(): Locator =
        main.getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("ルームコード"))

    private fun displayNameInput(): Locator =
        main.getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("表示名"))

    private fun joinRoomButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("参加"))
}
