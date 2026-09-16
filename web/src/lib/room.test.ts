import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
  confirmProposalState,
  createRoomState,
  formatRemainingMs,
  FUNNY_NICKNAMES,
  generateFunnyNickname,
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

describe("generateFunnyNickname", () => {
  it("picks from the funny nickname list", () => {
    const name = generateFunnyNickname(() => 0);
    assert.equal(name, FUNNY_NICKNAMES[0]);
    assert.ok(FUNNY_NICKNAMES.includes(name as (typeof FUNNY_NICKNAMES)[number]));
  });

  it("never returns empty", () => {
    for (let i = 0; i < FUNNY_NICKNAMES.length; i += 1) {
      const name = generateFunnyNickname(() => i / FUNNY_NICKNAMES.length);
      assert.ok(name.length > 0);
    }
  });
});

describe("requireRoomMember", () => {
  const alice = { id: "a", displayName: "Alice" };

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
  const alice = { id: "a", displayName: "Alice" };
  const bob = { id: "b", displayName: "Bob" };

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
