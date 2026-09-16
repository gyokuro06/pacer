import { NextResponse } from "next/server";
import {
  getRoom,
  updateDisplayName,
  updateRoomMinutes,
} from "@/lib/room-store";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

export async function GET(_request: Request, { params }: Params) {
  const { code } = await params;
  const room = await getRoom(code);
  if (!room) {
    return NextResponse.json({ error: "ルームが見つかりません" }, { status: 404 });
  }
  return NextResponse.json({ room });
}

export async function PATCH(request: Request, { params }: Params) {
  const { code } = await params;
  const body = (await request.json()) as {
    participantId?: string;
    workMinutes?: number;
    breakMinutes?: number;
    displayName?: string;
  };
  const participantId =
    typeof body.participantId === "string" ? body.participantId.trim() : "";
  if (!participantId) {
    return NextResponse.json({ error: "参加者IDは必須です" }, { status: 400 });
  }

  try {
    let room = await getRoom(code);
    if (!room) {
      return NextResponse.json({ error: "ルームが見つかりません" }, { status: 404 });
    }
    if (!room.participants.some((p) => p.id === participantId)) {
      return NextResponse.json({ error: "参加者が見つかりません" }, { status: 403 });
    }

    if (body.workMinutes !== undefined || body.breakMinutes !== undefined) {
      const workMinutes = Number(body.workMinutes ?? room.workMinutes);
      const breakMinutes = Number(body.breakMinutes ?? room.breakMinutes);
      room = await updateRoomMinutes(code, workMinutes, breakMinutes);
    }
    if (typeof body.displayName === "string") {
      room = await updateDisplayName(code, participantId, body.displayName);
    }
    return NextResponse.json({ room });
  } catch (error) {
    const message = error instanceof Error ? error.message : "更新に失敗しました";
    const status = message.includes("見つかりません") ? 404 : 400;
    return NextResponse.json({ error: message }, { status });
  }
}
