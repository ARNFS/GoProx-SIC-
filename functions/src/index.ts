import * as admin from "firebase-admin";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

admin.initializeApp();

exports.sendCallNotification = onDocumentWritten(
    {
        document: "calls/{userId}",
        region: "europe-west1",
    },
    async (event) => {
        const data = event.data?.after?.data();
        if (!data) return;

        // Միայն "ringing" status-ի դեպքում ուղարկել
        if (data.status !== "ringing") return;

        const calleeId:     string = event.params.userId;
        const callId:       string = data.callId       || "";
        const callerId:     string = data.callerId      || "";
        const callerName:   string = data.callerName    || "Unknown";
        const callerPhoto:  string = data.callerPhotoUrl || "";
        const channelName:  string = data.channelName   || "";
        const serviceTitle: string = data.serviceTitle  || "";

        // Callee-ի FCM token-ը վերցնել
        const userDoc = await admin.firestore()
            .collection("users")
            .doc(calleeId)
            .get();

        if (!userDoc.exists) return;

        const fcmToken: string | undefined = userDoc.data()?.fcmToken;
        if (!fcmToken) {
            console.log("No FCM token for user:", calleeId);
            return;
        }

        const message: admin.messaging.Message = {
            token: fcmToken,
            data: {
                type:           "call",
                callId:         callId,
                callerId:       callerId,
                callerName:     callerName,
                callerPhotoUrl: callerPhoto,
                channelName:    channelName,
                serviceTitle:   serviceTitle,
            },
            android: {
                priority: "high",
            },
        };

        try {
            await admin.messaging().send(message);
            console.log("Call notification sent to:", calleeId);
        } catch (error) {
            console.error("Error sending notification:", error);
        }
    }
);