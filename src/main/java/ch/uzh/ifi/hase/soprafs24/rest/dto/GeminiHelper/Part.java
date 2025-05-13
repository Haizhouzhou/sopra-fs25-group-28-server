package ch.uzh.ifi.hase.soprafs24.rest.dto.GeminiHelper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 部分内容对象
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part {
  private String text;
  // 可能还有其他字段，如 "inlineData" 等，根据需要添加

  // Getters and Setters
  public String getText() { return text; }
  public void setText(String text) { this.text = text; }
}