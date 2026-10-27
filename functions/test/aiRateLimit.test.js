const test = require("node:test");
const assert = require("node:assert/strict");
const { consumeAiQuota } = require("../lib/aiRateLimit.js");

test("AI quota allows five requests in a UTC minute and then blocks", () => {
    const now = Date.UTC(2026, 9, 27, 8, 15, 20);
    let state;
    for (let i = 0; i < 5; i++) {
        const decision = consumeAiQuota(state, now);
        assert.equal(decision.allowed, true);
        state = decision.state;
    }
    const blocked = consumeAiQuota(state, now);
    assert.equal(blocked.allowed, false);
    assert.equal(blocked.retryAfterSeconds, 40);
});

test("AI quota resets the minute while retaining the UTC daily count", () => {
    const first = consumeAiQuota(undefined, Date.UTC(2026, 9, 27, 8, 15, 59));
    const nextMinute = consumeAiQuota(first.state, Date.UTC(2026, 9, 27, 8, 16, 0));
    assert.equal(nextMinute.allowed, true);
    assert.equal(nextMinute.state.requestsInWindow, 1);
    assert.equal(nextMinute.state.requestsToday, 2);
});

test("AI quota enforces a UTC daily cap and resets on the next UTC date", () => {
    const now = Date.UTC(2026, 9, 27, 12, 0, 0);
    let state;
    for (let i = 0; i < 50; i++) {
        const decision = consumeAiQuota(state, now + i * 60_000);
        assert.equal(decision.allowed, true);
        state = decision.state;
    }
    const blocked = consumeAiQuota(state, now + 50 * 60_000);
    assert.equal(blocked.allowed, false);
    const nextDay = consumeAiQuota(state, Date.UTC(2026, 9, 28, 0, 0, 1));
    assert.equal(nextDay.allowed, true);
    assert.equal(nextDay.state.requestsToday, 1);
});
