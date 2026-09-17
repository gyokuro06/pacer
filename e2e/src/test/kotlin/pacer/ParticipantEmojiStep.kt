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

    @Step("参加者 <name> が表示名 <displayName> の絵文字を記憶する")
    fun 参加者が表示名の絵文字を記憶する(name: String, displayName: String) {
        val emoji = RoomPage(ParticipantSessions.page(name)).readParticipantEmoji(displayName)
        ParticipantSessions.rememberEmoji(displayName, emoji)
    }

    @Step("参加者 <name> の絵文字変更で選べるのは自分と未使用の絵文字だけである")
    fun 参加者の絵文字変更で選べるのは自分と未使用の絵文字だけである(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertOwnEmojiPickerOffersOnlyOwnAndUnused(name)
    }

    @Step("参加者 <name> が自分の絵文字を未使用のものに変更する")
    fun 参加者が自分の絵文字を未使用のものに変更する(name: String) {
        RoomPage(ParticipantSessions.page(name)).changeOwnEmojiToUnused(name)
    }

    @Step("参加者 <name> のルームに表示名 <displayName> の参加者が記憶と異なる絵文字で表示されている")
    fun 参加者のルームに表示名の参加者が記憶と異なる絵文字で表示されている(
        name: String,
        displayName: String,
    ) {
        val remembered = ParticipantSessions.rememberedEmoji(displayName)
        RoomPage(ParticipantSessions.page(name)).assertParticipantEmojiDiffers(displayName, remembered)
    }

    @Step("参加者 <name> のルームに表示名 <displayName> の参加者が記憶した絵文字で表示されている")
    fun 参加者のルームに表示名の参加者が記憶した絵文字で表示されている(
        name: String,
        displayName: String,
    ) {
        val remembered = ParticipantSessions.rememberedEmoji(displayName)
        RoomPage(ParticipantSessions.page(name)).assertParticipantEmojiEquals(displayName, remembered)
    }

    @Step("参加者 <name> のルームに表示名 <displayName> の参加者が Alice のルームと同じ絵文字で表示されている")
    fun 参加者のルームに表示名の参加者がAliceのルームと同じ絵文字で表示されている(
        name: String,
        displayName: String,
    ) {
        val expected = RoomPage(ParticipantSessions.page("Alice")).readParticipantEmoji(displayName)
        RoomPage(ParticipantSessions.page(name)).assertParticipantEmojiEquals(displayName, expected)
    }
}
