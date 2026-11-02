
const fs = require("node:fs");
const path = require("node:path");

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const { ref, set, update, remove } = require("firebase/database");
const { before, after, test } = require("node:test");

let testEnv;

before(async () => {
  const rulesPath = path.resolve(__dirname, "..", "database.rules.json");
  const rules = fs.readFileSync(rulesPath, "utf8");

  testEnv = await initializeTestEnvironment({
    projectId: "demo-goprox-security",
    database: { rules },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

async function prepareChat(chatId) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await set(
      ref(context.database(), `chats/${chatId}/participants`),
      { alice: true, bob: true }
    );
  });
}

function createMessage(id, senderId) {
  return {
    id,
    senderId,
    text: "Original message",
    type: "text",
    fileUrl: "",
    timestamp: Date.now(),
    voiceDuration: 0,
    read: false,
    uploading: false,
    uploadProgress: 0,
    fileSize: "",
  };
}

async function createAliceMessage(chatId, messageId) {
  const aliceDb = testEnv.authenticatedContext("alice").database();

  await assertSucceeds(
    set(
      ref(aliceDb, `chats/${chatId}/messages/${messageId}`),
      createMessage(messageId, "alice")
    )
  );

  return aliceDb;
}

test("1. Sender cannot revert read=true to read=false", async () => {
  const chatId = "test-read-revert";
  const messageId = "message-001";
  const messagePath = `chats/${chatId}/messages/${messageId}`;

  await prepareChat(chatId);
  const aliceDb = await createAliceMessage(chatId, messageId);
  const bobDb = testEnv.authenticatedContext("bob").database();

  await assertSucceeds(
    update(ref(bobDb, messagePath), { read: true })
  );

  await assertFails(
    update(ref(aliceDb, messagePath), { read: false })
  );
});

test("2. Sender can edit own message and set Edited metadata", async () => {
  const chatId = "test-own-edit";
  const messageId = "message-002";
  const messagePath = `chats/${chatId}/messages/${messageId}`;

  await prepareChat(chatId);
  const aliceDb = await createAliceMessage(chatId, messageId);

  await assertSucceeds(
    update(ref(aliceDb, messagePath), {
      text: "Edited message",
      edited: true,
      editedAt: Date.now(),
    })
  );
});

test("3. Another participant cannot edit sender's message", async () => {
  const chatId = "test-other-edit";
  const messageId = "message-003";
  const messagePath = `chats/${chatId}/messages/${messageId}`;

  await prepareChat(chatId);
  await createAliceMessage(chatId, messageId);

  const bobDb = testEnv.authenticatedContext("bob").database();

  await assertFails(
    update(ref(bobDb, messagePath), {
      text: "Unauthorized edit",
      edited: true,
      editedAt: Date.now(),
    })
  );
});

test("4. Bob cannot create a message pretending to be Alice", async () => {
  const chatId = "test-sender-spoof";
  const messageId = "message-004";

  await prepareChat(chatId);

  const bobDb = testEnv.authenticatedContext("bob").database();

  await assertFails(
    set(
      ref(bobDb, `chats/${chatId}/messages/${messageId}`),
      createMessage(messageId, "alice")
    )
  );
});

test("5. Sender can delete own message", async () => {
  const chatId = "test-own-delete";
  const messageId = "message-005";
  const messagePath = `chats/${chatId}/messages/${messageId}`;

  await prepareChat(chatId);
  const aliceDb = await createAliceMessage(chatId, messageId);

  await assertSucceeds(remove(ref(aliceDb, messagePath)));
});

test("6. Another participant cannot delete sender's message", async () => {
  const chatId = "test-other-delete";
  const messageId = "message-006";
  const messagePath = `chats/${chatId}/messages/${messageId}`;

  await prepareChat(chatId);
  await createAliceMessage(chatId, messageId);

  const bobDb = testEnv.authenticatedContext("bob").database();

  await assertFails(remove(ref(bobDb, messagePath)));
});

test("7. Sender cannot mark own message as read", async () => {
  const chatId = "test-own-read";
  const messageId = "message-007";
  const messagePath = `chats/${chatId}/messages/${messageId}`;

  await prepareChat(chatId);
  const aliceDb = await createAliceMessage(chatId, messageId);

  await assertFails(
    update(ref(aliceDb, messagePath), { read: true })
  );
});

test("8. Sender cannot change senderId after creation", async () => {
  const chatId = "test-change-sender";
  const messageId = "message-008";
  const messagePath = `chats/${chatId}/messages/${messageId}`;

  await prepareChat(chatId);
  const aliceDb = await createAliceMessage(chatId, messageId);

  await assertFails(
    update(ref(aliceDb, messagePath), { senderId: "bob" })
  );
});
