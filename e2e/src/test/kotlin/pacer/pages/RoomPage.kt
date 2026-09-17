package pacer.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.RequestOptions
import java.util.regex.Pattern
import pacer.config

class RoomPage(page: Page) : BasePage(page) {
    private val main = playwrightPage.getByRole(AriaRole.MAIN)
    private val minutePresets = listOf("60", "30", "15", "10")

    fun assertOnRoomPage() {
        playwrightPage.waitForURL(Pattern.compile(".*/room/[A-Za-z0-9]+/?$"))
    }

    fun setDisplayName(displayName: String) {
        PlaywrightAssertions.assertThat(displayNameInput()).isVisible()
        displayNameInput().fill(displayName)
        displayNameInput().blur()
    }

    fun setWorkAndBreakMinutes(workMinutes: String, breakMinutes: String) {
        selectWorkMinutesPreset(workMinutes)
        selectBreakMinutesPreset(breakMinutes)
        assertWorkAndBreakPresetsSelected(workMinutes, breakMinutes)
    }

    fun joinWithDisplayName(displayName: String) {
        PlaywrightAssertions.assertThat(joinButton()).isVisible()
        joinDisplayNameInput().fill(displayName)
        joinButton().click()
        PlaywrightAssertions.assertThat(joinButton()).hasCount(0)
    }

    fun assertWorkAndBreakMinutes(workMinutes: String, breakMinutes: String) {
        assertWorkAndBreakPresetsSelected(workMinutes, breakMinutes)
    }

    fun assertWorkAndBreakMinutesReadOnly(workMinutes: String, breakMinutes: String) {
        assertWorkAndBreakPresetsSelected(workMinutes, breakMinutes)
        for (preset in minutePresets) {
            PlaywrightAssertions.assertThat(workPresetOption(preset)).isDisabled()
            PlaywrightAssertions.assertThat(breakPresetOption(preset)).isDisabled()
        }
    }

    fun selectWorkMinutesPreset(minutes: String) {
        workPresetOption(minutes).click()
        PlaywrightAssertions.assertThat(workPresetOption(minutes)).hasAttribute("aria-checked", "true")
    }

    fun selectBreakMinutesPreset(minutes: String) {
        breakPresetOption(minutes).click()
        PlaywrightAssertions.assertThat(breakPresetOption(minutes)).hasAttribute("aria-checked", "true")
    }

    fun assertWorkAndBreakPresetsSelected(workMinutes: String, breakMinutes: String) {
        assertPresetSelected(workMinutesGroup(), workMinutes)
        assertPresetSelected(breakMinutesGroup(), breakMinutes)
    }

    fun assertWorkAndBreakCustomValuesWithPresetsUnselected(
        workMinutes: String,
        breakMinutes: String,
    ) {
        PlaywrightAssertions.assertThat(workCurrentMinutes()).hasText(workMinutes)
        PlaywrightAssertions.assertThat(breakCurrentMinutes()).hasText(breakMinutes)
        assertAllPresetsUnchecked(workMinutesGroup())
        assertAllPresetsUnchecked(breakMinutesGroup())
    }

    fun assertWorkPresetSelectedAndBreakCustomUnselected(
        workMinutes: String,
        breakMinutes: String,
    ) {
        assertPresetSelected(workMinutesGroup(), workMinutes)
        PlaywrightAssertions.assertThat(breakCurrentMinutes()).hasText(breakMinutes)
        assertAllPresetsUnchecked(breakMinutesGroup())
    }

    fun setWorkAndBreakMinutesViaApi(workMinutes: String, breakMinutes: String) {
        val code = readRoomCode()
        val participantId =
            playwrightPage.evaluate(
                "code => sessionStorage.getItem('pacer:' + code + ':participantId')",
                code,
            ) as String?
        require(!participantId.isNullOrBlank()) {
            "sessionStorage に参加者IDがありません: code=$code"
        }
        val origin = config.target.url.toString().trimEnd('/')
        val response =
            playwrightPage.request().patch(
                "$origin/api/rooms/${code.trim()}",
                RequestOptions.create()
                    .setHeader("Content-Type", "application/json")
                    .setData(
                        mapOf(
                            "participantId" to participantId,
                            "workMinutes" to workMinutes.toInt(),
                            "breakMinutes" to breakMinutes.toInt(),
                        ),
                    ),
            )
        require(response.ok()) {
            "作業・休憩の API 更新に失敗しました: status=${response.status()} body=${response.text()}"
        }
        playwrightPage.reload()
        assertOnRoomPage()
        PlaywrightAssertions.assertThat(roomCode()).isVisible()
    }

    fun assertAutoDisplayNameVisible() {
        PlaywrightAssertions.assertThat(displayNameInput()).isVisible()
        val name = displayNameInput().inputValue().trim()
        require(name.isNotEmpty()) { "表示名が自動で付いていません" }
        require(AUTO_DISPLAY_NAMES.contains(name)) {
            "表示名が候補一覧にありません: $name"
        }
    }

    fun assertStartButtonAbsent() {
        PlaywrightAssertions.assertThat(startButton()).hasCount(0)
    }

    fun assertShareUrlMatchesAddressBar() {
        PlaywrightAssertions.assertThat(shareUrl()).isVisible()
        val displayed = shareUrl().innerText().trim().ifEmpty { shareUrl().inputValue().trim() }
        val addressBar = playwrightPage.url().trimEnd('/')
        val normalizedDisplayed = displayed.trimEnd('/')
        require(normalizedDisplayed == addressBar) {
            "共有URLがアドレスバーと一致しません: displayed=$displayed addressBar=${playwrightPage.url()}"
        }
    }

    fun copyShareUrlAndAssertClipboard() {
        playwrightPage.context().grantPermissions(listOf("clipboard-read", "clipboard-write"))
        val expected =
            shareUrl().innerText().trim().ifEmpty { shareUrl().inputValue().trim() }.trimEnd('/')
        copyShareUrlButton().click()
        val actual = (playwrightPage.evaluate("navigator.clipboard.readText()") as String).trim().trimEnd('/')
        require(actual == expected) {
            "クリップボードが共有URLと一致しません: clipboard=$actual expected=$expected"
        }
    }

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

    fun changeOwnDisplayNameViaProfile(ownDisplayName: String, newDisplayName: String) {
        val emojiBefore = readParticipantEmoji(ownDisplayName)
        assertProfileDialogVisible()
        profileDisplayNameInput().fill(newDisplayName)
        profileSaveButton().click()
        PlaywrightAssertions.assertThat(profileDialog()).isHidden()
        assertParticipantAvatarWithName(newDisplayName)
        assertParticipantEmojiEquals(newDisplayName, emojiBefore)
        assertParticipantHasYou(newDisplayName)
    }

    fun attemptChangeOwnDisplayNameViaProfile(newDisplayName: String) {
        assertProfileDialogVisible()
        profileDisplayNameInput().fill(newDisplayName)
        profileSaveButton().click()
    }

    fun assertDisplayNameConflictError() {
        PlaywrightAssertions.assertThat(displayNameConflictError()).isVisible()
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
            .filter { it.isNotEmpty() && it != "保存" }

    private fun emojiOption(emoji: String): Locator =
        profileDialog().getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName(emoji))

    private fun profileDisplayNameInput(): Locator =
        profileDialog().getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("表示名"))

    private fun profileSaveButton(): Locator =
        profileDialog().getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("保存"))

    private fun displayNameConflictError(): Locator =
        main.getByRole(AriaRole.ALERT).filter(
            Locator.FilterOptions().setHasText("表示名"),
        )

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

    private fun shareUrl(): Locator =
        main.getByRole(AriaRole.STATUS, Locator.GetByRoleOptions().setName("共有URL"))

    private fun copyShareUrlButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("共有URLをコピー"))

    private fun workMinutesGroup(): Locator =
        main.getByRole(AriaRole.RADIOGROUP, Locator.GetByRoleOptions().setName("作業（分）"))

    private fun breakMinutesGroup(): Locator =
        main.getByRole(AriaRole.RADIOGROUP, Locator.GetByRoleOptions().setName("休憩（分）"))

    private fun workPresetOption(minutes: String): Locator =
        workMinutesGroup().getByRole(AriaRole.RADIO, Locator.GetByRoleOptions().setName(minutes))

    private fun breakPresetOption(minutes: String): Locator =
        breakMinutesGroup().getByRole(AriaRole.RADIO, Locator.GetByRoleOptions().setName(minutes))

    private fun workCurrentMinutes(): Locator =
        main.getByRole(AriaRole.STATUS, Locator.GetByRoleOptions().setName("作業の現在（分）"))

    private fun breakCurrentMinutes(): Locator =
        main.getByRole(AriaRole.STATUS, Locator.GetByRoleOptions().setName("休憩の現在（分）"))

    private fun assertPresetSelected(group: Locator, minutes: String) {
        val selected =
            group.getByRole(AriaRole.RADIO, Locator.GetByRoleOptions().setName(minutes))
        PlaywrightAssertions.assertThat(selected).hasAttribute("aria-checked", "true")
        for (preset in minutePresets) {
            if (preset == minutes) continue
            PlaywrightAssertions.assertThat(
                group.getByRole(AriaRole.RADIO, Locator.GetByRoleOptions().setName(preset)),
            ).hasAttribute("aria-checked", "false")
        }
    }

    private fun assertAllPresetsUnchecked(group: Locator) {
        for (preset in minutePresets) {
            PlaywrightAssertions.assertThat(
                group.getByRole(AriaRole.RADIO, Locator.GetByRoleOptions().setName(preset)),
            ).hasAttribute("aria-checked", "false")
        }
    }

    private fun displayNameInput(): Locator =
        main.getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("表示名"))

    private fun joinDisplayNameInput(): Locator =
        main.getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("参加用の表示名"))

    private fun joinButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("参加"))

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

    companion object {
        private val AUTO_DISPLAY_NAMES = setOf(
            "ねこぱんつ",
            "うどん侍",
            "もちもち太郎",
            "かりんとう姫",
            "ささみ騎士",
            "ぷりん将軍",
            "やきとり船長",
            "めんだこ博士",
        )
    }
}
