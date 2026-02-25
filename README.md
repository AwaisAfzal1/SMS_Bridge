# 📡 SMS Bridge

A complete system to send SMS messages through an Android phone from a web interface. Perfect for bulk SMS sending, automated notifications, or remote SMS capabilities.

## 🚀 Features

- **Web Dashboard**: Modern interface to send SMS from any device
- **Android App**: Background service that polls for messages and sends SMS
- **Python Backend**: Lightweight Flask server with message queue
- **Real-time Updates**: Auto-refreshing dashboard with message logs
- **Security**: Token-based authentication for Android app
- **Cross-platform**: Web interface works on any device

## 📋 Use Cases

- **Bulk SMS campaigns** from a computer interface
- **Automated notifications** from web applications
- **Remote SMS sending** when phone is not nearby
- **SMS gateway** for web apps that need text messaging
- **Emergency alerts** system
- **Two-factor authentication** (2FA) SMS delivery

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Web Dashboard │────▶│  Python Server  │◀────│  Android Phone  │
│   (index.html)  │     │   (server.py)   │     │   (APK)         │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │ 1. Submit SMS         │ 2. Queue message      │ 3. Poll for messages
        │                       │                       │
        │ 6. View log           │ 4. Return pending     │ 5. Send SMS & confirm
        │                       │                       │
```

## 🛠️ Installation & Setup

### Prerequisites
- Python 3.7+
- Android Studio / Gradle (for building APK)
- Android phone with SMS capability
- Local network access

### 1. Backend Server Setup

```bash
# Clone the repository
git clone <your-repo-url>
cd sms-bridge

# Install Python dependencies
pip install flask flask-cors

# Start the server
python server.py
```

**Server Configuration** (`server.py` line 12):
```python
SECRET_TOKEN = "my-secret-token-123"  # Change this for production!
```

### 2. Web Dashboard Setup

Open `index.html` in any modern browser. No installation needed!

**Dashboard Configuration** (`index.html` line 244):
```javascript
const SERVER_URL = "http://YOUR_SERVER_IP:5000"; // ← Change to your server IP
```

### 3. Android App Setup

**Build the APK:**
```bash
cd android
./gradlew assembleDebug  # On Windows: gradlew.bat assembleDebug
```

The APK will be at: `android/app/build/outputs/apk/debug/app-debug.apk`

**Android Configuration** (`MainActivity.java` lines 17-20):
```java
// ── CONFIG ─────────────────────────────────────────────────────────────────
private static final String SERVER_URL = "http://YOUR_SERVER_IP:5000";  // ← Your server IP
private static final String SECRET_TOKEN = "my-secret-token-123";       // ← Must match server.py
private static final int POLL_INTERVAL_MS = 5000; // poll every 5 seconds
// ──────────────────────────────────────────────────────────────────────────
```

**Installation Steps:**
1. Transfer `app-debug.apk` to your Android device
2. Enable "Install from unknown sources" in settings
3. Install the APK
4. Grant SMS permission when prompted
5. The app will start polling automatically

## 🔧 Configuration Guide

### Server IP Address
Find your local IP address:
- **Windows**: `ipconfig` → Look for "IPv4 Address"
- **Mac/Linux**: `ifconfig` or `ip addr`
- Use this IP in both `index.html` and `MainActivity.java`

### Changing Security Token
For production use, change the secret token:

1. **Server** (`server.py` line 12):
   ```python
   SECRET_TOKEN = "your-strong-random-token-here"
   ```

2. **Android App** (`MainActivity.java` line 19):
   ```java
   private static final String SECRET_TOKEN = "your-strong-random-token-here";
   ```

3. **Rebuild** the Android app after changing

### Network Security (Android)
The app includes network security config (`android/app/src/main/res/xml/network_security_config.xml`) allowing HTTP traffic to:
- Localhost (127.0.0.1, localhost, 10.0.2.2)
- Private networks (192.168.*, 172.16.*, 10.*)

**For production:** Use HTTPS and update the network security config.

### Local Testing with Public URL

To read more and fully configure the Tunnel : https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/
Android restricts HTTP traffic for security. For local testing with a public URL:

**Option 1: Cloudflare Tunnel (Recommended)**
```bash
# Install Cloudflare Tunnel for windows 66 bit
curl -L --output cloudflared.exe https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.msi

# Authenticate
cloudflared.exe service install eytoken

Cloudflare Tunnel provides HTTPS URLs automatically with no headers required.

**Option 2: Local Network**
- Ensure Android device and server are on same Wi-Fi network
- Use local IP address (192.168.x.x)
- Network security config already allows private network HTTP

**Option 3: Android Emulator**
- Use `10.0.2.2` for localhost from Android emulator
- Built-in network security config supports this

## 📡 API Documentation

### Base URL
`http://YOUR_SERVER_IP:5000`

### Endpoints

#### 1. Submit SMS Message
**`POST /send`** - Web dashboard submits new SMS

**Request:**
```json
{
  "to": "+1234567890",
  "body": "Hello from SMS Bridge!"
}
```

**Response:**
```json
{
  "ok": true,
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**No authentication required** - Open for web interface

---

#### 2. Poll for Pending Messages
**`GET /poll`** - Android app polls for messages to send

**Headers:**
```
X-Token: your-secret-token-here
```

**Response:**
```json
{
  "messages": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "to": "+1234567890",
      "body": "Hello from SMS Bridge!",
      "created_at": "2024-01-01T12:00:00.000Z",
      "status": "pending"
    }
  ]
}
```

**Authentication required** - Android app only

---

#### 3. Confirm Message Delivery
**`POST /confirm/{message_id}`** - Android app confirms SMS was sent

**Headers:**
```
X-Token: your-secret-token-here
```

**Request Body:** (empty JSON)
```json
{}
```

**Response:**
```json
{
  "ok": true
}
```

**Authentication required** - Android app only

---

#### 4. Get Message Log
**`GET /log`** - Web dashboard fetches all messages

**Response:**
```json
{
  "log": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "to": "+1234567890",
      "body": "Hello from SMS Bridge!",
      "created_at": "2024-01-01T12:00:00.000Z",
      "status": "sent",
      "sent_at": "2024-01-01T12:00:05.000Z"
    }
  ]
}
```

**No authentication required** - Read-only access

## 📱 Android App Details

### Permissions Required
- `SEND_SMS` - To send text messages
- `INTERNET` - To communicate with server
- `ACCESS_NETWORK_STATE` - To check network connectivity

### App Behavior
1. **Starts automatically** after installation
2. **Requests SMS permission** on first launch
3. **Polls server** every 5 seconds for new messages
4. **Sends SMS** immediately when messages are available
5. **Confirms delivery** back to server
6. **Logs activity** in app interface
7. **Runs in background** until stopped

### Building from Source
```bash
cd android
# Debug build
./gradlew assembleDebug

# Release build (signed)
./gradlew assembleRelease
```

## 🌐 Web Dashboard Features

### Interface Components
1. **Status Indicator** - Shows server connection status
2. **SMS Form** - Phone number and message input
3. **Message Log** - History of all sent/pending messages
4. **Setup Instructions** - Configuration guide

### Auto-refresh
- Status check: Every 5 seconds
- Log update: Every 5 seconds
- Real-time feedback on message delivery


## 🐛 Troubleshooting

### Common Issues

#### 1. "Server unreachable" in Android app
- Check server is running: `python server.py`
- Verify IP address in `MainActivity.java`
- Check firewall allows port 5000

#### 2. "Cleartext HTTP traffic not permitted"
- Android 9+ blocks HTTP by default for security
- **Solution 1**: Use local network IP (192.168.x.x) - network security config allows this
- **Solution 2**: Use Cloudflare Tunnel for HTTPS public URL (recommended for testing)
- **Solution 3**: Add `android:usesCleartextTraffic="true"` to AndroidManifest.xml (not recommended)
- **Note**: Avoid ngrok as it requires special headers; Cloudflare Tunnel works without headers

#### 3. SMS not sending
- Check Android app has SMS permission
- Verify phone has SMS capability
- Ensure phone number format is correct (+country code)
- Check carrier SMS limits

#### 4. Build errors
- Ensure Java JDK is installed
- Check Android SDK paths
- Clean build: `./gradlew clean assembleDebug`

### Logs & Debugging
- **Server logs**: Printed in terminal running `server.py`
- **Android logs**: Use `adb logcat` or Android Studio Logcat
- **Web console**: Browser Developer Tools (F12)

## 📁 Project Structure

```
sms-bridge/
├── README.md                   # This file
├── index.html                  # Web dashboard
├── server.py                   # Python backend
├── android/                    # Android app
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/smsbridge/app/
│   │   │   │   └── MainActivity.java  # Android app source
│   │   │   ├── res/
│   │   │   │   ├── layout/activity_main.xml
│   │   │   │   ├── values/strings.xml
│   │   │   │   └── xml/network_security_config.xml
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   ├── build.gradle
│   └── gradlew
└── android/app/build/outputs/apk/debug/
    └── app-debug.apk           # Built APK
```

## 🔄 Workflow Example

1. **Start server**: `python server.py`
2. **Open dashboard**: `open index.html` (or double-click)
3. **Install Android app**: Transfer and install APK
4. **Send test SMS**:
   - Enter phone number: `+1234567890`
   - Enter message: `Test from SMS Bridge`
   - Click "Send via Phone"
5. **Watch delivery**:
   - Web dashboard shows "Queued!"
   - Android app polls and sends SMS
   - Log updates to "sent" status

## 📞 Support

### Issues
1. Check [Troubleshooting](#-troubleshooting) section
2. Review server logs for errors
3. Verify all configurations match

### Contributing
1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📄 License

This project is open source. Feel free to modify and distribute.

## ⚠️ Disclaimer

- Use responsibly and comply with local SMS regulations
- Obtain consent before sending messages
- Respect privacy and anti-spam laws
- Not responsible for misuse of this software

---

**Happy SMS Bridging!** 📱➡️💻➡️📱
