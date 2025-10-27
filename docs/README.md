# GitHub Pages Setup

This folder contains the privacy policy and landing page for Ntfy TV Notifications.

## 🚀 How to Enable GitHub Pages

1. **Push these files to GitHub:**
   ```bash
   git add docs/
   git commit -m "Add privacy policy and landing page"
   git push
   ```

2. **Enable GitHub Pages in your repository:**
   - Go to https://github.com/clausilein/ntfy-tv-notifications
   - Click **Settings** tab
   - Scroll down to **Pages** section (left sidebar)
   - Under "Source", select:
     - Branch: `main` (or `master`)
     - Folder: `/docs`
   - Click **Save**

3. **Wait a few minutes** for GitHub to build and deploy your site

4. **Test your URLs:**
   - Landing page: https://clausilein.github.io/ntfy-tv-notifications/
   - Privacy policy: https://clausilein.github.io/ntfy-tv-notifications/privacy-policy.html

5. **Verify in the app:**
   - The privacy policy URL is already configured in `app/src/main/res/values/strings.xml`
   - It's set to: `https://clausilein.github.io/ntfy-tv-notifications/privacy-policy.html`

## 📝 Files in this folder

- **index.html** - Landing page with app information and features
- **privacy-policy.html** - Complete privacy policy (required for Play Store)
- **README.md** - This file with setup instructions

## ✅ After GitHub Pages is enabled

Once GitHub Pages is live, you should:
1. Test the privacy policy URL in a browser
2. Verify all links work correctly
3. Use this URL when submitting to Google Play Store

## 🔄 Updating the Privacy Policy

To update the privacy policy in the future:
1. Edit `docs/privacy-policy.html`
2. Update the "Last updated" date
3. Commit and push changes
4. GitHub Pages will automatically rebuild (takes 1-5 minutes)

---

**Note:** The privacy policy URL is required for Google Play Store submission. Make sure GitHub Pages is enabled and working before submitting your app.
