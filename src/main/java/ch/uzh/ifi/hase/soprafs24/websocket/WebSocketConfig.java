package ch.uzh.ifi.hase.soprafs24.websocket;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


@Configuration
public class WebSocketConfig{

  @Bean
  @Profile("!test") 
  public ServerEndpointExporter serverEndpointerExporter(){
    return new ServerEndpointExporter();
  }
  
}
