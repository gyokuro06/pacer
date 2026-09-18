import type { ProposalKind } from "./room";

export const ROOM_POLL_MS_IDLE = 3000;
export const ROOM_POLL_MS_PENDING = 500;

export function roomPollIntervalMs(
  pendingProposal: ProposalKind | null,
): number {
  void pendingProposal;
  return ROOM_POLL_MS_PENDING;
}
