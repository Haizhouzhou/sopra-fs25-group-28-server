package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MyWebSocketMessage {
  @JsonProperty("type")
  private String type;

  @JsonProperty("roomId")
  private String roomId;

  @JsonProperty("sessionId")
  private String sessionId;

  @JsonProperty("content")
  private Object content;

  // define message send by client type
  public static final String TYPE_CLIENT_CREATE_ROOM = "CREATE_ROOM";
  public static final String TYPE_CLIENT_JOIN_ROOM = "JOIN_ROOM";
  public static final String TYPE_CLIENT_LEAVE_ROOM = "LEAVE_ROOM";
  public static final String TYPE_CLIENT_PLAYER_MESSAGE = "PLAYER_MESSAGE";
  public static final String TYPE_SYSTEM = "SYSTEM";

  public static final String TYPE_CLIENT_GET_ROOMS = "GET_ROOMS";
  public static final String TYPE_SERVER_ROOM_LIST = "ROOM_LIST";

  public static final String TYPE_CLIENT_PLAYER_STATUS = "PLAYER_STATUS";


    // define message send by server type
  public static final String TYPE_SERVER_ROOM_CREATED = "ROOM_CREATED";
  public static final String TYPE_SERVER_ROOM_JOINED = "ROOM_JOINED";
  public static final String TYPE_SERVER_ROOM_LEFT = "ROOM_LEFT";
  // public static final String TYPE_SERVER_PLAYER_JOINED = "PLAYER_JOIN";
  // public static final String TYPE_SERVER_PLAYER_LEFT = "PLAYER_LEAVE";
  public static final String TYPE_SERVER_CHAT_MESSAGE = "CHAT_MESSAGE";
  public static final String TYPE_SERVER_ROOM_STATE = "ROOM_STATE";
  public static final String TYPE_SERVER_GAME_STATE = "GAME_STATE";

  public static final String TYPE_CLIENT_START_GAME = "START_GAME";


  private static final Set<String> VALID_CLIENT_TYPES = Set.of(
    TYPE_CLIENT_CREATE_ROOM, TYPE_CLIENT_JOIN_ROOM, TYPE_CLIENT_LEAVE_ROOM, TYPE_CLIENT_PLAYER_MESSAGE, TYPE_CLIENT_GET_ROOMS, TYPE_SYSTEM, TYPE_CLIENT_PLAYER_STATUS,TYPE_CLIENT_START_GAME
  );

  private static final Set<String> VALID_SERVER_TYPES = Set.of(
    TYPE_SERVER_ROOM_CREATED, TYPE_SERVER_ROOM_JOINED, TYPE_SERVER_ROOM_LEFT, TYPE_SERVER_CHAT_MESSAGE, TYPE_SERVER_ROOM_STATE, TYPE_SERVER_ROOM_LIST, TYPE_SERVER_GAME_STATE
  );

  private static final Set<String> ALL_VALID_TYPES = UnionSet(VALID_CLIENT_TYPES, VALID_SERVER_TYPES);


  public String getType(){return type;}
  public boolean  setType(String type){
    if(ALL_VALID_TYPES.contains(type)){
      this.type = type;
      return true;
    }
    return false;
  }

  public String getRoomId(){return roomId;}
  public void setRoomId(String roomId){this.roomId = roomId;}

  public String getSessionId(){return sessionId;}
  public void setSessionId(String sessionId){this.sessionId = sessionId;}

  public Object getContent(){return content;}
  public void setContent(Object content){this.content = content;}


  private static Set<String> UnionSet(Set<String> set1, Set<String>set2){
    Set<String> unionSet = new HashSet<>();
    unionSet.addAll(set1);
    unionSet.addAll(set2);
    return unionSet;
  }
  
}
