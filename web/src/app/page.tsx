"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import styles from "./page.module.css";

function storeParticipant(code: string, participantId: string) {
  sessionStorage.setItem(`pacer:${code}:participantId`, participantId);
}

export default function HomePage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const create = async () => {
      try {
        const response = await fetch("/api/rooms", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({}),
        });
        const data = await response.json();
        if (!response.ok) {
          throw new Error(data.error ?? "ルーム作成に失敗しました");
        }
        if (cancelled) return;
        storeParticipant(data.room.code, data.participantId);
        router.replace(`/room/${data.room.code}`);
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "ルーム作成に失敗しました");
        }
      }
    };
    void create();
    return () => {
      cancelled = true;
    };
  }, [router]);

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <h1 className={styles.brand}>pacer</h1>
        <p className={styles.tagline}>
          {error ?? "ルームを準備しています…"}
        </p>
      </main>
    </div>
  );
}
