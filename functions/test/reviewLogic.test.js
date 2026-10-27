const test = require("node:test");
const assert = require("node:assert/strict");
const { aggregateReviews, isEligibleReviewRequest, isValidReviewInput } = require("../lib/reviewLogic.js");

test("rating aggregates count one valid latest review per reviewer", () => {
    assert.deepEqual(aggregateReviews([
        { userId: "u1", requestId: "r1", rating: 2, timestamp: 100 },
        { userId: "u1", requestId: "r2", rating: 4.5, timestamp: 200 },
        { userId: "u2", requestId: "r3", rating: 5, timestamp: 100 },
        { userId: "bad", requestId: "r4", rating: 6, timestamp: 300 },
        { userId: "legacy", rating: 5, timestamp: 300 },
        { rating: 3, timestamp: 300 },
    ]), { ratingSum: 9.5, ratingCount: 2 });
});

test("review submission validates bounded identifiers, ratings, and comments", () => {
    assert.equal(isValidReviewInput("service-1", "request-1", 4.5, "Good"), true);
    assert.equal(isValidReviewInput("service-1", "request-1", 4.2, "Good"), false);
    assert.equal(isValidReviewInput("service/1", "request-1", 4, "Good"), false);
    assert.equal(isValidReviewInput("service-1", "request-1", 0, "Good"), false);
    assert.equal(isValidReviewInput("service-1", "request-1", 4, "x".repeat(1001)), false);
});

test("only the customer on a completed request for the same service is eligible", () => {
    const completed = { customerId: "customer-1", serviceId: "service-1", status: "completed" };
    assert.equal(isEligibleReviewRequest(completed, "customer-1", "service-1"), true);
    assert.equal(isEligibleReviewRequest({ ...completed, status: "in_progress" }, "customer-1", "service-1"), false);
    assert.equal(isEligibleReviewRequest(completed, "customer-2", "service-1"), false);
    assert.equal(isEligibleReviewRequest(completed, "customer-1", "service-2"), false);
});
