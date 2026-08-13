---
name: release
description: Package and publish a new version of the timeLine Android app — signed release APK, release/ folder, README download link, git commit, and annotated tag. Use whenever the user asks to "cut a release", "package a version", "打包一個版本", "發一個版本" or similar, for this repo.
---

# Release procedure (timeLine / 歷史上的今天跑馬燈 Widget)

Follow these steps in order. This mirrors exactly how v0.8.0 was cut. Do the git-sensitive
and secret-handling steps yourself directly (Bash/Edit/Write) rather than delegating to a
subagent — keep tight, auditable control over anything that touches signing keys or `git push`.

## 0. Preconditions

- Confirm `git status` is otherwise clean (or only contains the changes intended for this
  release) before starting — don't fold in unrelated in-progress work by accident.
- JDK 17 is required to run Gradle (JDK 25, if installed as the system default, is
  incompatible with this project's Gradle/Kotlin version). Set `JAVA_HOME` for the current
  shell only, e.g.:
  ```bash
  export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
  (Adjust the path if the JDK 17 install location has changed — check with
  `find "/c/Program Files" -iname "jdk-17*" -maxdepth 2` if unsure. Never change the user's
  global `JAVA_HOME`/environment — only export it in the shell session running the build.)

## 1. Release signing setup (first release only)

Check whether `android/keystore.properties` already exists.

- **If it exists**: skip to step 2. Reuse the existing keystore for every release — it's the
  app's signing identity. Regenerating it would make future updates unable to install over
  previous ones (Android requires the same signing key to accept an update).
- **If it does not exist** (first-ever release, or the keystore was lost): generate one:
  ```bash
  cd android
  mkdir -p keystore
  STORE_PASS=$(openssl rand -hex 24)
  KEY_PASS=$(openssl rand -hex 24)
  keytool -genkeypair -v \
    -keystore keystore/release.jks \
    -storetype JKS \
    -alias timeline-release \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
    -dname "CN=OnThisDay Widget, OU=timeLine, O=felixfu007, L=Taipei, ST=Taiwan, C=TW"
  cat > keystore.properties <<EOF
  storeFile=keystore/release.jks
  storePassword=$STORE_PASS
  keyAlias=timeline-release
  keyPassword=$KEY_PASS
  EOF
  ```
  Use `-storetype JKS` explicitly — PKCS12 (the modern keytool default) has hit a
  `Password is not ASCII` bug in this environment even with pure-ASCII passwords; JKS avoids it.

  **Immediately verify both `android/keystore.properties` and `android/keystore/*.jks` are
  git-ignored** (`git check-ignore -v android/keystore.properties android/keystore/release.jks`
  — both must print a matching rule) before doing anything else. If either is *not* ignored,
  add rules to `android/.gitignore` and re-verify before proceeding. Never let a keystore or
  its passwords reach a `git add`.

  Tell the user afterward: back up `android/keystore.properties` and
  `android/keystore/release.jks` somewhere safe outside git (password manager, encrypted
  drive) — losing them means future releases can never update this app installation again
  under the same identity.

  `android/app/build.gradle.kts` already reads this file (see the `signingConfigs`/
  `hasReleaseSigningConfig` block near the top) and wires it into the `release` build type
  automatically when present. No further Gradle changes needed for subsequent releases.

## 2. Bump the version

Edit `android/app/build.gradle.kts`:
- Increment `versionCode` by 1 (plain integer, always increasing).
- Update `versionName` (e.g. `"0.8.0"` → `"0.9.0"`, or whatever the user specifies). Ask the
  user for the target version if they haven't specified one and it isn't obvious from context
  (e.g. "next milestone done" vs "just a patch").

## 3. Build the signed release APK

```bash
cd android
./gradlew assembleRelease
```

Verify it's actually signed (not `-unsigned`) before proceeding:
```bash
find app/build/outputs/apk/release -name "*.apk"   # expect app-release.apk, NOT app-release-unsigned.apk
"$LOCALAPPDATA/Android/Sdk/build-tools/35.0.0/apksigner.bat" verify --verbose app/build/outputs/apk/release/app-release.apk
```
Expect `Verifies` and `Verified using v2 scheme: true`. If it says `app-release-unsigned.apk`
was produced instead, `keystore.properties` wasn't found/read correctly — fix that before
going further (don't ship an unsigned APK; most devices can't even install it).

## 4. Stage the release artifact

```bash
mkdir -p release
cp android/app/build/outputs/apk/release/app-release.apk release/onthisday-widget-vX.Y.Z.apk
```
(Replace `vX.Y.Z` with the actual version.) If an older APK from a previous version is sitting
in `release/`, leave it — keep prior versions available for download too, unless the user asks
to prune them.

## 5. Update README.md

Update the download link and version number in the "下載" section of `README.md` to point at
the new `release/onthisday-widget-vX.Y.Z.apk` file, and update the "專案狀態" section if the
milestone status has changed since the last release.

## 6. Clean up ephemeral verification artifacts

Before committing, delete any loose debugging/verification screenshots or scratch files that
piled up in `android/` (or elsewhere in the repo) during development — e.g.
`android/screenshot_*.png` — if their content is already captured in prose in
`android/QA_REPORT_M7.md` or a commit message. Don't let ad-hoc PNGs from manual testing rounds
bloat the git history release after release. Keep it if the user asks to preserve it, or if it
has no other record.

## 7. Security check, then commit

**Before staging**, confirm no secrets are about to be committed:
```bash
git status --short | grep -i "keystore\|\.jks\|\.keystore" && echo "STOP — secret would be committed" || echo "OK"
```
Then:
```bash
git add -A
git status --short   # eyeball the full list — nothing unexpected, no secrets, no huge unrelated binaries
git commit -m "release: vX.Y.Z — <one-line summary of what's new>

<bullet list of notable changes since the last release>

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

## 8. Push and tag

```bash
git push origin main
git tag -a vX.Y.Z -m "vX.Y.Z — <short summary>"
git push origin vX.Y.Z
```

## 9. Verify the download link actually works

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" "https://raw.githubusercontent.com/felixfu007/timeLine/main/release/onthisday-widget-vX.Y.Z.apk"
```
Expect `HTTP 200`.

## 10. Report to the user

Summarize: version number, what changed, confirm the APK is signed and the download link is
live, and remind them (only on a first-time keystore generation) to back up the keystore.
