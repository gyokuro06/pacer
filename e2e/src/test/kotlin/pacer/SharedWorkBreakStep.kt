package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.HomePage
import pacer.pages.RoomPage

class SharedWorkBreakStep {
    @Step("参加者 <name> としてホームを開く")
    fun 参加者としてホームを開く(name: String) {
        ParticipantSessions.openHome(name, SetupAndTeardown.browser())
    }

    @Step("参加者 <name> が表示名 <displayName> で作業 <workMinutes> 分・休憩 <breakMinutes> 分でルームを作成する")
    fun 参加者が表示名で作業分休憩分でルームを作成する(
        name: String,
        displayName: String,
        workMinutes: String,
        breakMinutes: String,
    ) {
        HomePage(ParticipantSessions.page(name)).createRoom(workMinutes, breakMinutes, displayName)
    }

    @Step("参加者 <name> にルームコードが表示されている")
    fun 参加者にルームコードが表示されている(name: String) {
        val code = RoomPage(ParticipantSessions.page(name)).readRoomCode()
        ParticipantSessions.rememberRoomCode(code)
    }

    @Step("参加者 <name> が Alice のルームコードと表示名 <displayName> で参加する")
    fun 参加者がAliceのルームコードと表示名で参加する(name: String, displayName: String) {
        HomePage(ParticipantSessions.page(name)).joinRoom(ParticipantSessions.roomCode(), displayName)
    }

    @Step("参加者 <name> がセッションを開始する")
    fun 参加者がセッションを開始する(name: String) {
        RoomPage(ParticipantSessions.page(name)).startSession()
    }

    @Step("参加者 <name> に作業フェーズと残り時間が表示されている")
    fun 参加者に作業フェーズと残り時間が表示されている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertWorkPhaseWithRemainingTime()
    }

    @Step("参加者 <name> の残り時間を記憶する")
    fun 参加者の残り時間を記憶する(name: String) {
        val remaining = RoomPage(ParticipantSessions.page(name)).readRemainingTime()
        ParticipantSessions.rememberRemainingTime(remaining)
    }

    @Step("参加者 <name> に作業フェーズと記憶した残り時間が復元されている")
    fun 参加者に作業フェーズと記憶した残り時間が復元されている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertWorkPhaseWithRestoredRemainingTime(
            ParticipantSessions.rememberedRemainingMmSs(),
            ParticipantSessions.rememberedRemainingAtMs(),
        )
    }

    @Step("参加者 <name> が休憩を提案する")
    fun 参加者が休憩を提案する(name: String) {
        RoomPage(ParticipantSessions.page(name)).proposeBreak()
    }

    @Step("参加者 <name> が休憩提案を確定する")
    fun 参加者が休憩提案を確定する(name: String) {
        RoomPage(ParticipantSessions.page(name)).confirmBreakProposal()
    }

    @Step("参加者 <name> に休憩フェーズが表示されている")
    fun 参加者に休憩フェーズが表示されている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertBreakPhase()
    }

    @Step("参加者 <name> が再開を提案する")
    fun 参加者が再開を提案する(name: String) {
        RoomPage(ParticipantSessions.page(name)).proposeResume()
    }

    @Step("参加者 <name> が再開提案を確定する")
    fun 参加者が再開提案を確定する(name: String) {
        RoomPage(ParticipantSessions.page(name)).confirmResumeProposal()
    }
}
