package ch.uzh.ifi.hase.soprafs24.rest.dto.GeminiHelper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 内容对象
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content{
  private List<Part> parts;
  private String role;

  // Getters and Setters
  public List<Part> getParts() { return parts; }
  public void setParts(List<Part> parts) { this.parts = parts; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
}