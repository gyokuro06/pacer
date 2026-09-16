package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class DirectTimerShareStep {
    @Step("参加者 <name> がルーム画面にいる")
    fun 参加者がルーム画面にいる(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertOnRoomPage()
    }

    @Step("参加者 <name> に作業 <workMinutes> 分・休憩 <breakMinutes> 分が表示されている")
    fun 参加者に作業分休憩分が表示されている(
        name: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        RoomPage(ParticipantSessions.page(name)).assertWorkAndBreakMinutes(workMinutes, breakMinutes)
    }

    @Step("参加者 <name> にアドレスバーと同じ共有URLが表示されている")
    fun 参加者にアドレスバーと同じ共有URLが表示されている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertShareUrlMatchesAddressBar()
    }

    @Step("参加者 <name> が共有URLをコピーするとクリップボードに共有URLが入る")
    fun 参加者が共有URLをコピーするとクリップボードに共有URLが入る(name: String) {
        RoomPage(ParticipantSessions.page(name)).copyShareUrlAndAssertClipboard()
    }

    @Step("参加者 <name> に表示名が自動で付いている")
    fun 参加者に表示名が自動で付いている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertAutoDisplayNameVisible()
    }

    @Step("参加者 <name> として共有ルームを開く")
    fun 参加者として共有ルームを開く(name: String) {
        ParticipantSessions.openSharedRoom(
            name,
            SetupAndTeardown.browser(),
            ParticipantSessions.roomCode(),
        )
    }

    @Step("参加者 <name> に作業 <workMinutes> 分・休憩 <breakMinutes> 分が読み取り専用で表示されている")
    fun 参加者に作業分休憩分が読み取り専用で表示されている(
        name: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        RoomPage(ParticipantSessions.page(name))
            .assertWorkAndBreakMinutesReadOnly(workMinutes, breakMinutes)
    }

    @Step("参加者 <name> にスタートボタンがない")
    fun 参加者にスタートボタンがない(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertStartButtonAbsent()
    }

    @Step("参加者 <name> が表示名 <displayName> で参加する")
    fun 参加者が表示名で参加する(name: String, displayName: String) {
        RoomPage(ParticipantSessions.page(name)).joinWithDisplayName(displayName)
    }
}
