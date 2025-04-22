package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;

public class Noble {
  private final Long id;
  private final Long points;
  private final Map<GemColor, Long> cost;

  @JsonCreator
  public Noble(
    @JsonProperty("id") Long id, 
    @JsonProperty("influence") Long points,
    @JsonProperty("cost") Map<GemColor, Long> cost
    ){
    this.id = id;
    this.points = points;
    this.cost = cost;
  }

  // getter
  public Long getId(){return id;}
  public Long getPoints(){return points;}
  public Map<GemColor, Long> getCost(){return cost;}
}
