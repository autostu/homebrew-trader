#/bin/sh

PROC_HOME=$(cd `dirname $0`/..; pwd)

kill `cat $PROC_HOME/trader.pid`