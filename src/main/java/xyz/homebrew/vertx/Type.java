package xyz.homebrew.vertx;

import xyz.homebrew.vertx.fcoin.FCoinAccount;
import xyz.homebrew.vertx.fcoin.FCoinMarket;

public enum Type {

  fcoin(FCoinMarket.class, FCoinAccount.class, "fcoin"),

  ;

  final Class<? extends VertxMarket> market;

  final Class<? extends VertxAccount> account;

  final String id;

  Type(Class<? extends VertxMarket> market, Class<? extends VertxAccount> account, String id) {
    this.market = market;
    this.account = account;
    this.id = id;
  }
}
