package pacer.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole

class HomePage(page: Page) : BasePage(page) {
    private val main = playwrightPage.getByRole(AriaRole.MAIN)

    fun createRoom(workMinutes: String, breakMinutes: String, displayName: String) {
        workMinutesInput().fill(workMinutes)
        breakMinutesInput().fill(breakMinutes)
        createDisplayNameInput().fill(displayName)
        createRoomButton().click()
    }

    fun joinRoom(roomCode: String, displayName: String) {
        roomCodeInput().fill(roomCode)
        joinDisplayNameInput().fill(displayName)
        joinRoomButton().click()
    }

    private fun createForm(): Locator =
        main.locator("form").filter(Locator.FilterOptions().setHasText("ルームを作成"))

    private fun joinForm(): Locator =
        main.locator("form").filter(Locator.FilterOptions().setHasText("ルームに参加"))

    private fun workMinutesInput(): Locator =
        main.getByRole(AriaRole.SPINBUTTON, Locator.GetByRoleOptions().setName("作業（分）"))

    private fun breakMinutesInput(): Locator =
        main.getByRole(AriaRole.SPINBUTTON, Locator.GetByRoleOptions().setName("休憩（分）"))

    private fun createDisplayNameInput(): Locator =
        createForm().getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("表示名"))

    private fun createRoomButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("ルームを作成"))

    private fun roomCodeInput(): Locator =
        joinForm().getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("ルームコード"))

    private fun joinDisplayNameInput(): Locator =
        joinForm().getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("表示名"))

    private fun joinRoomButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("参加"))
}
