from flask import Flask, request, jsonify
from flask_cors import CORS
import uuid
import time
from datetime import datetime

app = Flask(__name__)
CORS(app)

# In-memory queue — Android app polls this
pending_messages = []
sent_log = []

SECRET_TOKEN = "my-secret-token-123"  # Change this! Must match Android app config

def auth(req):
    return req.headers.get("X-Token") == SECRET_TOKEN

# ── Web Dashboard calls this ──────────────────────────────────────────────────
@app.route("/send", methods=["POST"])
def send():
    data = request.json
    to = data.get("to", "").strip()
    body = data.get("body", "").strip()

    if not to or not body:
        return jsonify({"error": "Missing 'to' or 'body'"}), 400

    msg = {
        "id": str(uuid.uuid4()),
        "to": to,
        "body": body,
        "created_at": datetime.utcnow().isoformat(),
        "status": "pending"
    }
    pending_messages.append(msg)
    return jsonify({"ok": True, "id": msg["id"]}), 200


# ── Android App polls this ────────────────────────────────────────────────────
@app.route("/poll", methods=["GET"])
def poll():
    if not auth(request):
        return jsonify({"error": "Unauthorized"}), 401
    msgs = [m for m in pending_messages if m["status"] == "pending"]
    return jsonify({"messages": msgs}), 200


# ── Android App confirms delivery ─────────────────────────────────────────────
@app.route("/confirm/<msg_id>", methods=["POST"])
def confirm(msg_id):
    if not auth(request):
        return jsonify({"error": "Unauthorized"}), 401
    for m in pending_messages:
        if m["id"] == msg_id:
            m["status"] = "sent"
            m["sent_at"] = datetime.utcnow().isoformat()
            sent_log.append(m)
            pending_messages.remove(m)
            return jsonify({"ok": True}), 200
    return jsonify({"error": "Not found"}), 404


# ── Web Dashboard fetches log ─────────────────────────────────────────────────
@app.route("/log", methods=["GET"])
def log():
    all_msgs = sent_log + [m for m in pending_messages]
    return jsonify({"log": sorted(all_msgs, key=lambda x: x["created_at"], reverse=True)}), 200


if __name__ == "__main__":
    print("SMS Bridge running on http://0.0.0.0:5000")
    app.run(host="0.0.0.0", port=5000, debug=True)
