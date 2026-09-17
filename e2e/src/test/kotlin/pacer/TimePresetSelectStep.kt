package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class TimePresetSelectStep {
    @Step("参加者 <name> に作業 <workMinutes> 分・休憩 <breakMinutes> 分のプリセットが選ばれている")
    fun 参加者に作業分休憩分のプリセットが選ばれている(
        name: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        RoomPage(ParticipantSessions.page(name))
            .assertWorkAndBreakPresetsSelected(workMinutes, breakMinutes)
    }

    @Step("参加者 <name> が作業を <minutes> 分に選ぶ")
    fun 参加者が作業を分に選ぶ(name: String, minutes: String) {
        RoomPage(ParticipantSessions.page(name)).selectWorkMinutesPreset(minutes)
    }

    @Step("参加者 <name> が休憩を <minutes> 分に選ぶ")
    fun 参加者が休憩を分に選ぶ(name: String, minutes: String) {
        RoomPage(ParticipantSessions.page(name)).selectBreakMinutesPreset(minutes)
    }

    @Step("参加者 <name> のルームの作業を <workMinutes> 分・休憩を <breakMinutes> 分にAPIで設定する")
    fun 参加者のルームの作業を分休憩を分にAPIで設定する(
        name: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        RoomPage(ParticipantSessions.page(name))
            .setWorkAndBreakMinutesViaApi(workMinutes, breakMinutes)
    }

    @Step("参加者 <name> に作業の現在値 <workMinutes> 分と休憩の現在値 <breakMinutes> 分が表示されプリセットは未選択である")
    fun 参加者に作業の現在値と休憩の現在値が表示されプリセットは未選択である(
        name: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        RoomPage(ParticipantSessions.page(name))
            .assertWorkAndBreakCustomValuesWithPresetsUnselected(workMinutes, breakMinutes)
    }

    @Step("参加者 <name> に作業 <workMinutes> 分のプリセットが選ばれ休憩の現在値 <breakMinutes> 分が表示され休憩プリセットは未選択である")
    fun 参加者に作業のプリセットが選ばれ休憩の現在値が表示され休憩プリセットは未選択である(
        name: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        RoomPage(ParticipantSessions.page(name))
            .assertWorkPresetSelectedAndBreakCustomUnselected(workMinutes, breakMinutes)
    }
}
