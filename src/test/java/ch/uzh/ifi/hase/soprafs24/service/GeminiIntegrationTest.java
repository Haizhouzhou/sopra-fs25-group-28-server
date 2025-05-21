package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class GeminiIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(GeminiIntegrationTest.class);

  @Autowired
  private GeminiService geminiService;
  
  @Test
  @DisplayName("calling Gemini API should give back JSON response")
  void testGeminiAPISuccess_returnValidJsonString(){
    // Arrange
    String userPrompt = "Explain AI with one sentence.";
    String systemInstruction = null;
    
    log.info("测试实际调用gemini api");
    log.info("used prompt: {}", userPrompt);

    // Act
    String rawJsonResponse = null;
    try {
        rawJsonResponse = geminiService.getGeminiHint(userPrompt, systemInstruction);
    } catch (Exception e) {

    }

    // Assert
    log.info("rawResponse: {}", rawJsonResponse);
    assertNotNull(rawJsonResponse);

  }

}
