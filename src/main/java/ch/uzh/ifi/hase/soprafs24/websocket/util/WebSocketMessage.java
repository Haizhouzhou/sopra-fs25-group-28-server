package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebSocketMessage {
  @JsonProperty("type")
  private String type;

  @JsonProperty("roomId")
  private String roomId;

  @JsonProperty("playerName")
  private String playerName;

  @JsonProperty("content")
  private String content;

  // define message type
  public static final String TYPE_CREATE_ROOM = "CREATE_ROOM";
  public static final String TYPE_JOIN_ROOM = "JOIN_ROOM";
  public static final String TYPE_LEAVE_ROOM = "LEAVE_ROOM";
  public static final String TYPE_PLAYER_MESSAGE = "PLAYER_MESSAGE";
  public static final String TYPE_SYSTEM = "SYSTEM";

  private static final Set<String> VALID_TYPES = Set.of(
    TYPE_CREATE_ROOM, TYPE_JOIN_ROOM, TYPE_LEAVE_ROOM, TYPE_LEAVE_ROOM, TYPE_PLAYER_MESSAGE, TYPE_SYSTEM
  );

  public String getType(){return type;}
  public boolean  setType(String type){
    if(VALID_TYPES.contains(type)){
      this.type = type;
      return true;
    }
    return false;
  }

  public String getRoomId(){return roomId;}
  public void setRoomId(String roomId){this.roomId = roomId;}

  public String getPlayerName(){return playerName;}
  public void setPlayerName(String playerName){this.playerName = playerName;}

  public String getContent(){return content;}
  public void setContent(String content){this.content = content;}

  
}
