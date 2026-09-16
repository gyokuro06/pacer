import { NextResponse } from "next/server";
import { startRoom } from "@/lib/room-store";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

export async function POST(_request: Request, { params }: Params) {
  const { code } = await params;
  try {
    const room = await startRoom(code);
    return NextResponse.json({ room });
  } catch (error) {
    const message = error instanceof Error ? error.message : "開始に失敗しました";
    const status = message.includes("見つかりません") ? 404 : 400;
    return NextResponse.json({ error: message }, { status });
  }
}
