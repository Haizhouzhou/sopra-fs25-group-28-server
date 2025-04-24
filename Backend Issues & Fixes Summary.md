# Backend Issues & Fixes Summary

Based on [16c9f7e](https://github.com/Haizhouzhou/sopra-fs25-group-28-server/commit/16c9f7e1d623e4666d36bbc35e4f8f73698525c2)

Modified by Yiming Xiao

2025-04-22

| Issue / Change                              | Description                                                  | Affected Files                                               |
| ------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `UserService` is null in WebSocket          | Caused by manually new-ing `GameRoomManager`, not managed by Spring, so dependencies can't be injected | `GameRoomManager.java` (add `@Component`, remove `static` singleton) |
| Token couldn't be used to retrieve user     | Refactored to use Spring-injected `UserService`              | `GameRoomManager.java` (add `@Autowired UserService`)        |
| Cannot get Spring Bean in `WebSocketServer` | Since `GameRoomManager` is no longer static, now fetched from Spring context | `WebSocketServer.java` (use `SpringContext.getBean(...)`)    |
| Project lacked Spring helper class          | Added `SpringContext` to obtain beans from `ApplicationContext` | `SpringContext.java` (new)                                   |
| Missing room list feature                   | Added `handleGetRooms` method to return summaries of all existing rooms | `WebSocketServer.java` (added `handleGetRooms` method)       |
| Message protocol lacked room list types     | Added new message types `GET_ROOMS` and `ROOM_LIST` for frontend/backend communication | `MyWebSocketMessage.java` (add constants)                    |
| `GameRoom` lacked display fields            | Added `getRoomId`, `getOwnerName`, `getMaxPlayer`, and `roomName` for frontend rendering | `GameRoom.java` (ensure all info exported)                   |
| Rewrote `generateUniqueRandomUsername`      | Random usernames are generated; backend now returns `User.name` in WebSocket | `UserService.java`                                           |
| User avatar changed to a string field       | Avatar used to be complex/array; simplified to a single image path string | All user-related models/entities                             |
| `joinRoom` didn't return message            | Added `ROOM_JOINED` message upon successful join so frontend can redirect | `GameRoomManager.java`                                       |

