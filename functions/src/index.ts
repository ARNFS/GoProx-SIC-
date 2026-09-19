import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

admin.initializeApp();

export const ensureChatParticipant = onCall(
    {
        region: "europe-west1",
    },
    async (request) => {
        if (!request.auth) {
            throw new HttpsError(
                "unauthenticated",
                "Authentication is required."
            );
        }

        const callerUid = request.auth.uid;
        const otherUserId = request.data?.otherUserId;

        if (
            typeof otherUserId !== "string" ||
            otherUserId.trim().length === 0
        ) {
            throw new HttpsError(
                "invalid-argument",
                "otherUserId must be a non-empty string."
            );
        }

        const targetUid = otherUserId.trim();

        if (targetUid === callerUid) {
            throw new HttpsError(
                "invalid-argument",
                "You cannot create a chat with yourself."
            );
        }

        const targetUserDoc = await admin
            .firestore()
            .collection("users")
            .doc(targetUid)
            .get();

        if (!targetUserDoc.exists) {
            throw new HttpsError(
                "not-found",
                "Target user does not exist."
            );
        }

        const chatId =
            callerUid.localeCompare(targetUid) < 0
                ? `${callerUid}_${targetUid}`
                : `${targetUid}_${callerUid}`;

        const participantsRef = admin
            .database()
            .ref(`chats/${chatId}/participants`);

        await participantsRef.update({
            [callerUid]: true,
            [targetUid]: true,
        });

        return {
            success: true,
            chatId,
            callerUid,
            otherUserId: targetUid,
        };
    }
);

export const sendCallNotification = onDocumentWritten(
    {
        document: "calls/{userId}",
        region: "europe-west1",
    },
    async (event) => {
        const data = event.data?.after?.data();

        if (!data) {
            return;
        }

        if (data.status !== "ringing") {
            return;
        }

        const calleeId: string = event.params.userId;
        const callId: string = data.callId || "";
        const callerId: string = data.callerId || "";
        const callerName: string = data.callerName || "Unknown";
        const callerPhoto: string = data.callerPhotoUrl || "";
        const channelName: string = data.channelName || "";
        const serviceTitle: string = data.serviceTitle || "";

        const userDoc = await admin
            .firestore()
            .collection("users")
            .doc(calleeId)
            .get();

        if (!userDoc.exists) {
            return;
        }

        const fcmToken: string | undefined =
            userDoc.data()?.fcmToken;

        if (!fcmToken) {
            console.log("No FCM token for user:", calleeId);
            return;
        }

        const message: admin.messaging.Message = {
            token: fcmToken,
            data: {
                type: "call",
                callId,
                callerId,
                callerName,
                callerPhotoUrl: callerPhoto,
                channelName,
                serviceTitle,
            },
            android: {
                priority: "high",
            },
        };

        try {
            await admin.messaging().send(message);

            console.log(
                "Call notification sent to:",
                calleeId
            );
        } catch (error) {
            console.error(
                "Error sending notification:",
                error
            );
        }
    }
);