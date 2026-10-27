export interface ReviewForAggregation {
    userId?: unknown;
    rating?: unknown;
    timestamp?: unknown;
    requestId?: unknown;
}

export interface ReviewAggregate {
    ratingSum: number;
    ratingCount: number;
}

export function isEligibleReviewRequest(
    request: { customerId?: unknown; serviceId?: unknown; status?: unknown } | undefined,
    uid: string,
    serviceId: string
): boolean {
    return request?.customerId === uid && request.serviceId === serviceId &&
        request.status === "completed";
}

/** Aggregate only server-verified review evidence, keeping the latest score per reviewer. */
export function aggregateReviews(
    reviews: readonly ReviewForAggregation[]
): ReviewAggregate {
    const latestByReviewer = new Map<string, { rating: number; timestamp: number }>();

    for (const review of reviews) {
        if (typeof review.userId !== "string" || !review.userId) continue;
        if (typeof review.requestId !== "string" || !review.requestId) continue;
        if (typeof review.rating !== "number" || !Number.isFinite(review.rating) ||
            review.rating < 1 || review.rating > 5) continue;

        const timestamp = typeof review.timestamp === "number" &&
            Number.isFinite(review.timestamp) ? review.timestamp : 0;
        const previous = latestByReviewer.get(review.userId);
        if (!previous || timestamp >= previous.timestamp) {
            latestByReviewer.set(review.userId, { rating: review.rating, timestamp });
        }
    }

    let ratingSum = 0;
    for (const review of latestByReviewer.values()) ratingSum += review.rating;
    return { ratingSum, ratingCount: latestByReviewer.size };
}

export function isValidReviewInput(
    serviceId: unknown,
    requestId: unknown,
    rating: unknown,
    comment: unknown
): boolean {
    return typeof serviceId === "string" && serviceId.trim().length > 0 &&
        serviceId.length <= 150 && !serviceId.includes("/") &&
        typeof requestId === "string" && requestId.trim().length > 0 &&
        requestId.length <= 150 && !requestId.includes("/") &&
        typeof rating === "number" && Number.isFinite(rating) &&
        rating >= 1 && rating <= 5 && Number.isInteger(rating * 2) &&
        typeof comment === "string" && comment.length <= 1000;
}
