import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
  confirmProposalState,
  createRoomState,
  formatRemainingMs,
  isRoomExpired,
  joinRoomState,
  MAX_PARTICIPANTS,
  proposeState,
  SESSION_REJOIN_TTL_MS,
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
    const waiting = joinRoomState(createRoomState(25, 5, alice, "ABCDEF"), bob)
      .room;
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

describe("display-name rejoin and TTL", () => {
  const now = 1_000_000;
  const alice = { id: "a", displayName: "Alice" };
  const bob = { id: "b", displayName: "Bob" };
  const carol = { id: "c", displayName: "Carol" };
  const dave = { id: "d", displayName: "Dave" };

  function fullRoom() {
    let room = createRoomState(25, 5, alice, "ABCDEF", now);
    room = joinRoomState(room, bob, now).room;
    room = joinRoomState(room, carol, now).room;
    room = joinRoomState(room, dave, now).room;
    assert.equal(room.participants.length, MAX_PARTICIPANTS);
    return room;
  }

  it("rejoins by display name without consuming a slot", () => {
    const room = fullRoom();
    const result = joinRoomState(
      room,
      { id: "new-bob", displayName: "Bob" },
      now + 60_000,
    );
    assert.equal(result.participantId, bob.id);
    assert.equal(result.room.participants.length, MAX_PARTICIPANTS);
    assert.equal(result.room.lastActivityAt, now + 60_000);
  });

  it("rejects join after TTL expires", () => {
    const room = createRoomState(25, 5, alice, "ABCDEF", now);
    const shortTtlMs = 1_000;
    assert.equal(isRoomExpired(room, now + shortTtlMs, shortTtlMs), true);
    assert.throws(
      () =>
        joinRoomState(
          room,
          { id: "b", displayName: "Bob" },
          now + shortTtlMs,
          shortTtlMs,
        ),
      /見つかりません/,
    );
  });

  it("allows join within short TTL", () => {
    const room = createRoomState(25, 5, alice, "ABCDEF", now);
    const shortTtlMs = 1_000;
    const result = joinRoomState(
      room,
      bob,
      now + shortTtlMs - 1,
      shortTtlMs,
    );
    assert.equal(result.participantId, bob.id);
    assert.equal(result.room.participants.length, 2);
  });

  it("uses SESSION_REJOIN_TTL_MS for default expiry boundary", () => {
    const room = createRoomState(25, 5, alice, "ABCDEF", now);
    assert.equal(isRoomExpired(room, now + SESSION_REJOIN_TTL_MS - 1), false);
    assert.equal(isRoomExpired(room, now + SESSION_REJOIN_TTL_MS), true);
  });
});
