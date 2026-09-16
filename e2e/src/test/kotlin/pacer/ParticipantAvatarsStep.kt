package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class ParticipantAvatarsStep {
    @Step("参加者 <participant> に自分のアバターと名前が表示されている")
    fun assertOwnAvatarAndNameVisible(participant: String) {
        val session = ParticipantSessions.get(participant)
        val roomPage = RoomPage(session.page)
        roomPage.assertOwnAvatarVisible()
        roomPage.assertParticipantNamesVisible(participant)
    }

    @Step("参加者 <participant> の自分のアバターに <label> ラベルが表示されている")
    fun assertYouLabelVisible(participant: String, label: String) {
        val session = ParticipantSessions.get(participant)
        val roomPage = RoomPage(session.page)
        roomPage.assertYouLabelVisible()
    }

    @Step("参加者 <participant> に <name1> と <name2> のアバターが表示されている")
    fun assertTwoParticipantsVisible(participant: String, name1: String, name2: String) {
        val session = ParticipantSessions.get(participant)
        val roomPage = RoomPage(session.page)
        roomPage.assertParticipantsCount(2)
        roomPage.assertParticipantNamesVisible(name1, name2)
    }

    @Step("参加者 <participant> が自分の絵文字を <emoji> に変更する")
    fun changeOwnEmoji(participant: String, emoji: String) {
        val session = ParticipantSessions.get(participant)
        val roomPage = RoomPage(session.page)
        roomPage.changeOwnEmoji(emoji)
    }

    @Step("参加者 <participant> に自分の絵文字 <emoji> が表示されている")
    fun assertOwnEmojiVisible(participant: String, emoji: String) {
        val session = ParticipantSessions.get(participant)
        val roomPage = RoomPage(session.page)
        roomPage.assertOwnEmojiVisible(emoji)
    }

    @Step("参加者 <participant> に <emoji1> と <emoji2> の絵文字が表示されている")
    fun assertTwoEmojisVisible(participant: String, emoji1: String, emoji2: String) {
        val session = ParticipantSessions.get(participant)
        val roomPage = RoomPage(session.page)
        roomPage.assertEmojisVisible(emoji1, emoji2)
    }

    @Step("参加者 <participant> に 4人のアバターが表示されている")
    fun assertFourParticipantsVisible(participant: String) {
        val session = ParticipantSessions.get(participant)
        val roomPage = RoomPage(session.page)
        roomPage.assertParticipantsCount(4)
    }
}
