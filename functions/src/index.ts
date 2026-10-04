import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

admin.initializeApp();

const db = admin.firestore();
const realtimeDb = admin.database();

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

        const targetUserDoc = await db
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

        /*
         * =====================================================
         * RTDB CHAT MEMBERSHIP
         * =====================================================
         *
         * The client is NOT allowed to write participants.
         * This trusted Cloud Function creates both participants.
         */
        const participantsRef = realtimeDb
            .ref(`chats/${chatId}/participants`);

        await participantsRef.update({
            [callerUid]: true,
            [targetUid]: true,
        });

        /*
         * =====================================================
         * FIRESTORE CHAT ACCESS CONTROL
         * =====================================================
         *
         * Cloud Storage Security Rules cannot use RTDB
         * membership directly.
         *
         * Therefore Firestore becomes the authoritative access
         * source for chat attachment authorization.
         *
         * Client users cannot write these documents because
         * firestore.rules explicitly denies client access.
         */
        const chatAccessRef = db
            .collection("chatAccess")
            .doc(chatId);

        const batch = db.batch();

        batch.set(
            chatAccessRef.collection("participants").doc(callerUid),
            {
                uid: callerUid,
                chatId: chatId,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
            },
            {
                merge: true,
            }
        );

        batch.set(
            chatAccessRef.collection("participants").doc(targetUid),
            {
                uid: targetUid,
                chatId: chatId,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
            },
            {
                merge: true,
            }
        );

        await batch.commit();

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

        const deviceDoc = await db
            .collection("users")
            .doc(calleeId)
            .collection("private")
            .doc("device")
            .get();

        if (!deviceDoc.exists) {
            return;
        }

        const fcmToken: string | undefined =
            deviceDoc.data()?.fcmToken;

        if (!fcmToken) {
            console.log(
                "No FCM token for user:",
                calleeId
            );
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