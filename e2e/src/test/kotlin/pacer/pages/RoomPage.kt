package pacer.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions
import com.microsoft.playwright.options.AriaRole

class RoomPage(page: Page) : BasePage(page) {
    private val main = playwrightPage.getByRole(AriaRole.MAIN)

    fun readRoomCode(): String {
        PlaywrightAssertions.assertThat(roomCode()).isVisible()
        return roomCode().innerText().trim()
    }

    fun startSession() {
        startButton().click()
    }

    fun assertWorkPhaseWithRemainingTime() {
        PlaywrightAssertions.assertThat(phaseLabel("作業")).isVisible()
        PlaywrightAssertions.assertThat(remainingTime()).isVisible()
    }

    fun readRemainingTime(): String {
        PlaywrightAssertions.assertThat(remainingTime()).isVisible()
        return remainingTime().innerText().trim()
    }

    fun assertWorkPhaseWithRestoredRemainingTime(
        rememberedMmSs: String,
        rememberedAtMs: Long,
        toleranceSeconds: Long = 5,
    ) {
        PlaywrightAssertions.assertThat(phaseLabel("作業")).isVisible()
        val actualMmSs = readRemainingTime()
        val actualSeconds = parseMmSsToSeconds(actualMmSs)
        val rememberedSeconds = parseMmSsToSeconds(rememberedMmSs)
        val elapsedSeconds = (System.currentTimeMillis() - rememberedAtMs) / 1000.0
        val expectedSeconds = rememberedSeconds - elapsedSeconds
        val delta = kotlin.math.abs(actualSeconds - expectedSeconds)
        require(delta <= toleranceSeconds) {
            "残り時間が復元されていません: actual=$actualMmSs " +
                "remembered=$rememberedMmSs elapsed≈${"%.1f".format(elapsedSeconds)}s " +
                "delta≈${"%.1f".format(delta)}s (tolerance=${toleranceSeconds}s)"
        }
    }

    private fun parseMmSsToSeconds(mmSs: String): Double {
        val parts = mmSs.split(":")
        require(parts.size == 2) { "残り時間の形式が不正です: $mmSs" }
        return parts[0].toDouble() * 60 + parts[1].toDouble()
    }

    fun proposeBreak() {
        proposeBreakButton().click()
    }

    fun confirmBreakProposal() {
        confirmProposalButton().click()
    }

    fun proposeResume() {
        proposeResumeButton().click()
    }

    fun confirmResumeProposal() {
        confirmProposalButton().click()
    }

    fun assertBreakPhase() {
        PlaywrightAssertions.assertThat(phaseLabel("休憩")).isVisible()
    }

    private fun roomCode(): Locator =
        main.getByRole(AriaRole.STATUS, Locator.GetByRoleOptions().setName("ルームコード"))

    private fun startButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("スタート"))

    private fun phaseLabel(phase: String): Locator =
        main.getByRole(AriaRole.STATUS, Locator.GetByRoleOptions().setName("フェーズ"))
            .filter(Locator.FilterOptions().setHasText(phase))

    private fun remainingTime(): Locator =
        main.getByRole(AriaRole.TIMER, Locator.GetByRoleOptions().setName("残り時間"))

    private fun proposeBreakButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("休憩を提案"))

    private fun proposeResumeButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("再開を提案"))

    private fun confirmProposalButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("提案を確定"))
}
