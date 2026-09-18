import type { Room } from "./room";

export function coalesceRoomByActivity(
  current: Room | null,
  incoming: Room,
): Room {
  if (current == null) return incoming;
  return incoming.lastActivityAt >= current.lastActivityAt ? incoming : current;
}
