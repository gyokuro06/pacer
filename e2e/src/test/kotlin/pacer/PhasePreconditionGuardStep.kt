package pacer

import com.thoughtworks.gauge.Step
import pacer.pages.RoomPage

class PhasePreconditionGuardStep {
    @Step("参加者 <nameA> と <nameB> がほぼ同時にセッションを開始する")
    fun 参加者がほぼ同時にセッションを開始する(nameA: String, nameB: String) {
        val outcome =
            RoomPage(ParticipantSessions.page(nameA)).postPhaseActionConcurrentlyWith(
                RoomPage(ParticipantSessions.page(nameB)),
                "/start",
            )
        ParticipantSessions.rememberConcurrentPhaseOutcome(
            outcome.successCount,
            outcome.conflictCount,
        )
    }

    @Step("参加者 <nameA> と <nameB> がほぼ同時に休憩を提案する")
    fun 参加者がほぼ同時に休憩を提案する(nameA: String, nameB: String) {
        val outcome =
            RoomPage(ParticipantSessions.page(nameA)).postPhaseActionConcurrentlyWith(
                RoomPage(ParticipantSessions.page(nameB)),
                "/propose",
                mapOf("kind" to "break"),
            )
        ParticipantSessions.rememberConcurrentPhaseOutcome(
            outcome.successCount,
            outcome.conflictCount,
        )
    }

    @Step("参加者 <nameA> と <nameB> がほぼ同時に休憩提案を確定する")
    fun 参加者がほぼ同時に休憩提案を確定する(nameA: String, nameB: String) {
        val outcome =
            RoomPage(ParticipantSessions.page(nameA)).postPhaseActionConcurrentlyWith(
                RoomPage(ParticipantSessions.page(nameB)),
                "/confirm",
            )
        ParticipantSessions.rememberConcurrentPhaseOutcome(
            outcome.successCount,
            outcome.conflictCount,
        )
    }

    @Step("直前の同時フェーズ操作は成功1回と前提不一致1回である")
    fun 直前の同時フェーズ操作は成功1回と前提不一致1回である() {
        val success = ParticipantSessions.lastConcurrentPhaseSuccessCount()
        val conflict = ParticipantSessions.lastConcurrentPhaseConflictCount()
        require(success == 1) {
            "同時フェーズ操作の成功が $success 回です（ちょうど1回であるべき）"
        }
        require(conflict == 1) {
            "同時フェーズ操作の前提不一致(409)が $conflict 回です（ちょうど1回であるべき）"
        }
    }

    @Step("参加者 <name> のルーム取得を待機中の状態で固定する")
    fun 参加者のルーム取得を待機中の状態で固定する(name: String) {
        val room = RoomPage(ParticipantSessions.page(name))
        room.assertStartButtonVisible()
        room.freezeRoomGetAsCurrentSnapshot(name)
    }

    @Step("参加者 <name> のルーム取得を提案確定前の状態で固定する")
    fun 参加者のルーム取得を提案確定前の状態で固定する(name: String) {
        val room = RoomPage(ParticipantSessions.page(name))
        room.assertConfirmProposalVisible()
        room.freezeRoomGetAsCurrentSnapshot(name)
    }

    @Step("参加者 <name> が固定表示のままセッション開始を試行する")
    fun 参加者が固定表示のままセッション開始を試行する(name: String) {
        val observation =
            RoomPage(ParticipantSessions.page(name)).startSessionCapturingConflictUi()
        ParticipantSessions.rememberAlertFlashSeen(observation.alertFlashed)
        ParticipantSessions.rememberConflictToastSeen(observation.toastFlashed)
    }

    @Step("参加者 <name> が固定表示のまま休憩提案の確定を試行する")
    fun 参加者が固定表示のまま休憩提案の確定を試行する(name: String) {
        val observation =
            RoomPage(ParticipantSessions.page(name)).confirmProposalCapturingConflictUi()
        ParticipantSessions.rememberAlertFlashSeen(observation.alertFlashed)
        ParticipantSessions.rememberConflictToastSeen(observation.toastFlashed)
    }

    @Step("参加者 <name> のルーム取得の固定を解除する")
    fun 参加者のルーム取得の固定を解除する(name: String) {
        RoomPage(ParticipantSessions.page(name)).unfreezeRoomGet(name)
    }

    @Step("参加者 <name> に衝突の共通トーストが一瞬表示された")
    fun 参加者に衝突の共通トーストが一瞬表示された(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertConflictToastFlashed(
            ParticipantSessions.lastConflictToastSeen(),
        )
    }

    @Step("参加者 <name> にフェーズ操作のインラインエラーが表示されていない")
    fun 参加者にフェーズ操作のインラインエラーが表示されていない(name: String) {
        RoomPage(ParticipantSessions.page(name)).assertNoPhaseActionError(
            ParticipantSessions.lastAlertFlashSeen(),
        )
    }
}
