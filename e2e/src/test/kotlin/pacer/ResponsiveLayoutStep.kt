package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.LayoutContract

class ResponsiveLayoutStep {
    @Step("参加者 <name> のページに device-width の viewport meta がある")
    fun 参加者のページにDeviceWidthのViewportMetaがある(name: String) {
        LayoutContract.assertViewportDeviceWidth(ParticipantSessions.page(name))
    }

    @Step("参加者 <name> のページに横スクロールがない")
    fun 参加者のページに横スクロールがない(name: String) {
        LayoutContract.assertNoHorizontalOverflow(ParticipantSessions.page(name))
    }

    @Step("参加者 <name> のホーム main カラムがワイド画面で十分な幅を使う")
    fun 参加者のホームMainカラムがワイド画面で十分な幅を使う(name: String) {
        LayoutContract.assertMainColumnWideEnough(ParticipantSessions.page(name))
    }

    @Step("参加者 <name> のホーム見出しが読みやすい流体サイズである")
    fun 参加者のホーム見出しが読みやすい流体サイズである(name: String) {
        LayoutContract.assertHomeBrandFluidTypography(ParticipantSessions.page(name))
    }

    @Step("参加者 <name> のホーム主要ボタンがタッチしやすいサイズである")
    fun 参加者のホーム主要ボタンがタッチしやすいサイズである(name: String) {
        LayoutContract.assertHomePrimaryButtons(ParticipantSessions.page(name))
    }

    @Step("参加者 <name> のルーム main カラムがワイド画面で十分な幅を使う")
    fun 参加者のルームMainカラムがワイド画面で十分な幅を使う(name: String) {
        LayoutContract.assertMainColumnWideEnough(ParticipantSessions.page(name))
    }

    @Step("参加者 <name> のタイマー表示が読みやすい流体サイズである")
    fun 参加者のタイマー表示が読みやすい流体サイズである(name: String) {
        LayoutContract.assertRoomTimerFluidTypography(ParticipantSessions.page(name))
    }

    @Step("参加者 <name> のルーム操作ボタンがタッチしやすいサイズである")
    fun 参加者のルーム操作ボタンがタッチしやすいサイズである(name: String) {
        LayoutContract.assertRoomStartButton(ParticipantSessions.page(name))
    }
}
