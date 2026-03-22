#!/bin/bash
# start.sh — Start backend (Spring Boot) + frontend (React/Vite) as one unit
# Usage:  ./start.sh
# Stop:   Ctrl+C


RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
BLUE='\033[0;34m'; MAGENTA='\033[0;35m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PORT=8090
FRONTEND_PORT=3000

echo ""
echo -e "${BOLD}${CYAN}  🤖  AI ChatBot — Local Server${NC}"
echo -e "${CYAN}  ─────────────────────────────${NC}"
echo ""

# ---- Prerequisite checks ----
check_cmd() {
  if ! command -v "$1" &> /dev/null; then
    echo -e "${RED}❌ '$1' not found. $2${NC}"; exit 1
  fi
}
check_cmd java  "Install JDK 17+: https://adoptium.net"
check_cmd mvn   "Install Maven:   https://maven.apache.org"
check_cmd node  "Install Node.js: https://nodejs.org"
check_cmd npm   "Install Node.js: https://nodejs.org"

JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
  echo -e "${RED}❌ Java 17+ required (found Java $JAVA_VER)${NC}"; exit 1
fi

# ---- Free ports if occupied (no prompts) ----
free_port() {
  local PIDS
  PIDS=$(lsof -ti :"$1" 2>/dev/null)
  if [ -n "$PIDS" ]; then
    echo -e "${YELLOW}⚠️  Port $1 in use — freeing it...${NC}"
    echo "$PIDS" | xargs kill -9 2>/dev/null
    sleep 1
    echo -e "${GREEN}✓ Port $1 freed${NC}"
  fi
}
free_port $BACKEND_PORT
free_port $FRONTEND_PORT

# ---- Data dir ----
mkdir -p "$SCRIPT_DIR/data"
echo -e "${GREEN}✓ Data directory ready${NC}"

# ---- Frontend deps ----
if [ ! -d "$SCRIPT_DIR/frontend/node_modules" ]; then
  echo -e "${YELLOW}📦 Installing frontend dependencies...${NC}"
  cd "$SCRIPT_DIR/frontend" && npm install
fi

# ---- Named pipes for live log streaming ----
BACKEND_PIPE=/tmp/chatbot-backend-pipe
FRONTEND_PIPE=/tmp/chatbot-frontend-pipe
rm -f "$BACKEND_PIPE" "$FRONTEND_PIPE"
mkfifo "$BACKEND_PIPE"
mkfifo "$FRONTEND_PIPE"

# Stream backend logs with prefix
while IFS= read -r line; do
  echo -e "${BLUE}[BACKEND]${NC}  $line"
done < "$BACKEND_PIPE" &
BACKEND_STREAM_PID=$!

# Stream frontend logs with prefix
while IFS= read -r line; do
  echo -e "${MAGENTA}[FRONTEND]${NC} $line"
done < "$FRONTEND_PIPE" &
FRONTEND_STREAM_PID=$!

# ---- Start Backend ----
echo ""
echo -e "${CYAN}🚀 Starting backend (Spring Boot) on port $BACKEND_PORT...${NC}"
cd "$SCRIPT_DIR/backend"
mvn spring-boot:run > "$BACKEND_PIPE" 2>&1 &
BACKEND_PID=$!

# Wait until backend is actually up
echo -e "${YELLOW}   Waiting for backend to be ready...${NC}"
for i in $(seq 1 90); do
  sleep 1
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$BACKEND_PORT/api/payment/packages 2>/dev/null)
  if [ "$STATUS" = "200" ]; then
    echo -e "${GREEN}✓ Backend is up on http://localhost:$BACKEND_PORT${NC}"
    break
  fi
  if ! kill -0 $BACKEND_PID 2>/dev/null; then
    echo -e "${RED}❌ Backend process died. Check logs above.${NC}"
    kill $BACKEND_STREAM_PID $FRONTEND_STREAM_PID 2>/dev/null
    rm -f "$BACKEND_PIPE" "$FRONTEND_PIPE"
    exit 1
  fi
  if [ $i -eq 90 ]; then
    echo -e "${RED}❌ Backend did not respond after 90 seconds. Check logs above.${NC}"
    kill $BACKEND_PID $BACKEND_STREAM_PID $FRONTEND_STREAM_PID 2>/dev/null
    rm -f "$BACKEND_PIPE" "$FRONTEND_PIPE"
    exit 1
  fi
done

# ---- Start Frontend ----
echo ""
echo -e "${CYAN}🚀 Starting frontend (React/Vite) on port $FRONTEND_PORT...${NC}"
cd "$SCRIPT_DIR/frontend"
npx vite --port $FRONTEND_PORT > "$FRONTEND_PIPE" 2>&1 &
FRONTEND_PID=$!
sleep 3

echo ""
echo -e "${BOLD}${GREEN}  ✅ ChatBot is running!${NC}"
echo ""
echo -e "  ${BOLD}Open:${NC}    http://localhost:$FRONTEND_PORT"
echo -e "  ${BOLD}API:${NC}     http://localhost:$BACKEND_PORT/api"
echo -e "  ${BOLD}Data:${NC}    $SCRIPT_DIR/data/"
echo ""
echo -e "${YELLOW}  ⚙️  Config: backend/src/main/resources/application.properties${NC}"
echo -e "${YELLOW}  📖 Keys:   API_KEYS_GUIDE.txt${NC}"
echo ""
echo -e "${CYAN}  Press Ctrl+C to stop both servers.${NC}"
echo ""

# ---- Clean shutdown ----
cleanup() {
  echo ""
  echo -e "${YELLOW}Stopping servers...${NC}"
  kill $BACKEND_PID $FRONTEND_PID $BACKEND_STREAM_PID $FRONTEND_STREAM_PID 2>/dev/null
  pkill -f "spring-boot:run" 2>/dev/null
  pkill -f "vite" 2>/dev/null
  rm -f "$BACKEND_PIPE" "$FRONTEND_PIPE"
  echo -e "${GREEN}Done.${NC}"
  exit 0
}
trap cleanup INT TERM

wait $BACKEND_PID $FRONTEND_PID
