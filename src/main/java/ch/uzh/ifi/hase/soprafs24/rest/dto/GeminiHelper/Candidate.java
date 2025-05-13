package ch.uzh.ifi.hase.soprafs24.rest.dto.GeminiHelper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Candidate{
  private Content content;
  private String finishReason;

  // 可以添加 List<SafetyRating> safetyRatings; 等其他字段
  // private Double avgLogprobs; // 如果需要可以添加

  // Getters and Setters
  public Content getContent() { return content; }
  public void setContent(Content content) { this.content = content; }
  public String getFinishReason() { return finishReason; }
  public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
  // public Double getAvgLogprobs() { return avgLogprobs; }
  // public void setAvgLogprobs(Double avgLogprobs) { this.avgLogprobs = avgLogprobs; }

  // 辅助方法：从此 Candidate 中提取文本
  public String extractText() {
    if (content != null && content.getParts() != null && !content.getParts().isEmpty()) {
      Part firstPart = content.getParts().get(0);
      if (firstPart != null) {
        return firstPart.getText();
      }
    }
    return null; // 或 "" （空字符串）
  }
}