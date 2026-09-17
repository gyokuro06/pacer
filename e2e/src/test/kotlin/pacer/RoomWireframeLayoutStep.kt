package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class RoomWireframeLayoutStep {
    @Step("参加者 <name> のルーム第一画面がワイヤの骨格である")
    fun 参加者のルーム第一画面がワイヤの骨格である(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertWireframeFirstScreenSkeleton()
    }

    @Step("参加者 <name> の参加者枠に自分の表示名が見える")
    fun 参加者の参加者枠に自分の表示名が見える(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertOwnDisplayNameInParticipantSlot()
    }

    @Step("参加者 <name> に作業フェーズがタイマーの上に表示されている")
    fun 参加者に作業フェーズがタイマーの上に表示されている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertWorkPhaseAboveTimer()
    }

    @Step("参加者 <name> のアドレスバーからルームコードを記憶する")
    fun 参加者のアドレスバーからルームコードを記憶する(name: String) {
        val code = RoomPage(ParticipantSessions.page(name)).readRoomCodeFromUrl()
        ParticipantSessions.rememberRoomCode(code)
    }

    @Step("参加者 <name> が参加ダイアログから表示名 <displayName> で参加する")
    fun 参加者が参加ダイアログから表示名で参加する(name: String, displayName: String) {
        RoomPage(ParticipantSessions.page(name)).joinViaJoinDialog(displayName)
    }

    @Step("参加者 <name> の参加者枠に表示名 <displayName> が見える")
    fun 参加者の参加者枠に表示名が見える(name: String, displayName: String) {
        RoomPage(ParticipantSessions.page(name)).assertDisplayNameInParticipantSlot(displayName)
    }

    @Step("参加者 <name> が参加者枠の自分の表示名からプロフィールを開く")
    fun 参加者が参加者枠の自分の表示名からプロフィールを開く(name: String) {
        RoomPage(ParticipantSessions.page(name)).openOwnProfileViaParticipantSlot()
    }
}
