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
}
