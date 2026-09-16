import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { resolveRoomStoreKind } from "./room-durable";

describe("resolveRoomStoreKind", () => {
  it("defaults to file when unset", () => {
    assert.equal(resolveRoomStoreKind({}), "file");
  });

  it("uses postgres when DATABASE_URL is set", () => {
    assert.equal(
      resolveRoomStoreKind({ DATABASE_URL: "postgres://example" }),
      "postgres",
    );
  });

  it("honors explicit PACER_ROOM_STORE over DATABASE_URL", () => {
    assert.equal(
      resolveRoomStoreKind({
        PACER_ROOM_STORE: "file",
        DATABASE_URL: "postgres://example",
      }),
      "file",
    );
  });

  it("accepts memory", () => {
    assert.equal(resolveRoomStoreKind({ PACER_ROOM_STORE: "memory" }), "memory");
  });
});
