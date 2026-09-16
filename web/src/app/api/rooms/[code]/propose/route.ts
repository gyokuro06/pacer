import { NextResponse } from "next/server";
import { propose } from "@/lib/room-store";
import type { ProposalKind } from "@/lib/room";

export const runtime = "nodejs";

type Params = { params: Promise<{ code: string }> };

export async function POST(request: Request, { params }: Params) {
  const { code } = await params;
  const body = (await request.json()) as { kind?: ProposalKind };
  const kind = body.kind ?? "break";
  try {
    const room = propose(code, kind);
    return NextResponse.json({ room });
  } catch (error) {
    const message = error instanceof Error ? error.message : "提案に失敗しました";
    const status = message.includes("見つかりません") ? 404 : 400;
    return NextResponse.json({ error: message }, { status });
  }
}
