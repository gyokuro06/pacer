export type Phase = "waiting" | "work" | "break";

export type ProposalKind = "break" | "work";

export type Participant = {
  id: string;
  displayName: string;
};

export type Room = {
  code: string;
  workMinutes: number;
  breakMinutes: number;
  participants: Participant[];
  phase: Phase;
  phaseEndsAt: number | null;
  pendingProposal: ProposalKind | null;
};

export const MIN_PARTICIPANTS_TO_START = 2;
export const MAX_PARTICIPANTS = 4;

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

export function generateRoomCode(length = 6): string {
  let code = "";
  for (let i = 0; i < length; i += 1) {
    code += CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)];
  }
  return code;
}

export function formatRemainingMs(remainingMs: number): string {
  const clamped = Math.max(0, remainingMs);
  const totalSeconds = Math.ceil(clamped / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export function remainingMs(room: Room, now = Date.now()): number | null {
  if (room.phaseEndsAt == null) return null;
  return room.phaseEndsAt - now;
}

export function phaseLabel(phase: Phase): string {
  switch (phase) {
    case "work":
      return "作業";
    case "break":
      return "休憩";
    default:
      return "待機";
  }
}

export function canStart(room: Room): boolean {
  return (
    room.phase === "waiting" &&
    room.participants.length >= MIN_PARTICIPANTS_TO_START
  );
}

export function createRoomState(
  workMinutes: number,
  breakMinutes: number,
  creator: Participant,
  code = generateRoomCode(),
): Room {
  return {
    code,
    workMinutes,
    breakMinutes,
    participants: [creator],
    phase: "waiting",
    phaseEndsAt: null,
    pendingProposal: null,
  };
}

export function joinRoomState(
  room: Room,
  participant: Participant,
): Room {
  if (room.participants.some((p) => p.id === participant.id)) {
    return room;
  }
  if (room.participants.length >= MAX_PARTICIPANTS) {
    throw new Error("ルームは満員です");
  }
  return {
    ...room,
    participants: [...room.participants, participant],
  };
}

export function startSessionState(room: Room, now = Date.now()): Room {
  if (!canStart(room)) {
    throw new Error("開始条件を満たしていません");
  }
  return {
    ...room,
    phase: "work",
    phaseEndsAt: now + room.workMinutes * 60_000,
    pendingProposal: null,
  };
}

export function proposeState(room: Room, kind: ProposalKind): Room {
  if (room.phase === "waiting") {
    throw new Error("セッション開始前は提案できません");
  }
  if (kind === "break" && room.phase !== "work") {
    throw new Error("作業中のみ休憩を提案できます");
  }
  if (kind === "work" && room.phase !== "break") {
    throw new Error("休憩中のみ作業再開を提案できます");
  }
  return { ...room, pendingProposal: kind };
}

export function confirmProposalState(room: Room, now = Date.now()): Room {
  if (room.pendingProposal == null) {
    throw new Error("確定する提案がありません");
  }
  if (room.pendingProposal === "break") {
    return {
      ...room,
      phase: "break",
      phaseEndsAt: now + room.breakMinutes * 60_000,
      pendingProposal: null,
    };
  }
  return {
    ...room,
    phase: "work",
    phaseEndsAt: now + room.workMinutes * 60_000,
    pendingProposal: null,
  };
}
