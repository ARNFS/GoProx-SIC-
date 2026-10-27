const test = require("node:test");
const assert = require("node:assert/strict");
const {
    getAgoraUid,
    isAuthorizedAcceptedCall,
} = require("../lib/agoraAuthorization.js");
const { RtcRole, RtcTokenBuilder } = require("agora-token");

const acceptedCall = {
    callId: "callee_uid_caller_uid",
    callerId: "caller_uid",
    calleeId: "callee_uid",
    channelName: "callee_uid_caller_uid",
    status: "accepted",
};

test("accepts each authenticated participant for the exact accepted channel", () => {
    assert.equal(
        isAuthorizedAcceptedCall("caller_uid", "callee_uid_caller_uid", acceptedCall),
        true
    );
    assert.equal(
        isAuthorizedAcceptedCall("callee_uid", "callee_uid_caller_uid", acceptedCall),
        true
    );
});

test("rejects unauthenticated identities and non-participants", () => {
    assert.equal(
        isAuthorizedAcceptedCall("other_uid", "callee_uid_caller_uid", acceptedCall),
        false
    );
});

test("rejects ringing calls and channel substitutions", () => {
    assert.equal(
        isAuthorizedAcceptedCall(
            "caller_uid",
            "callee_uid_caller_uid",
            { ...acceptedCall, status: "ringing" }
        ),
        false
    );
    assert.equal(
        isAuthorizedAcceptedCall("caller_uid", "attacker_channel", acceptedCall),
        false
    );
});

test("rejects inconsistent document fields and self-calls", () => {
    assert.equal(
        isAuthorizedAcceptedCall(
            "caller_uid",
            "callee_uid_caller_uid",
            { ...acceptedCall, callId: "different_channel" }
        ),
        false
    );
    assert.equal(
        isAuthorizedAcceptedCall(
            "caller_uid",
            "caller_uid_caller_uid",
            {
                ...acceptedCall,
                calleeId: "caller_uid",
                callId: "caller_uid_caller_uid",
                channelName: "caller_uid_caller_uid",
            }
        ),
        false
    );
});

test("keeps Agora UID mapping compatible and safe for Integer.MIN_VALUE", () => {
    assert.equal(getAgoraUid("abc"), 96355);
    assert.equal(getAgoraUid("polygenelubricants"), 83649);
    assert.ok(getAgoraUid("user-1") >= 1);
    assert.ok(getAgoraUid("user-1") <= 100000);
});

test("Agora token builder issues a channel and UID-bound publisher token", () => {
    const token = RtcTokenBuilder.buildTokenWithUid(
        "00000000000000000000000000000000",
        "11111111111111111111111111111111",
        "callee_uid_caller_uid",
        getAgoraUid("caller_uid"),
        RtcRole.PUBLISHER,
        60,
        60
    );

    assert.equal(typeof token, "string");
    assert.ok(token.length > 50);
});
