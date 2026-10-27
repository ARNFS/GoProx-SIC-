export interface AgoraCallData {
    callId?: unknown;
    calleeId?: unknown;
    callerId?: unknown;
    channelName?: unknown;
    status?: unknown;
}

export function getAgoraUid(firebaseUid: string): number {
    let hash = 0;

    for (let index = 0; index < firebaseUid.length; index += 1) {
        hash = (Math.imul(hash, 31) + firebaseUid.charCodeAt(index)) | 0;
    }

    return Math.abs(hash % 100000) + 1;
}

export function isAuthorizedAcceptedCall(
    authenticatedUid: string,
    requestedChannel: string,
    call: AgoraCallData | undefined
): boolean {
    if (!call || call.status !== "accepted") {
        return false;
    }

    const callerId = call.callerId;
    const calleeId = call.calleeId;

    if (
        typeof callerId !== "string" ||
        typeof calleeId !== "string" ||
        !callerId ||
        !calleeId ||
        callerId === calleeId ||
        (authenticatedUid !== callerId && authenticatedUid !== calleeId)
    ) {
        return false;
    }

    const expectedChannel = [callerId, calleeId].sort().join("_");

    return call.channelName === requestedChannel &&
        call.callId === requestedChannel &&
        requestedChannel === expectedChannel;
}
