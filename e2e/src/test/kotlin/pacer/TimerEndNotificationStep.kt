package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class TimerEndNotificationStep {
    @Step("参加者 <name> として通知を許可した状態でホームを開く")
    fun 参加者として通知を許可した状態でホームを開く(name: String) {
        ParticipantSessions.openHome(
            name,
            SetupAndTeardown.browser(),
            NotificationPermissionMode.GRANTED,
        )
    }

    @Step("参加者 <name> として通知を許可せずホームを開く")
    fun 参加者として通知を許可せずホームを開く(name: String) {
        ParticipantSessions.openHome(
            name,
            SetupAndTeardown.browser(),
            NotificationPermissionMode.NOT_GRANTED,
        )
    }

    @Step("参加者 <name> のタイマーを終了まで進める")
    fun 参加者のタイマーを終了まで進める(name: String) {
        RoomPage(ParticipantSessions.page(name)).advanceTimerPastEnd()
    }

    @Step("参加者 <name> のタイマーをさらに <seconds> 秒進める")
    fun 参加者のタイマーをさらに秒進める(name: String, seconds: String) {
        RoomPage(ParticipantSessions.page(name)).advanceTimerBySeconds(seconds.toLong())
    }

    @Step("参加者 <name> に残り時間 <mmSs> が表示されている")
    fun 参加者に残り時間が表示されている(name: String, mmSs: String) {
        RoomPage(ParticipantSessions.page(name)).assertRemainingTime(mmSs)
    }

    @Step("参加者 <name> にブラウザ通知 <title> が1回出ている")
    fun 参加者にブラウザ通知が1回出ている(name: String, title: String) {
        RoomPage(ParticipantSessions.page(name)).assertBrowserNotificationCount(title, 1)
    }

    @Step("参加者 <name> に終了チャイムが1回鳴っている")
    fun 参加者に終了チャイムが1回鳴っている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertEndChimePlayCount(1)
    }

    @Step("参加者 <name> に通知の許可が求められている")
    fun 参加者に通知の許可が求められている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertNotificationPermissionRequested()
    }

    @Step("参加者 <name> に通知をオンボタンが表示されている")
    fun 参加者に通知をオンボタンが表示されている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertEnableNotificationsVisible()
    }

    @Step("参加者 <name> が通知をオンボタンを押し通知を許可する")
    fun 参加者が通知をオンボタンを押し通知を許可する(name: String) {
        val page = ParticipantSessions.page(name)
        page.context().grantPermissions(listOf("notifications"))
        RoomPage(page).clickEnableNotifications()
    }

    @Step("参加者 <name> のブラウザ通知が許可されている")
    fun 参加者のブラウザ通知が許可されている(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertNotificationsGranted()
    }
}
