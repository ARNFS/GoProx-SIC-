
const fs = require("node:fs");
const path = require("node:path");

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const { ref, set } = require("firebase/database");
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Alice can write to her own chat index", async () => {
  const db = testEnv.authenticatedContext("alice").database();

  await assertSucceeds(
    set(ref(db, "user_chats/alice/bob"), {
      chatId: "chat-alice-bob",
    })
  );
});

test("Alice must not write to Bob's chat index", async () => {
  const db = testEnv.authenticatedContext("alice").database();

  await assertFails(
    set(ref(db, "user_chats/bob/alice"), {
      chatId: "unauthorized-chat",
    })
  );
});

test("Alice must not write to another unrelated user's chat index", async () => {
  const db = testEnv.authenticatedContext("alice").database();

  await assertFails(
    set(ref(db, "user_chats/bob/charlie"), {
      chatId: "unauthorized-chat",
    })
  );
});
