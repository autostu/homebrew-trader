package xyz.homebrew.vertx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

@Slf4j
public final class Configurations {

  private Configurations() {
  }

  public static JsonObject load() {
    String conf = System.getProperty("conf");
    URL url;
    try {
      url = Paths.get(conf).toUri().toURL();
    } catch (Throwable t) {
      url = Resources.getResource("config.yml");
    }
    log.info("Using configuration {}", url);
    try {
      ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
      JsonNode json = yamlReader.readTree(url);
      return JsonObject.mapFrom(json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
