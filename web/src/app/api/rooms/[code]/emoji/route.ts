import { NextResponse } from "next/server";
import { changeParticipantEmoji } from "@/lib/room-store";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

export async function POST(request: Request, { params }: Params) {
  const { code } = await params;
  const body = (await request.json()) as {
    participantId?: string;
    emoji?: string;
  };
  const participantId =
    typeof body.participantId === "string" ? body.participantId.trim() : "";
  const emoji = typeof body.emoji === "string" ? body.emoji.trim() : "";
  if (!participantId) {
    return NextResponse.json({ error: "参加者が不正です" }, { status: 400 });
  }
  if (!emoji) {
    return NextResponse.json({ error: "絵文字は必須です" }, { status: 400 });
  }
  try {
    const room = await changeParticipantEmoji(code, participantId, emoji);
    return NextResponse.json({ room });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "絵文字の変更に失敗しました";
    const status = message.includes("見つかりません") ? 404 : 400;
    return NextResponse.json({ error: message }, { status });
  }
}
