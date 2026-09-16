import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
  confirmProposalState,
  createRoomState,
  formatRemainingMs,
  joinRoomState,
  proposeState,
  startSessionState,
} from "./room";

describe("formatRemainingMs", () => {
  it("formats whole minutes", () => {
    assert.equal(formatRemainingMs(25 * 60_000), "25:00");
  });

  it("clamps negative to zero", () => {
    assert.equal(formatRemainingMs(-1), "00:00");
  });

  it("ceils partial seconds", () => {
    assert.equal(formatRemainingMs(1500), "00:02");
  });
});

describe("session flow", () => {
  const alice = { id: "a", displayName: "Alice" };
  const bob = { id: "b", displayName: "Bob" };

  it("rejects start with fewer than two participants", () => {
    const room = createRoomState(25, 5, alice, "ABCDEF");
    assert.throws(() => startSessionState(room), /開始条件/);
  });

  it("starts work then confirms break proposal", () => {
    const waiting = joinRoomState(createRoomState(25, 5, alice, "ABCDEF"), bob);
    const now = 1_000_000;
    const work = startSessionState(waiting, now);
    assert.equal(work.phase, "work");
    assert.equal(work.phaseEndsAt, now + 25 * 60_000);

    const proposed = proposeState(work, "break");
    assert.equal(proposed.pendingProposal, "break");

    const onBreak = confirmProposalState(proposed, now + 1000);
    assert.equal(onBreak.phase, "break");
    assert.equal(onBreak.pendingProposal, null);
    assert.equal(onBreak.phaseEndsAt, now + 1000 + 5 * 60_000);
  });
});
