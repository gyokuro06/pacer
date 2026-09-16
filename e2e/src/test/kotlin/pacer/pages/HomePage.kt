package pacer.pages

import com.microsoft.playwright.Page
import pacer.config

class HomePage(page: Page) : BasePage(page) {
    fun createRoom(workMinutes: String, breakMinutes: String, displayName: String) {
        RoomPage(playwrightPage).assertOnRoomPage()
        RoomPage(playwrightPage).setDisplayName(displayName)
        RoomPage(playwrightPage).setWorkAndBreakMinutes(workMinutes, breakMinutes)
    }

    fun joinRoom(roomCode: String, displayName: String) {
        val origin = config.target.url.toString().trimEnd('/')
        playwrightPage.navigate("$origin/room/${roomCode.trim()}")
        RoomPage(playwrightPage).joinWithDisplayName(displayName)
    }
}
