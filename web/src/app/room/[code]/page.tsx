"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  canStart,
  formatRemainingMs,
  phaseLabel,
  remainingMs,
  type Room,
} from "@/lib/room";
import styles from "./page.module.css";

const POLL_MS = 500;

export default function RoomPage() {
  const params = useParams<{ code: string }>();
  const code = String(params.code ?? "").toUpperCase();
  const [room, setRoom] = useState<Room | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());

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
        if (!cancelled) setError(null);
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

  async function postAction(path: string, body?: unknown) {
    setError(null);
    const response = await fetch(path, {
      method: "POST",
      headers: body ? { "Content-Type": "application/json" } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
    const data = await response.json();
    if (!response.ok) {
      setError(data.error ?? "操作に失敗しました");
      return;
    }
    setRoom(data.room);
  }

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

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <h1 className={styles.brand}>pacer</h1>
        <div role="status" aria-label="ルームコード" className={styles.code}>
          {room.code}
        </div>

        {room.phase !== "waiting" ? (
          <div role="status" aria-label="フェーズ" className={styles.phase}>
            {phaseLabel(room.phase)}
          </div>
        ) : null}

        {showTimer ? (
          <div role="timer" aria-label="残り時間" className={styles.timer}>
            {formatRemainingMs(remaining)}
          </div>
        ) : null}

        <div className={styles.actions}>
          {room.phase === "waiting" ? (
            <button
              type="button"
              disabled={!canStart(room)}
              onClick={() =>
                void postAction(`/api/rooms/${encodeURIComponent(code)}/start`)
              }
            >
              スタート
            </button>
          ) : null}

          {room.phase === "work" && room.pendingProposal == null ? (
            <button
              type="button"
              onClick={() =>
                void postAction(`/api/rooms/${encodeURIComponent(code)}/propose`, {
                  kind: "break",
                })
              }
            >
              休憩を提案
            </button>
          ) : null}

          {room.phase === "break" && room.pendingProposal == null ? (
            <button
              type="button"
              onClick={() =>
                void postAction(`/api/rooms/${encodeURIComponent(code)}/propose`, {
                  kind: "work",
                })
              }
            >
              再開を提案
            </button>
          ) : null}

          {room.pendingProposal != null ? (
            <button
              type="button"
              onClick={() =>
                void postAction(`/api/rooms/${encodeURIComponent(code)}/confirm`)
              }
            >
              提案を確定
            </button>
          ) : null}
        </div>

        {error ? <p className={styles.error}>{error}</p> : null}
      </main>
    </div>
  );
}
