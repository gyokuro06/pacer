"use client";

import {
  FormEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useParams } from "next/navigation";
import {
  canStart,
  crossedToTimerEnd,
  formatRemainingMs,
  PARTICIPANT_EMOJIS,
  phaseLabel,
  remainingMs,
  timerEndNotificationTitle,
  type Room,
} from "@/lib/room";
import styles from "./page.module.css";

const POLL_MS = 500;
const MINUTE_PRESETS = [60, 30, 15, 10] as const;

function isMinutePreset(value: number): boolean {
  return (MINUTE_PRESETS as readonly number[]).includes(value);
}

const TIMER_END_CHIME_URL = "/timer-end-chime.wav";

type NotificationPermissionState = NotificationPermission | "unsupported";

let timerEndChime: HTMLAudioElement | null = null;

function participantStorageKey(code: string) {
  return `pacer:${code}:participantId`;
}

function getTimerEndChime(): HTMLAudioElement {
  if (!timerEndChime) {
    timerEndChime = new Audio(TIMER_END_CHIME_URL);
    timerEndChime.preload = "auto";
  }
  return timerEndChime;
}

function unlockAudio() {
  try {
    const audio = getTimerEndChime();
    audio.load();
    audio.muted = true;
    audio.volume = 0;
    const primed = audio.play();
    audio.pause();
    audio.currentTime = 0;
    audio.muted = false;
    audio.volume = 1;
    void primed.catch(() => undefined);
  } catch {
    /* best-effort unlock via existing clicks */
  }
}

function playTimerEndChime() {
  try {
    const audio = getTimerEndChime();
    audio.muted = false;
    audio.volume = 1;
    audio.currentTime = 0;
    void audio.play().catch(() => undefined);
  } catch {
    /* autoplay / decode may fail; notification path is independent */
  }
}

function readParticipantId(code: string): string | null {
  if (typeof window === "undefined") return null;
  try {
    return sessionStorage.getItem(participantStorageKey(code));
  } catch {
    return null;
  }
}

function storeParticipant(code: string, participantId: string) {
  sessionStorage.setItem(participantStorageKey(code), participantId);
}

export default function RoomPage() {
  const params = useParams<{ code: string }>();
  const code = String(params.code ?? "").toUpperCase();
  const [room, setRoom] = useState<Room | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const [participantId, setParticipantId] = useState<string | null>(null);
  const [joinName, setJoinName] = useState("");
  const [joinDialogOpen, setJoinDialogOpen] = useState(false);
  const [shareUrl, setShareUrl] = useState("");
  const [copyDone, setCopyDone] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileDisplayName, setProfileDisplayName] = useState("");
  const [notificationPermission, setNotificationPermission] =
    useState<NotificationPermissionState>("default");
  const profileOpenRef = useRef(false);
  profileOpenRef.current = profileOpen;
  const previousRemainingRef = useRef<number | null>(null);
  const notifiedPhaseKeyRef = useRef<string | null>(null);
  const pendingEndPhaseKeyRef = useRef<string | null>(null);
  const chimedPhaseKeyRef = useRef<string | null>(null);

  useEffect(() => {
    setParticipantId(readParticipantId(code));
  }, [code]);

  useEffect(() => {
    setShareUrl(window.location.href);
  }, [code]);

  const refresh = useCallback(async () => {
    const response = await fetch(`/api/rooms/${encodeURIComponent(code)}`, {
      cache: "no-store",
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error ?? "ルーム取得に失敗しました");
    }
    setRoom(data.room);
  }, [code]);

  useEffect(() => {
    let cancelled = false;
    const tick = async () => {
      try {
        await refresh();
        if (!cancelled && !profileOpenRef.current) setError(null);
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "同期に失敗しました");
        }
      }
    };
    void tick();
    const pollId = window.setInterval(() => void tick(), POLL_MS);
    const clockId = window.setInterval(() => setNow(Date.now()), 250);
    return () => {
      cancelled = true;
      window.clearInterval(pollId);
      window.clearInterval(clockId);
    };
  }, [refresh]);

  useEffect(() => {
    if (!profileOpen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setProfileOpen(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [profileOpen]);

  async function postAction(
    path: string,
    body?: Record<string, unknown>,
  ): Promise<boolean> {
    if (!participantId) {
      setError("参加が必要です");
      return false;
    }
    setError(null);
    const response = await fetch(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...body, participantId }),
    });
    const data = await response.json();
    if (!response.ok) {
      if (response.status === 409) {
        await refresh();
        return false;
      }
      setError(data.error ?? "操作に失敗しました");
      return false;
    }
    setRoom(data.room);
    return true;
  }

  function openProfile(displayName: string) {
    setProfileDisplayName(displayName);
    setError(null);
    setProfileOpen(true);
  }

  async function selectEmoji(emoji: string) {
    if (!participantId) return;
    const ok = await postAction(`/api/rooms/${encodeURIComponent(code)}/emoji`, {
      emoji,
    });
    if (ok) setProfileOpen(false);
  }

  async function saveDisplayName() {
    if (!participantId) return;
    const ok = await postAction(
      `/api/rooms/${encodeURIComponent(code)}/display-name`,
      {
        displayName: profileDisplayName,
      },
    );
    if (ok) {
      setProfileOpen(false);
    }
  }

  async function patchRoom(body: Record<string, unknown>): Promise<boolean> {
    if (!participantId) return false;
    setError(null);
    const response = await fetch(`/api/rooms/${encodeURIComponent(code)}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...body, participantId }),
    });
    const data = await response.json();
    if (!response.ok) {
      setError(data.error ?? "更新に失敗しました");
      return false;
    }
    setRoom(data.room);
    return true;
  }

  async function onJoin(event: FormEvent) {
    event.preventDefault();
    unlockAudio();
    setError(null);
    const response = await fetch(`/api/rooms/${encodeURIComponent(code)}/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: joinName }),
    });
    const data = await response.json();
    if (!response.ok) {
      setError(data.error ?? "参加に失敗しました");
      return;
    }
    storeParticipant(data.room.code, data.participantId);
    setParticipantId(data.participantId);
    setRoom(data.room);
    setJoinDialogOpen(false);
    setJoinName("");
  }

  async function copyShareUrl() {
    const url = shareUrl || window.location.href;
    try {
      await navigator.clipboard.writeText(url);
      setError(null);
      setCopyDone(true);
      window.setTimeout(() => setCopyDone(false), 1500);
    } catch {
      setCopyDone(false);
      setError("共有URLのコピーに失敗しました");
    }
  }

  const isParticipant = useMemo(() => {
    if (!room || !participantId) return false;
    return room.participants.some((p) => p.id === participantId);
  }, [room, participantId]);

  useEffect(() => {
    if (!room) return;
    if (!isParticipant) {
      setJoinDialogOpen(true);
    } else {
      setJoinDialogOpen(false);
    }
  }, [room, isParticipant]);

  const readNotificationPermission = useCallback((): NotificationPermissionState => {
    if (typeof Notification === "undefined") return "unsupported";
    return Notification.permission;
  }, []);

  const requestNotificationPermission = useCallback(async () => {
    if (typeof Notification === "undefined") {
      setNotificationPermission("unsupported");
      return;
    }
    const result = await Notification.requestPermission();
    setNotificationPermission(result);
  }, []);

  useEffect(() => {
    setNotificationPermission(readNotificationPermission());
  }, [readNotificationPermission]);

  useEffect(() => {
    if (!isParticipant) return;
    void requestNotificationPermission();
  }, [isParticipant, requestNotificationPermission]);

  useEffect(() => {
    if (!room || room.phase === "waiting") {
      previousRemainingRef.current = null;
      pendingEndPhaseKeyRef.current = null;
      return;
    }

    const phaseKey = `${room.phase}:${room.phaseEndsAt}`;
    const remaining = remainingMs(room, now);
    const previous = previousRemainingRef.current;
    previousRemainingRef.current = remaining;

    if (crossedToTimerEnd(previous, remaining)) {
      const title = timerEndNotificationTitle(room.phase);
      if (title) {
        pendingEndPhaseKeyRef.current = phaseKey;
        if (chimedPhaseKeyRef.current !== phaseKey) {
          chimedPhaseKeyRef.current = phaseKey;
          playTimerEndChime();
        }
      }
    } else if (
      pendingEndPhaseKeyRef.current &&
      pendingEndPhaseKeyRef.current !== phaseKey
    ) {
      pendingEndPhaseKeyRef.current = null;
    }

    const pendingKey = pendingEndPhaseKeyRef.current;
    if (!pendingKey || pendingKey !== phaseKey) return;
    if (notifiedPhaseKeyRef.current === pendingKey) {
      pendingEndPhaseKeyRef.current = null;
      return;
    }

    const livePermission =
      typeof Notification === "undefined"
        ? "unsupported"
        : Notification.permission;
    if (livePermission !== "granted") return;

    const title = timerEndNotificationTitle(room.phase);
    if (!title) return;

    notifiedPhaseKeyRef.current = pendingKey;
    pendingEndPhaseKeyRef.current = null;
    try {
      new Notification(title);
    } catch {
      /* browser may reject; permission UI still covers recovery */
    }
  }, [room, now, notificationPermission]);

  const minutesEditable = isParticipant;

  if (!room) {
    return (
      <div className={styles.page}>
        <main className={styles.main}>
          <p>{error ?? "読み込み中…"}</p>
        </main>
      </div>
    );
  }

  const remaining = remainingMs(room, now);
  const showTimer = room.phase !== "waiting" && remaining != null;
  const timerEndedTitle =
    showTimer && remaining <= 0 ? timerEndNotificationTitle(room.phase) : null;
  const takenByOthers = new Set(
    room.participants
      .filter((p) => p.id !== participantId)
      .map((p) => p.emoji),
  );
  const showJoinDialog = !isParticipant && joinDialogOpen;

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <div className={styles.top}>
          <h1 className={styles.brand}>pacer</h1>
          <div className={styles.share}>
            <div role="status" aria-label="共有URL" className={styles.shareUrl}>
              {shareUrl}
            </div>
            <button
              type="button"
              className={styles.copyButton}
              aria-label="共有URLをコピー"
              onClick={() => void copyShareUrl()}
            >
              <svg
                aria-hidden="true"
                width="1em"
                height="1em"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <rect x="9" y="9" width="13" height="13" rx="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
            </button>
            {copyDone ? (
              <span className={styles.copyHint}>コピーしました</span>
            ) : null}
          </div>
        </div>

        <div className={styles.session}>
          <section
            role="region"
            aria-label="タイマー"
            className={styles.timerRegion}
          >
            {room.phase !== "waiting" ? (
              <div role="status" aria-label="フェーズ" className={styles.phase}>
                {phaseLabel(room.phase)}
              </div>
            ) : null}

            {timerEndedTitle ? (
              <p
                role="status"
                aria-live="assertive"
                className={styles.timerEndNotice}
              >
                {timerEndedTitle}
              </p>
            ) : null}

            {showTimer ? (
              <div role="timer" aria-label="残り時間" className={styles.timer}>
                {formatRemainingMs(remaining)}
              </div>
            ) : (
              <div className={styles.timerPlaceholder} aria-hidden="true">
                --:--
              </div>
            )}
          </section>

          <div className={styles.actions}>
            {room.phase === "waiting" && isParticipant ? (
              <button
                type="button"
                className={styles.actionPrimary}
                disabled={!canStart(room)}
                onClick={() => {
                  unlockAudio();
                  void requestNotificationPermission();
                  void postAction(
                    `/api/rooms/${encodeURIComponent(code)}/start`,
                  );
                }}
              >
                スタート
              </button>
            ) : null}

            {isParticipant &&
            notificationPermission !== "granted" &&
            notificationPermission !== "unsupported" ? (
              <button
                type="button"
                className={styles.actionSecondary}
                onClick={() => {
                  unlockAudio();
                  void requestNotificationPermission();
                }}
              >
                通知をオン
              </button>
            ) : null}

            {room.phase === "work" &&
            room.pendingProposal == null &&
            isParticipant ? (
              <button
                type="button"
                className={styles.actionSecondary}
                onClick={() =>
                  void postAction(
                    `/api/rooms/${encodeURIComponent(code)}/propose`,
                    { kind: "break" },
                  )
                }
              >
                休憩を提案
              </button>
            ) : null}

            {room.phase === "break" &&
            room.pendingProposal == null &&
            isParticipant ? (
              <button
                type="button"
                className={styles.actionSecondary}
                onClick={() =>
                  void postAction(
                    `/api/rooms/${encodeURIComponent(code)}/propose`,
                    { kind: "work" },
                  )
                }
              >
                再開を提案
              </button>
            ) : null}

            {room.pendingProposal != null && isParticipant ? (
              <button
                type="button"
                className={styles.actionPrimary}
                onClick={() =>
                  void postAction(`/api/rooms/${encodeURIComponent(code)}/confirm`)
                }
              >
                提案を確定
              </button>
            ) : null}

            {!isParticipant && !showJoinDialog ? (
              <button
                type="button"
                className={styles.actionPrimary}
                onClick={() => setJoinDialogOpen(true)}
              >
                参加する
              </button>
            ) : null}
          </div>
        </div>

        <div className={styles.minutes}>
          <div className={styles.field}>
            <span>作業（分）</span>
            {!isMinutePreset(room.workMinutes) ? (
              <div
                role="status"
                aria-label="作業の現在（分）"
                className={styles.currentMinutes}
              >
                {room.workMinutes}
              </div>
            ) : null}
            <div
              role="radiogroup"
              aria-label="作業（分）"
              className={styles.presetGroup}
            >
              {MINUTE_PRESETS.map((minutes) => (
                <button
                  key={`work-${minutes}`}
                  type="button"
                  role="radio"
                  aria-checked={room.workMinutes === minutes}
                  aria-label={String(minutes)}
                  className={styles.presetOption}
                  disabled={!minutesEditable}
                  onClick={() => {
                    if (!minutesEditable) return;
                    void patchRoom({ workMinutes: minutes });
                  }}
                >
                  {minutes}
                </button>
              ))}
            </div>
          </div>
          <div className={styles.field}>
            <span>休憩（分）</span>
            {!isMinutePreset(room.breakMinutes) ? (
              <div
                role="status"
                aria-label="休憩の現在（分）"
                className={styles.currentMinutes}
              >
                {room.breakMinutes}
              </div>
            ) : null}
            <div
              role="radiogroup"
              aria-label="休憩（分）"
              className={styles.presetGroup}
            >
              {MINUTE_PRESETS.map((minutes) => (
                <button
                  key={`break-${minutes}`}
                  type="button"
                  role="radio"
                  aria-checked={room.breakMinutes === minutes}
                  aria-label={String(minutes)}
                  className={styles.presetOption}
                  disabled={!minutesEditable}
                  onClick={() => {
                    if (!minutesEditable) return;
                    void patchRoom({ breakMinutes: minutes });
                  }}
                >
                  {minutes}
                </button>
              ))}
            </div>
          </div>
        </div>

        <ul aria-label="参加者" className={styles.participants}>
          {room.participants.map((participant) => {
            const isSelf = participant.id === participantId;
            return (
              <li key={participant.id} className={styles.participant}>
                {isSelf ? (
                  <button
                    type="button"
                    className={styles.avatar}
                    aria-label={`${participant.displayName}のアバター`}
                    onClick={() => openProfile(participant.displayName)}
                  >
                    {participant.emoji}
                  </button>
                ) : (
                  <span
                    className={styles.avatar}
                    aria-label={`${participant.displayName}のアバター`}
                  >
                    {participant.emoji}
                  </span>
                )}
                {isSelf ? (
                  <button
                    type="button"
                    className={styles.displayName}
                    onClick={() => openProfile(participant.displayName)}
                  >
                    {participant.displayName}
                  </button>
                ) : (
                  <span className={styles.displayName}>
                    {participant.displayName}
                  </span>
                )}
                {isSelf ? <span className={styles.youLabel}>You</span> : null}
              </li>
            );
          })}
        </ul>

        {showJoinDialog ? (
          <div className={styles.profileBackdrop}>
            <div
              role="dialog"
              aria-modal="true"
              aria-label="参加"
              className={styles.profileDialog}
            >
              <form onSubmit={onJoin}>
                <label className={styles.profileField}>
                  <span>表示名</span>
                  <input
                    type="text"
                    aria-label="参加用の表示名"
                    value={joinName}
                    onChange={(e) => setJoinName(e.target.value)}
                    required
                  />
                </label>
                <button type="submit" className={styles.profileSave}>
                  参加
                </button>
              </form>
            </div>
          </div>
        ) : null}

        {profileOpen ? (
          <div className={styles.profileBackdrop}>
            <div
              role="dialog"
              aria-modal="true"
              aria-label="プロフィール"
              className={styles.profileDialog}
            >
              <label className={styles.profileField}>
                <span>表示名</span>
                <input
                  type="text"
                  aria-label="表示名"
                  value={profileDisplayName}
                  onChange={(e) => setProfileDisplayName(e.target.value)}
                />
              </label>
              <div className={styles.emojiOptions}>
                {PARTICIPANT_EMOJIS.map((emoji) => (
                  <button
                    key={emoji}
                    type="button"
                    disabled={takenByOthers.has(emoji)}
                    onClick={() => void selectEmoji(emoji)}
                  >
                    {emoji}
                  </button>
                ))}
              </div>
              <button
                type="button"
                className={styles.profileSave}
                onClick={() => void saveDisplayName()}
              >
                保存
              </button>
            </div>
          </div>
        ) : null}

        {error ? (
          <p role="alert" className={styles.error}>
            {error}
          </p>
        ) : null}
      </main>
    </div>
  );
}
