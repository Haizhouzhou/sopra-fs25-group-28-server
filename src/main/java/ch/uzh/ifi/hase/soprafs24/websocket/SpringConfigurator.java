package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.config.SpringContext;
import javax.websocket.server.ServerEndpointConfig;

public class SpringConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return SpringContext.getBean(clazz);
    }
}
