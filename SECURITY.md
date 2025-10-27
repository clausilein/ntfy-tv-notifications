# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 2.0.x   | :white_check_mark: |
| 1.0.x   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in Ntfy TV Notifications, please report it responsibly. We take all security reports seriously and will respond promptly.

### How to Report

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, please report security vulnerabilities by:

1. **Email**: android-ntfy@clausr.net
   - Use subject line: `[SECURITY] Brief description of issue`
   - Include as much detail as possible (see below)

2. **GitHub Security Advisories** (preferred for verified vulnerabilities)
   - Go to: https://github.com/clausilein/ntfy-tv-notifications/security/advisories
   - Click "Report a vulnerability"

### What to Include

Please include the following information in your report:

- **Type of vulnerability** (e.g., SQL injection, XSS, permission bypass, etc.)
- **Full paths of affected source files**
- **Step-by-step instructions to reproduce** the issue
- **Proof-of-concept or exploit code** (if applicable)
- **Impact assessment** - What could an attacker do with this vulnerability?
- **Suggested fix** (if you have one)
- **Your contact information** for follow-up questions

### Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 1 week
- **Status Update**: At least every 2 weeks until resolved
- **Fix Timeline**: Depends on severity
  - **Critical**: 1-7 days
  - **High**: 1-4 weeks
  - **Medium**: 1-3 months
  - **Low**: Best effort

### Disclosure Policy

- We request that you **do not publicly disclose** the vulnerability until we have released a fix
- We will work with you to understand the issue and develop a fix
- Once a fix is ready, we will:
  1. Release a patched version
  2. Publish a security advisory
  3. Credit you in the advisory (if you wish)
- **Coordinated disclosure**: We aim for 90 days from report to public disclosure

### Security Best Practices for Users

While using this app:

1. **Keep the app updated** - Always install the latest version
2. **Review permissions** - Understand what permissions the app requests
3. **Secure your device** - Use device encryption and lock screen
4. **Be cautious with topics** - Anyone who knows your topic can send you notifications
5. **Use ntfy.sh access controls** - Consider password-protecting your topics on ntfy.sh
6. **Review notification content** - Be cautious of suspicious notifications

### Known Security Considerations

#### Topic Privacy
- Topic names are not encrypted in transit to ntfy.sh
- Anyone who knows your topic name can send you notifications
- Use hard-to-guess topic names or ntfy.sh authentication features

#### Data Storage
- Subscriptions and messages are stored locally in an unencrypted database
- Data is protected by Android's application sandboxing
- On rooted devices, data may be accessible to other apps

#### Network Security
- WebSocket connection to ntfy.sh uses TLS encryption
- Messages are encrypted in transit but not end-to-end encrypted
- ntfy.sh can see all message content (this is inherent to the service)

#### Overlay Permissions
- SYSTEM_ALERT_WINDOW permission is powerful and could be abused
- This app only uses it to display notification overlays
- Review the source code to verify responsible usage

### Security Features

This app implements several security best practices:

- ✅ **Input validation** - Topic names and notification content are validated and sanitized
- ✅ **SQL injection prevention** - Room database with parameterized queries
- ✅ **Dependency updates** - Regular updates to address known vulnerabilities
- ✅ **Open source** - Code is publicly reviewable for transparency
- ✅ **Minimal permissions** - Only requests necessary permissions
- ✅ **No tracking** - No analytics or user tracking
- ✅ **Local-first** - Data stored locally, not transmitted to third parties (except ntfy.sh)

### Out of Scope

The following are **not** considered security vulnerabilities for this app:

- **ntfy.sh service vulnerabilities** - Report these to ntfy.sh maintainers
- **Android OS vulnerabilities** - Report to Google Android Security Team
- **Social engineering attacks** - User education issues
- **Physical device access attacks** - Requires compromised device
- **Denial of Service** - User can simply uninstall or disconnect

### Bug Bounty

This is a free, open-source hobby project with no funding. We do not offer monetary rewards for security reports, but we will:

- Publicly credit you (if you wish) in security advisories and release notes
- Add you to our Hall of Fame (see below)
- Provide our sincere gratitude and appreciation

### Security Hall of Fame

Security researchers who have responsibly disclosed vulnerabilities:

*None yet - you could be first!*

### Additional Resources

- **Android Security Best Practices**: https://developer.android.com/privacy-and-security/security-tips
- **OWASP Mobile Security**: https://owasp.org/www-project-mobile-security/
- **ntfy.sh Security**: https://docs.ntfy.sh/security/

### Contact

- **Security Email**: android-ntfy@clausr.net
- **GitHub**: https://github.com/clausilein/ntfy-tv-notifications
- **Maintainer**: Claus Regenbrecht

---

**Thank you for helping keep Ntfy TV Notifications secure!**

Last updated: January 2025
