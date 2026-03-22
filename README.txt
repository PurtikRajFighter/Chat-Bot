================================================================================
                        AI CHATBOT — LOCAL-FIRST APPLICATION
                            Complete Setup & Run Guide
================================================================================

This is a fully local AI chatbot with:
  - Spring Boot backend (Java)
  - React + Vite frontend
  - Excel-based storage (no database needed)
  - Razorpay test payments
  - JWT authentication
  - Local AI via Ollama (or OpenAI-compatible API)

================================================================================
                              IMPORTANT GUIDE FILES
================================================================================

  README.txt              ← You are here — how to run the app
  API_KEYS_GUIDE.txt      ← Step-by-step: how to get Ollama, Razorpay, OpenAI keys
  CONFIGURATION_GUIDE.txt ← Every setting explained: tokens, prices, modes, AI, etc.

  READ THESE TWO FILES BEFORE DOING ANYTHING ELSE.

================================================================================
                              PROJECT STRUCTURE
================================================================================

  Chat Bot/
  ├── README.txt                  ← You are here
  ├── API_KEYS_GUIDE.txt          ← How to get every API key (Ollama/Razorpay/OpenAI)
  ├── CONFIGURATION_GUIDE.txt     ← Every configurable setting explained in detail
  ├── README.md                   ← Markdown version of docs
  ├── start.sh                    ← One-command launcher (macOS/Linux)
  ├── backend/                    ← Spring Boot (Java 17)
  │   ├── pom.xml
  │   └── src/main/
  │       ├── java/com/chatbot/
  │       │   ├── controller/     AuthController, ChatController, PaymentController
  │       │   ├── service/        ExcelService, ChatService, TokenService, SchedulerService
  │       │   ├── model/          User, Chat, Transaction
  │       │   ├── security/       JwtUtil, JwtFilter, SecurityConfig
  │       │   ├── dto/            AuthRequest, ChatRequest, PaymentRequest
  │       │   └── config/         SchedulerConfig
  │       └── resources/
  │           └── application.properties   ← ALL config lives here
  ├── frontend/                   ← React + Vite
  │   ├── package.json
  │   ├── vite.config.js
  │   └── src/
  │       ├── pages/              AuthPage, ChatPage
  │       ├── components/         ChatWindow, ModeSelector, TokenDashboard
  │       ├── context/            AuthContext
  │       └── api/                axios.js
  └── data/                       ← Excel storage (auto-created on first run)
      ├── users.xlsx
      ├── chats.xlsx
      └── transactions.xlsx

================================================================================
                         PREREQUISITES — INSTALL THESE FIRST
================================================================================

You need the following installed on the system before running the project:

  1. JAVA JDK 17 or higher
     -----------------------------------------------------------------------
     Check: java -version
     Install from: https://adoptium.net   (choose "Temurin 17" or "21")

     macOS (Homebrew):   brew install temurin@17
     Ubuntu/Debian:      sudo apt install openjdk-17-jdk
     Windows:            Download installer from https://adoptium.net

  2. APACHE MAVEN 3.8+
     -----------------------------------------------------------------------
     Check: mvn -version
     Install from: https://maven.apache.org/download.cgi

     macOS (Homebrew):   brew install maven
     Ubuntu/Debian:      sudo apt install maven
     Windows:            Download zip, extract, add bin/ to PATH

  3. NODE.JS 18+ (includes npm)
     -----------------------------------------------------------------------
     Check: node -v   and   npm -v
     Install from: https://nodejs.org  (choose "LTS" version)

     macOS (Homebrew):   brew install node
     Ubuntu/Debian:      sudo apt install nodejs npm
     Windows:            Download installer from https://nodejs.org

  4. OLLAMA (optional — only if you want to use a local AI model)
     -----------------------------------------------------------------------
     Install from: https://ollama.ai
     macOS:   Download the .dmg from https://ollama.ai/download
     Linux:   curl -fsSL https://ollama.ai/install.sh | sh
     Windows: Download installer from https://ollama.ai/download

     After installing, pull a model:
       ollama pull llama3.2    (recommended, ~2GB)
       OR: ollama pull mistral, ollama pull gemma2, etc.

  NOTE: If you do NOT want to use Ollama, you can use OpenAI or any
        OpenAI-compatible API (see CONFIGURATION section below).

================================================================================
                         STEP-BY-STEP SETUP ON A NEW SYSTEM
================================================================================

STEP 1 — CLONE / COPY THE PROJECT
----------------------------------------------------------------------
  If cloning from git:
    git clone <your-repo-url>
    cd "Chat Bot"

  If copying manually, make sure the full folder structure is intact.

STEP 2 — CONFIGURE THE APPLICATION
----------------------------------------------------------------------
  The app works OUT OF THE BOX with Ollama (local AI, free, no key needed).
  The ONLY things you MUST configure before running are:
    - Razorpay keys (if you want the payment/buy-tokens feature)
    - AI model (if you want a different AI than Ollama)

  Config file location:
    backend/src/main/resources/application.properties

  DEFAULT AI (already configured — Ollama, local, free):
    ai.provider=ollama
    ai.api.url=http://localhost:11434/v1/chat/completions
    ai.api.key=                      ← intentionally blank, no key needed
    ai.model=llama3.2

  RAZORPAY KEYS (paste your test keys here):
    razorpay.key.id=rzp_test_XXXXXXXXXX      ← replace with your key
    razorpay.key.secret=XXXXXXXXXXXXXXXXXX   ← replace with your secret

  For detailed step-by-step key generation instructions, see:
    → API_KEYS_GUIDE.txt        (how to get Ollama, Razorpay, OpenAI keys)
    → CONFIGURATION_GUIDE.txt   (every setting explained with examples)

STEP 3 — START OLLAMA (only if using local AI)
----------------------------------------------------------------------
  Open a terminal and run:
    ollama serve

  In another terminal (first time only):
    ollama pull llama3.2

  Keep the "ollama serve" terminal running in the background.

STEP 4 — START THE APPLICATION
----------------------------------------------------------------------
  You have TWO options:

  OPTION A — One-command start (macOS/Linux only):
  ................................................
    Open a terminal in the project root and run:
      chmod +x start.sh
      ./start.sh

    This script will:
      - Check all prerequisites
      - Install frontend dependencies automatically
      - Start the Spring Boot backend on port 8080
      - Start the React frontend on port 3000
      - Show a clean status output
      - Handle Ctrl+C to shut both down cleanly

  OPTION B — Start backend and frontend separately:
  .................................................
    TERMINAL 1 — Backend:
      cd backend
      mvn spring-boot:run

      Wait until you see: "Started ChatBotApplication"
      Backend will be available at: http://localhost:8090

    TERMINAL 2 — Frontend (open a NEW terminal window/tab):
      cd frontend
      npm install          ← only needed the first time
      npm run dev

      Frontend will be available at: http://localhost:3000

STEP 5 — OPEN THE APP
----------------------------------------------------------------------
  Open your browser and go to:
    http://localhost:3000

  - Register a new account
  - You'll receive 10 free tokens
  - Start chatting!

================================================================================
                              DO I NEED TWO TERMINALS?
================================================================================

  YES, backend and frontend are two separate processes.

  - BACKEND  (Spring Boot / Java) runs on port 8080
  - FRONTEND (React / Vite)       runs on port 3000

  You need both running at the same time for the app to work.

  EASIEST WAY: Use start.sh (./start.sh) — it starts both with one command
  and manages both processes together. Press Ctrl+C to stop both.

  If you prefer separate terminals (e.g., for debugging logs):
    Terminal 1:  cd backend && mvn spring-boot:run
    Terminal 2:  cd frontend && npm run dev

================================================================================
                              FIRST-TIME RUN NOTES
================================================================================

  - The /data folder and Excel files are created automatically on first run.
    Do NOT create them manually.

  - Maven will download dependencies on first run (~2-5 minutes, needs internet).
    Subsequent runs are fast.

  - npm install only needs internet on the first run (~1-2 minutes).
    Subsequent runs skip this step.

  - The backend may take 15-30 seconds to fully start on the first run.
    The start.sh script waits for it automatically.

================================================================================
                              PORTS USED
================================================================================

  Port 8090 — Spring Boot backend API
  Port 3000 — React frontend (Vite dev server)
  Port 11434 — Ollama AI server (if using local AI)

  If any port is already in use, change server.port in application.properties
  and update target in frontend/vite.config.js to match.

================================================================================
                               API ENDPOINTS
================================================================================

  Method  URL                          Auth   Description
  ------  ---------------------------  -----  --------------------------------
  POST    /api/auth/register           No     Register new user
  POST    /api/auth/login              No     Login, returns JWT token
  GET     /api/auth/me                 Yes    Get current user + token balance
  POST    /api/chat/send               Yes    Send message, get AI reply
  GET     /api/chat/history            Yes    Chat history (?mode=NORMAL)
  POST    /api/payment/create-order    Yes    Create Razorpay order
  POST    /api/payment/verify          Yes    Verify payment + add tokens

  Auth = Yes means: send header  →  Authorization: Bearer <your-jwt-token>

================================================================================
                              TOKEN SYSTEM
================================================================================

  - New users get 10 free tokens on registration (configurable)
  - Each message sent costs 1 token (configurable)
  - Tokens reset every 24 hours automatically (configurable)
  - If tokens reach 0, messages are blocked until reset or purchase
  - Buy tokens via Razorpay: ₹10 = 100 tokens, ₹25 = 250 tokens, etc.

================================================================================
                              CHAT MODES
================================================================================

  NORMAL     — Helpful general-purpose assistant
  FANTASY    — Storytelling & adventure narrator
  COMPANION  — Warm, empathetic emotional chat
  FLIRT      — Playful, charming (tasteful/non-explicit only)

================================================================================
                          RAZORPAY TEST PAYMENT DETAILS
================================================================================

  Use these test credentials in the Razorpay checkout popup:

  Card Number : 4111 1111 1111 1111
  Expiry      : Any future date (e.g., 12/30)
  CVV         : Any 3 digits (e.g., 123)
  OTP         : 1234 (if asked)

  NO real money is charged in test mode.

================================================================================
                              EXCEL STORAGE
================================================================================

  All data is stored in the /data folder as Excel files:

  users.xlsx        — id, username, email, password(hashed), tokens, last_reset_time
  chats.xlsx        — id, user_id, mode, message, role, timestamp
  transactions.xlsx — id, user_id, amount, tokens_added, status, timestamp

  You can open these files in Excel or LibreOffice to view stored data.
  Do NOT edit them while the server is running.

================================================================================
                           SWITCHING AI PROVIDERS
================================================================================

  All changes go in:
    backend/src/main/resources/application.properties

  Ollama (local, free):
    ai.api.url=http://localhost:11434/v1/chat/completions
    ai.api.key=
    ai.model=llama3.2

  OpenAI:
    ai.api.url=https://api.openai.com/v1/chat/completions
    ai.api.key=sk-your-key-here
    ai.model=gpt-4o-mini

  LM Studio (local, free):
    ai.api.url=http://localhost:1234/v1/chat/completions
    ai.api.key=lm-studio
    ai.model=<whatever model you loaded>

  Groq (fast cloud inference, free tier available):
    ai.api.url=https://api.groq.com/openai/v1/chat/completions
    ai.api.key=gsk_your-groq-key-here
    ai.model=llama3-8b-8192

================================================================================
                              WINDOWS USERS
================================================================================

  The start.sh script is for macOS/Linux only.
  On Windows, start each part manually:

  1. Open Command Prompt or PowerShell
  2. Start backend:
       cd backend
       mvn spring-boot:run

  3. Open a NEW Command Prompt or PowerShell window
  4. Start frontend:
       cd frontend
       npm install
       npm run dev

  5. Open browser at http://localhost:3000

================================================================================
                             TROUBLESHOOTING
================================================================================

  Problem: "java: command not found" or "mvn: command not found"
  Fix: Install Java JDK 17+ and Maven, then restart your terminal.
       Make sure JAVA_HOME is set correctly.

  Problem: "node: command not found" or "npm: command not found"
  Fix: Install Node.js from https://nodejs.org and restart your terminal.

  Problem: Backend starts but AI responses fail
  Fix: If using Ollama — make sure "ollama serve" is running in a terminal.
       If using OpenAI — check your API key in application.properties.
       Check backend logs: /tmp/chatbot-backend.log (or your terminal output)

  Problem: Port 8080 or 3000 already in use
  Fix: macOS/Linux:  lsof -ti :8080 | xargs kill -9
       Windows:      netstat -ano | findstr :8080 → taskkill /PID <pid> /F

  Problem: "401 Unauthorized" errors in the frontend
  Fix: Your JWT token may have expired (24h TTL). Log out and log back in.

  Problem: Excel file errors / corrupted data
  Fix: Stop the server, delete the affected .xlsx file in /data,
       restart the server (file will be recreated fresh).

  Problem: Maven downloads slow / failing
  Fix: Check your internet connection. Maven downloads dependencies only once
       and caches them in ~/.m2/repository.

  Problem: "CORS error" in browser console
  Fix: Make sure you're accessing frontend at http://localhost:3000
       (not a different port). Check application.properties CORS setting.

================================================================================
                              CONFIGURATION REFERENCE
================================================================================

  File: backend/src/main/resources/application.properties

  Key                              Default    Description
  ---                              -------    -----------
  server.port                      8080       Backend port
  tokens.default.count             10         Free tokens on registration
  tokens.cost.per.message          1          Tokens per message
  tokens.reset.interval.hours      24         Hours between free resets
  tokens.reset.amount              10         Tokens given on reset
  jwt.expiration.ms                86400000   JWT TTL (24 hours in ms)
  ai.provider                      ollama     'ollama' or 'openai'
  ai.api.url                       (ollama)   AI endpoint URL
  ai.api.key                       (empty)    API key (empty for Ollama)
  ai.model                         llama3.2   Model name
  razorpay.key.id                  (set it)   Razorpay test Key ID
  razorpay.key.secret              (set it)   Razorpay test Key Secret
  payment.tokens.per.rupee         10         Tokens added per ₹1 paid
  excel.users.path                 ../data/   Path to users.xlsx
  excel.chats.path                 ../data/   Path to chats.xlsx
  excel.transactions.path          ../data/   Path to transactions.xlsx

================================================================================
                                    SUMMARY
================================================================================

  Quick steps for a fresh clone:

  1. Install: Java 17+, Maven, Node.js 18+
  2. (Optional) Install Ollama → run: ollama serve && ollama pull llama3.2
  3. Edit application.properties → set AI keys + Razorpay keys
  4. Run: chmod +x start.sh && ./start.sh
  5. Open: http://localhost:3000
  6. Register → Chat → Enjoy!

================================================================================

