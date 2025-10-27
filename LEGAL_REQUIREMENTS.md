# Legal Requirements for App Publication

This document outlines all legal information that must be included in the app and Play Store listing.

---

## 📍 Your Location Matters

**Are you based in Germany/Austria/EU?** → You need **Impressum** (TMG §5)
**Are you based elsewhere?** → You still need **Privacy Policy** + **Open Source Licenses**

---

## ✅ What You MUST Include

### 1. Privacy Policy (REQUIRED - Global)

**Required by:** Google Play Store, GDPR (EU), various privacy laws worldwide

**Must explain:**
- What data is collected (subscriptions, messages stored locally)
- How data is used (displaying notifications)
- Where data is stored (locally on device, Room database)
- Data retention (until user deletes)
- User rights (can delete all data by uninstalling)
- Third-party services (ntfy.sh - user configured)
- No tracking, no analytics, no ads

**Where to include:**
- [ ] Hosted online (URL required for Play Store)
- [ ] Link in app Settings → "Privacy Policy"
- [ ] Link in Play Store listing

**Template:** See below

---

### 2. Open Source Licenses (REQUIRED - Global)

**Required by:** Licenses of libraries you use (Apache, MIT, etc.)

**Libraries to credit:**
- OkHttp (Apache 2.0)
- Kotlin Coroutines (Apache 2.0)
- AndroidX libraries (Apache 2.0)
- Jetpack Compose (Apache 2.0)
- Room Database (Apache 2.0)

**Where to include:**
- [ ] In app: Settings → "Open Source Licenses"
- [ ] Can use Google's OSS Licenses plugin (recommended - auto-generates)

**How to implement:** See implementation section below

---

### 3. Impressum (REQUIRED - Germany/Austria/EU)

**Required by:** TMG §5 (Telemediengesetz) in Germany, similar laws in Austria/EU

**Required if:**
- You're based in Germany, Austria, or EU
- Your app is commercial (even if free, if it has potential business purpose)

**Must include:**
- Full name (first and last name)
- Complete address (street, house number, postal code, city, country)
- Email address
- Phone number (optional for individuals, required for businesses)
- Tax ID or VAT number (if applicable)

**Where to include:**
- [ ] In app: Settings → "Legal Notice" or "Impressum"
- [ ] On your privacy policy website
- [ ] In Play Store contact information

**Example format:**
```
Legal Notice / Impressum

Information according to § 5 TMG:

[Your Full Name]
[Street Address]
[Postal Code, City]
[Country]

Contact:
Email: [your@email.com]
Phone: [optional]

Responsible for content according to § 55 Abs. 2 RStV:
[Your Name]
```

---

### 4. Terms of Service (RECOMMENDED)

**Not legally required but highly recommended**

**Purpose:**
- Protects you from liability
- Sets expectations for users
- Clarifies your responsibilities

**Should cover:**
- Service "as-is" (no warranty)
- ntfy.sh is third-party service (not your responsibility)
- No guarantee of uptime or reliability
- User responsible for their own data
- Limitation of liability
- Right to discontinue service
- Governing law and jurisdiction

**Where to include:**
- [ ] Hosted online
- [ ] Link in app Settings → "Terms of Service"
- [ ] Can be combined with Privacy Policy

---

## 🎨 How to Implement in Your App

### Step 1: Add Settings Screen with Legal Links

**Create a new screen:** `SettingsScreen.kt` in `app/src/main/java/net/clausr/ntfytvnotifications/ui/`

**Include:**
- About section (app name, version, description)
- Privacy Policy (opens in browser)
- Terms of Service (opens in browser) [if you create one]
- Open Source Licenses (opens in-app screen)
- Legal Notice / Impressum (if applicable - opens in-app screen)

### Step 2: Add to MainActivity Navigation

Add a "Settings" button on main screen that navigates to SettingsScreen

### Step 3: Implement Open Source Licenses (Easy Way)

Add to `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.google.android.gms.oss-licenses-plugin")
}

dependencies {
    implementation("com.google.android.gms:play-services-oss-licenses:17.1.0")
}
```

Then add to project-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.android.gms.oss-licenses-plugin") version "0.10.6" apply false
}
```

This auto-generates a screen with all licenses!

### Step 4: Host Privacy Policy & Legal Docs

**Option A: GitHub Pages (Free & Easy)**
1. Create `docs/` folder in your repo
2. Add `privacy-policy.html` and `impressum.html`
3. Enable GitHub Pages in repo settings
4. Access at: `https://clausilein.github.io/ntfy-tv-notifications/privacy-policy.html`

**Option B: Google Sites (Free)**
1. Create a simple Google Site
2. Add privacy policy page
3. Publish and get URL

**Option C: Your Own Website**
If you have a domain/hosting

---

## 📄 Document Templates

### Privacy Policy Template

```markdown
# Privacy Policy for Ntfy TV Notifications

Last updated: [DATE]

## Introduction

Ntfy TV Notifications ("we", "our", "the app") is an Android TV application that
receives notifications from the ntfy.sh service. We respect your privacy and are
committed to protecting your personal data.

## Developer Information

[Your Name/Company]
[Address - if Impressum required]
Email: [your@email.com]

## Data We Collect

### Locally Stored Data
The app stores the following data **locally on your device only**:
- Subscription topics you configure
- Received notification messages (last 100 in memory, more in local database)
- App preferences and settings

**Important:** All data is stored locally on your device using Android Room database
and SharedPreferences. No data is sent to us or any third party.

### Data We Do NOT Collect
- We do not collect personal information
- We do not use analytics or tracking
- We do not display advertisements
- We do not share any data with third parties

## Third-Party Services

### ntfy.sh
This app connects to ntfy.sh, a third-party notification service that you configure.
When you subscribe to a topic:
- The app connects to ntfy.sh servers via WebSocket
- Messages sent to your topics are received by the app
- Please review ntfy.sh privacy policy: https://ntfy.sh/docs/privacy/

**Important:** We do not control ntfy.sh and are not responsible for their data practices.

### Google Play Services
If you install this app from Google Play Store, Google's privacy policy applies to
the download and update process: https://policies.google.com/privacy

## How We Use Data

Data stored locally is used solely to:
- Display your subscription topics
- Show received notifications as overlays or system notifications
- Maintain your app preferences
- Keep notification history

## Data Storage and Security

- All data is stored locally on your Android TV device
- Data is protected by Android's application sandboxing
- Data is encrypted at rest using device-level encryption (if your device supports it)
- No data is transmitted to external servers except your configured ntfy.sh connection

## Your Data Rights

You have the right to:
- **Access:** View all your data within the app
- **Delete:** Clear all data by uninstalling the app or clearing app data
- **Modify:** Change or delete subscriptions at any time
- **Export:** Currently not supported (future feature)

### How to Delete Your Data
1. In app: Manage Subscriptions → Delete all subscriptions
2. Android Settings → Apps → Ntfy TV Notifications → Storage → Clear Data
3. Uninstall the app

## Children's Privacy

This app does not knowingly collect information from children under 13 years of age.

## Changes to Privacy Policy

We may update this privacy policy from time to time. Updates will be posted at this URL
and within the app. Continued use after changes indicates acceptance.

## Contact Us

If you have questions about this privacy policy:
- Email: [your@email.com]
- GitHub: https://github.com/clausilein/ntfy-tv-notifications/issues

## Open Source

This app is open source. You can review the source code at:
https://github.com/clausilein/ntfy-tv-notifications

## Legal Basis (for GDPR)

If you are in the European Union:
- Processing is based on your consent and legitimate interest in using the app
- You can withdraw consent at any time by deleting data or uninstalling
- Data controller: [Your Name/Company], [Address], [Email]

---

This privacy policy was last updated on [DATE].
```

---

### Impressum Template (Germany)

```markdown
# Legal Notice / Impressum

## Information according to § 5 TMG

[Your Full Name]
[Street and House Number]
[Postal Code] [City]
[Country]

## Contact

Email: [your@email.com]
Phone: [your phone number - optional for individuals]

## Responsible for content according to § 55 Abs. 2 RStV

[Your Full Name]
[Address as above]

## Disclaimer

### Liability for Content

The contents of our app have been created with the greatest possible care. However,
we cannot guarantee the contents' accuracy, completeness, or topicality. According
to statutory provisions, we are responsible for our own content on this app.
However, we are not obligated to monitor transmitted or stored third-party information
or investigate circumstances indicating illegal activity.

### Liability for Links

Our app contains links to external third-party websites (ntfy.sh). We have no
influence on the contents of those websites; therefore, we cannot assume any liability
for these external contents. The respective provider or operator of the linked pages
is always responsible for the contents.

### Copyright

The content and works created by the app operators on this app are subject to German
copyright law. Contributions by third parties are indicated as such. Duplication,
processing, distribution, or any form of commercialization beyond the scope of copyright
law requires the prior written consent of its respective author or creator.

---

Last updated: [DATE]
```

---

### Terms of Service Template (Optional)

```markdown
# Terms of Service

Last updated: [DATE]

## Acceptance of Terms

By using Ntfy TV Notifications, you agree to these Terms of Service.

## Description of Service

Ntfy TV Notifications is a free, open-source Android TV application that connects
to the ntfy.sh notification service to display real-time notifications.

## No Warranty

The app is provided "as-is" without any warranty. We make no guarantees about:
- Availability or uptime
- Reliability of notifications
- Compatibility with all devices
- Freedom from bugs or errors

## Third-Party Services

This app connects to ntfy.sh, which is operated by a third party. We are not
responsible for:
- ntfy.sh service availability
- ntfy.sh data practices
- Content of notifications you receive

## User Responsibilities

You are responsible for:
- Configuring appropriate notification topics
- Securing your device
- Complying with ntfy.sh terms of service
- Not using the app for illegal purposes

## Limitation of Liability

To the maximum extent permitted by law, we are not liable for:
- Any damages arising from use or inability to use the app
- Loss of data or notifications
- Service interruptions
- Third-party actions or omissions

## Changes to Service

We reserve the right to:
- Modify or discontinue the app at any time
- Update these terms (with notice)

## Termination

You may stop using the app at any time by uninstalling it.

## Governing Law

These terms are governed by the laws of [Your Country/State].

## Contact

Questions about these terms: [your@email.com]

---

Last updated: [DATE]
```

---

## ✅ Implementation Checklist

### In Your App

- [ ] Add Settings/About screen to UI
- [ ] Add "Privacy Policy" button (opens browser)
- [ ] Add "Open Source Licenses" button (opens generated screen)
- [ ] Add "Legal Notice" or "Impressum" button (if applicable)
- [ ] Add "Terms of Service" button (if you create one - optional)
- [ ] Add app version display
- [ ] Add GitHub repository link

### Online

- [ ] Write privacy policy (use template, customize)
- [ ] Write Impressum if required (use template, fill in your info)
- [ ] Optional: Write terms of service
- [ ] Host documents online (GitHub Pages, Google Sites, or your domain)
- [ ] Get URLs for each document
- [ ] Test links work

### Play Store

- [ ] Add privacy policy URL to Play Console
- [ ] Add contact email to Play Console
- [ ] Complete Data Safety form
- [ ] If Impressum required, ensure contact info matches

### Repository

- [ ] Add LICENSE file to GitHub (choose: MIT, Apache 2.0, GPL, etc.)
- [ ] Consider adding SECURITY.md for vulnerability reporting
- [ ] Update README with legal/contact info

---

## 🚨 Important Notes

### Do NOT Include in Public Code:
- Your home address (keep in online docs only, not hardcoded in app)
- Your phone number
- Personal details beyond what's required

### Minimal Approach:
If you want minimal legal overhead:
1. **Must have:** Privacy Policy + Open Source Licenses
2. **Must have if in Germany/EU:** Impressum
3. **Nice to have:** Terms of Service

### Get Legal Advice:
This is general guidance, not legal advice. If you have concerns:
- Consult a lawyer familiar with your jurisdiction
- Review Germany's TMG requirements if applicable
- Check GDPR requirements if in EU

---

## 📞 Questions to Answer

Before finalizing legal docs, you need to decide:

1. **Are you in Germany/Austria/EU?** → Need Impressum
2. **Do you want to publish your home address?** → Required for Impressum
3. **Do you have a business entity?** → Affects what info is required
4. **Where will you host privacy policy?** → GitHub Pages, Google Sites, your domain?
5. **What license for your code?** → MIT, Apache 2.0, GPL?

---

## 🎯 Recommended Next Steps

1. **Determine your location requirements** (Impressum needed?)
2. **Write privacy policy** using template above
3. **Set up GitHub Pages** to host it (easiest free option)
4. **Add Open Source Licenses** plugin to app
5. **Create Settings screen** with links to legal docs
6. **Test all links** before Play Store submission

---

## Resources

- **GDPR Info:** https://gdpr.eu/
- **TMG (German):** https://www.gesetze-im-internet.de/tmg/
- **Privacy Policy Generator:** https://www.privacypolicygenerator.info/
- **GitHub Pages Setup:** https://pages.github.com/
- **OSS Licenses Plugin:** https://developers.google.com/android/guides/opensource

---

Last updated: 2025-01-27
