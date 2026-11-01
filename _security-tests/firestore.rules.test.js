
const fs = require("node:fs");
const path = require("node:path");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const { doc, getDoc } = require("firebase/firestore");
const { before, after, test } = require("node:test");

let testEnv;

before(async () => {
  const rulesPath = path.resolve(__dirname, "..", "firestore.rules");
  const rules = fs.readFileSync(rulesPath, "utf8");

  testEnv = await initializeTestEnvironment({
    projectId: "demo-goprox-security",
    firestore: { rules },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Unauthenticated users cannot read services", async () => {
  const db = testEnv.unauthenticatedContext().firestore();

  await assertFails(
    getDoc(doc(db, "services/security-test-service"))
  );
});

test("Authenticated users can read services", async () => {
  const db = testEnv.authenticatedContext("user-alice").firestore();

  await assertSucceeds(
    getDoc(doc(db, "services/security-test-service"))
  );
});

test("Users can read their own user document", async () => {
  const db = testEnv.authenticatedContext("user-alice").firestore();

  await assertSucceeds(
    getDoc(doc(db, "users/user-alice"))
  );
});

test("Users cannot read another user's private document", async () => {
  const db = testEnv.authenticatedContext("user-alice").firestore();

  await assertFails(
    getDoc(doc(db, "users/user-bob/private/device"))
  );
});

test("Users cannot read another user's main document", async () => {
  const db = testEnv.authenticatedContext("user-alice").firestore();

  await assertFails(
    getDoc(doc(db, "users/user-bob"))
  );
});
