# OneOnlineBackend - Complete Codebase Analysis

## Executive Summary

**Project**: ONE Online Backend  
**Type**: Spring Boot REST API with WebSocket support  
**Architecture**: Service-oriented with design patterns  
**Authentication**: JWT + OAuth2 (Google, GitHub)  
**Database**: PostgreSQL  
**Real-time Communication**: WebSocket (STOMP over SockJS)  
**Deployment**: Docker-ready (Heroku/Vercel compatible)

---

## 1. PROJECT STRUCTURE & ORGANIZATION

### Directory Hierarchy
```
src/main/java/com/oneonline/backend/
├── config/                          # Spring configuration
│   ├── CorsConfig.java             # Cross-origin resource sharing
│   ├── OAuth2Config.java           # OAuth2 authentication setup
│   ├── SecurityConfig.java         # JWT & endpoint security
│   └── WebSocketConfig.java        # STOMP/WebSocket configuration
│
├── controller/                      # REST API endpoints
│   ├── AuthController.java         # Login, register, refresh tokens
│   ├── GameController.java         # Game actions (play, draw, uno)
│   ├── RankingController.java      # Leaderboard queries
│   ├── RoomController.java         # Room management (create, join, leave)
│   └── WebSocketGameController.java # Real-time game events
│
├── model/
│   ├── domain/                     # In-memory domain objects (not JPA)
│   │   ├── Room.java               # Game room container
│   │   ├── GameSession.java        # Active game instance
│   │   ├── Player.java             # Player state (human)
│   │   ├── BotPlayer.java          # AI player
│   │   ├── Card.java               # Card types hierarchy
│   │   ├── GameConfiguration.java  # Game rules & settings
│   │   ├── Deck.java               # Card deck management
│   │   └── ...                     # Other card types
│   │
│   ├── entity/                     # JPA entities (PostgreSQL)
│   │   ├── User.java               # User accounts
│   │   ├── GameHistory.java        # Completed games
│   │   ├── PlayerStats.java        # Player statistics
│   │   └── GlobalRanking.java      # Leaderboard data
│   │
│   └── enums/
│       ├── RoomStatus.java         # WAITING, STARTING, IN_PROGRESS, FINISHED
│       ├── GameStatus.java         # LOBBY, DEALING_CARDS, PLAYING, PAUSED, GAME_OVER
│       ├── PlayerStatus.java       # Player states
│       └── CardColor.java          # RED, YELLOW, GREEN, BLUE, WILD
│
├── service/                         # Business logic layer
│   ├── game/
│   │   ├── GameManager.java        # Singleton: manages all rooms & sessions
│   │   ├── RoomManager.java        # Room lifecycle & player management
│   │   ├── GameEngine.java         # Game logic orchestrator
│   │   ├── CardValidator.java      # Move validation
│   │   ├── EffectProcessor.java    # Card effect application
│   │   ├── TurnManager.java        # Turn order & sequence
│   │   └── OneManager.java         # UNO call & penalties
│   │
│   ├── auth/
│   │   ├── AuthService.java        # Login & registration
│   │   ├── JwtService.java         # Token generation & validation
│   │   ├── UserService.java        # User profile management
│   │   └── OAuth2Service.java      # Third-party authentication
│   │
│   ├── bot/
│   │   ├── BotFactory.java         # Bot creation
│   │   └── BotStrategy.java        # AI decision-making
│   │
│   └── ranking/
│       ├── RankingService.java     # Leaderboard data
│       └── StatsService.java       # Player statistics
│
├── pattern/                         # Design patterns (11 patterns)
│   ├── behavioral/
│   │   ├── observer/               # Observer: WebSocket notifications
│   │   │   ├── GameObserver.java  # Interface
│   │   │   └── WebSocketObserver.java # Implementation
│   │   ├── command/                # Command: Move history for undo
│   │   └── state/                  # State: Game state transitions
│   │
│   ├── creational/
│   │   ├── singleton/              # Singleton: GameManager
│   │   ├── builder/                # Builder: RoomBuilder, GameConfigBuilder
│   │   ├── factory/                # Factory: CardFactory, BotFactory
│   │   └── prototype/              # Prototype: GameStatePrototype
│   │
│   └── structural/
│       ├── adapter/                # Adapter: BotPlayerAdapter
│       └── decorator/              # Decorator: CardDecorator, PowerUpDecorator
│
├── dto/
│   ├── request/
│   │   ├── CreateRoomRequest.java
│   │   ├── JoinRoomRequest.java
│   │   ├── PlayCardRequest.java
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   │
│   └── response/
│       ├── RoomResponse.java       # Room + players + config
│       ├── GameStateResponse.java  # Game state for sync
│       ├── AuthResponse.java       # JWT token & user info
│       └── ...
│
├── repository/                      # JPA data access
│   └── UserRepository.java
│
├── security/
│   ├── JwtAuthFilter.java          # JWT validation filter
│   └── OAuth2SuccessHandler.java   # OAuth2 callback handler
│
├── exception/
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice
│   ├── GameNotFoundException.java
│   ├── RoomNotFoundException.java
│   ├── UnauthorizedException.java
│   └── ...
│
├── datastructure/
│   ├── CircularDoublyLinkedList.java # For turn order
│   └── ...
│
├── util/
│   ├── CodeGenerator.java          # Room code & player ID generation
│   └── JwtUtil.java
│
└── BackendApplication.java         # Main Spring Boot class
```

---

## 2. CORE DOMAIN MODELS & RELATIONSHIPS

### Room (Game Container)
```java
Room {
    roomId: UUID                           // Unique identifier
    roomCode: String (6 chars, uppercase)  // Human-readable code (ABC123)
    roomName: String                       // Optional display name
    roomLeader: Player                     // Room creator/manager
    players: List<Player>                  // Human players (max 4 total with bots)
    bots: List<BotPlayer>                  // AI players (max 3)
    gameSession: GameSession               // Active game (null if waiting)
    config: GameConfiguration             // Game rules
    status: RoomStatus (enum)              // WAITING, STARTING, IN_PROGRESS, FINISHED
    privateRoom: boolean                   // Visibility flag
}
```

### GameSession (Active Game Instance)
```java
GameSession {
    sessionId: UUID                        // Unique session identifier
    room: Room                             // Parent room reference
    mainDeck: Deck                         // Draw pile
    discardPile: Stack<Card>               // Play pile
    turnOrder: LinkedList<Player>          // Players in turn sequence
    currentPlayer: Player                  // Whose turn it is
    currentState: GameStatus               // PLAYING, GAME_OVER, etc.
    clockwise: boolean                     // Direction of play
    pendingDrawCount: int                  // Stacked draw cards
    winner: Player                         // Null until game ends
}
```

### Player (Human Player)
```java
Player {
    playerId: UUID                         // Unique player ID
    userId: Long                           // Reference to User entity (optional)
    userEmail: String                      // For authentication lookup
    nickname: String                       // Display name
    hand: List<Card>                       // Cards in hand
    score: int                             // Game points
    connected: boolean                     // WebSocket connection status
    roomLeader: boolean                    // Can manage room
    status: PlayerStatus                   // WAITING, PLAYING, etc.
    calledOne: boolean                     // Called "UNO" this turn
}
```

### BotPlayer (AI Player - extends Player)
```java
BotPlayer extends Player {
    temporary: boolean                     // Replaces disconnected player?
    originalPlayer: Player                 // For reconnection
    
    // AI Methods
    chooseCard(topCard, session): Card     // Strategic card selection
    chooseColor(): CardColor               // Optimal color for wild
    shouldCallOne(): boolean               // 90% success rate
    executeTurn(topCard, session): boolean // Auto-play turn
}
```

### Key Relationships
```
User (JPA Entity in DB)
  └─ 1:many → GameHistory
  └─ 1:1   → PlayerStats
  └─ 1:1   → GlobalRanking

Room (In-memory, in GameManager)
  ├─ 1:1   → GameConfiguration
  ├─ 1:1   → GameSession (when playing)
  ├─ 1:many → Player (humans)
  └─ 1:many → BotPlayer (AI)

GameSession
  ├─ 1:1   → Room
  ├─ 1:many → Card (in deck & discard)
  └─ 1:many → Player (all game players)
```

---

## 3. ROOM MANAGEMENT FLOW

### Room Creation
```
1. Client: POST /api/rooms (CreateRoomRequest)
   ├─ Authentication required
   └─ Payload: { isPrivate, maxPlayers, turnTimeLimit, ... }

2. RoomController.createRoom()
   ├─ Create Player from authenticated user
   ├─ Build GameConfiguration
   └─ Call RoomManager.createRoom()

3. RoomManager.createRoom()
   ├─ Generate unique 6-char room code
   ├─ Build Room using RoomBuilder pattern
   ├─ Add creator as first player → becomes roomLeader
   ├─ Register room with GameManager.createRoom()
   ├─ NOTIFY: WebSocketObserver.onRoomCreated()
   │         └─ Broadcast to /topic/lobby
   ├─ NOTIFY: WebSocketObserver.onPlayerJoined()
   │         └─ Broadcast to /topic/room/{roomCode}
   └─ Return room

4. Server: ResponseEntity<RoomResponse>
   └─ Maps Room to RoomResponse DTO with all player info
```

### Joining Room
```
1. Client: POST /api/rooms/{code}/join (JoinRoomRequest)
   ├─ Authentication required
   └─ Payload: { nickname }

2. RoomController.joinRoom()
   ├─ Create Player from request
   └─ Call RoomManager.joinRoom()

3. RoomManager.joinRoom()
   ├─ Get room by code from GameManager
   ├─ Validate: room exists, not full, player not duplicate
   ├─ Call room.addPlayer(player)
   │  └─ First player becomes leader
   ├─ NOTIFY: WebSocketObserver.onPlayerJoined()
   │         └─ Send event to /topic/room/{roomCode}
   │            All players see: PLAYER_JOINED event
   └─ Return updated room

4. Server: ResponseEntity<RoomResponse>
```

### Adding Bots
```
1. Client: POST /api/rooms/{code}/bot

2. RoomController.addBot()
   └─ Call RoomManager.addBot(code)

3. RoomManager.addBot()
   ├─ Validate: room not full, max 3 bots
   ├─ Call room.addBot()
   │  ├─ Generate playerId
   │  ├─ Create BotPlayer with nickname "Bot_N"
   │  └─ Add to room.bots list
   ├─ NOTIFY: WebSocketObserver.onPlayerJoined(bot, room)
   │         └─ Send PLAYER_JOINED event with isBot: true
   └─ Return bot

4. All room players receive notification
```

---

## 4. GAME INITIALIZATION & START FLOW

### Starting a Game
```
1. Client: POST /api/rooms/{code}/start
   └─ Only leader can start

2. RoomController.startGame()
   ├─ Verify caller is room leader
   ├─ Validate minimum players (2 including bots)
   ├─ Create GameSession
   │  └─ GameSession.builder()
   │     .room(room)
   │     .build()
   ├─ Register session: GameManager.startGameSession(session)
   ├─ CRITICAL: Initialize game: session.start()
   │  └─ Calls GameSession.start() method
   ├─ Update room status to IN_PROGRESS
   ├─ room.setGameSession(session)
   └─ Return { sessionId, roomCode, status: "STARTED" }

3. GameSession.start() - Critical initialization
   ├─ Verify minimum players (2)
   ├─ currentState = DEALING_CARDS
   ├─ Initialize deck: mainDeck.initialize()
   ├─ Shuffle deck: mainDeck.shuffle()
   ├─ Deal cards to ALL players (humans + bots)
   │  └─ getConfig().getInitialCardCount() per player (default 7)
   ├─ Setup turn order with ALL players:
   │  └─ turnOrder = room.getAllPlayers()
   ├─ Place first card on discard pile
   │  └─ Skip wild cards for first card
   ├─ Set first player: currentPlayer = turnOrder.getFirst()
   ├─ currentState = PLAYING
   ├─ turnStartTime = System.currentTimeMillis()
   └─ Game ready for players to play

4. DATA FLOW: Room → GameSession
   ├─ Room.getAllPlayers() → includes humans + bots
   ├─ Room.getConfig() → game rules
   ├─ Room.getRoomCode() → reference to room
   └─ Room properties available in session
```

### Critical Data Propagation Points
```
ROOM → GAMESESSION:
  Room.roomCode ──→ GameSession references it
  Room.players ──→ GameSession.turnOrder includes them
  Room.bots ────→ GameSession.turnOrder includes them
  Room.config ──→ Game rules used in session

ISSUE: Room.roomLeader is a Player object
  └─ If leader disconnects/leaves, leadership transfers
  └─ GameSession doesn't track leader after game starts
```

---

## 5. WEBSOCKET CONFIGURATION & MESSAGE HANDLING

### WebSocket Setup (WebSocketConfig.java)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    // MESSAGE BROKER CONFIGURATION
    registry.enableSimpleBroker("/topic", "/queue")
    registry.setApplicationDestinationPrefixes("/app")
    registry.setUserDestinationPrefix("/user")
    
    // STOMP ENDPOINT
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(
                "http://localhost:3000",              // Dev
                "http://localhost:5173",              // Vite
                "https://*.vercel.app",               // Vercel
                "https://oneonline-frontend.vercel.app" // Prod
            )
            .withSockJS()  // Fallback for older browsers
}
```

### WebSocket Message Flow
```
CLIENT → SERVER (from client):
  /app/game/{sessionId}/play-card
  /app/game/{sessionId}/draw-card
  /app/game/{sessionId}/call-uno
  /app/game/{sessionId}/chat
  /app/game/{sessionId}/join
  /app/game/{sessionId}/leave

SERVER → ALL PLAYERS (broadcast):
  /topic/game/{sessionId}    (game events)
  /topic/room/{roomCode}     (room events)
  /topic/lobby               (public room updates)

SERVER → SPECIFIC USER (point-to-point):
  /user/queue/notification   (personal messages)
  /user/queue/errors         (error responses)
```

### WebSocketGameController Handlers
```java
@MessageMapping("/game/{sessionId}/play-card")
@SendTo("/topic/game/{sessionId}")
handlePlayCard() {
    1. Get session from GameManager
    2. Find player by Principal (authenticated user)
    3. Find card from player's hand
    4. Call gameEngine.processMove(player, card, session)
    5. Build GameStateResponse
    6. Return (auto-broadcasts to @SendTo destination)
}

@MessageMapping("/game/{sessionId}/draw-card")
@SendTo("/topic/game/{sessionId}")
handleDrawCard() {
    1. Get session
    2. Find player
    3. Call gameEngine.drawCard()
    4. Advance turn
    5. Broadcast updated game state
}

@MessageMapping("/game/{sessionId}/call-uno")
handleCallUno() {
    1. Broadcast "ONE_CALLED" event
    2. messagingTemplate.convertAndSend()
}

@MessageMapping("/game/{sessionId}/chat")
handleChatMessage() {
    1. Extract message from payload
    2. Broadcast to /topic/game/{sessionId}/chat
    3. Include sender, timestamp
}
```

---

## 6. OBSERVER PATTERN: REAL-TIME NOTIFICATIONS

### GameObserver Interface
```java
interface GameObserver {
    // Room events
    onPlayerJoined(Player, Room)
    onPlayerLeft(Player, Room)
    onRoomCreated(Room)
    onRoomDeleted(Room)
    
    // Game events
    onGameStarted(GameSession)
    onGameEnded(Player winner, GameSession)
    onTurnChanged(Player, GameSession)
    
    // Card events
    onCardPlayed(Player, Card, GameSession)
    onCardDrawn(Player, int cardCount, GameSession)
    
    // Special events
    onOneCalled(Player, GameSession)
    onOnePenalty(Player, int penaltyCards, GameSession)
    onDirectionReversed(boolean clockwise, GameSession)
    onPlayerSkipped(Player, GameSession)
    onColorChanged(Player, CardColor, GameSession)
    
    // Connection events
    onPlayerDisconnected(Player, GameSession)
    onPlayerReconnected(Player, GameSession)
    onGamePaused(GameSession)
    onGameResumed(GameSession)
}
```

### WebSocketObserver Implementation
```java
@Component
public class WebSocketObserver implements GameObserver {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Override
    public void onPlayerJoined(Player player, Room room) {
        Map<String, Object> event = createEvent("PLAYER_JOINED", {
            playerId, nickname, isBot, roomCode, totalPlayerCount
        });
        sendToRoom(room.getRoomCode(), event)
        // messagingTemplate.convertAndSend("/topic/room/{roomCode}", event)
    }
    
    // Similar for other events...
    
    private void sendToGame(String sessionId, Map<String, Object> event) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId, event)
    }
    
    private void sendToRoom(String roomCode, Map<String, Object> event) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode, event)
    }
    
    private void sendToLobby(Map<String, Object> event) {
        messagingTemplate.convertAndSend("/topic/lobby", event)
    }
}
```

### Notification Flow When Game Starts
```
FLOW 1: Through REST API (Initial Game Start)
┌─────────────────────────────────────────────────────────────┐
│ RoomController.startGame(code)                              │
│  ├─ Create GameSession                                      │
│  ├─ session.start() ← Initialize game                       │
│  ├─ room.setGameSession(session)                            │
│  └─ Return { sessionId, roomCode, "STARTED" }              │
│                                                             │
│ NOTE: No automatic WebSocket notification here!             │
│ ISSUE: Frontend must poll or subscribe to know game started │
└─────────────────────────────────────────────────────────────┘

FLOW 2: Through WebSocket (Game Events During Play)
┌──────────────────────────────────────────────────────────────┐
│ Client subscribes to /topic/game/{sessionId}                │
│ Game events arrive: CARD_PLAYED, CARD_DRAWN, TURN_CHANGED  │
│  ├─ Event format:                                           │
│  │  {                                                       │
│  │    "eventType": "CARD_PLAYED",                          │
│  │    "timestamp": 1699536000000,                          │
│  │    "data": {                                            │
│  │      "playerId": "uuid",                                │
│  │      "cardId": "uuid",                                  │
│  │      "cardType": "NUMBER",                              │
│  │      "cardColor": "RED",                                │
│  │      "remainingCards": 6                                │
│  │    }                                                     │
│  │  }                                                       │
│  └─ All players receive same broadcast                     │
└──────────────────────────────────────────────────────────────┘
```

### Missing: Game Start Notification ⚠️
```
CRITICAL ISSUE IDENTIFIED:
When game starts via REST /api/rooms/{code}/start:
  ✓ GameSession created and initialized
  ✓ Room status updated to IN_PROGRESS
  ✗ NO WebSocket notification sent to players!
  
CONSEQUENCE:
  - Frontend doesn't know game started unless it polls
  - Players don't automatically get redirected to game
  - Player hand data not synchronized

SOLUTION NEEDED:
  In RoomController.startGame() after session.start():
    webSocketObserver.onGameStarted(session)
      └─ Broadcast to /topic/room/{roomCode}:
         {
           "eventType": "GAME_STARTED",
           "data": {
             "sessionId": "...",
             "startingPlayer": "...",
             "players": [...],
             "initialHands": {...}  // Player hands here
           }
         }
```

---

## 7. ROOM & GAME DATA FLOW

### Complete Request → Response → Notification Flow

#### Step 1: Create Room
```
POST /api/rooms
{
  "isPrivate": false,
  "maxPlayers": 4,
  "turnTimeLimit": 60
}

RoomController.createRoom()
  └─ Create Player: { playerId, userEmail, nickname }
  └─ Build GameConfiguration
  └─ RoomManager.createRoom(creator, config)
      └─ Generate roomCode (e.g., "ABC123")
      └─ New Room: {
           roomId: UUID,
           roomCode: "ABC123",
           roomLeader: creator,
           players: [creator],
           bots: [],
           status: WAITING,
           gameSession: null
         }
      └─ GameManager.createRoom(room)  ← Store in memory
      └─ WebSocketObserver.onRoomCreated(room)
          └─ Broadcast /topic/lobby: ROOM_CREATED

Response 201 Created:
{
  "roomId": "ABC123",
  "roomCode": "ABC123",
  "hostId": "player-uuid",
  "status": "WAITING",
  "players": [{playerId, nickname, userEmail, isHost: true}],
  "currentPlayers": 1,
  "maxPlayers": 4,
  "config": {...}
}
```

#### Step 2: Join Room
```
POST /api/rooms/ABC123/join
{
  "nickname": "Player2"
}

RoomController.joinRoom("ABC123", request)
  └─ Create Player: { playerId, userEmail: auth.getName(), nickname }
  └─ RoomManager.joinRoom("ABC123", player)
      └─ Room room = GameManager.getRoom("ABC123")
      └─ Validate: not full, not duplicate
      └─ room.addPlayer(player)  ← Add to players list
      └─ WebSocketObserver.onPlayerJoined(player, room)
          └─ Broadcast /topic/room/ABC123:
             {
               "eventType": "PLAYER_JOINED",
               "data": {
                 "playerId": "player2-uuid",
                 "nickname": "Player2",
                 "isBot": false,
                 "roomCode": "ABC123",
                 "totalPlayerCount": 2
               }
             }

Response 200 OK:
{
  "roomCode": "ABC123",
  "players": [
    {playerId, nickname, userEmail, isBot: false, isHost: true},
    {playerId, nickname, userEmail, isBot: false, isHost: false}
  ],
  "currentPlayers": 2,
  "status": "WAITING"
}
```

#### Step 3: Add Bot
```
POST /api/rooms/ABC123/bot

RoomController.addBot("ABC123")
  └─ RoomManager.addBot("ABC123")
      └─ room = GameManager.getRoom("ABC123")
      └─ room.addBot()
          ├─ Generate botId (UUID)
          ├─ Create BotPlayer {
               playerId: botId,
               nickname: "Bot_1",
               temporary: false
             }
          └─ Add to bots list
      └─ WebSocketObserver.onPlayerJoined(bot, room)
          └─ Broadcast /topic/room/ABC123:
             {
               "eventType": "PLAYER_JOINED",
               "data": {
                 "playerId": "bot-uuid",
                 "nickname": "Bot_1",
                 "isBot": true,
                 "roomCode": "ABC123",
                 "totalPlayerCount": 3
               }
             }

Response 200 OK:
{
  "roomCode": "ABC123",
  "players": [
    {player1},
    {player2},
    {playerId: "bot-uuid", nickname: "Bot_1", isBot: true}
  ],
  "currentPlayers": 3
}
```

#### Step 4: Start Game
```
POST /api/rooms/ABC123/start
(Leader only)

RoomController.startGame("ABC123")
  ├─ room = GameManager.getRoom("ABC123")
  ├─ Verify: caller is leader
  ├─ Validate: minimum 2 players (2 ≤ total ≤ 4)
  ├─ Create GameSession:
  │  └─ session = GameSession.builder()
  │     .room(room)
  │     .build()
  │
  ├─ GameManager.startGameSession(session)
  │  └─ activeSessions.put(sessionId, session)
  │
  ├─ session.start()  ← CRITICAL INITIALIZATION
  │  ├─ currentState = DEALING_CARDS
  │  ├─ mainDeck.initialize() ← Create 108 cards
  │  ├─ mainDeck.shuffle()
  │  ├─ For each player in room.getAllPlayers():
  │  │  └─ Draw 7 cards: player.drawCard(mainDeck.drawCard())
  │  ├─ turnOrder = LinkedList(room.getAllPlayers())
  │  ├─ firstCard = mainDeck.drawCard() (skip wilds)
  │  ├─ discardPile.push(firstCard)
  │  ├─ currentPlayer = turnOrder.getFirst()
  │  ├─ currentState = PLAYING
  │  └─ turnStartTime = System.currentTimeMillis()
  │
  ├─ room.setStatus(IN_PROGRESS)
  ├─ room.setGameSession(session)
  │
  └─ ⚠️ NO WebSocket notification here! ← MISSING!

Response 200 OK:
{
  "sessionId": "session-uuid",
  "roomCode": "ABC123",
  "status": "STARTED",
  "message": "Game started successfully"
}

DATA NOW AVAILABLE:
┌────────────────────────────────────────────────┐
│ GameSession in GameManager.activeSessions      │
├────────────────────────────────────────────────┤
│ sessionId: UUID                                │
│ room: Room (reference)                         │
│ currentPlayer: Player (first in turn order)    │
│ turnOrder: [Player1, Player2, Bot_1]           │
│ mainDeck: ~89 cards remaining                  │
│ discardPile: [1 card]                          │
│ players: room.getAllPlayers() → all 3          │
│ currentState: PLAYING                          │
└────────────────────────────────────────────────┘
```

### Data Propagation Summary
```
ROOM PROPERTIES → GAMESESSION:
┌─ Room.roomCode ────────→ GameSession accessible via room.getRoomCode()
├─ Room.roomLeader ──────→ Room.getLeader() (but not stored in session)
├─ Room.players ────────→ GameSession.turnOrder (includes bots)
├─ Room.bots ──────────→ GameSession.turnOrder (mixed with humans)
├─ Room.config ────────→ GameSession.getConfiguration()
└─ Room.status ────────→ RoomStatus.IN_PROGRESS when game starts

CRITICAL: Players list doesn't distinguish humans from bots in turnOrder
  → getAllPlayers() returns combined list
  → Can check with: player instanceof BotPlayer

HAND SYNCHRONIZATION:
  ✓ Each player gets their cards dealt in session.start()
  ✓ Player.hand contains actual Card objects
  ✓ Frontend needs to get hand from WebSocket notification or REST call
  ? How does frontend know about initial hand? No automatic push!
```

---

## 8. CRITICAL ISSUES & POTENTIAL PROBLEMS

### Issue 1: No Game Start Notification ⚠️ CRITICAL
```
Problem:
  When /api/rooms/{code}/start returns successfully,
  frontend receives sessionId but NO WebSocket broadcast
  to /topic/room/{roomCode} or /topic/game/{sessionId}
  
Impact:
  - Players don't know game started unless they poll
  - Initial hand not synchronized via WebSocket
  - Game state not broadcast
  
Location: /api/rooms/{code}/start endpoint
File: RoomController.java:393-447

Solution:
  After session.start() and room.setGameSession(session):
    webSocketObserver.onGameStarted(session);
    
  This would broadcast to /topic/game/{sessionId}:
    {
      "eventType": "GAME_STARTED",
      "sessionId": "...",
      "startingPlayer": "...",
      "players": [
        { playerId, nickname, cardCount: 7, ... }
      ]
    }
```

### Issue 2: Player Hand Not Broadcast on Game Start ⚠️
```
Problem:
  Each player gets dealt 7 cards in session.start():
    for (Player player : room.getAllPlayers()) {
      for (int i = 0; i < 7; i++) {
        player.drawCard(mainDeck.drawCard());
      }
    }
  
  But card details are NEVER sent to clients!
  Frontend must call GET /api/game/{sessionId}/state
  to get their own hand
  
Impact:
  - Clients can't start playing until they fetch state
  - Race condition: client sends move before getting cards
  - Suboptimal UX with extra HTTP requests

File: GameSession.java:100-106

Solution:
  In WebSocketObserver.onGameStarted():
    For each player, send individual notification to:
      /user/{playerId}/queue/notification
    with their hand details
```

### Issue 3: Bots in Room Don't Get Added to GameSession Correctly? 
```
Status: APPEARS OK but needs verification

Room.bots list is separate from Room.players
GameSession.start() uses:
  room.getAllPlayers() 
  which returns: List<Player> allPlayers = new ArrayList<>(players)
                                           allPlayers.addAll(bots)
                                           return allPlayers

This SHOULD work, but ensure:
  ✓ Bots have proper playerId (they do, Room.addBot() generates it)
  ✓ Turn order includes bots (it does, uses getAllPlayers())
  ✓ Bots don't cause NPE (need to verify instanceof BotPlayer checks)

Potential issue:
  If anywhere in code assumes player.userId != null
  → BotPlayer.userId is null
  → Could cause NPE
```

### Issue 4: Room Code vs Room ID Confusion 🔸
```
Problem:
  Room has TWO identifiers:
    - roomId: UUID (unique but verbose)
    - roomCode: String 6-char (user-friendly)
  
  WebSocket topics and DTOs sometimes use wrong identifier:
    
  ✓ CORRECT:
    /api/rooms/{code}/join          (uses code)
    /topic/room/{roomCode}          (WebSocketObserver)
    RoomResponse.roomCode           (shows 6-char)
    
  ✗ INCONSISTENT:
    RoomResponse.roomId set to roomCode (mismatch)
    WebSocketObserver.onRoomCreated sends roomId (UUID)
    Could cause routing issues if frontend expects code

File: RoomController.java:480
  response.put("roomId", room.getRoomCode());  // Using roomCode!

File: WebSocketObserver.java:234
  event.put("roomId", room.getRoomId());  // Using UUID!

Solution:
  Use roomCode for all client-facing operations
  Use roomId only internally
```

### Issue 5: Room Leadership Not Preserved in GameSession ⚠️
```
Problem:
  Room.roomLeader is a reference to the leader Player
  GameSession doesn't store reference to room leader
  If leader disconnects mid-game, nobody knows who was leader
  
  GameSession can access via:
    room.getLeader()
  But this requires:
    session.getRoom() not null
    room.getLeader() not null
  
Impact:
  When determining winner or handling disconnection,
  can't easily identify who created the game
  
Solution:
  Store leaderId in GameSession:
    leaderId: String = room.roomLeader.playerId
```

### Issue 6: Card Hand Exposed in GameStateResponse 🔴
```
Problem:
  GameStateResponse includes "hand" field
  This contains complete card details
  ONLY should be sent to that specific player
  
Location: GameController.java:327-348
  
Current implementation:
  if (authentication != null) {
    currentPlayer = find player by authentication.getName()
    hand = currentPlayer.getHand() // EXPOSED!
    response.setHand(hand)
  }
  
  Then response is returned in ResponseEntity
  which sends to requester only (good)
  
  BUT in WebSocket:
    buildGameStateResponse(session) at line 329
    Does NOT include hand (good)
    Only sent via REST API (fine)

✓ Actually seems OK, just needs careful handling
```

### Issue 7: GameManager Singleton Not Spring Managed ⚠️
```
Problem:
  GameManager is a Singleton using Bill Pugh pattern:
  
    public static GameManager getInstance() {
      return SingletonHolder.INSTANCE;
    }
  
  But it's registered as @Service (Spring-managed)
  This creates two instances:
    1. Spring's autowired instance
    2. Singleton's static instance
  
Location: GameManager.java:36-75

Files referencing it:
  RoomManager: gameManager = GameManager.getInstance()
  GameEngine (implicit): GameManager.getInstance()
  WebSocketGameController: gameManager = GameManager.getInstance()

Impact:
  - Room/session data might be in wrong instance
  - Spring's instance differs from getInstance() instance
  
Solution Option A (preferred):
  Remove @Service, only use getInstance()
  → Manage as pure Singleton, not Spring bean
  
Solution Option B:
  Remove Singleton pattern
  → Inject GameManager as @Autowired
  → Spring manages lifecycle
```

### Issue 8: Bot Selection in Rooms ⚠️
```
Problem:
  When adding bots via POST /api/rooms/{code}/bot,
  all bots are identical "difficulty"
  No strategic selection based on player skill
  
Location: RoomController.java:283-301

Current:
  BotPlayer bot = roomManager.addBot(code);
  // No difficulty parameter used
  
Request body (optional):
  { "difficulty": "MEDIUM" }
  // Not actually used!

Impact:
  Bots always play with same strategy
  No adaptive difficulty
  
Solution:
  Pass difficulty to BotFactory
  Create bots with different strateginess
```

### Issue 9: Missing TURN_CHANGED Notifications During Play
```
Problem:
  GameSession.nextTurn() doesn't notify observers
  
Location: GameSession.java:232-244
  
Method:
  public void nextTurn() {
    Player lastPlayer = turnOrder.removeFirst()
    turnOrder.addLast(lastPlayer)
    currentPlayer = turnOrder.getFirst()
    turnStartTime = System.currentTimeMillis()
    // NO OBSERVER NOTIFICATION!
  }

Impact:
  All players need to poll to know whose turn it is
  Real-time updates via WebSocket missing
  
Solution:
  After setting currentPlayer:
    notifyObservers(observer -> observer.onTurnChanged(currentPlayer, this))
  
  But GameSession doesn't have observer list!
  Need to add:
    List<GameObserver> observers = new ArrayList<>()
    addObserver(GameObserver obs)
    notifyObservers()
```

### Issue 10: No Reconnection Handling ⚠️
```
Problem:
  If player disconnects mid-game:
    - Player.connected set to false (how? not in code)
    - GameSession continues
    - What happens to that player's turn?
    - Can player reconnect and resume?
  
No implementation found for:
  - Detecting disconnection
  - Replacing with temporary bot
  - Restoring player when reconnecting
  - Timeout for disconnected players

Files checked:
  GameSession.java - no disconnect handling
  WebSocketGameController.java - no disconnect listener
  Player.java - has connected field but never set to false

Solution Needed:
  Implement disconnect listener in WebSocket config
  Set player.connected = false on disconnect
  Create temporary bot to take over turn
  Queue reconnection attempt with timeout
```

---

## 9. REQUEST/RESPONSE DATA FLOWS

### Room Data Flow (REST)
```
CLIENT REQUEST (POST /api/rooms)
├─ Headers: Authorization: Bearer <JWT>
└─ Body: CreateRoomRequest {
    isPrivate?: boolean,
    maxPlayers?: number,
    turnTimeLimit?: number,
    initialHandSize?: number,
    pointsToWin?: number,
    tournamentMode?: boolean
  }

SERVER PROCESSING:
  RoomController.createRoom()
    ├─ Extract authentication user email
    ├─ Create Player { playerId, userEmail, nickname }
    ├─ Build GameConfiguration from request
    ├─ Call RoomManager.createRoom()
    │ └─ GameManager.createRoom() stores in memory
    ├─ Call WebSocketObserver.onRoomCreated()
    │ └─ Broadcast /topic/lobby: ROOM_CREATED
    └─ Call WebSocketObserver.onPlayerJoined()
      └─ Broadcast /topic/room/{code}: PLAYER_JOINED

CLIENT RESPONSE (201 Created)
└─ RoomResponse {
    roomCode: "ABC123",
    roomId: "ABC123" (actually roomCode),
    roomName?: string,
    hostId: "player-uuid",
    status: "WAITING",
    isPrivate: false,
    players: [PlayerInfo { playerId, nickname, userEmail, isBot, isHost }],
    currentPlayers: 1,
    maxPlayers: 4,
    config: GameConfig { initialHandSize, turnTimeLimit, ... },
    createdAt: timestamp
  }

WEBSOCKET BROADCASTS (Real-time):
  1. /topic/lobby:
     {
       eventType: "ROOM_CREATED",
       timestamp: 1699536000000,
       data: { roomId, roomCode, isPrivate }
     }
  
  2. /topic/room/ABC123:
     {
       eventType: "PLAYER_JOINED",
       timestamp: 1699536000000,
       data: {
         playerId, nickname, isBot, roomCode, totalPlayerCount
       }
     }
```

### Game Start Data Flow
```
CLIENT REQUEST (POST /api/rooms/{code}/start)
├─ Path: /api/rooms/ABC123/start
├─ Headers: Authorization: Bearer <JWT>
└─ Body: (empty)

SERVER PROCESSING:
  RoomController.startGame("ABC123")
    ├─ room = GameManager.getRoom("ABC123")
    ├─ Verify authentication.getName() == room.getLeader().userEmail
    ├─ Validate: 2 ≤ totalPlayerCount ≤ 4
    ├─ Create GameSession:
    │  └─ session = GameSession.builder().room(room).build()
    ├─ GameManager.startGameSession(session) [stores in memory]
    ├─ session.start() [INITIALIZATION]
    │  ├─ mainDeck.initialize() [108 cards]
    │  ├─ mainDeck.shuffle()
    │  ├─ For Player in room.getAllPlayers(): player.drawCard(×7)
    │  ├─ turnOrder = LinkedList(room.getAllPlayers())
    │  ├─ discardPile.push(firstCard)
    │  └─ currentPlayer = turnOrder.getFirst()
    ├─ room.setStatus(IN_PROGRESS)
    ├─ room.setGameSession(session)
    └─ ⚠️ Missing: WebSocket notification!

CLIENT RESPONSE (200 OK)
└─ {
    sessionId: "session-uuid",
    roomCode: "ABC123",
    status: "STARTED",
    message: "Game started successfully"
  }

WHAT'S MISSING:
  ✗ No /topic/room/ABC123 broadcast
  ✗ No /topic/game/{sessionId} broadcast
  ✗ No /user/{playerId}/queue/notification with hand data
  ✗ Clients don't know game state automatically
```

### Game Play Data Flow
```
CLIENT WEBSOCKET MESSAGE (play-card)
├─ Destination: /app/game/{sessionId}/play-card
├─ Payload:
│  {
│    "cardId": "card-uuid",
│    "chosenColor": "RED"  // Only for wild cards
│  }
└─ Principal: authenticated user

SERVER HANDLER:
  WebSocketGameController.handlePlayCard()
    ├─ Find session: GameManager.getSession(sessionId)
    ├─ Find player: session.getPlayers().find(email == principal)
    ├─ Find card: player.getHand().find(cardId)
    ├─ gameEngine.processMove(player, card, session)
    │  ├─ Validate turn, card legality
    │  ├─ Create PlayCardCommand (for undo)
    │  ├─ Remove card from hand
    │  ├─ Add to discard pile
    │  ├─ Apply effects (draw +2, skip, reverse)
    │  ├─ Check win
    │  └─ nextTurn()
    ├─ Build GameStateResponse (no hand)
    └─ Return (auto-broadcasts to @SendTo)

SERVER BROADCAST (auto)
├─ Destination: /topic/game/{sessionId}
└─ GameStateResponse {
    sessionId,
    roomCode,
    status: "PLAYING",
    currentPlayerId: "next-player-uuid",
    topCard: CardInfo { cardId, type, color, value },
    players: [
      { playerId, nickname, cardCount, calledOne, score, isBot }
    ],
    deckSize: 88,
    discardPileSize: 2,
    direction: "CLOCKWISE",
    pendingDrawCount: 0
    // Note: hand NOT included (good!)
  }

CLIENT RECEIVES:
  All players get updated game state
  Can see who played, updated turn, remaining cards
  Each player knows their own cardCount (but not others' cards)
```

---

## 10. KEY FILES & THEIR PURPOSES

### Configuration Files
| File | Purpose | Key Config |
|------|---------|-----------|
| WebSocketConfig.java | STOMP broker setup | /topic, /queue, /app, /ws endpoint |
| SecurityConfig.java | JWT & OAuth2 setup | Public endpoints, auth filter |
| CorsConfig.java | Cross-origin requests | Allowed origins |
| application.properties | Database, JWT, logging | PostgreSQL, JWT secret |

### Core Services
| File | Purpose | Key Methods |
|------|---------|-----------|
| GameManager.java | Singleton managing all rooms/sessions | getInstance(), createRoom(), getSession() |
| RoomManager.java | Room lifecycle | createRoom(), joinRoom(), addBot(), leaveRoom() |
| GameEngine.java | Game logic orchestrator | processMove(), drawCard(), checkWinCondition() |
| WebSocketObserver.java | Real-time notifications | onPlayerJoined(), onCardPlayed(), onGameStarted() |

### Controllers
| File | Purpose | Endpoints |
|------|---------|-----------|
| RoomController.java | Room REST API | /api/rooms/* (CRUD) |
| GameController.java | Game REST API | /api/game/* (play, draw, uno) |
| WebSocketGameController.java | WebSocket handlers | @MessageMapping for real-time events |
| AuthController.java | Authentication | /api/auth/* (login, register, refresh) |

### Domain Models
| File | Represents | Key Fields |
|------|-----------|-----------|
| Room.java | Game container | roomCode, roomLeader, players, bots, gameSession |
| GameSession.java | Active game | sessionId, room, currentPlayer, turnOrder, deck |
| Player.java | Human player | playerId, userId, nickname, hand, score |
| BotPlayer.java | AI player (extends Player) | temporary, originalPlayer, chooseCard() |
| GameConfiguration.java | Game rules | maxPlayers, turnTimeLimit, pointsToWin |

### DTOs
| File | Used For | Key Fields |
|------|----------|-----------|
| RoomResponse.java | Room REST response | roomCode, players, config, status |
| GameStateResponse.java | Game state sync | currentPlayerId, hand, topCard, players |
| CreateRoomRequest.java | POST /api/rooms | isPrivate, maxPlayers, config |
| PlayCardRequest.java | WebSocket play | cardId, chosenColor |

---

## 11. DEPLOYMENT & ENVIRONMENT

### Application Properties
```
Database:
  spring.datasource.url=jdbc:postgresql://localhost:5432/oneonline_db
  spring.datasource.username=postgres
  spring.datasource.password=postgres
  (Overridable with DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD env vars)

JWT:
  jwt.secret=${JWT_SECRET:base64-encoded-string}
  jwt.expiration=86400000 (24 hours)
  jwt.refresh-expiration=604800000 (7 days)

OAuth2:
  Google, GitHub credentials from environment variables
  GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
  GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET

CORS:
  http://localhost:3000 (dev frontend)
  http://localhost:5173 (vite dev)
  https://*.vercel.app (vercel preview)
  https://oneonline-frontend.vercel.app (production)

WebSocket:
  spring.websocket.max-text-message-size=64000
  spring.websocket.max-binary-message-size=64000
```

### Dependencies (Key)
```gradle
// Spring Boot 3.5.7
- spring-boot-starter-web (REST)
- spring-boot-starter-websocket (WebSocket)
- spring-boot-starter-security (Authentication)
- spring-boot-starter-data-jpa (Database)

// Authentication
- jjwt-api:0.12.6 (JWT tokens)
- oauth2-client (OAuth2 providers)

// Database
- postgresql (driver)
- spring-boot-starter-data-jpa (JPA)

// Utilities
- lombok (code generation)
- hypersistence-utils-hibernate-63 (JSON types)
```

---

## 12. SUMMARY OF DATA PROPAGATION FLOW

```
ROOM CREATION FLOW:
┌─────────┐
│ Player  │ Creates room request
└────┬────┘
     │ POST /api/rooms
     ▼
┌──────────────────────┐
│ RoomController       │ Authenticates, validates
└────┬─────────────────┘
     │ roomManager.createRoom(player, config)
     ▼
┌──────────────────────┐
│ RoomManager          │ Generates code, builds room
└────┬─────────────────┘
     │ gameManager.createRoom(room)
     ▼
┌──────────────────────┐
│ GameManager          │ Stores in ConcurrentHashMap
│ activeRooms["ABC123"]│
└────┬─────────────────┘
     │ webSocketObserver.onRoomCreated()
     ├─→ /topic/lobby: ROOM_CREATED
     │
     │ webSocketObserver.onPlayerJoined()
     └─→ /topic/room/ABC123: PLAYER_JOINED


GAME START FLOW:
┌─────────┐
│ Leader  │ Starts game request
└────┬────┘
     │ POST /api/rooms/{code}/start
     ▼
┌──────────────────────┐
│ RoomController       │ Verifies leader, validates players
└────┬─────────────────┘
     │ GameSession.builder().room(room).build()
     ▼
┌──────────────────────┐
│ GameManager          │ Registers session
│ activeSessions[uuid] │
└────┬─────────────────┘
     │ session.start()
     │  ├─ deck.initialize() & shuffle
     │  ├─ Deal cards to all players (7 each)
     │  ├─ turnOrder = getAllPlayers()
     │  ├─ Place first card
     │  └─ Set currentPlayer
     ▼
┌──────────────────────┐
│ Room updated         │ Status = IN_PROGRESS
│ room.gameSession set │ GameSession linked
└────┬─────────────────┘
     │ ⚠️ NO WebSocket notification!
     │
     └─→ Return { sessionId, roomCode, "STARTED" }


DATA AVAILABLE IN GAMESESSION:
┌──────────────────────────────────────────────┐
│ GameSession                                  │
├──────────────────────────────────────────────┤
│ sessionId: UUID                              │
│ room: Room ──────┬──→ roomCode, roomLeader   │
│ currentPlayer: Player ─→ playerId, hand      │
│ turnOrder: [Player1, Player2, Bot_1]         │
│ mainDeck: Deck (89 cards remaining)          │
│ discardPile: [firstCard]                     │
│ currentState: PLAYING                        │
└──────────────────────────────────────────────┘
```

---

## 13. ARCHITECTURE SUMMARY

### Layered Architecture
```
┌────────────────────────────────────┐
│         Frontend (React)            │  WebSocket + REST
├────────────────────────────────────┤
│         REST API Layer              │
│  RoomController, GameController    │
├────────────────────────────────────┤
│        Service Layer                │
│  RoomManager, GameManager          │  Business logic
│  GameEngine, WebSocketObserver     │
├────────────────────────────────────┤
│        Domain/Model Layer           │
│  Room, GameSession, Player          │  In-memory objects
│  Card, Deck, BotPlayer             │
├────────────────────────────────────┤
│        Repository/DAO Layer         │
│  JPA Repositories                  │  Database access
│  UserRepository, etc.              │
├────────────────────────────────────┤
│        Data Layer                   │
│  PostgreSQL Database               │
│  User, GameHistory, PlayerStats    │
└────────────────────────────────────┘
```

### Design Patterns Used (11 total)
1. **Singleton**: GameManager (thread-safe Bill Pugh)
2. **Builder**: RoomBuilder, GameConfigBuilder
3. **Factory**: CardFactory, BotFactory
4. **Observer**: GameObserver, WebSocketObserver
5. **Strategy**: BotStrategy (card selection)
6. **Command**: PlayCardCommand (for undo)
7. **State**: GameState (PLAYING, GAME_OVER)
8. **Adapter**: BotPlayerAdapter
9. **Decorator**: PowerUpDecorator, EffectDecorator
10. **Prototype**: GameStatePrototype (for state copying)
11. **MVC**: Spring Controller-Service-Repository pattern

---

## 14. RECOMMENDED FIXES (Priority Order)

### HIGH PRIORITY
1. **Add Game Start WebSocket Notification**
   - File: RoomController.java, GameSession.java
   - Add: webSocketObserver.onGameStarted() after session.start()
   - Send: Initial game state to all players

2. **Send Player Hand on Game Start**
   - Add: onGameStarted() notification with player hands
   - Send individual hands to /user/{playerId}/queue/notification

3. **Add Turn Change Notifications**
   - File: GameSession.java nextTurn()
   - Add observer pattern to GameSession
   - Notify when turn changes

4. **Implement Disconnect Handling**
   - File: WebSocketConfig.java
   - Add StompSessionConnectedEvent listener
   - Track connected players, create temp bots

### MEDIUM PRIORITY
5. **Fix GameManager Singleton vs Spring Bean**
   - Choose either pure Singleton OR Spring @Service
   - Ensure single instance used everywhere

6. **Standardize Room ID vs Room Code**
   - Use roomCode for client-facing operations
   - Use roomId only internally

7. **Add Observer Pattern to GameSession**
   - Decouple game logic from notifications
   - Register WebSocketObserver in game loop

### LOW PRIORITY
8. **Add Reconnection Queue**
   - Track disconnected players
   - Allow reconnection within timeout
   - Restore game state

9. **Implement Difficulty Levels**
   - BotFactory with difficulty parameter
   - Create bots with different strategies

10. **Add Metrics Tracking**
    - Track card plays, UNO calls, game duration
    - Implement stats persistence

---

## 15. TESTING CHECKLIST

### Room Management
- [ ] Create room → returns roomCode
- [ ] Join room → updates playerCount
- [ ] Add bot → BotPlayer added to room.bots
- [ ] Remove bot → BotPlayer removed
- [ ] Leave room → updates room, removes player
- [ ] Transfer leadership → new leader set
- [ ] Kick player → only leader can kick

### Game Flow
- [ ] Start game → GameSession created, cards dealt
- [ ] Players in turn order → all players included (humans + bots)
- [ ] Each player has 7 cards → initial hand correct
- [ ] Play card → card removed from hand, added to discard
- [ ] Draw card → card added to hand
- [ ] Turn advance → nextTurn() moves to next player
- [ ] Bot plays → BotPlayer.chooseCard() works

### WebSocket
- [ ] Connect to /ws → connection established
- [ ] Subscribe to /topic/game/{sessionId} → receive broadcasts
- [ ] Send /app/game/{sessionId}/play-card → card plays
- [ ] Receive game state → all players updated
- [ ] Chat messages → broadcast correctly

### Edge Cases
- [ ] Room with only bots → can start game?
- [ ] Disconnect mid-game → what happens?
- [ ] Leader leaves → leadership transfers?
- [ ] Room becomes empty → room deleted?
- [ ] Card deck empties → refill from discard?

