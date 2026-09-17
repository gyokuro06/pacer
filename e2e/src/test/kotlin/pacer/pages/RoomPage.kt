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

    fun assertParticipantAvatarWithName(displayName: String) {
        PlaywrightAssertions.assertThat(participantItem(displayName)).isVisible()
        PlaywrightAssertions.assertThat(participantAvatar(displayName)).isVisible()
        PlaywrightAssertions.assertThat(participantDisplayName(displayName)).isVisible()
        val emoji = readParticipantEmoji(displayName)
        require(emoji.isNotEmpty()) {
            "表示名 \"$displayName\" の参加者に絵文字がありません"
        }
        assertCircularAvatar(participantAvatar(displayName), displayName)
    }

    fun assertParticipantHasYou(displayName: String) {
        PlaywrightAssertions.assertThat(participantYouLabel(displayName)).isVisible()
        PlaywrightAssertions.assertThat(participantDisplayName(displayName)).isVisible()
        val youCount =
            participants()
                .getByText("You", Locator.GetByTextOptions().setExact(true))
                .count()
        require(youCount == 1) {
            "You は自分だけに表示されるべきです: count=$youCount (expected self=\"$displayName\")"
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
        PlaywrightAssertions.assertThat(participantAvatar(displayName)).isVisible()
        val emoji = participantAvatar(displayName).innerText().trim()
        require(emoji.isNotEmpty()) {
            "表示名 \"$displayName\" の参加者に絵文字がありません"
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

    fun openOwnProfileViaAvatar(ownDisplayName: String) {
        participantAvatar(ownDisplayName).click()
        assertProfileDialogVisible()
    }

    fun openOwnProfileViaName(ownDisplayName: String) {
        participantDisplayName(ownDisplayName).click()
        assertProfileDialogVisible()
    }

    fun assertProfileDialogVisible() {
        PlaywrightAssertions.assertThat(profileDialog()).isVisible()
        assertDialogFullyInViewport(profileDialog())
    }

    fun assertOtherAvatarDoesNotOpenProfile(otherDisplayName: String) {
        val avatar = participantAvatar(otherDisplayName)
        assertNonInteractiveParticipantControl(avatar, "アバター", otherDisplayName)
        avatar.click()
        PlaywrightAssertions.assertThat(profileDialog()).isHidden()
    }

    fun assertOtherNameDoesNotOpenProfile(otherDisplayName: String) {
        val name = participantDisplayName(otherDisplayName)
        assertNonInteractiveParticipantControl(name, "表示名", otherDisplayName)
        name.click()
        PlaywrightAssertions.assertThat(profileDialog()).isHidden()
    }

    fun assertProfileEmojiOffersOnlyOwnAndUnused(ownDisplayName: String) {
        val others = participantEmojis()
            .filterKeys { it != ownDisplayName }
            .values
            .toSet()
        val own = readParticipantEmoji(ownDisplayName)
        assertProfileDialogVisible()
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
    }

    fun changeOwnEmojiViaProfileToUnused(ownDisplayName: String) {
        val others = participantEmojis()
            .filterKeys { it != ownDisplayName }
            .values
            .toSet()
        val own = readParticipantEmoji(ownDisplayName)
        assertProfileDialogVisible()
        val next =
            enabledEmojiOptions().firstOrNull { it != own && it !in others }
                ?: error("未使用の絵文字がありません: own=$own others=$others")
        emojiOption(next).click()
        PlaywrightAssertions.assertThat(profileDialog()).isHidden()
        assertParticipantEmojiEquals(ownDisplayName, next)
    }

    private fun participantEmojis(): Map<String, String> {
        PlaywrightAssertions.assertThat(participants()).isVisible()
        return participants()
            .getByRole(AriaRole.LISTITEM)
            .all()
            .associate { item ->
                val displayName =
                    item
                        .locator("[aria-label$='のアバター']")
                        .getAttribute("aria-label")
                        ?.removeSuffix("のアバター")
                        ?: error("参加者の表示名が読めません: \"${item.innerText().trim()}\"")
                displayName to readParticipantEmoji(displayName)
            }
    }

    private fun enabledEmojiOptions(): List<String> =
        profileDialog()
            .getByRole(AriaRole.BUTTON)
            .all()
            .filter { it.isEnabled }
            .map { it.innerText().trim() }
            .filter { it.isNotEmpty() }

    private fun emojiOption(emoji: String): Locator =
        profileDialog().getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName(emoji))

    private fun profileDialog(): Locator =
        playwrightPage.getByRole(AriaRole.DIALOG, Page.GetByRoleOptions().setName("プロフィール"))

    private fun participants(): Locator =
        main.getByRole(AriaRole.LIST, Locator.GetByRoleOptions().setName("参加者"))

    private fun participantItem(displayName: String): Locator =
        participants()
            .getByRole(AriaRole.LISTITEM)
            .filter(
                Locator.FilterOptions().setHas(
                    playwrightPage.getByLabel("${displayName}のアバター"),
                ),
            )

    private fun participantAvatar(displayName: String): Locator =
        participantItem(displayName).getByLabel("${displayName}のアバター")

    private fun participantDisplayName(displayName: String): Locator =
        participantItem(displayName)
            .getByText(displayName, Locator.GetByTextOptions().setExact(true))

    private fun participantYouLabel(displayName: String): Locator =
        participantItem(displayName).getByText("You", Locator.GetByTextOptions().setExact(true))

    private fun assertCircularAvatar(avatar: Locator, displayName: String) {
        @Suppress("UNCHECKED_CAST")
        val metrics =
            avatar.evaluate(
                """
                el => {
                  const box = el.getBoundingClientRect();
                  return {
                    width: box.width,
                    height: box.height,
                    borderRadius: getComputedStyle(el).borderRadius,
                  };
                }
                """.trimIndent(),
            ) as Map<String, Any?>
        val width = (metrics["width"] as Number).toDouble()
        val height = (metrics["height"] as Number).toDouble()
        require(kotlin.math.abs(width - height) < 1.0) {
            "表示名 \"$displayName\" のアバターが円形ではありません: ${width}x$height"
        }
        val borderRadius = metrics["borderRadius"].toString()
        val circular =
            borderRadius == "50%" ||
                borderRadius.split(Regex("\\s+")).all { token ->
                    val radiusPx = token.removeSuffix("px").toDoubleOrNull()
                    radiusPx != null && kotlin.math.abs(radiusPx - width / 2.0) < 1.0
                }
        require(circular) {
            "表示名 \"$displayName\" のアバターが円形ではありません: borderRadius=$borderRadius size=$width"
        }
    }

    private fun assertDialogFullyInViewport(dialog: Locator) {
        val box =
            dialog.boundingBox()
                ?: error("プロフィールダイアログの位置が取得できません")
        val viewport =
            playwrightPage.viewportSize()
                ?: error("viewport サイズが取得できません")
        require(box.x >= 0 && box.y >= 0) {
            "プロフィールダイアログが viewport 外です: x=${box.x} y=${box.y}"
        }
        require(box.x + box.width <= viewport.width + 1) {
            "プロフィールダイアログが viewport 右外です: right=${box.x + box.width} width=${viewport.width}"
        }
        require(box.y + box.height <= viewport.height + 1) {
            "プロフィールダイアログが viewport 下外です: bottom=${box.y + box.height} height=${viewport.height}"
        }
    }

    private fun assertNonInteractiveParticipantControl(
        control: Locator,
        kind: String,
        displayName: String,
    ) {
        val tagName = control.evaluate("el => el.tagName") as String
        require(tagName != "BUTTON") {
            "他参加者 \"$displayName\" の$kind が button のままです: tag=$tagName"
        }
        val cursor = control.evaluate("el => getComputedStyle(el).cursor") as String
        require(cursor != "pointer") {
            "他参加者 \"$displayName\" の$kind が pointer カーソルです: cursor=$cursor"
        }
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
