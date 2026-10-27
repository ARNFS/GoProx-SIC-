export interface AiQuotaState {
    windowStartMs?: unknown;
    requestsInWindow?: unknown;
    dayKey?: unknown;
    requestsToday?: unknown;
}

export interface AiQuotaDecision {
    allowed: boolean;
    state: {
        windowStartMs: number;
        requestsInWindow: number;
        dayKey: string;
        requestsToday: number;
    };
    retryAfterSeconds: number;
}

export const AI_MAX_REQUESTS_PER_MINUTE = 5;
export const AI_MAX_REQUESTS_PER_UTC_DAY = 50;
const WINDOW_MS = 60_000;

/** Fixed UTC minute/day quotas; caller must persist the returned state atomically. */
export function consumeAiQuota(
    previous: AiQuotaState | undefined,
    nowMs: number
): AiQuotaDecision {
    const safeNow = Number.isFinite(nowMs) && nowMs >= 0 ? nowMs : Date.now();
    const windowStartMs = Math.floor(safeNow / WINDOW_MS) * WINDOW_MS;
    const dayKey = new Date(safeNow).toISOString().slice(0, 10);

    const previousWindowStart = previous?.windowStartMs;
    const windowCount = previousWindowStart === windowStartMs &&
        Number.isSafeInteger(previous?.requestsInWindow) &&
        (previous?.requestsInWindow as number) >= 0
        ? previous?.requestsInWindow as number
        : 0;
    const requestsToday = previous?.dayKey === dayKey &&
        Number.isSafeInteger(previous?.requestsToday) &&
        (previous?.requestsToday as number) >= 0
        ? previous?.requestsToday as number
        : 0;

    const state = {
        windowStartMs,
        requestsInWindow: windowCount,
        dayKey,
        requestsToday,
    };

    if (requestsToday >= AI_MAX_REQUESTS_PER_UTC_DAY) {
        return { allowed: false, state, retryAfterSeconds: secondsUntilUtcDayEnds(safeNow) };
    }
    if (windowCount >= AI_MAX_REQUESTS_PER_MINUTE) {
        return { allowed: false, state, retryAfterSeconds: Math.ceil((windowStartMs + WINDOW_MS - safeNow) / 1000) };
    }

    return {
        allowed: true,
        state: {
            ...state,
            requestsInWindow: windowCount + 1,
            requestsToday: requestsToday + 1,
        },
        retryAfterSeconds: 0,
    };
}

function secondsUntilUtcDayEnds(nowMs: number): number {
    const nextDay = Date.UTC(
        new Date(nowMs).getUTCFullYear(),
        new Date(nowMs).getUTCMonth(),
        new Date(nowMs).getUTCDate() + 1
    );
    return Math.max(1, Math.ceil((nextDay - nowMs) / 1000));
}
