import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
  ROOM_POLL_MS_IDLE,
  ROOM_POLL_MS_PENDING,
  roomPollIntervalMs,
} from "./room-poll";

describe("roomPollIntervalMs", () => {
  it("uses 3s when no pending proposal", () => {
    assert.equal(roomPollIntervalMs(null), ROOM_POLL_MS_IDLE);
    assert.equal(ROOM_POLL_MS_IDLE, 3000);
  });

  it("uses 500ms when a proposal is pending", () => {
    assert.equal(roomPollIntervalMs("break"), ROOM_POLL_MS_PENDING);
    assert.equal(roomPollIntervalMs("work"), ROOM_POLL_MS_PENDING);
    assert.equal(ROOM_POLL_MS_PENDING, 500);
  });
});
