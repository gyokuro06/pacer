import { NextResponse } from "next/server";
import { joinRoom, newParticipantId } from "@/lib/room-store";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

export async function POST(request: Request, { params }: Params) {
  const { code } = await params;
  const body = (await request.json()) as { displayName?: string };
  const displayName =
    typeof body.displayName === "string" ? body.displayName.trim() : "";
  if (!displayName) {
    return NextResponse.json({ error: "表示名は必須です" }, { status: 400 });
  }
  try {
    const { room, participantId } = joinRoom(code, {
      id: newParticipantId(),
      displayName,
    });
    return NextResponse.json({ room, participantId });
  } catch (error) {
    const message = error instanceof Error ? error.message : "参加に失敗しました";
    const status = message.includes("見つかりません") ? 404 : 400;
    return NextResponse.json({ error: message }, { status });
  }
}
