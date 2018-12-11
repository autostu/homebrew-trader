package xyz.homebrew.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import xyz.homebrew.core.Account;

import java.util.concurrent.CompletableFuture;

public abstract class VertxAccount extends AbstractVerticle implements Account {

  private final CompletableFuture<Void> deployFuture = new CompletableFuture<>();

  public abstract void init(JsonObject config);

  public CompletableFuture<Void> getDeployFuture() {
    return deployFuture;
  }

  public void completeDeployment() {
    deployFuture.complete(null);
  }

  public void failDeployment(Throwable t) {
    deployFuture.completeExceptionally(t);
  }
}
