package xyz.homebrew.vertx;

import lombok.Data;
import org.reflections.Reflections;
import xyz.homebrew.core.Sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class Type {

  private Class<? extends VertxMarket> market;

  private Class<? extends VertxAccount> account;

  private String id;

  private static final Map<String, Type> types = new HashMap<>();

  static {
    Reflections r = new Reflections();
    Map<String, Class<? extends VertxAccount>> accounts = r.getSubTypesOf(VertxAccount.class).stream()
        .filter(clazz -> clazz.getAnnotation(Sdk.class) != null)
        .collect(Collectors.toMap(clazz -> clazz.getAnnotation(Sdk.class).value(), clazz -> clazz));
    Map<String, Class<? extends VertxMarket>> markets = r.getSubTypesOf(VertxMarket.class).stream()
        .filter(clazz -> clazz.getAnnotation(Sdk.class) != null)
        .collect(Collectors.toMap(clazz -> clazz.getAnnotation(Sdk.class).value(), clazz -> clazz));
    for (Map.Entry<String, Class<? extends VertxAccount>> stringClassEntry : accounts.entrySet()) {
      if (markets.containsKey(stringClassEntry.getKey())) {
        types.put(stringClassEntry.getKey(), new Type(markets.get(stringClassEntry.getKey()), stringClassEntry.getValue(), stringClassEntry.getKey()));
      }
    }
  }

  private Type(Class<? extends VertxMarket> market, Class<? extends VertxAccount> account, String id) {
    this.market = market;
    this.account = account;
    this.id = id;
  }

  static Type valueOf(String id) {
    Type t = types.get(id);
    if (t == null) {
      throw new IllegalArgumentException(String.format("sdk %s not exists", id));
    }
    return t;
  }
}
