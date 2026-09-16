import { NextResponse } from "next/server";
import {
  getRoom,
  updateDisplayName,
  updateEmoji,
  updateRoomMinutes,
} from "@/lib/room-store";
import { requireRoomMember } from "@/lib/room";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

function errorStatus(message: string): number {
  if (message.includes("参加者ID")) return 400;
  if (message.includes("参加者") && message.includes("見つかりません")) return 403;
  if (message.includes("見つかりません")) return 404;
  return 400;
}

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
    emoji?: string;
  };

  try {
    let room = await getRoom(code);
    if (!room) {
      return NextResponse.json({ error: "ルームが見つかりません" }, { status: 404 });
    }
    const participantId = requireRoomMember(room, body.participantId);

    if (body.workMinutes !== undefined || body.breakMinutes !== undefined) {
      const workMinutes = Number(body.workMinutes ?? room.workMinutes);
      const breakMinutes = Number(body.breakMinutes ?? room.breakMinutes);
      room = await updateRoomMinutes(code, workMinutes, breakMinutes);
    }
    if (typeof body.displayName === "string") {
      room = await updateDisplayName(code, participantId, body.displayName);
    }
    if (typeof body.emoji === "string") {
      room = await updateEmoji(code, participantId, body.emoji);
    }
    return NextResponse.json({ room });
  } catch (error) {
    const message = error instanceof Error ? error.message : "更新に失敗しました";
    return NextResponse.json({ error: message }, { status: errorStatus(message) });
  }
}
