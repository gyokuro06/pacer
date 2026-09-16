import { NextResponse } from "next/server";
import { createRoom, newParticipantId } from "@/lib/room-store";

export const runtime = "nodejs";

export async function POST(request: Request) {
  const body = (await request.json()) as {
    workMinutes?: number;
    breakMinutes?: number;
    displayName?: string;
  };
  const workMinutes = Number(body.workMinutes);
  const breakMinutes = Number(body.breakMinutes);
  if (!Number.isFinite(workMinutes) || workMinutes <= 0) {
    return NextResponse.json({ error: "作業時間が不正です" }, { status: 400 });
  }
  if (!Number.isFinite(breakMinutes) || breakMinutes <= 0) {
    return NextResponse.json({ error: "休憩時間が不正です" }, { status: 400 });
  }
  const displayName =
    typeof body.displayName === "string" && body.displayName.trim()
      ? body.displayName.trim()
      : "ホスト";
  const participantId = newParticipantId();
  const room = createRoom(workMinutes, breakMinutes, {
    id: participantId,
    displayName,
  });
  return NextResponse.json({ room, participantId });
}
