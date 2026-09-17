"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import styles from "./page.module.css";

const HOME_ROOM_KEY = "pacer:homeRoom";
const HOME_CREATE_INFLIGHT_KEY = "pacer:homeCreateInflight";

type HomeRoomCache = { code: string; participantId: string };

function storeParticipant(code: string, participantId: string) {
  sessionStorage.setItem(`pacer:${code}:participantId`, participantId);
}

function readHomeRoomCache(): HomeRoomCache | null {
  const raw = sessionStorage.getItem(HOME_ROOM_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as HomeRoomCache;
    if (
      typeof parsed?.code === "string" &&
      parsed.code &&
      typeof parsed?.participantId === "string" &&
      parsed.participantId
    ) {
      return parsed;
    }
  } catch {
    /* ignore corrupt cache */
  }
  return null;
}

function writeHomeRoomCache(cache: HomeRoomCache) {
  sessionStorage.setItem(HOME_ROOM_KEY, JSON.stringify(cache));
}

async function waitForHomeRoomCache(
  timeoutMs = 5000,
): Promise<HomeRoomCache | null> {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    const cached = readHomeRoomCache();
    if (cached) return cached;
    if (sessionStorage.getItem(HOME_CREATE_INFLIGHT_KEY) !== "1") {
      return null;
    }
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
  return readHomeRoomCache();
}

async function createHomeRoomOnce(): Promise<HomeRoomCache> {
  const cached = readHomeRoomCache();
  if (cached) return cached;

  if (sessionStorage.getItem(HOME_CREATE_INFLIGHT_KEY) === "1") {
    const waited = await waitForHomeRoomCache();
    if (waited) return waited;
  }

  sessionStorage.setItem(HOME_CREATE_INFLIGHT_KEY, "1");
  try {
    const again = readHomeRoomCache();
    if (again) return again;

    const response = await fetch("/api/rooms", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error ?? "ルーム作成に失敗しました");
    }
    const created: HomeRoomCache = {
      code: data.room.code,
      participantId: data.participantId,
    };
    writeHomeRoomCache(created);
    return created;
  } finally {
    sessionStorage.removeItem(HOME_CREATE_INFLIGHT_KEY);
  }
}

export default function HomePage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const go = async () => {
      try {
        const created = await createHomeRoomOnce();
        storeParticipant(created.code, created.participantId);
        if (!cancelled) {
          router.replace(`/room/${created.code}`);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "ルーム作成に失敗しました");
        }
      }
    };
    void go();
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
