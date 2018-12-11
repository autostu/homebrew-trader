#/bin/sh

PROC_HOME=$(cd `dirname $0`/..; pwd)

LIB_DIR="$PROC_HOME/lib"

CONF_DIR="$PROC_HOME/conf"

if [ ! -r "$CONF_DIR/config.yml" ]; then
    cp "$CONF_DIR/config.yml.template" "$CONF_DIR/config.yml"
fi

if [ -r "$PROC_HOME/bin/setenv.sh" ]; then
    . "$PROC_HOME/bin/setenv.sh"
fi

for jar in $LIB_DIR/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

BOOTCLASS="xyz.homebrew.app.Bootstrap"

STDOUT="$PROC_HOME/trader.out"

PID="$PROC_HOME/trader.pid"

JAVA_OPTS=`echo $JAVA_OPTS | tr -d "\r\t\n"`
JAVA_OPTS="$JAVA_OPTS -Dconf=$CONF_DIR/config.yml"

echo "PROC_HOME: $PROC_HOME"
echo "Using JAVA_OPTS: $JAVA_OPTS"
echo "Using CLASSPATH: $CLASSPATH"

nohup java -classpath "$CLASSPATH" $JAVA_OPTS "$BOOTCLASS">>"$STDOUT" 2>&1 & echo $!>"$PID"