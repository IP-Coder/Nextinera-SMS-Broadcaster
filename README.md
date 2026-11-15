# SMS Broadcaster

Forwards **every incoming SMS** (OTPs, bank messages, etc.) from your Android phone
to your own server, which then **broadcasts each message to Telegram**.

```
 Android phone  ──POST──►  sms.php (Hostinger)  ──►  Telegram
 (SmsReceiver)             checks secret, logs         your chat
                           + forwards to Telegram
```

Everything is yours: your phone, your server, your Telegram. A shared **secret**
makes sure only your phone can post to the server.

---

## What's in here

```
server/                 ← upload this to Hostinger
  sms.php               the endpoint the phone posts to
  dashboard.php         a web page to view recent messages
  config.php            YOUR settings (secret, Telegram token, chat id)
  data/                 messages are stored here (web-access blocked)

app/ , build.gradle.kts , gradlew , settings.gradle.kts   ← the Android app source
.github/workflows/build-apk.yml   ← builds the installable APK automatically on GitHub
```

---

## Setup — 4 steps

### 1) Create a Telegram bot (1 min)
1. In Telegram, open **@BotFather** → send `/newbot` → follow prompts.
2. Copy the **bot token** (looks like `123456789:ABCdef...`).
3. Get your **chat id**: message **@userinfobot** on Telegram — it replies with your `Id`.
   (For a group: add the bot to the group, then use a negative group id.)
4. Open a chat with **your new bot** and send it any message once (so it can message you back).

### 2) Configure & upload the server
1. Open `server/config.php` and set:
   - `app_secret` → a long random string (you'll paste the same into the app)
   - `telegram_token` → from BotFather
   - `telegram_chat_id` → your id from @userinfobot
   - `dashboard_pass` → a password for the web dashboard
2. Upload the whole `server/` folder to Hostinger (hPanel → File Manager →
   `public_html`). Put it in a subfolder, e.g. `public_html/sms/`.
3. Your endpoint URL is then: `https://YOURDOMAIN/sms/sms.php`
4. Test the dashboard in a browser: `https://YOURDOMAIN/sms/dashboard.php?pass=YOURPASS`

> Requires PHP with cURL — Hostinger has this on by default.

### 3) Get the APK (built in the cloud — no Android Studio needed)
1. Push this project to a **GitHub repo** (private is fine).
2. GitHub Actions runs **Build APK** automatically. When it finishes:
   - Download `app-debug.apk` from the run's **Artifacts**, or from the
     **"latest" Release** on the repo page.
3. Copy the APK to your phone.

### 4) Install & configure the app
1. On the phone, open the APK and allow **install from unknown sources**.
2. Open **SMS Broadcaster** and:
   - **Server URL**: `https://YOURDOMAIN/sms/sms.php`
   - **Shared secret**: the exact `app_secret` from `config.php`
   - Tap **Save**, grant **SMS permission**, and tap **Allow background**.
3. Tap **Send test message** → you should get it on Telegram within seconds.
4. Done. Every new SMS now forwards automatically.

---

## Notes & tips

- **Reliability:** messages are queued with WorkManager, so if the phone is
  offline the SMS is sent as soon as it reconnects. "Allow background /
  ignore battery optimization" keeps it working when the screen is off —
  recommended, and on Xiaomi/Realme/Oppo/Vivo also enable **Autostart** for
  the app in system settings.
- **Security:** keep the `app_secret` private. If it ever leaks, change it in
  both `config.php` and the app. OTPs are sensitive — only run this for your
  own numbers.
- **Turn off forwarding** anytime with the switch in the app, or
  `send_to_telegram => false` in `config.php`.
- **Add WhatsApp / a web dashboard / another webhook later:** the server already
  stores everything; those are easy to bolt on.

## Build the APK locally (optional)
If you ever want to build on your own machine with Android Studio / SDK:
```
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

<!-- build trigger -->
