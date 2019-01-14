# Homebrew Trader


I'm a new hand of quantitative and try to build my personal trading system, the `Homebrew Trader`. It currently only support fcoin sdk.

## Try it

Simply open the project by IntelliJ IDEA or Eclipse, and run `Bootstrap`.

## Configuration

Before you try it, you may need to configurate something. Edit `config.yml.template` and rename it to `config.yml`:

```
redis:
  host: localhost
  port: 6379

proxy:
  type: SOCKS5
  host: localhost
  port: 1080

accounts:
  - type: fcoin
    id: fcoin_main
    # contract is an abstraction in the trading system and you can define it yourself.
    # You can only buy or sell 1 2 3.. pieces of contract, and each contract has {amount} (0.01) {base} btc.
    contract:
      base: btc
      quote: usdt
      amount: 0.01
      scale: 2
    # put your key and secret here
    key:
    secret:

markets:
  - type: fcoin
    # you can add muliple symbols if you would like to take other symbols as reference
    symbols:
      - btcusdt

traders:
  # your strategy class
  - class: xyz.homebrew.app.strategy.JavaTrader
    # you can manage multiple accounts in a single trader
    accounts:
      - fcoin_main
    # you can subscribe multiple markets as reference
    watches:
      - fcoin

```
