import { NextResponse } from "next/server";
import {
  isPhaseConflictError,
  requireRoomMember,
  type ProposalKind,
} from "@/lib/room";
import { getRoom, propose } from "@/lib/room-store";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

function errorStatus(error: unknown, message: string): number {
  if (isPhaseConflictError(error)) return 409;
  if (message.includes("参加者ID")) return 400;
  if (message.includes("参加者") && message.includes("見つかりません")) return 403;
  if (message.includes("見つかりません")) return 404;
  return 400;
}

export async function POST(request: Request, { params }: Params) {
  const { code } = await params;
  const body = (await request.json().catch(() => ({}))) as {
    kind?: ProposalKind;
    participantId?: string;
  };
  const kind = body.kind ?? "break";
  try {
    const existing = await getRoom(code);
    if (!existing) {
      return NextResponse.json({ error: "ルームが見つかりません" }, { status: 404 });
    }
    requireRoomMember(existing, body.participantId);
    const room = await propose(code, kind);
    return NextResponse.json({ room });
  } catch (error) {
    const message = error instanceof Error ? error.message : "提案に失敗しました";
    return NextResponse.json({ error: message }, { status: errorStatus(error, message) });
  }
}
