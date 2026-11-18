import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const projectId = "vire-7b583";
initializeApp({ credential: applicationDefault(), projectId });
const db = getFirestore();

function strings(value, limit = 20) {
  return Array.isArray(value)
    ? value.filter((item) => typeof item === "string").slice(0, limit)
    : [];
}

function safeText(value, maxLength) {
  return typeof value === "string" ? value.slice(0, maxLength) : "";
}

function httpsUrl(...values) {
  const url = values.find((value) => typeof value === "string" && value.startsWith("https://"));
  return url ? url.slice(0, 2048) : "";
}

async function migrate() {
  const users = await db.collection("users").get();
  let batch = db.batch();
  let pendingWrites = 0;
  let migrated = 0;

  for (const doc of users.docs) {
    const data = doc.data();
    const rawUsername = typeof data.username === "string" ? data.username.trim() : "";
    if (rawUsername.length < 3) continue;

    const username = rawUsername.slice(0, 64);
    const publicProfile = {
      uid: doc.id,
      username,
      usernameLowercase: username.toLowerCase(),
      favoriteGames: strings(data.favoriteGames),
      favoriteGenres: strings(data.favoriteGenres),
      skillLevel: safeText(data.skillLevel, 100),
      localArea: safeText(data.localArea, 200),
      gamerBio: safeText(data.gamerBio, 4000),
      aboutMe: safeText(data.aboutMe, 4000),
      profileImageUrl: httpsUrl(data.profileImageUrl, data.profileImage, data.avatarUrl),
      coverImageUrl: httpsUrl(data.coverImageUrl, data.coverImage, data.coverUrl),
    };

    batch.set(db.collection("publicProfiles").doc(doc.id), publicProfile, { merge: true });
    pendingWrites++;
    migrated++;

    if (pendingWrites === 400) {
      await batch.commit();
      batch = db.batch();
      pendingWrites = 0;
    }
  }

  if (pendingWrites > 0) await batch.commit();
  console.log(`Migrated ${migrated} searchable profiles; skipped ${users.size - migrated} records without a valid username.`);
}

migrate().catch((error) => {
  console.error("Public profile migration failed:", error.message);
  process.exitCode = 1;
});
