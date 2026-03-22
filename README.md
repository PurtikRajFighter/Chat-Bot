# AI ChatBot — Local-First Application

A fully local AI chatbot with Spring Boot backend, React frontend, Excel-based storage, and Razorpay test payments.

---

## 🏗 Project Structure

```
Chat Bot/
├── backend/          # Spring Boot (Java 17)
│   ├── src/main/java/com/chatbot/
│   │   ├── controller/   AuthController, ChatController, PaymentController
│   │   ├── service/      ExcelService, ChatService, TokenService, SchedulerService
│   │   ├── model/        User, Chat, Transaction
│   │   ├── security/     JwtUtil, JwtFilter, SecurityConfig
│   │   ├── dto/          AuthRequest, ChatRequest, PaymentRequest
│   │   └── config/       SchedulerConfig
│   └── src/main/resources/application.properties
├── frontend/         # React + Vite
│   └── src/
│       ├── pages/    AuthPage, ChatPage
│       ├── components/ ChatWindow, ModeSelector, TokenDashboard
│       ├── context/  AuthContext
│       └── api/      axios.js
└── data/             # Excel files (auto-created on first run)
    ├── users.xlsx
    ├── chats.xlsx
    └── transactions.xlsx
```

---

## ⚙️ Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| Node.js | 18+ | https://nodejs.org |
| Ollama (optional) | latest | https://ollama.ai |

---

## 🚀 Quick Start

### 1. Configure the application

Edit `backend/src/main/resources/application.properties`:

```properties
# AI Provider — choose 'ollama' or 'openai'
ai.provider=ollama
ai.api.url=http://localhost:11434/v1/chat/completions
ai.model=llama3.2

# Razorpay test keys — get from https://dashboard.razorpay.com (Test Mode)
razorpay.key.id=rzp_test_XXXXXXXXXX
razorpay.key.secret=XXXXXXXXXXXXXXXXXX
```

### 2. Start Ollama (if using local AI)

```bash
ollama serve
ollama pull llama3.2    # or: llama3, mistral, gemma2, etc.
```

### 3. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

Backend starts at: **http://localhost:8080**

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at: **http://localhost:3000**

---

## 🔑 API Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/auth/register` | ❌ | Register new user |
| POST | `/api/auth/login` | ❌ | Login, returns JWT |
| GET  | `/api/auth/me` | ✅ | Current user + balance |
| POST | `/api/chat/send` | ✅ | Send message, get AI reply |
| GET  | `/api/chat/history` | ✅ | Chat history (`?mode=NORMAL`) |
| POST | `/api/payment/create-order` | ✅ | Create Razorpay order |
| POST | `/api/payment/verify` | ✅ | Verify payment + add tokens |

---

## 💬 Chat Modes

| Mode | Personality |
|------|-------------|
| 🧠 NORMAL | Helpful general assistant |
| 🐉 FANTASY | Storytelling & adventure narrator |
| 🤗 COMPANION | Warm, empathetic emotional chat |
| 💫 FLIRT | Playful, charming (tasteful only) |

---

## 🪙 Token System

- **New users** get **10 free tokens** on registration
- **Each message** costs **1 token**
- **Token reset**: every 24 hours (configurable in `application.properties`)
- **Buy tokens**: ₹10 = 100 tokens, ₹25 = 250 tokens, ₹50 = 500 tokens

---

## 💳 Razorpay Test Payments

Use these test card details in checkout:
- **Card**: 4111 1111 1111 1111
- **Expiry**: Any future date
- **CVV**: Any 3 digits
- **OTP**: 1234 (if asked)

---

## 📊 Excel Storage

Data is stored in `data/` folder (auto-created on first run):

| File | Contents |
|------|----------|
| `users.xlsx` | id, username, email, password(hashed), tokens, last_reset_time |
| `chats.xlsx` | id, user_id, mode, message, role, timestamp |
| `transactions.xlsx` | id, user_id, amount, tokens_added, status, timestamp, razorpay_order_id |

---

## ⚙️ Configuration Reference

All settings in `backend/src/main/resources/application.properties`:

```properties
# Tokens
tokens.default.count=10          # Free tokens on registration
tokens.cost.per.message=1        # Tokens per message
tokens.reset.interval.hours=24   # Hours between resets
tokens.reset.amount=10           # Tokens given on reset

# AI
ai.provider=ollama               # 'ollama' or 'openai'
ai.api.url=http://localhost:11434/v1/chat/completions
ai.api.key=                      # Leave empty for Ollama
ai.model=llama3.2

# Razorpay
razorpay.key.id=rzp_test_...
razorpay.key.secret=...
payment.tokens.per.rupee=10      # 10 tokens per ₹1

# Scheduler
scheduler.token.reset.cron=0 0 * * * *   # Every hour check
```

---

## 🛠 Switching AI Providers

**Ollama (default — fully local, free):**
```properties
ai.api.url=http://localhost:11434/v1/chat/completions
ai.api.key=
ai.model=llama3.2
```

**OpenAI:**
```properties
ai.api.url=https://api.openai.com/v1/chat/completions
ai.api.key=sk-your-key-here
ai.model=gpt-4o-mini
```

**Any OpenAI-compatible API** (LM Studio, Groq, Together.ai, etc.) — just change the URL and key.

---

## 🔒 Security Notes

- Passwords are hashed with **BCrypt**
- JWT tokens expire after **24 hours**
- Tokens cannot go below **0** (server-side guard)
- Razorpay payments verified using **HMAC-SHA256 signature**
- Change `jwt.secret` in production!

