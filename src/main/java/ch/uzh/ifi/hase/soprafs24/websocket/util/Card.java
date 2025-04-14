package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;

public class Card {

  private final Long id;
  private final int tier;
  private final GemColor color;
  private final Long points;
  private final Map<GemColor, Long> cost;
  
  @JsonCreator
  public Card(
    @JsonProperty("id") Long id, 
    @JsonProperty("tier") int tier, 
    @JsonProperty("color") GemColor color, 
    @JsonProperty("points") Long points,
    @JsonProperty("cost") Map<GemColor, Long> cost
    ){
    this.id = id;
    this.tier = tier;
    this.color = color;
    this.points = points;
    this.cost = cost;
  }

  // getter and setter
  public Long getId(){return id;}
  public int getTier(){return tier;}
  public GemColor getColor(){return color;}
  public Long getPoints(){return points;}
  public Map<GemColor, Long> getCost(){return cost;}
  
}
