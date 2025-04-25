package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeminiService {
  private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

  private final RestTemplate restTemplate;
  private final String apiKey;
  private final String apiUrl;

  @Autowired
  public GeminiService(RestTemplate restTemplate,
                      @Value("${gemini.api.key}") String apiKey,
                      @Value("${gemini.api.url}") String apiUrl){
                        this.restTemplate = restTemplate;
                        this.apiKey = apiKey;
                        this.apiUrl = apiUrl;                      
  }

  public String getGeminiHint(String userPrompt, String systemInstruction){
    // configurate request URL
    String urlWithKey = UriComponentsBuilder.fromHttpUrl(apiUrl).queryParam("key", apiKey).toUriString();
    log.debug("Calling Gemini API URL: {}", apiUrl);

    // request header
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // request body
    Map<String, Object> requestBody = new HashMap<>();
    List<Map<String, Object>> contents = new ArrayList<>();

    if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
      Map<String, Object> sysTextPart = Map.of("text", systemInstruction.trim());
      List<Map<String, Object>> sysPartsList = Collections.singletonList(sysTextPart);
      Map<String, Object> systemInstructionMap = Map.of("parts", sysPartsList);
      requestBody.put("system_instruction", systemInstructionMap);
      log.debug("Including system instruction.");
    } else {
        log.debug("No system instruction provided.");
    }

    // --- 构建 contents 部分 ---
    Map<String, Object> userTextPart = Map.of("text", userPrompt);
    List<Map<String, Object>> userPartsList = Collections.singletonList(userTextPart);
    // (optional add) "role": "user"
    Map<String, Object> userContentMap = Map.of("parts", userPartsList);
    // contents 是一个列表，包含一个或多个 content map
    List<Map<String, Object>> contentsList = Collections.singletonList(userContentMap);
    requestBody.put("contents", contentsList);

    // 将最终构建的请求体记录到日志（调试时非常有用）
    // 注意：实际生产中可能需要考虑是否记录敏感信息
    log.debug("Sending request body to Gemini: {}", requestBody);

    // 4. 创建 HTTP 实体
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    // 5. Send POST Request, expecting a String response
    try {
      // *** Changed expected response type to String.class ***
      ResponseEntity<String> responseEntity = restTemplate.exchange(
              urlWithKey,
              HttpMethod.POST,
              entity,
              String.class // Expect the raw JSON response as a String
      );

      // 6. Handle Response
      if (responseEntity.getStatusCode().is2xxSuccessful()) {
        String rawJsonResponse = responseEntity.getBody();
        log.debug("Received raw JSON response from Gemini ({}): {}", responseEntity.getStatusCode(), rawJsonResponse);
        // Return the raw JSON string directly
        return rawJsonResponse;
      } else {
        // Handle non-2xx responses that still might have a body
        String errorBody = responseEntity.getBody() != null ? responseEntity.getBody() : "(No response body)";
        log.error("Gemini API request failed with status code: {}. Response body: {}", responseEntity.getStatusCode(), errorBody);
        // Return a descriptive error message, not the raw error JSON unless you specifically want to
        return "Error: Gemini API returned status " + responseEntity.getStatusCodeValue();
      }
    } catch (HttpClientErrorException e) {
      // Handle 4xx/5xx errors where getResponseBodyAsString() is useful
      log.error("Client/Server error calling Gemini API: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
      // Return a descriptive error message including the status code
      return "Error: Failed to call Gemini API (" + e.getStatusCode().value() + "). Check logs for details.";
    } catch (RestClientException e) {
      // Handle connectivity or other lower-level errors
      log.error("Error connecting or executing request to Gemini API: {}", e.getMessage(), e);
      return "Error: Could not connect to Gemini API. " + e.getMessage();
    } catch (Exception e) {
      // Catch any other unexpected errors
      log.error("Unexpected error during Gemini API call: {}", e.getMessage(), e);
      return "Error: An unexpected error occurred. Check logs.";

    }
  }

}
