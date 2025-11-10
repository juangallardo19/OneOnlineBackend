# OneOnlineBackend - Key Findings Summary

## Quick Overview

**Project**: ONE Online multiplayer card game backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.5.7
- **Architecture**: Layered (REST API + WebSocket)
- **Database**: PostgreSQL
- **Real-time Communication**: STOMP over WebSocket
- **Authentication**: JWT + OAuth2
- **Code Size**: ~2000 lines in domain models alone
- **Design Patterns**: 11 patterns implemented

---

## ARCHITECTURE AT A GLANCE

```
Layers (Bottom to Top):
1. PostgreSQL Database (User, GameHistory, PlayerStats, GlobalRanking)
2. Repository/DAO (JPA) + Domain Models (In-memory: Room, GameSession, Player)
3. Services (GameManager, RoomManager, GameEngine, WebSocketObserver)
4. Controllers (REST API) + WebSocket Handlers
5. Frontend (React via REST + WebSocket)
```

---

## CRITICAL FLOW: Room → Game → Notifications

### 1. Room Creation
```
POST /api/rooms → RoomManager.createRoom() → GameManager stores
→ WebSocketObserver broadcasts ROOM_CREATED to /topic/lobby
```

### 2. Player Join
```
POST /api/rooms/{code}/join → room.addPlayer() 
→ WebSocketObserver broadcasts PLAYER_JOINED to /topic/room/{code}
```

### 3. Add Bot
```
POST /api/rooms/{code}/bot → room.addBot() (generates playerId, adds to bots list)
→ WebSocketObserver broadcasts PLAYER_JOINED with isBot:true
```

### 4. Start Game (THE CRITICAL PART)
```
POST /api/rooms/{code}/start
    ↓
RoomController.startGame()
    ├─ Verify leader
    ├─ Validate 2+ players
    ├─ Create GameSession
    ├─ GameManager.startGameSession() [stores in memory]
    ├─ session.start() [INITIALIZATION]
    │   ├─ Initialize deck (108 cards)
    │   ├─ Shuffle
    │   ├─ Deal 7 cards to EACH player (humans + bots)
    │   ├─ Setup turnOrder with getAllPlayers()
    │   ├─ Place first card on discard pile
    │   └─ Set currentPlayer = first player
    ├─ room.setStatus(IN_PROGRESS)
    └─ ⚠️ NO WebSocket notification! ← CRITICAL BUG
```

### 5. Data Available in GameSession
```
GameSession {
    sessionId: UUID
    room: Room (reference) ← Access roomCode, roomLeader, config
    currentPlayer: Player (whose turn)
    turnOrder: [Player1, Player2, BotPlayer1] ← ALL players mixed
    mainDeck: Deck (~89 cards left)
    discardPile: Stack [firstCard]
    currentState: PLAYING
}
```

---

## CRITICAL ISSUES FOUND

### 🔴 Issue #1: NO GAME START NOTIFICATION
**File**: `RoomController.java` lines 393-447

**Problem**: When POST /api/rooms/{code}/start returns:
- ✓ GameSession created and initialized
- ✓ Cards dealt to all players
- ✓ Room status updated
- ✗ NO broadcast to WebSocket clients
- ✗ Clients don't know game started
- ✗ Players' hands not synchronized

**Impact**: Frontend must poll to discover game state

**Solution**:
```java
// After session.start() in RoomController.startGame():
webSocketObserver.onGameStarted(session);  // Add this line!
```

This would broadcast to `/topic/game/{sessionId}`:
```json
{
  "eventType": "GAME_STARTED",
  "data": {
    "sessionId": "...",
    "startingPlayer": "player-id",
    "direction": "CLOCKWISE",
    "players": [...]
  }
}
```

---

### 🔴 Issue #2: PLAYER HANDS NOT BROADCAST
**File**: `GameSession.java` lines 100-106

**Problem**: Cards are dealt but never sent to clients:
```java
for (Player player : room.getAllPlayers()) {
    for (int i = 0; i < cardCount; i++) {
        player.drawCard(mainDeck.drawCard());  // ← Cards in memory only!
    }
}
```

**Impact**: Each client must call `GET /api/game/{sessionId}/state` to get hand

**Solution**: Send hand to each player in `onGameStarted()`:
```java
for (Player player : session.getPlayers()) {
    messagingTemplate.convertAndSendToUser(
        player.getPlayerId(),
        "/queue/notification",
        buildGameStarted(session, player.getHand())
    );
}
```

---

### 🟡 Issue #3: MISSING TURN CHANGE NOTIFICATIONS
**File**: `GameSession.java` lines 232-244

**Problem**: `nextTurn()` method doesn't notify observers:
```java
public void nextTurn() {
    Player lastPlayer = turnOrder.removeFirst();
    turnOrder.addLast(lastPlayer);
    currentPlayer = turnOrder.getFirst();
    turnStartTime = System.currentTimeMillis();
    // ← NO observer notification!
}
```

**Impact**: Clients can't detect whose turn it is without polling

**Solution**: Add observer list to GameSession and notify:
```java
observers.forEach(obs -> obs.onTurnChanged(currentPlayer, this));
```

---

### 🟡 Issue #4: NO DISCONNECT/RECONNECTION HANDLING
**File**: `GameSession.java`, `WebSocketGameController.java`

**Problem**: No code detects disconnections or handles reconnections
- `Player.connected` field exists but never set to false
- No listener for WebSocket disconnections
- No temp bot replacement for disconnected players
- No timeout for reconnection attempts

**Solution**: Implement WebSocket session listeners to:
1. Detect disconnection
2. Create temporary bot
3. Queue reconnection attempt
4. Restore player if reconnected in time

---

### 🟡 Issue #5: GAMEMANAGER SINGLETON VS SPRING BEAN CONFLICT
**File**: `GameManager.java` lines 36-75

**Problem**: GameManager is BOTH a Singleton AND a Spring @Service:
```java
@Service  // ← Spring manages
public class GameManager {
    // ... Bill Pugh Singleton pattern
    public static GameManager getInstance() {  // ← Manual singleton
        return SingletonHolder.INSTANCE;
    }
}
```

**Impact**: Two instances of GameManager may exist!
- Spring's autowired instance
- Static singleton instance
- Room data might be in wrong instance

**Solution**: Choose ONE approach:
- Option A: Remove @Service, use pure Singleton
- Option B: Remove Singleton, use Spring @Autowired everywhere

---

### 🟡 Issue #6: ROOM CODE VS ROOM ID INCONSISTENCY
**Files**: `RoomController.java:480`, `WebSocketObserver.java:234`

**Problem**: Two identifiers used inconsistently:
- roomId: UUID (long, unique)
- roomCode: String 6-char (short, user-friendly)

```
✓ CORRECT: /api/rooms/{code}/join
✗ WRONG: RoomResponse.roomId = roomCode (confusing!)
```

**Solution**: Use roomCode for all client-facing operations, roomId internally

---

### 🟡 Issue #7: BOTS NOT DISTINGUISHED FROM HUMANS IN TURN ORDER
**File**: `GameSession.java` lines 109-111

**Problem**: turnOrder mixes humans and bots:
```java
turnOrder.addAll(room.getAllPlayers());  // ← Could be human or bot
```

Later code assumes:
```java
player instanceof BotPlayer  // ← Must check type each time
```

**Better approach**: Keep separate lists or add type field

---

## ROOM → GAME DATA FLOW VERIFICATION

### What Gets Copied From Room to GameSession ✓
```
✓ room reference (full Room object)
✓ All players from room.getAllPlayers() → turnOrder
✓ Game config from room.getConfig()
✓ Game rules for card validation
```

### What's Missing or Problematic ✗
```
✗ No GAME_STARTED notification to clients
✗ No player hands broadcast
✗ No turn change notifications
✗ Room leader not preserved in session
✗ No reconnection support
```

---

## WEBSOCKET MESSAGE ROUTING

### Subscriptions (Client → Server)
```
/app/game/{sessionId}/play-card      ← Play a card
/app/game/{sessionId}/draw-card      ← Draw from deck
/app/game/{sessionId}/call-uno       ← Call UNO
/app/game/{sessionId}/chat           ← Chat message
/app/game/{sessionId}/join           ← Player joined
/app/game/{sessionId}/leave          ← Player leaving
```

### Broadcasts (Server → All Players)
```
/topic/game/{sessionId}              ← Game state updates
/topic/room/{roomCode}               ← Room events
/topic/lobby                         ← Public room listings
```

### Point-to-Point (Server → Specific Player)
```
/user/{playerId}/queue/notification  ← Personal messages
/user/{playerId}/queue/errors        ← Error messages
```

---

## DESIGN PATTERNS IMPLEMENTED

| Pattern | Class | Purpose |
|---------|-------|---------|
| Singleton | GameManager | Single instance managing all rooms |
| Builder | RoomBuilder, GameConfigBuilder | Complex object creation |
| Factory | CardFactory, BotFactory | Card/bot instantiation |
| Observer | GameObserver, WebSocketObserver | Loose coupling |
| Strategy | BotStrategy | Different AI behaviors |
| Command | PlayCardCommand | Undo/redo functionality |
| State | GameState | Game state transitions |
| Adapter | BotPlayerAdapter | Interface adaptation |
| Decorator | CardDecorator | Add behaviors dynamically |
| Prototype | GameStatePrototype | Copy game state |
| MVC | Spring Controllers | Separation of concerns |

---

## KEY FILES FOR DEBUGGING

| File | Lines | Purpose |
|------|-------|---------|
| RoomController.java | 500 | REST endpoints for rooms |
| GameSession.java | 423 | Game state management |
| Room.java | 424 | Room container |
| WebSocketObserver.java | 340 | Real-time notifications |
| GameManager.java | 302 | Singleton room/session store |
| RoomManager.java | 427 | Room lifecycle |
| WebSocketGameController.java | 361 | WebSocket handlers |
| WebSocketConfig.java | 99 | STOMP configuration |

---

## TEST CHECKLIST

- [ ] Create room → roomCode generated
- [ ] Join room → player added to players list
- [ ] Add bot → BotPlayer created, added to bots list
- [ ] Start game → GameSession created, cards dealt
- [ ] All players in turnOrder → humans + bots included
- [ ] Each player has 7 cards → correct initial hand
- [ ] Game started WebSocket notification sent → **FAILING**
- [ ] Player hand received by each player → **FAILING**
- [ ] Play card → card removed, added to discard
- [ ] Next turn → turn advances correctly
- [ ] Bot plays → chooseCard() works
- [ ] Disconnect detected → player.connected set to false
- [ ] Temp bot created → replacement for disconnected
- [ ] Reconnect → player restored from temp bot
- [ ] Chat messages → broadcast correctly

---

## DATABASE SCHEMA

```sql
-- User accounts (JPA Entity)
User {
    id: BIGINT PRIMARY KEY
    email: VARCHAR UNIQUE
    username: VARCHAR
    password: VARCHAR (hashed)
    created_at: TIMESTAMP
}

-- Game history (JPA Entity)
GameHistory {
    id: BIGINT PRIMARY KEY
    user_id: BIGINT FK
    room_code: VARCHAR
    game_date: TIMESTAMP
    duration_seconds: INT
    winner_id: BIGINT
    player_count: INT
}

-- Player statistics (JPA Entity)
PlayerStats {
    id: BIGINT PRIMARY KEY
    user_id: BIGINT FK UNIQUE
    total_games: INT
    wins: INT
    losses: INT
    total_cards_played: INT
}

-- Global ranking (JPA Entity)
GlobalRanking {
    id: BIGINT PRIMARY KEY
    user_id: BIGINT FK
    rank: INT
    rating: DOUBLE
    last_updated: TIMESTAMP
}
```

Note: Room, GameSession, Player, Card are NOT persisted to DB
- Stored in-memory in GameManager
- Lost on server restart
- Could be persisted for recovery

---

## PERFORMANCE CONSIDERATIONS

### Memory Usage
- Each room: ~5-10 KB (metadata)
- Each game session: ~50-100 KB (108 cards, 4 players)
- Active rooms limit: ~1000 (estimate)
- Active sessions limit: ~500 (estimate)

### Scalability Issues
- GameManager is single-threaded (ConcurrentHashMap is fine)
- WebSocket broadcast to all players O(n) where n = players
- Room cleanup happens manually (removeInactiveRooms())
- No rate limiting on API endpoints

### Optimization Opportunities
- Cache public rooms list
- Implement async notifications
- Add message queue for broadcasts
- Database persistence for recovery

---

## DEPLOYMENT NOTES

### Environment Variables Required
```
DATABASE_URL=postgresql://...
DATABASE_USER=postgres
DATABASE_PASSWORD=***
JWT_SECRET=<base64-encoded-string>
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
FRONTEND_URL=https://...
PORT=8080
```

### Docker Support
```dockerfile
# Multi-stage build available
# Compatible with Heroku/Vercel deployment
# Uses gradle wrapper (no gradle installation needed)
```

---

## RECOMMENDATIONS (Priority)

### CRITICAL - Fix Before Production
1. Add GAME_STARTED WebSocket notification
2. Broadcast player hands on game start
3. Implement disconnect/reconnection handling
4. Add turn change notifications

### HIGH - Important Features
5. Fix GameManager Singleton vs Spring Bean conflict
6. Implement persistent game state recovery
7. Add rate limiting to API endpoints
8. Implement reconnection queue with timeout

### MEDIUM - Nice to Have
9. Add game metrics/analytics
10. Implement difficulty levels for bots
11. Add spectator mode
12. Implement game pause/resume

---

## RELATED FILES

Full analysis available in: `/home/user/OneOnlineBackend/CODEBASE_ANALYSIS.md`
- 1513 lines
- Sections 1-15
- Covers architecture, data flows, issues, and solutions

