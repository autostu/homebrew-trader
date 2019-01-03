#!/bin/sh

sh /tmp/homebrew-trader/bin/stop.sh

if [ -d "/tmp/homebrew-trader" ]; then
    rm -rf "/tmp/homebrew-trader"
fi

mvn clean package

unzip trader-app/target/homebrew-trader.zip -d /tmp

cp trader-app/src/main/resources/config.yml /tmp/homebrew-trader/conf/

sh /tmp/homebrew-trader/bin/start.sh
