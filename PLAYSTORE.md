# Google Play Store Release Checklist

This document tracks all requirements for publishing Ntfy TV Notifications on the Google Play Store for Android TV.

## ⚠️ Critical Issues to Fix First

### 1. ProGuard Configuration
- [ ] **Enable ProGuard/R8** - Currently disabled (`isMinifyEnabled = false`)
  - **File**: `app/build.gradle.kts` line 23
  - **Change to**: `isMinifyEnabled = true`
  - **Why**: Reduces APK size, improves performance, basic code obfuscation
  - **Action**: Test thoroughly after enabling to ensure nothing breaks

### 2. Data Backup & Privacy
- [ ] **Implement backup rules** - Currently using default `allowBackup="true"`
  - **File**: `app/src/main/AndroidManifest.xml` line 20
  - **Action**: Add `android:dataExtractionRules="@xml/data_extraction_rules"` (Android 12+)
  - **Action**: Add `android:fullBackupContent="@xml/backup_rules"` (Android 11 and below)
  - **Why**: Control what data gets backed up (database, preferences)
  - **Create**: `app/src/main/res/xml/backup_rules.xml`
  - **Create**: `app/src/main/res/xml/data_extraction_rules.xml`

### 3. Privacy Policy (REQUIRED)
- [ ] **Create Privacy Policy** - Required for apps requesting sensitive permissions
  - **Permissions that require it**:
    - `SYSTEM_ALERT_WINDOW` (overlay)
    - `POST_NOTIFICATIONS` (notifications)
    - `INTERNET` (network access)
  - **Action**: Write privacy policy explaining:
    - What data you collect (topics, messages stored locally)
    - How data is used (displaying notifications)
    - Third-party services (ntfy.sh - explain it's user-configured)
    - Data storage (local only, Room database)
    - No analytics, no ads, no tracking
  - **Host it**: On GitHub Pages, your website, or Google Sites
  - **Required for**: Play Store listing

### 4. App Signing
- [ ] **Generate upload key** for Play Store signing
  - **Action**: Generate keystore file
  - **Action**: Configure signing in `build.gradle.kts`
  - **Security**: NEVER commit keystore to Git
  - **Backup**: Store keystore securely (you'll need it forever)

---

## 📱 Application Code Changes

### Build Configuration (`app/build.gradle.kts`)

- [ ] **Version Management**
  - Current: `versionCode = 1`, `versionName = "1.0"`
  - Action: Keep for first release, increment for updates
  - Note: Play Store requires incrementing versionCode for each release

- [ ] **Enable Shrinking, Obfuscation, and Optimization**
  ```kotlin
  buildTypes {
      release {
          isMinifyEnabled = true
          isShrinkResources = true
          proguardFiles(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              "proguard-rules.pro"
          )
      }
  }
  ```

- [ ] **Add Signing Configuration**
  ```kotlin
  android {
      signingConfigs {
          create("release") {
              storeFile = file("your-keystore.jks")
              storePassword = System.getenv("KEYSTORE_PASSWORD")
              keyAlias = System.getenv("KEY_ALIAS")
              keyPassword = System.getenv("KEY_PASSWORD")
          }
      }
      buildTypes {
          release {
              signingConfig = signingConfigs.getByName("release")
              // ... other settings
          }
      }
  }
  ```

- [ ] **Target SDK Check**
  - Current: `targetSdk = 36` - ✅ Good (latest)
  - Note: Play Store requires targeting recent SDK versions

- [ ] **Consider adding version suffix for debug builds**
  ```kotlin
  defaultConfig {
      versionNameSuffix = ".debug" // for debug builds only
  }
  ```

### AndroidManifest.xml Changes

- [ ] **Add backup rules** (mentioned above)
  ```xml
  <application
      android:dataExtractionRules="@xml/data_extraction_rules"
      android:fullBackupContent="@xml/backup_rules"
      android:allowBackup="true"
      ...>
  ```

- [ ] **Review permission justifications**
  - All permissions look justified ✅
  - `SYSTEM_ALERT_WINDOW` - for overlay notifications
  - `FOREGROUND_SERVICE` - for persistent connection
  - `POST_NOTIFICATIONS` - for notification display
  - `INTERNET` - for WebSocket connection
  - `ACCESS_NETWORK_STATE` - for connectivity checks

- [ ] **Verify TV compatibility declarations**
  - `android.hardware.touchscreen` required="false" - ✅ Good
  - `android.software.leanback` required="false" - ⚠️ Should be "true" for TV-only app
    - **Decision needed**: Is this TV-only or also for mobile?
    - If TV-only: Change to `android:required="true"`
    - If also mobile: Keep as `false`

- [ ] **Add TV feature requirement** (if TV-only)
  ```xml
  <uses-feature
      android:name="android.software.leanback"
      android:required="true" />
  ```

### String Resources (`app/src/main/res/values/strings.xml`)

- [ ] **Review and complete all strings**
  - Check: Are all UI strings extracted to strings.xml?
  - Add: Short description (80 chars)
  - Add: Full description (4000 chars)
  - Action: Run lint check for hardcoded strings

### Create Missing XML Files

- [ ] **Create backup rules** (`app/src/main/res/xml/backup_rules.xml`)
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <full-backup-content>
      <!-- Include user preferences -->
      <include domain="sharedpref" path="." />

      <!-- Include database -->
      <include domain="database" path="." />

      <!-- Exclude sensitive data if any -->
      <!-- <exclude domain="sharedpref" path="auth_token.xml" /> -->
  </full-backup-content>
  ```

- [ ] **Create data extraction rules** (`app/src/main/res/xml/data_extraction_rules.xml`)
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <data-extraction-rules>
      <cloud-backup>
          <include domain="sharedpref" path="." />
          <include domain="database" path="." />
      </cloud-backup>
      <device-transfer>
          <include domain="sharedpref" path="." />
          <include domain="database" path="." />
      </device-transfer>
  </data-extraction-rules>
  ```

- [ ] **Create network security config** (recommended)
  - File: `app/src/main/res/xml/network_security_config.xml`
  - Add to manifest: `android:networkSecurityConfig="@xml/network_security_config"`
  - Purpose: Define trusted certificates for ntfy.sh

---

## 🎨 Graphics & Assets Required

### App Icons (Already have basic ones ✅)
- [x] Launcher icon - Different densities (mdpi to xxxhdpi) - ✅ Present
- [ ] **Adaptive icon** - Foreground + background layers (recommended)
  - Create: `res/mipmap-anydpi-v26/ic_launcher.xml`
  - Create: `res/drawable/ic_launcher_foreground.xml`
  - Create: `res/drawable/ic_launcher_background.xml`
  - Tool: Use Android Studio → New → Image Asset

- [ ] **High-res icon** for Play Store
  - Size: 512 x 512 px
  - Format: PNG (32-bit with alpha)
  - No rounded corners (Google adds them)
  - **Required for**: Play Store listing

### TV Banner (Have one ✅)
- [x] TV Banner - 1280 x 720 px - ✅ Present at `drawable/tv_banner.webp`
- [ ] **Verify banner quality**
  - Check resolution meets 1280 x 720
  - Verify text is readable on TV screens
  - Test on actual TV device

### Feature Graphic (REQUIRED for Play Store)
- [ ] **Create feature graphic**
  - Size: 1024 x 500 px
  - Format: PNG or JPG
  - Purpose: Featured section banner on Play Store
  - Content: App name, key feature, attractive visual
  - **Required for**: Play Store listing

### Screenshots (REQUIRED - Minimum 2)

#### TV Screenshots (Primary - since this is Android TV app)
- [ ] **TV Screenshot 1**: Main screen with subscription list
  - Size: 1920 x 1080 px (min)
  - Format: PNG or JPG
  - Show: Active subscriptions, connection status

- [ ] **TV Screenshot 2**: Subscription management screen
  - Size: 1920 x 1080 px (min)
  - Show: Adding a subscription, topic input

- [ ] **TV Screenshot 3**: Overlay notification example
  - Size: 1920 x 1080 px (min)
  - Show: Sample notification with priority color

- [ ] **TV Screenshot 4**: Message history
  - Size: 1920 x 1080 px (min)
  - Show: List of received messages with tags

- [ ] **Recommended**: 4-8 screenshots total
- [ ] **Capture method**:
  - Real device: `adb shell screencap -p /sdcard/screenshot.png`
  - Emulator: Use emulator screenshot tool
  - Android Studio: Logcat tool has screenshot button

#### Phone Screenshots (Optional - if also targeting mobile)
- [ ] Phone screenshots (1080 x 1920 or similar)
- Only if supporting non-TV devices

### Video (Optional but Recommended)
- [ ] **Promo video** (YouTube)
  - Length: 30 seconds to 2 minutes
  - Content: Quick demo of adding subscription, receiving notification
  - Tool: Screen recording + simple editing
  - Benefits: Higher conversion rate

---

## 📝 Play Store Listing Content

### Text Content to Prepare

- [ ] **App Title** (max 30 characters)
  - Current: "Ntfy TV Notifications" (21 chars) ✅
  - Alternative: "Ntfy for Android TV" (19 chars)

- [ ] **Short Description** (max 80 characters)
  - Example: "Real-time ntfy.sh notifications on your Android TV with overlay alerts"
  - Draft: _______________________________________________________

- [ ] **Full Description** (max 4000 characters)
  - Structure:
    1. Opening hook (what it does)
    2. Key features (bullet points)
    3. How it works (brief)
    4. Requirements
    5. Privacy info
    6. Support/Contact
  - Draft in separate file or below this checklist

- [ ] **What's New** (for this release)
  - Example: "Initial release with multi-topic subscriptions and dual notification system"

### Store Listing Details

- [ ] **Application Category**
  - Recommended: "Tools" or "Productivity"

- [ ] **Content Rating**
  - Complete questionnaire in Play Console
  - Expected: "Everyone" (no sensitive content)

- [ ] **Tags** (up to 5)
  - Suggestions: notifications, ntfy, android-tv, productivity, tools

- [ ] **Contact Details**
  - Email: (your support email)
  - Website: (optional - could use GitHub repo)
  - Privacy Policy URL: (REQUIRED - see above)

---

## 🔐 Security & Compliance

### App Signing & Security

- [ ] **Generate Upload Key**
  ```bash
  keytool -genkey -v -keystore ntfy-tv-upload.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias ntfy-tv-key
  ```
  - Store securely (password manager + encrypted backup)
  - Never commit to Git (add to .gitignore)

- [ ] **Configure App Signing** in build.gradle
- [ ] **Backup keystore** to secure location
- [ ] **Document keystore info** (alias, location) securely

### Privacy & Data Safety

- [ ] **Data Safety Form** (in Play Console)
  - Data collected: None or specify (topics, messages - local only)
  - Data shared: None (no third-party sharing)
  - Security practices: Data encrypted at rest (device encryption)
  - Data deletion: User can clear app data

- [ ] **Privacy Policy** (mentioned above)
  - Host online
  - Add URL to Play Console
  - Include in app (Settings → Privacy Policy link)

### Permissions Declaration

- [ ] **Justify sensitive permissions** (in Play Console)
  - `SYSTEM_ALERT_WINDOW`: Required for overlay notifications
  - `POST_NOTIFICATIONS`: Required to show notifications
  - `FOREGROUND_SERVICE`: Required for persistent WebSocket connection
  - Provide video demo showing permission usage

---

## 🧪 Testing Requirements

### Pre-Release Testing

- [ ] **Test on real Android TV device**
  - Test navigation with TV remote
  - Verify all D-pad interactions
  - Test focus indicators
  - Verify text readability from 10 feet

- [ ] **Test release build** (with ProGuard enabled)
  ```bash
  ./gradlew assembleRelease
  adb install app/build/outputs/apk/release/app-release.apk
  ```
  - Test all features work after obfuscation
  - Verify no crashes
  - Check ProGuard rules are sufficient

- [ ] **Test on different Android versions**
  - Minimum: Android 10 (API 29)
  - Recommended: Test on Android 10, 11, 12, 13, 14+

- [ ] **Test permission flows**
  - Fresh install → permission requests
  - Denial → fallback behavior
  - Grant → feature works

- [ ] **Test edge cases**
  - No internet connection
  - No active subscriptions
  - Invalid topic names
  - Rapid message arrival
  - App backgrounding/foregrounding

- [ ] **Test backup & restore**
  - Install app, add subscriptions
  - Backup (adb backup or cloud)
  - Uninstall & reinstall
  - Restore and verify subscriptions persisted

### Internal Testing Track

- [ ] **Create internal testing release** in Play Console
  - Upload first APK/AAB
  - Test with small group
  - Gather feedback
  - Fix critical issues

### Beta Testing (Recommended)

- [ ] **Set up closed beta track**
  - Invite testers (friends, Reddit community, etc.)
  - Collect feedback
  - Monitor crash reports
  - Iterate based on feedback

---

## 📦 Building Release APK/AAB

### Pre-Build Checklist

- [ ] Enable ProGuard (mentioned above)
- [ ] Configure signing (mentioned above)
- [ ] Update version codes if needed
- [ ] Test thoroughly
- [ ] Review all TODOs in code
- [ ] Remove debug logging (or use timber with release tree that doesn't log)

### Build Options

#### Option 1: Android App Bundle (AAB) - Recommended
- [ ] **Build AAB**
  ```bash
  ./gradlew bundleRelease
  ```
  - Output: `app/build/outputs/bundle/release/app-release.aab`
  - Benefits: Smaller downloads, Play Store optimizes per-device
  - **Recommended by Google**

#### Option 2: APK
- [ ] **Build APK**
  ```bash
  ./gradlew assembleRelease
  ```
  - Output: `app/build/outputs/apk/release/app-release.apk`
  - Use case: Direct distribution, testing

### Post-Build Verification

- [ ] **Verify signing**
  ```bash
  jarsigner -verify -verbose -certs app-release.apk
  ```

- [ ] **Check APK size**
  - Analyze APK in Android Studio
  - Identify large resources
  - Optimize if needed

- [ ] **Test signed APK/AAB**
  - Install on clean device
  - Verify all features work

---

## 🚀 Play Console Setup & Submission

### Initial Play Console Setup

- [ ] **Create Google Play Developer Account**
  - Cost: $25 one-time registration fee
  - URL: https://play.google.com/console

- [ ] **Create app in Play Console**
  - Select "Android TV"
  - Enter app details

### Store Listing

- [ ] **Upload graphics** (all from Assets section above)
- [ ] **Enter text content** (title, descriptions)
- [ ] **Select category & tags**
- [ ] **Add contact details**
- [ ] **Set pricing** (Free)

### Content Rating

- [ ] **Complete content rating questionnaire**
  - Takes 5-10 minutes
  - Answer honestly about content
  - Expected rating: Everyone

### Data Safety

- [ ] **Complete Data Safety form**
  - What data is collected
  - How it's used
  - Security practices

### App Content

- [ ] **Provide privacy policy URL**
- [ ] **Declare ads** (None in this app)
- [ ] **Target audience** (Everyone)
- [ ] **COVID-19 contact tracing** (No)
- [ ] **Government app** (No)

### Release

#### Production Track (Final Release)
- [ ] **Upload AAB/APK** to Production track
- [ ] **Set rollout percentage** (optional - start with 20%, increase gradually)
- [ ] **Review release**
- [ ] **Submit for review**

### Review Process

- [ ] **Wait for review** (usually 1-3 days, can be up to 7 days)
- [ ] **Address any issues** if rejected
- [ ] **Resubmit** if needed

---

## 🎯 Post-Release Tasks

### Monitoring

- [ ] **Monitor crash reports** in Play Console
- [ ] **Check reviews** and ratings
- [ ] **Monitor install/uninstall metrics**
- [ ] **Check ANRs** (Application Not Responding errors)

### User Support

- [ ] **Respond to reviews** (especially negative ones)
- [ ] **Set up support channel** (email, GitHub issues)
- [ ] **Create FAQ** (optional)

### Marketing (Optional)

- [ ] **Announce on social media**
- [ ] **Post to relevant subreddits** (r/androidtv, r/ntfy, r/selfhosted)
- [ ] **Share on GitHub** discussions
- [ ] **Create demo video** for YouTube

---

## 📋 Quick Reference Checklist

### Must-Do Before Submission
- [ ] Enable ProGuard
- [ ] Generate & configure app signing
- [ ] Create privacy policy
- [ ] Add backup rules
- [ ] Create 512x512 icon
- [ ] Create feature graphic
- [ ] Take 2-4 TV screenshots
- [ ] Write store description
- [ ] Test release build thoroughly

### Should-Do
- [ ] Add adaptive icon
- [ ] Create promo video
- [ ] Beta test with users
- [ ] Set up analytics (Firebase, etc.)
- [ ] Add in-app feedback mechanism

### Nice-to-Have
- [ ] Professional icon design
- [ ] Localization (other languages)
- [ ] In-app tutorials/onboarding
- [ ] Dark/Light theme

---

## 📚 Resources & References

### Official Documentation
- [Google Play Console](https://play.google.com/console)
- [Android TV App Quality Guidelines](https://developer.android.com/docs/quality-guidelines/tv-app-quality)
- [Launch Checklist](https://developer.android.com/distribute/best-practices/launch/launch-checklist)
- [Store Listing Guidelines](https://support.google.com/googleplay/android-developer/answer/9866151)

### Tools
- [App Signing Key Management](https://developer.android.com/studio/publish/app-signing)
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/) - Icon generator
- [Privacy Policy Generator](https://www.privacypolicygenerator.info/)

### Testing
- [Pre-Launch Report](https://support.google.com/googleplay/android-developer/answer/7002270) - Google's automated testing
- [Firebase Test Lab](https://firebase.google.com/docs/test-lab) - Test on real devices

---

## ✅ Completion Status

**Overall Progress**: 0% (0/XX tasks completed)

Last updated: 2025-01-27

---

## Notes & Decisions

### Key Decisions Needed:
1. **TV-only or also mobile?** → Affects manifest leanback requirement
2. **Privacy policy hosting** → Where will you host it?
3. **Beta testing strategy** → Public, closed, or none?
4. **Rollout strategy** → Immediate 100% or gradual?

### Known Issues:
- Build log file (`build.log`) should not be in Git → Add to .gitignore
- Consider if NtfyWorker.kt is needed (seems unused based on ForegroundService implementation)

### Future Enhancements (Post-Launch):
- In-app update mechanism
- Analytics integration
- User feedback form
- Notification sound customization
- Export/import functionality
