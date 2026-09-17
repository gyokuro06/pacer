package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class ParticipantEmojiStep {
    @Step("参加者 <name> のルームに表示名 <displayName> の参加者が絵文字付きで表示されている")
    fun 参加者のルームに表示名の参加者が絵文字付きで表示されている(
        name: String,
        displayName: String,
    ) {
        RoomPage(ParticipantSessions.page(name)).assertParticipantVisibleWithEmoji(displayName)
    }

    @Step("参加者 <name> のルームで表示名 <displayNameA> と <displayNameB> の絵文字が異なる")
    fun 参加者のルームで表示名の絵文字が異なる(
        name: String,
        displayNameA: String,
        displayNameB: String,
    ) {
        RoomPage(ParticipantSessions.page(name)).assertParticipantEmojisDistinct(displayNameA, displayNameB)
    }
}
