import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
  confirmProposalState,
  createRoomState,
  formatRemainingMs,
  AUTO_DISPLAY_NAMES,
  generateAutoDisplayName,
  isRoomExpired,
  joinRoomState,
  MAX_PARTICIPANTS,
  proposeState,
  requireRoomMember,
  SESSION_REJOIN_TTL_MS,
  startSessionState,
  updateRoomMinutesState,
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

describe("generateAutoDisplayName", () => {
  it("picks from the auto display name list", () => {
    const name = generateAutoDisplayName(() => 0);
    assert.equal(name, AUTO_DISPLAY_NAMES[0]);
    assert.ok(AUTO_DISPLAY_NAMES.includes(name as (typeof AUTO_DISPLAY_NAMES)[number]));
  });

  it("never returns empty", () => {
    for (let i = 0; i < AUTO_DISPLAY_NAMES.length; i += 1) {
      const name = generateAutoDisplayName(() => i / AUTO_DISPLAY_NAMES.length);
      assert.ok(name.length > 0);
    }
  });
});

describe("requireRoomMember", () => {
  const alice = { id: "a", displayName: "Alice", emoji: "😀" };

  it("returns participant id when member", () => {
    const room = createRoomState(60, 10, alice, "ABCDEF");
    assert.equal(requireRoomMember(room, "a"), "a");
  });

  it("rejects missing participant id", () => {
    const room = createRoomState(60, 10, alice, "ABCDEF");
    assert.throws(() => requireRoomMember(room, ""), /参加者IDは必須/);
    assert.throws(() => requireRoomMember(room, null), /参加者IDは必須/);
  });

  it("rejects non-member", () => {
    const room = createRoomState(60, 10, alice, "ABCDEF");
    assert.throws(() => requireRoomMember(room, "outsider"), /参加者が見つかりません/);
  });
});

describe("session flow", () => {
  const alice = { id: "a", displayName: "Alice", emoji: "😀" };
  const bob = { id: "b", displayName: "Bob", emoji: "😃" };

  it("allows solo start with one participant", () => {
    const room = createRoomState(60, 10, alice, "ABCDEF");
    const now = 1_000_000;
    const work = startSessionState(room, now);
    assert.equal(work.phase, "work");
    assert.equal(work.phaseEndsAt, now + 60 * 60_000);
  });

  it("rejects start with zero participants", () => {
    const room = {
      ...createRoomState(25, 5, alice, "ABCDEF"),
      participants: [],
    };
    assert.throws(() => startSessionState(room), /開始条件/);
  });

  it("updates minutes only while waiting", () => {
    const waiting = createRoomState(60, 10, alice, "ABCDEF");
    const updated = updateRoomMinutesState(waiting, 25, 5);
    assert.equal(updated.workMinutes, 25);
    assert.equal(updated.breakMinutes, 5);

    const work = startSessionState(updated, 1_000_000);
    assert.throws(() => updateRoomMinutesState(work, 30, 5), /待機中のみ/);
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
  const alice = { id: "a", displayName: "Alice", emoji: "😀" };
  const bob = { id: "b", displayName: "Bob", emoji: "😃" };
  const carol = { id: "c", displayName: "Carol", emoji: "😄" };
  const dave = { id: "d", displayName: "Dave", emoji: "😁" };

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
