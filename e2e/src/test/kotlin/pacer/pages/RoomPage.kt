package pacer.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.RequestOptions
import java.util.regex.Pattern
import pacer.ParticipantSessions
import pacer.config

class RoomPage(page: Page) : BasePage(page) {
    private val main = playwrightPage.getByRole(AriaRole.MAIN)
    private val minutePresets = listOf("60", "30", "15", "10")

    fun assertOnRoomPage() {
        playwrightPage.waitForURL(Pattern.compile(".*/room/[A-Za-z0-9]+/?$"))
    }

    fun assertWireframeFirstScreenSkeleton() {
        PlaywrightAssertions.assertThat(brand()).isVisible()
        PlaywrightAssertions.assertThat(shareUrl()).isVisible()
        PlaywrightAssertions.assertThat(timerRegion()).isVisible()
        PlaywrightAssertions.assertThat(primaryCta()).isVisible()
        PlaywrightAssertions.assertThat(workMinutesGroup()).isVisible()
        PlaywrightAssertions.assertThat(breakMinutesGroup()).isVisible()
        PlaywrightAssertions.assertThat(participants()).isVisible()
        assertWireframeVerticalOrder()
        assertBrandLeftOfShareUrl()
    }

    fun assertOwnDisplayNameInParticipantSlot() {
        PlaywrightAssertions.assertThat(ownParticipantSlot()).isVisible()
        val displayName = readOwnDisplayNameFromSlot()
        require(displayName.isNotEmpty()) {
            "参加者枠に自分の表示名が見えません"
        }
        PlaywrightAssertions.assertThat(
            ownParticipantSlot().getByText(displayName, Locator.GetByTextOptions().setExact(true)),
        ).isVisible()
        PlaywrightAssertions.assertThat(
            ownParticipantSlot().getByText("You", Locator.GetByTextOptions().setExact(true)),
        ).isVisible()
    }

    fun assertDisplayNameInParticipantSlot(displayName: String) {
        PlaywrightAssertions.assertThat(participantSlotContaining(displayName)).isVisible()
        PlaywrightAssertions.assertThat(
            participantSlotContaining(displayName)
                .getByText(displayName, Locator.GetByTextOptions().setExact(true)),
        ).isVisible()
    }

    fun assertWorkPhaseAboveTimer() {
        PlaywrightAssertions.assertThat(phaseLabel("作業")).isVisible()
        PlaywrightAssertions.assertThat(remainingTime()).isVisible()
        val phaseBox =
            phaseLabel("作業").boundingBox()
                ?: error("フェーズの位置が取得できません")
        val timerBox =
            remainingTime().boundingBox()
                ?: error("タイマーの位置が取得できません")
        require(phaseBox.y + phaseBox.height <= timerBox.y + 8) {
            "作業フェーズがタイマーの上にありません: phase.bottom=${phaseBox.y + phaseBox.height} timer.y=${timerBox.y}"
        }
    }

    fun readRoomCodeFromUrl(): String {
        val matcher = ROOM_CODE_IN_URL.matcher(playwrightPage.url())
        require(matcher.find()) {
            "アドレスバーからルームコードを読めません: ${playwrightPage.url()}"
        }
        return matcher.group(1)
    }

    fun joinViaJoinDialog(displayName: String) {
        PlaywrightAssertions.assertThat(shareUrl()).isVisible()
        val dialog = joinDialog()
        if (dialog.count() == 0) {
            PlaywrightAssertions.assertThat(openJoinDialogButton()).isVisible()
            openJoinDialogButton().click()
        }
        PlaywrightAssertions.assertThat(dialog).isVisible()
        dialog
            .getByRole(AriaRole.TEXTBOX, Locator.GetByRoleOptions().setName("参加用の表示名"))
            .fill(displayName)
        dialog.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("参加")).click()
        PlaywrightAssertions.assertThat(dialog).hasCount(0)
    }

    fun openOwnProfileViaParticipantSlot() {
        val displayName = readOwnDisplayNameFromSlot()
        ownParticipantSlot()
            .getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName(displayName).setExact(true))
            .click()
        assertProfileDialogVisible()
    }

    private fun assertWireframeVerticalOrder() {
        val brandBox = requireBox(brand(), "ブランド")
        val shareBox = requireBox(shareUrl(), "共有URL")
        val timerBox = requireBox(timerRegion(), "タイマー")
        val ctaBox = requireBox(primaryCta(), "主CTA")
        val workBox = requireBox(workMinutesGroup(), "作業プリセット")
        val slotsBox = requireBox(participants(), "参加者枠")
        require(brandBox.y < timerBox.y) {
            "ブランドがタイマーより上にありません: brand.y=${brandBox.y} timer.y=${timerBox.y}"
        }
        require(shareBox.y < timerBox.y) {
            "共有URLがタイマーより上にありません: share.y=${shareBox.y} timer.y=${timerBox.y}"
        }
        require(timerBox.y + timerBox.height <= ctaBox.y + 8) {
            "主CTAがタイマーの下にありません: timer.bottom=${timerBox.y + timerBox.height} cta.y=${ctaBox.y}"
        }
        require(ctaBox.y + ctaBox.height <= workBox.y + 8) {
            "work/rest が主CTAの下にありません: cta.bottom=${ctaBox.y + ctaBox.height} work.y=${workBox.y}"
        }
        require(workBox.y + workBox.height <= slotsBox.y + 8) {
            "参加者枠が下段にありません: work.bottom=${workBox.y + workBox.height} slots.y=${slotsBox.y}"
        }
    }

    private fun assertBrandLeftOfShareUrl() {
        val brandBox = requireBox(brand(), "ブランド")
        val shareBox = requireBox(shareUrl(), "共有URL")
        require(brandBox.x < shareBox.x) {
            "ブランドが共有URLより左にありません: brand.x=${brandBox.x} share.x=${shareBox.x}"
        }
    }

    private fun requireBox(locator: Locator, label: String): com.microsoft.playwright.options.BoundingBox =
        locator.boundingBox() ?: error("${label}の位置が取得できません")

    private fun brand(): Locator =
        main.getByRole(AriaRole.HEADING, Locator.GetByRoleOptions().setName("pacer"))

    private fun timerRegion(): Locator =
        main.getByRole(AriaRole.REGION, Locator.GetByRoleOptions().setName("タイマー"))

    private fun primaryCta(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("スタート"))

    private fun ownParticipantSlot(): Locator =
        participants()
            .getByRole(AriaRole.LISTITEM)
            .filter(
                Locator.FilterOptions().setHas(
                    playwrightPage.getByText("You", Page.GetByTextOptions().setExact(true)),
                ),
            )

    private fun participantSlotContaining(displayName: String): Locator =
        participants()
            .getByRole(AriaRole.LISTITEM)
            .filter(
                Locator.FilterOptions().setHas(
                    playwrightPage.getByText(displayName, Page.GetByTextOptions().setExact(true)),
                ),
            )

    private fun readOwnDisplayNameFromSlot(): String {
        PlaywrightAssertions.assertThat(ownParticipantSlot()).isVisible()
        val label =
            ownParticipantSlot()
                .locator("[aria-label$='のアバター']")
                .getAttribute("aria-label")
                ?.removeSuffix("のアバター")
                ?.trim()
                .orEmpty()
        require(label.isNotEmpty()) {
            "参加者枠から自分の表示名を読めません"
        }
        return label
    }

    private fun joinDialog(): Locator =
        playwrightPage.getByRole(AriaRole.DIALOG, Page.GetByRoleOptions().setName("参加"))

    private fun openJoinDialogButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("参加する"))

    fun setDisplayName(displayName: String) {
        openOwnProfileViaParticipantSlot()
        profileDisplayNameInput().fill(displayName)
        profileSaveButton().click()
        PlaywrightAssertions.assertThat(profileDialog()).isHidden()
        assertDisplayNameInParticipantSlot(displayName)
    }

    fun setWorkAndBreakMinutes(workMinutes: String, breakMinutes: String) {
        selectWorkMinutesPreset(workMinutes)
        selectBreakMinutesPreset(breakMinutes)
        assertWorkAndBreakPresetsSelected(workMinutes, breakMinutes)
    }

    fun joinWithDisplayName(displayName: String) {
        joinViaJoinDialog(displayName)
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
        PlaywrightAssertions.assertThat(shareUrl()).isVisible()
    }

    fun assertAutoDisplayNameVisible() {
        val name = readOwnDisplayNameFromSlot()
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

    fun readRoomCode(): String = readRoomCodeFromUrl()

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
        assertRestoredRemainingTime(rememberedMmSs, rememberedAtMs, toleranceSeconds)
    }

    fun assertBreakPhaseWithRestoredRemainingTime(
        rememberedMmSs: String,
        rememberedAtMs: Long,
        toleranceSeconds: Long = 5,
    ) {
        PlaywrightAssertions.assertThat(phaseLabel("休憩")).isVisible()
        assertRestoredRemainingTime(rememberedMmSs, rememberedAtMs, toleranceSeconds)
    }

    private fun assertRestoredRemainingTime(
        rememberedMmSs: String,
        rememberedAtMs: Long,
        toleranceSeconds: Long,
    ) {
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

    fun assertRemainingTimeApproximately(minutes: Int, toleranceSeconds: Long = 15) {
        PlaywrightAssertions.assertThat(remainingTime()).isVisible()
        val actualMmSs = readRemainingTime()
        val actualSeconds = parseMmSsToSeconds(actualMmSs)
        val expectedSeconds = minutes * 60.0
        val delta = kotlin.math.abs(actualSeconds - expectedSeconds)
        require(delta <= toleranceSeconds) {
            "残り時間がおよそ ${minutes} 分ではありません: actual=$actualMmSs " +
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

    fun readParticipantId(): String {
        val code = readRoomCode()
        val participantId =
            playwrightPage.evaluate(
                "code => sessionStorage.getItem('pacer:' + code + ':participantId')",
                code,
            ) as String?
        require(!participantId.isNullOrBlank()) {
            "sessionStorage に参加者IDがありません: code=$code"
        }
        return participantId
    }

    fun postPhaseActionConcurrentlyWith(
        other: RoomPage,
        pathSuffix: String,
        body: Map<String, Any?> = emptyMap(),
    ): ConcurrentPhaseOutcome {
        val code = readRoomCode()
        val origin = config.target.url.toString().trimEnd('/')
        val selfId = readParticipantId()
        val otherId = other.readParticipantId()
        @Suppress("UNCHECKED_CAST")
        val result =
            playwrightPage.evaluate(
                """async ({ origin, code, pathSuffix, selfId, otherId, body }) => {
                     const post = async (participantId) => {
                       const response = await fetch(
                         origin + '/api/rooms/' + encodeURIComponent(code) + pathSuffix,
                         {
                           method: 'POST',
                           headers: { 'Content-Type': 'application/json' },
                           body: JSON.stringify({ ...body, participantId }),
                         },
                       );
                       return response.status;
                     };
                     const statuses = await Promise.all([post(selfId), post(otherId)]);
                     return { statuses };
                   }""",
                mapOf(
                    "origin" to origin,
                    "code" to code,
                    "pathSuffix" to pathSuffix,
                    "selfId" to selfId,
                    "otherId" to otherId,
                    "body" to body,
                ),
            ) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val statuses = (result["statuses"] as List<Any?>).map { (it as Number).toInt() }
        val successCount = statuses.count { it in 200..299 }
        val conflictCount = statuses.count { it == 409 }
        playwrightPage.reload()
        assertOnRoomPage()
        other.playwrightPage.reload()
        other.assertOnRoomPage()
        return ConcurrentPhaseOutcome(successCount = successCount, conflictCount = conflictCount)
    }

    data class ConcurrentPhaseOutcome(val successCount: Int, val conflictCount: Int)

    fun freezeRoomGetAsCurrentSnapshot(freezeKey: String) {
        val code = readRoomCode()
        val origin = config.target.url.toString().trimEnd('/')
        val response = playwrightPage.request().get("$origin/api/rooms/${code.trim()}")
        require(response.ok()) {
            "ルーム取得の固定用スナップショットに失敗: status=${response.status()}"
        }
        val json = response.text()
        ParticipantSessions.rememberFrozenRoomGetJson(freezeKey, json)
        val pattern = "**/api/rooms/$code"
        playwrightPage.unroute(pattern)
        playwrightPage.route(pattern) { route ->
            if (route.request().method().equals("GET", ignoreCase = true)) {
                route.fulfill(
                    com.microsoft.playwright.Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody(ParticipantSessions.frozenRoomGetJson(freezeKey)),
                )
            } else {
                route.resume()
            }
        }
    }

    fun unfreezeRoomGet(freezeKey: String) {
        val code = readRoomCode()
        playwrightPage.unroute("**/api/rooms/$code")
        ParticipantSessions.clearFrozenRoomGetJson(freezeKey)
        playwrightPage.reload()
        assertOnRoomPage()
    }

    fun assertConfirmProposalVisible() {
        PlaywrightAssertions.assertThat(confirmProposalButton()).isVisible()
    }

    fun confirmProposalCapturingAlertFlash(): Boolean {
        installAlertFlashSpy()
        PlaywrightAssertions.assertThat(confirmProposalButton()).isVisible()
        confirmProposalButton().click()
        playwrightPage.waitForTimeout(1_200.0)
        return alertFlashSeen()
    }

    fun assertNoPhaseActionError(alertFlashed: Boolean) {
        require(!alertFlashed) {
            "フェーズ操作のエラーが一瞬でも表示されました"
        }
        PlaywrightAssertions.assertThat(phaseActionError()).hasCount(0)
    }

    fun assertStartButtonVisible() {
        PlaywrightAssertions.assertThat(startButton()).isVisible()
    }

    fun startSessionCapturingAlertFlash(): Boolean {
        installAlertFlashSpy()
        PlaywrightAssertions.assertThat(startButton()).isVisible()
        startButton().click()
        playwrightPage.waitForTimeout(1_200.0)
        return alertFlashSeen()
    }

    private fun installAlertFlashSpy() {
        playwrightPage.evaluate(
            """() => {
                 window.__pacerAlertFlashSeen = false;
                 if (window.__pacerAlertFlashObserver) {
                   window.__pacerAlertFlashObserver.disconnect();
                 }
                 const mark = () => {
                   if (document.querySelector('[role="alert"]')) {
                     window.__pacerAlertFlashSeen = true;
                   }
                 };
                 mark();
                 const observer = new MutationObserver(mark);
                 observer.observe(document.documentElement, {
                   childList: true,
                   subtree: true,
                   attributes: true,
                 });
                 window.__pacerAlertFlashObserver = observer;
               }""",
        )
    }

    private fun alertFlashSeen(): Boolean =
        playwrightPage.evaluate("() => !!window.__pacerAlertFlashSeen") as Boolean

    private fun phaseActionError(): Locator =
        playwrightPage.getByRole(AriaRole.ALERT)

    fun advanceTimerPastEnd() {
        ParticipantSessions.ensureClockInstalled(playwrightPage)
        val remainingMs = (parseMmSsToSeconds(readRemainingTime()) * 1000).toLong() + 1_000
        playwrightPage.clock().fastForward(remainingMs.coerceAtLeast(1_000))
    }

    fun advanceTimerBySeconds(seconds: Long) {
        ParticipantSessions.ensureClockInstalled(playwrightPage)
        playwrightPage.clock().fastForward(seconds * 1_000)
    }

    fun assertRemainingTime(mmSs: String) {
        PlaywrightAssertions.assertThat(remainingTime()).hasText(mmSs)
    }

    fun assertBrowserNotificationCount(title: String, expectedCount: Int) {
        playwrightPage.waitForFunction(
            """({ title, expectedCount }) => {
                 const list = window.__pacerNotifications || [];
                 return list.filter((n) => String(n.title).includes(title)).length === expectedCount;
               }""",
            mapOf("title" to title, "expectedCount" to expectedCount),
            Page.WaitForFunctionOptions().setTimeout(10_000.0),
        )
        @Suppress("UNCHECKED_CAST")
        val titles =
            playwrightPage.evaluate(
                """title => (window.__pacerNotifications || [])
                     .filter(n => String(n.title).includes(title))
                     .map(n => n.title)""",
                title,
            ) as List<*>
        require(titles.size == expectedCount) {
            "ブラウザ通知「$title」の回数が一致しません: actual=${titles.size} expected=$expectedCount titles=$titles"
        }
    }

    fun assertEndChimePlayCount(expectedCount: Int) {
        playwrightPage.waitForFunction(
            """expectedCount => (window.__pacerChimePlays || []).length === expectedCount""",
            expectedCount,
            Page.WaitForFunctionOptions().setTimeout(10_000.0),
        )
        @Suppress("UNCHECKED_CAST")
        val plays =
            playwrightPage.evaluate("() => window.__pacerChimePlays || []") as List<*>
        require(plays.size == expectedCount) {
            "終了チャイムの再生回数が一致しません: actual=${plays.size} expected=$expectedCount plays=$plays"
        }
    }

    fun assertNotificationPermissionRequested() {
        playwrightPage.waitForFunction(
            "() => (window.__pacerPermissionRequests || 0) >= 1",
            null,
            Page.WaitForFunctionOptions().setTimeout(10_000.0),
        )
    }

    fun assertEnableNotificationsVisible() {
        PlaywrightAssertions.assertThat(enableNotificationsButton()).isVisible()
    }

    fun assertNotificationsGranted() {
        playwrightPage.waitForFunction(
            "() => Notification.permission === 'granted'",
            null,
            Page.WaitForFunctionOptions().setTimeout(5_000.0),
        )
    }

    fun clickEnableNotifications() {
        enableNotificationsButton().click()
    }

    private fun enableNotificationsButton(): Locator =
        main.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("通知をオン"))

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
            .mapNotNull { item ->
                val avatar = item.locator("[aria-label$='のアバター']")
                if (avatar.count() == 0) return@mapNotNull null
                val displayName =
                    avatar.getAttribute("aria-label")
                        ?.removeSuffix("のアバター")
                        ?: error("参加者の表示名が読めません: \"${item.innerText().trim()}\"")
                displayName to readParticipantEmoji(displayName)
            }
            .toMap()
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
        private val ROOM_CODE_IN_URL = Pattern.compile(".*/room/([A-Za-z0-9]+)/?$")
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
