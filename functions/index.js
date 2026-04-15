const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
const serviceAccount = require('./service-account-key.json');

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: 'myappproject-442cf',
});

const db = admin.firestore();

exports.sendCallNotification = onDocumentCreated('calls/{callId}', async (event) => {
    const snap = event.data;
    if (!snap) return;
    const callData = snap.data();
    const calleeId = callData.calleeId;
    const callerName = callData.callerName || 'User';

    const userDoc = await db.collection('users').doc(calleeId).get();
    const fcmToken = userDoc.data()?.fcmToken;
    if (!fcmToken) return;

    const payload = {
        token: fcmToken,
        data: {
            type: 'call',
            callId: event.params.callId,
            callerId: callData.callerId,
            callerName: callerName,
            isAudioOnly: String(callData.audioOnly || false),
        },
        android: { priority: 'high' },
    };

    try {
        await admin.messaging().send(payload);
        console.log('Notification sent to:', calleeId);
    } catch (error) {
        console.error('Error sending notification:', error);
    }
});