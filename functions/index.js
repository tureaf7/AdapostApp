const { onDocumentUpdated, onDocumentCreated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');

// Inițializează Firebase Admin o singură dată
admin.initializeApp();

// Definește db și messaging o singură dată
const db = getFirestore();
const messaging = getMessaging();

// Funcția pentru aprobare/respingere
exports.sendAdoptionNotification = onDocumentUpdated('AdoptionApplications/{applicationId}', (event) => {
    const newValue = event.data.after.data();
    const previousValue = event.data.before.data();

    if (newValue.status !== previousValue.status) {
        const userId = newValue.userId;
        const animalId = newValue.animalId;
        const applicationId = event.params.applicationId;

        return Promise.all([
            db.collection('users').doc(userId).get(),
            db.collection('Animals').doc(animalId).get()
        ]).then(([userDoc, animalDoc]) => {
            if (!userDoc.exists || !animalDoc.exists) {
                console.log('User or Animal document not found');
                return null;
            }

            const fcmToken = userDoc.data().fcmToken;
            const animalName = animalDoc.data().name;

            if (!fcmToken) {
                console.log('No FCM token for user:', userId);
                return null;
            }

            let payload;
            let notificationType;
            if (newValue.status === 'Aprobat') {
                payload = {
                    notification: {
                        title: 'Cerere aprobată!',
                        body: `Cererea ta pentru ${animalName} a fost aprobată!`,
                    },
                    data: {
                        click_action: 'APPROVAL_ACTIVITY',
                        animalName: animalName
                    },
                    token: fcmToken
                };
                notificationType = 'APPROVAL';
            } else if (newValue.status === 'Respins') {
                payload = {
                    notification: {
                        title: 'Cerere respinsă',
                        body: `Ne pare rău, cererea ta pentru ${animalName} a fost respinsă.`,
                    },
                    data: {
                        click_action: 'REJECTION_ACTIVITY',
                        animalName: animalName
                    },
                    token: fcmToken
                };
                notificationType = 'REJECTION';
            } else {
                return null;
            }

            // Salvează notificarea în colecția Notifications
            const notificationDoc = {
                userId: userId,
                title: payload.notification.title,
                body: payload.notification.body,
                timestamp: Timestamp.fromDate(new Date()),
                type: notificationType,
                applicationId: applicationId,
                viewed: false
            };

            return Promise.all([
                messaging.send(payload),
                db.collection('Notifications').add(notificationDoc)
            ]).then(([_, docRef]) => {
                console.log(`Notification sent to user ${userId} for status: ${newValue.status}, saved as ${docRef.id}`);
            }).catch(error => {
                console.error('Error sending notification or saving to Firestore:', error);
            });
        }).catch(error => {
            console.error('Error fetching documents:', error);
            return null;
        });
    }
    return null;
});

// Funcția pentru notificări la actualizarea cererilor de voluntariat
exports.sendVolunteerNotification = onDocumentUpdated('VolunteerRequests/{applicationId}', (event) => {
    const newValue = event.data.after.data();
    const previousValue = event.data.before.data();

    // Verifică dacă statusul s-a schimbat
    if (newValue.status !== previousValue.status) {
        const userId = newValue.userId;
        const applicationId = event.params.applicationId;

        // Preia datele utilizatorului
        return db.collection('users').doc(userId).get()
            .then((userDoc) => {
                if (!userDoc.exists) {
                    console.log('User document not found for userId:', userId);
                    return null;
                }

                const fcmToken = userDoc.data().fcmToken;
                const userName = userDoc.data().name || 'Utilizator';

                if (!fcmToken) {
                    console.log('No FCM token for user:', userId);
                    return null;
                }

                let payload;
                let notificationType;
                if (newValue.status === 'Aprobat') {
                    payload = {
                        notification: {
                            title: 'Cerere aprobată!',
                            body: `Cererea ta de voluntariat a fost aprobată, ${userName}!`,
                        },
                        data: {
                            click_action: 'VOLUNTEER_APPROVAL_ACTIVITY',
                            applicationId: applicationId
                        },
                        token: fcmToken
                    };
                    notificationType = 'VOLUNTEER_APPROVAL';
                } else if (newValue.status === 'Respins') {
                    payload = {
                        notification: {
                            title: 'Cerere respinsă',
                            body: `Ne pare rău, cererea ta de voluntariat a fost respinsă, ${userName}.`,
                        },
                        data: {
                            click_action: 'VOLUNTEER_REJECTION_ACTIVITY',
                            applicationId: applicationId
                        },
                        token: fcmToken
                    };
                    notificationType = 'VOLUNTEER_REJECTION';
                } else {
                    console.log('Status not relevant for notification:', newValue.status);
                    return null;
                }

                // Salvează notificarea în colecția Notifications
                const notificationDoc = {
                    userId: userId,
                    title: payload.notification.title,
                    body: payload.notification.body,
                    timestamp: Timestamp.fromDate(new Date()),
                    type: notificationType,
                    applicationId: applicationId,
                    viewed: false
                };

                return Promise.all([
                    messaging.send(payload),
                    db.collection('Notifications').add(notificationDoc)
                ])
                    .then(([_, docRef]) => {
                        console.log(`Notification sent to user ${userId} for status: ${newValue.status}, saved as ${docRef.id}`);
                    })
                    .catch(error => {
                        console.error('Error sending notification or saving to Firestore:', error);
                    });
            })
            .catch(error => {
                console.error('Error fetching user document:', error);
                return null;
            });
    }
    return null;
});

// Funcția pentru notificarea adminilor la crearea unei cereri
exports.notifyAdminsOnNewApplication = onDocumentCreated('AdoptionApplications/{applicationId}', (event) => {
    const application = event.data.data();
    const userId = application.userId;
    const animalId = application.animalId;
    const applicationId = event.params.applicationId;

    return Promise.all([
        db.collection('users').doc(userId).get(),
        db.collection('Animals').doc(animalId).get(),
        db.collection('users').where('role', '==', 'admin').get()
    ]).then(([userDoc, animalDoc, adminSnapshot]) => {
        if (!userDoc.exists || !animalDoc.exists) {
            console.log('User or Animal document not found');
            return null;
        }

        const userName = userDoc.data().name;
        const animalName = animalDoc.data().name;

        const adminNotifications = adminSnapshot.docs.map(adminDoc => {
            const adminFcmToken = adminDoc.data().fcmToken;
            const adminId = adminDoc.id;

            if (!adminFcmToken) {
                console.log(`No FCM token for admin: ${adminId}`);
                return Promise.resolve();
            }

            const payload = {
                notification: {
                    title: 'Cerere de adopție nouă!',
                    body: `${userName} a trimis o cerere de adopție pentru ${animalName}.`,
                },
                data: {
                    click_action: 'NEW_APPLICATION_ACTIVITY',
                    applicationId: applicationId
                },
                token: adminFcmToken
            };

            // Salvează notificarea în colecția Notifications pentru admin
            const notificationDoc = {
                userId: adminId,
                title: payload.notification.title,
                body: payload.notification.body,
                timestamp: Timestamp.fromDate(new Date()),
                type: 'NEW_APPLICATION',
                applicationId: applicationId,
                viewed: false
            };

            return Promise.all([
                messaging.send(payload),
                db.collection('Notifications').add(notificationDoc)
            ]).then(([_, docRef]) => {
                console.log(`Notification sent to admin ${adminId}, saved as ${docRef.id}`);
            }).catch(error => {
                console.error(`Error sending notification to admin ${adminId} or saving to Firestore:`, error);
            });
        });

        return Promise.all(adminNotifications);
    }).catch(error => {
        console.error('Error processing new application notification:', error);
        return null;
    });
});

// Funcția pentru notificarea de chat
exports.sendChatNotification = onDocumentCreated('Chats/{chatId}/Messages/{messageId}', async (event) => {
    try {
        // Preia datele mesajului
        const message = event.data.data();
        const chatId = event.params.chatId;

        // Verifică dacă mesajul conține câmpurile necesare
        if (!message || !message.senderId || !message.receiverId || !message.message) {
            console.error('Invalid message structure:', message);
            return null;
        }

        const senderId = message.senderId;
        const receiverId = message.receiverId;
        const messageText = message.message;

        // Verifică dacă chat-ul există și dacă participanții sunt valizi
        const chatDoc = await db.collection('Chats').doc(chatId).get();
        if (!chatDoc.exists) {
            console.error('Chat does not exist:', chatId);
            return null;
        }

        const chatData = chatDoc.data();
        if (!chatData.participants || !chatData.participants.includes(senderId) || !chatData.participants.includes(receiverId)) {
            console.error('Invalid participants in chat:', { chatId, senderId, receiverId, participants: chatData.participants });
            return null;
        }

        // Preia token-ul FCM al destinatarului și numele expeditorului
        const [userDoc, senderDoc] = await Promise.all([
            db.collection('users').doc(receiverId).get(),
            db.collection('users').doc(senderId).get()
        ]);

        if (!userDoc.exists) {
            console.log('Receiver not found:', receiverId);
            return null;
        }

        if (!senderDoc.exists) {
            console.log('Sender not found:', senderId);
            return null;
        }

        const fcmToken = userDoc.data().fcmToken;
        const senderName = senderDoc.data().name || 'Utilizator necunoscut';

        if (!fcmToken) {
            console.log('No FCM token for user:', receiverId);
            return null;
        }

        // Trunchiază mesajul dacă este prea lung
        const truncatedMessage = messageText.length > 50 ? messageText.substring(0, 47) + '...' : messageText;

        // Construiește payload-ul pentru notificarea FCM
        const payload = {
            data: {
                title: `Mesaj nou de la ${senderName}`,
                body: truncatedMessage,
                click_action: 'CHAT_ACTIVITY',
                chatId: chatId,
                otherUserId: senderId
            },
            token: fcmToken
        };

        try {
            // Trimite notificarea
            await messaging.send(payload);
            console.log(`Chat notification sent to user ${receiverId} with token ${fcmToken}`);
        } catch (messagingError) {
            if (messagingError.code === 'messaging/registration-token-not-registered') {
                console.log(`Invalid FCM token for user ${receiverId}: ${fcmToken}. Removing token.`);
                // Șterge token-ul invalid din Firestore
                await db.collection('users').doc(receiverId).update({
                    fcmToken: admin.firestore.FieldValue.delete()
                });
                console.log(`Removed invalid FCM token for user ${receiverId}`);
            } else {
                throw messagingError; // Aruncă alte erori pentru a fi gestionate de blocul catch principal
            }
        }

        return null;
    } catch (error) {
        console.error('Error in sendChatNotification:', error);
        return null;
    }
});

exports.notifyAdminsOnNewVolunteerRequest = onDocumentCreated('VolunteerRequests/{requestId}', (event) => {
    const request = event.data.data();
    const userId = request.userId;
    const requestId = event.params.requestId;

    // Preia datele utilizatorului și lista adminilor
    return Promise.all([
        db.collection('users').doc(userId).get(),
        db.collection('users').where('role', '==', 'admin').get()
    ])
        .then(([userDoc, adminSnapshot]) => {
            if (!userDoc.exists) {
                console.log('User document not found for userId:', userId);
                return null;
            }

            const userName = userDoc.data().name || 'Utilizator necunoscut';

            // Creează notificări pentru fiecare admin
            const adminNotifications = adminSnapshot.docs.map(adminDoc => {
                const adminFcmToken = adminDoc.data().fcmToken;
                const adminId = adminDoc.id;

                if (!adminFcmToken) {
                    console.log(`No FCM token for admin: ${adminId}`);
                    return Promise.resolve();
                }

                const payload = {
                    notification: {
                        title: 'Cerere de voluntariat nouă!',
                        body: `${userName} a trimis o cerere de voluntariat.`,
                    },
                    data: {
                        click_action: 'NEW_VOLUNTEER_REQUEST_ACTIVITY',
                        requestId: requestId
                    },
                    token: adminFcmToken
                };

                // Salvează notificarea în colecția Notifications pentru admin
                const notificationDoc = {
                    userId: adminId,
                    title: payload.notification.title,
                    body: payload.notification.body,
                    timestamp: Timestamp.fromDate(new Date()),
                    type: 'NEW_VOLUNTEER_REQUEST',
                    requestId: requestId,
                    viewed: false
                };

                return Promise.all([
                    messaging.send(payload),
                    db.collection('Notifications').add(notificationDoc)
                ])
                    .then(([_, docRef]) => {
                        console.log(`Notification sent to admin ${adminId}, saved as ${docRef.id}`);
                    })
                    .catch(error => {
                        console.error(`Error sending notification to admin ${adminId} or saving to Firestore:`, error);
                    });
            });

            // Așteaptă finalizarea tuturor notificărilor
            return Promise.all(adminNotifications);
        })
        .catch(error => {
            console.error('Error processing new volunteer request notification:', error);
            return null;
        });
});