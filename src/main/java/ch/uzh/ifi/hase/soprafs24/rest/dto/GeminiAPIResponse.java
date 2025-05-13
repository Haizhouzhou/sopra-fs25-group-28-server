package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GeminiHelper.Candidate;

// 最外层响应对象
@JsonIgnoreProperties(ignoreUnknown=true) //忽略JSON中有但是类中没有定义的字段
public class GeminiAPIResponse {
  private List<Candidate> candidates;
  // private UsageMetadata usageMetadata;
  private String modelVersion; // optional

  // getter and setter
  public List<Candidate> getCandidates() { return candidates; }
  public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }
  // public UsageMetadata getUsageMetadata() { return usageMetadata; }
  // public void setUsageMetadata(UsageMetadata usageMetadata) { this.usageMetadata = usageMetadata; }
  public String getModelVersion() { return modelVersion; }
  public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

  // 获取第一个候选者文本
  public String getFirstCandidateText() {
    if (candidates != null && !candidates.isEmpty()) {
      Candidate firstCandidate = candidates.get(0);
      if (firstCandidate != null && "STOP".equalsIgnoreCase(firstCandidate.getFinishReason())) {
        return firstCandidate.extractText();
      } else if (firstCandidate != null) {
        // 如果不是 STOP，可以返回原因或特定消息
        return "生成未成功完成，原因: " + firstCandidate.getFinishReason();
      }
    }
    return null; // 或者返回默认错误消息
  }

}

// // 使用量元数据对象
// @JsonIgnoreProperties(ignoreUnknown = true)
// class UsageMetadata {
//   private int promptTokenCount;
//   private int candidatesTokenCount;
//   private int totalTokenCount;
//   // promptTokensDetails 和 candidatesTokensDetails 是列表，如果需要可以定义更详细的 DTO
//   // private List<Map<String, Object>> promptTokensDetails;
//   // private List<Map<String, Object>> candidatesTokensDetails;

//   // Getters and Setters
//   public int getPromptTokenCount() { return promptTokenCount; }
//   public void setPromptTokenCount(int promptTokenCount) { this.promptTokenCount = promptTokenCount; }
//   public int getCandidatesTokenCount() { return candidatesTokenCount; }
//   public void setCandidatesTokenCount(int candidatesTokenCount) { this.candidatesTokenCount = candidatesTokenCount; }
//   public int getTotalTokenCount() { return totalTokenCount; }
//   public void setTotalTokenCount(int totalTokenCount) { this.totalTokenCount = totalTokenCount; }
// }