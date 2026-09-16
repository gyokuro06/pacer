import { NextResponse } from "next/server";
import {
  DEFAULT_BREAK_MINUTES,
  DEFAULT_WORK_MINUTES,
  generateAutoDisplayName,
  generateAutoEmoji,
} from "@/lib/room";
import { createRoom, newParticipantId } from "@/lib/room-store";

export const runtime = "nodejs";

export async function POST(request: Request) {
  const body = (await request.json().catch(() => ({}))) as {
    workMinutes?: number;
    breakMinutes?: number;
    displayName?: string;
  };
  const workMinutes =
    body.workMinutes === undefined
      ? DEFAULT_WORK_MINUTES
      : Number(body.workMinutes);
  const breakMinutes =
    body.breakMinutes === undefined
      ? DEFAULT_BREAK_MINUTES
      : Number(body.breakMinutes);
  if (!Number.isFinite(workMinutes) || workMinutes <= 0) {
    return NextResponse.json({ error: "作業時間が不正です" }, { status: 400 });
  }
  if (!Number.isFinite(breakMinutes) || breakMinutes <= 0) {
    return NextResponse.json({ error: "休憩時間が不正です" }, { status: 400 });
  }
  const displayName =
    typeof body.displayName === "string" && body.displayName.trim()
      ? body.displayName.trim()
      : generateAutoDisplayName();
  const emoji = generateAutoEmoji();
  const participantId = newParticipantId();
  const room = await createRoom(workMinutes, breakMinutes, {
    id: participantId,
    displayName,
    emoji,
  });
  return NextResponse.json({ room, participantId });
}
