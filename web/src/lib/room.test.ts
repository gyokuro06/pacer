import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
  changeParticipantDisplayNameState,
  changeParticipantEmojiState,
  confirmProposalState,
  createRoomState,
  formatRemainingMs,
  isRoomExpired,
  joinRoomState,
  MAX_PARTICIPANTS,
  PARTICIPANT_EMOJIS,
  pickUnusedEmoji,
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

describe("pickUnusedEmoji", () => {
  it("picks the first emoji when none used", () => {
    assert.equal(pickUnusedEmoji([]), PARTICIPANT_EMOJIS[0]);
  });

  it("skips used emojis", () => {
    assert.equal(
      pickUnusedEmoji([PARTICIPANT_EMOJIS[0], PARTICIPANT_EMOJIS[1]]),
      PARTICIPANT_EMOJIS[2],
    );
  });

  it("throws when the pool is exhausted", () => {
    assert.throws(
      () => pickUnusedEmoji([...PARTICIPANT_EMOJIS]),
      /利用可能な絵文字がありません/,
    );
  });
});

describe("emoji auto-assign on create and join", () => {
  it("assigns distinct unused emojis to new participants", () => {
    const room = createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF");
    assert.equal(room.participants[0]?.emoji, PARTICIPANT_EMOJIS[0]);

    const withBob = joinRoomState(room, { id: "b", displayName: "Bob" }).room;
    assert.equal(withBob.participants[1]?.emoji, PARTICIPANT_EMOJIS[1]);
    assert.notEqual(
      withBob.participants[0]?.emoji,
      withBob.participants[1]?.emoji,
    );
  });
});

describe("changeParticipantEmojiState", () => {
  it("changes own emoji to an unused one", () => {
    const room = joinRoomState(
      createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF"),
      { id: "b", displayName: "Bob" },
    ).room;
    const next = PARTICIPANT_EMOJIS[2];
    const updated = changeParticipantEmojiState(room, "a", next);
    assert.equal(updated.participants[0]?.emoji, next);
    assert.equal(updated.participants[1]?.emoji, PARTICIPANT_EMOJIS[1]);
  });

  it("allows keeping the current emoji", () => {
    const room = createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF");
    const updated = changeParticipantEmojiState(
      room,
      "a",
      PARTICIPANT_EMOJIS[0],
    );
    assert.equal(updated.participants[0]?.emoji, PARTICIPANT_EMOJIS[0]);
  });

  it("rejects emoji taken by another participant", () => {
    const room = joinRoomState(
      createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF"),
      { id: "b", displayName: "Bob" },
    ).room;
    assert.throws(
      () => changeParticipantEmojiState(room, "a", PARTICIPANT_EMOJIS[1]),
      /他の参加者が使用中/,
    );
  });

  it("rejects emoji outside the pool", () => {
    const room = createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF");
    assert.throws(
      () => changeParticipantEmojiState(room, "a", "🍕"),
      /選べません/,
    );
  });
});

describe("changeParticipantDisplayNameState", () => {
  it("renames self to an unused display name", () => {
    const room = joinRoomState(
      createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF"),
      { id: "b", displayName: "Bob" },
    ).room;
    const updated = changeParticipantDisplayNameState(room, "a", "Alicia");
    assert.equal(updated.participants[0]?.displayName, "Alicia");
    assert.equal(updated.participants[0]?.emoji, PARTICIPANT_EMOJIS[0]);
    assert.equal(updated.participants[1]?.displayName, "Bob");
  });

  it("allows keeping the current display name", () => {
    const room = createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF");
    const updated = changeParticipantDisplayNameState(room, "a", "Alice");
    assert.equal(updated.participants[0]?.displayName, "Alice");
  });

  it("rejects a display name taken by another participant", () => {
    const room = joinRoomState(
      createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF"),
      { id: "b", displayName: "Bob" },
    ).room;
    assert.throws(
      () => changeParticipantDisplayNameState(room, "a", "Bob"),
      /表示名は他の参加者が使用中/,
    );
  });

  it("rejects blank display names", () => {
    const room = createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF");
    assert.throws(
      () => changeParticipantDisplayNameState(room, "a", "   "),
      /表示名は必須/,
    );
  });

  it("rejoins by the new display name and not the old one", () => {
    const room = joinRoomState(
      createRoomState(25, 5, { id: "a", displayName: "Alice" }, "ABCDEF"),
      { id: "b", displayName: "Bob" },
    ).room;
    const renamed = changeParticipantDisplayNameState(room, "b", "Bobby");
    const asBobby = joinRoomState(renamed, {
      id: "new-bobby",
      displayName: "Bobby",
    });
    assert.equal(asBobby.participantId, "b");
    assert.equal(asBobby.room.participants.length, 2);

    const asOldBob = joinRoomState(renamed, {
      id: "new-bob",
      displayName: "Bob",
    });
    assert.equal(asOldBob.participantId, "new-bob");
    assert.equal(asOldBob.room.participants.length, 3);
    assert.equal(
      asOldBob.room.participants.find((p) => p.id === "b")?.displayName,
      "Bobby",
    );
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
