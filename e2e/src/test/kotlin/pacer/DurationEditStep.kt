package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class DurationEditStep {
    @Step("参加者 <name> が作業 <workMinutes> 分・休憩 <breakMinutes> 分に変更する")
    fun 参加者が作業分休憩分に変更する(
        name: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        RoomPage(ParticipantSessions.page(name)).setWorkAndBreakMinutes(workMinutes, breakMinutes)
    }

    @Step("参加者 <name> に残り時間がおよそ <minutes> 分で表示されている")
    fun 参加者に残り時間がおよそ分で表示されている(name: String, minutes: String) {
        RoomPage(ParticipantSessions.page(name))
            .assertRemainingTimeApproximately(minutes.toInt())
    }
}
