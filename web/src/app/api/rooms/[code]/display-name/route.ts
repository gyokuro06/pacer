import { NextResponse } from "next/server";
import { changeParticipantDisplayName } from "@/lib/room-store";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

export async function POST(request: Request, { params }: Params) {
  const { code } = await params;
  const body = (await request.json()) as {
    participantId?: string;
    displayName?: string;
  };
  const participantId =
    typeof body.participantId === "string" ? body.participantId.trim() : "";
  const displayName =
    typeof body.displayName === "string" ? body.displayName.trim() : "";
  if (!participantId) {
    return NextResponse.json({ error: "参加者が不正です" }, { status: 400 });
  }
  if (!displayName) {
    return NextResponse.json({ error: "表示名は必須です" }, { status: 400 });
  }
  try {
    const room = await changeParticipantDisplayName(
      code,
      participantId,
      displayName,
    );
    return NextResponse.json({ room });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "表示名の変更に失敗しました";
    const status = message.includes("見つかりません") ? 404 : 400;
    return NextResponse.json({ error: message }, { status });
  }
}
