import assert from "node:assert/strict";
import { describe, it } from "node:test";
import type { Room } from "./room";
import { coalesceRoomByActivity } from "./room-snapshot";

function room(lastActivityAt: number, phase: Room["phase"] = "waiting"): Room {
  return {
    code: "ABC123",
    workMinutes: 15,
    breakMinutes: 10,
    participants: [],
    phase,
    phaseEndsAt: null,
    pendingProposal: null,
    lastActivityAt,
  };
}

describe("coalesceRoomByActivity", () => {
  it("applies incoming when current is null", () => {
    const incoming = room(100, "work");
    assert.equal(coalesceRoomByActivity(null, incoming), incoming);
  });

  it("discards older lastActivityAt", () => {
    const current = room(200, "work");
    const stale = room(100, "waiting");
    assert.equal(coalesceRoomByActivity(current, stale), current);
  });

  it("applies newer lastActivityAt", () => {
    const current = room(100, "waiting");
    const newer = room(200, "work");
    assert.equal(coalesceRoomByActivity(current, newer), newer);
  });

  it("applies incoming when lastActivityAt is equal", () => {
    const current = room(100, "waiting");
    const sameAge = room(100, "work");
    assert.equal(coalesceRoomByActivity(current, sameAge), sameAge);
  });
});
