"use client";

import { FormEvent, useState } from "react";
import styles from "./page.module.css";

function storeParticipant(code: string, participantId: string) {
  sessionStorage.setItem(`pacer:${code}:participantId`, participantId);
}

export default function HomePage() {
  const [workMinutes, setWorkMinutes] = useState("25");
  const [breakMinutes, setBreakMinutes] = useState("5");
  const [createDisplayName, setCreateDisplayName] = useState("");
  const [roomCode, setRoomCode] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onCreate(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const response = await fetch("/api/rooms", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          workMinutes: Number(workMinutes),
          breakMinutes: Number(breakMinutes),
          displayName: createDisplayName,
        }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error ?? "ルーム作成に失敗しました");
      }
      storeParticipant(data.room.code, data.participantId);
      window.location.assign(`/room/${data.room.code}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "ルーム作成に失敗しました");
      setBusy(false);
    }
  }

  async function onJoin(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const code = roomCode.trim().toUpperCase();
    try {
      const response = await fetch(`/api/rooms/${encodeURIComponent(code)}/join`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ displayName }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error ?? "参加に失敗しました");
      }
      storeParticipant(data.room.code, data.participantId);
      window.location.assign(`/room/${data.room.code}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "参加に失敗しました");
      setBusy(false);
    }
  }

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <h1 className={styles.brand}>pacer</h1>
        <p className={styles.tagline}>共有の作業と休憩リズム</p>

        <form className={styles.panel} onSubmit={onCreate}>
          <h2>ルームを作成</h2>
          <label className={styles.field}>
            <span>表示名</span>
            <input
              type="text"
              aria-label="表示名"
              value={createDisplayName}
              onChange={(e) => setCreateDisplayName(e.target.value)}
              required
            />
          </label>
          <label className={styles.field}>
            <span>作業（分）</span>
            <input
              type="number"
              min={1}
              aria-label="作業（分）"
              value={workMinutes}
              onChange={(e) => setWorkMinutes(e.target.value)}
              required
            />
          </label>
          <label className={styles.field}>
            <span>休憩（分）</span>
            <input
              type="number"
              min={1}
              aria-label="休憩（分）"
              value={breakMinutes}
              onChange={(e) => setBreakMinutes(e.target.value)}
              required
            />
          </label>
          <button type="submit" disabled={busy}>
            ルームを作成
          </button>
        </form>

        <form className={styles.panel} onSubmit={onJoin}>
          <h2>ルームに参加</h2>
          <label className={styles.field}>
            <span>ルームコード</span>
            <input
              type="text"
              aria-label="ルームコード"
              value={roomCode}
              onChange={(e) => setRoomCode(e.target.value)}
              autoComplete="off"
              required
            />
          </label>
          <label className={styles.field}>
            <span>表示名</span>
            <input
              type="text"
              aria-label="表示名"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
            />
          </label>
          <button type="submit" disabled={busy}>
            参加
          </button>
        </form>

        {error ? <p className={styles.error}>{error}</p> : null}
      </main>
    </div>
  );
}
