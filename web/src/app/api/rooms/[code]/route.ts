import { NextResponse } from "next/server";
import { getRoom } from "@/lib/room-store";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

export async function GET(_request: Request, { params }: Params) {
  const { code } = await params;
  const room = getRoom(code);
  if (!room) {
    return NextResponse.json({ error: "ルームが見つかりません" }, { status: 404 });
  }
  return NextResponse.json({ room });
}
