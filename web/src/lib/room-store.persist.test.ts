import assert from "node:assert/strict";
import { afterEach, describe, it } from "node:test";
import { createRoom, getRoom } from "./room-store";

type GlobalRooms = typeof globalThis & {
  __pacerRooms?: Map<string, unknown>;
};

function dropProcessMemory(): void {
  delete (globalThis as GlobalRooms).__pacerRooms;
}

afterEach(() => {
  dropProcessMemory();
});

describe("room store persistence beyond process memory", () => {
  it("still finds a created room after in-process memory is cleared", async () => {
    const created = await createRoom(25, 5, { id: "a", displayName: "Alice" });

    dropProcessMemory();

    const found = await getRoom(created.code);
    assert.ok(
      found,
      "room must survive beyond a single Node process heap (file/sqlite/kv/db)",
    );
    assert.equal(found.code, created.code);
    assert.equal(found.workMinutes, 25);
    assert.equal(found.breakMinutes, 5);
    assert.equal(found.participants[0]?.displayName, "Alice");
  });
});
