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

    fun assertParticipantVisibleWithEmoji(displayName: String) {
        PlaywrightAssertions.assertThat(participantItem(displayName)).isVisible()
        val emoji = readParticipantEmoji(displayName)
        require(emoji.isNotEmpty()) {
            "表示名 \"$displayName\" の参加者に絵文字がありません"
        }
    }

    fun assertParticipantEmojisDistinct(displayNameA: String, displayNameB: String) {
        val emojiA = readParticipantEmoji(displayNameA)
        val emojiB = readParticipantEmoji(displayNameB)
        require(emojiA != emojiB) {
            "絵文字が重複しています: \"$displayNameA\"=$emojiA \"$displayNameB\"=$emojiB"
        }
    }

    fun readParticipantEmoji(displayName: String): String {
        PlaywrightAssertions.assertThat(participantItem(displayName)).isVisible()
        val text = participantItem(displayName).innerText().trim()
        val emoji = text
            .replace(displayName, "")
            .replace("絵文字を変更", "")
            .trim()
        require(emoji.isNotEmpty()) {
            "表示名 \"$displayName\" の参加者に絵文字がありません: \"$text\""
        }
        return emoji
    }

    fun assertParticipantEmojiEquals(displayName: String, expectedEmoji: String) {
        val actual = readParticipantEmoji(displayName)
        require(actual == expectedEmoji) {
            "表示名 \"$displayName\" の絵文字が一致しません: actual=$actual expected=$expectedEmoji"
        }
    }

    fun assertParticipantEmojiDiffers(displayName: String, previousEmoji: String) {
        val actual = readParticipantEmoji(displayName)
        require(actual != previousEmoji) {
            "表示名 \"$displayName\" の絵文字が変わっていません: $actual"
        }
    }

    fun assertOwnEmojiPickerOffersOnlyOwnAndUnused(ownDisplayName: String) {
        val others = participantEmojis()
            .filterKeys { it != ownDisplayName }
            .values
            .toSet()
        val own = readParticipantEmoji(ownDisplayName)
        openOwnEmojiPicker(ownDisplayName)
        val enabled = enabledEmojiOptions()
        require(own in enabled) {
            "自分の絵文字 \"$own\" が選べません: $enabled"
        }
        val taken = enabled.intersect(others)
        require(taken.isEmpty()) {
            "他参加者が使用中の絵文字が選べてしまいます: $taken"
        }
        require(enabled.any { it != own }) {
            "未使用の絵文字が選べません: $enabled"
        }
        closeOwnEmojiPicker()
    }

    fun changeOwnEmojiToUnused(ownDisplayName: String) {
        val others = participantEmojis()
            .filterKeys { it != ownDisplayName }
            .values
            .toSet()
        val own = readParticipantEmoji(ownDisplayName)
        openOwnEmojiPicker(ownDisplayName)
        val next =
            enabledEmojiOptions().firstOrNull { it != own && it !in others }
                ?: error("未使用の絵文字がありません: own=$own others=$others")
        emojiOption(next).click()
        PlaywrightAssertions.assertThat(emojiPicker()).isHidden()
        assertParticipantEmojiEquals(ownDisplayName, next)
    }

    private fun participantEmojis(): Map<String, String> {
        PlaywrightAssertions.assertThat(participants()).isVisible()
        return participants()
            .getByRole(AriaRole.LISTITEM)
            .all()
            .associate { item ->
                val text = item.innerText().trim()
                val displayName =
                    text
                        .replace("絵文字を変更", "")
                        .trim()
                        .split(Regex("\\s+"))
                        .lastOrNull()
                        ?: error("参加者の表示名が読めません: \"$text\"")
                displayName to readParticipantEmoji(displayName)
            }
    }

    private fun openOwnEmojiPicker(ownDisplayName: String) {
        changeEmojiButton(ownDisplayName).click()
        PlaywrightAssertions.assertThat(emojiPicker()).isVisible()
    }

    private fun closeOwnEmojiPicker() {
        if (emojiPicker().isVisible) {
            playwrightPage.keyboard().press("Escape")
        }
        PlaywrightAssertions.assertThat(emojiPicker()).isHidden()
    }

    private fun enabledEmojiOptions(): List<String> =
        emojiPicker()
            .getByRole(AriaRole.BUTTON)
            .all()
            .filter { it.isEnabled }
            .map { it.innerText().trim() }
            .filter { it.isNotEmpty() }

    private fun emojiOption(emoji: String): Locator =
        emojiPicker().getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName(emoji))

    private fun changeEmojiButton(displayName: String): Locator =
        participantItem(displayName)
            .getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("絵文字を変更"))

    private fun emojiPicker(): Locator =
        playwrightPage.getByRole(AriaRole.DIALOG, Page.GetByRoleOptions().setName("絵文字を選ぶ"))

    private fun participants(): Locator =
        main.getByRole(AriaRole.LIST, Locator.GetByRoleOptions().setName("参加者"))

    private fun participantItem(displayName: String): Locator =
        participants()
            .getByRole(AriaRole.LISTITEM)
            .filter(Locator.FilterOptions().setHasText(displayName))

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
