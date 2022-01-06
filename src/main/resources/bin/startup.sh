#!/bin/sh

PATH_TO_JAR="$HOME/TelegramBot/lib/SimpleBot-jar-with-dependencies.jar"
JAVA_OPT="-Dlogback.configurationFile=$HOME/TelegramBot/config/logback.xml"
JAVA_OPT="$JAVA_OPT -Djna.library.path=$UASYS_HOME/oam/lib"

JAVA_CONF="$HOME/TelegramBot/config"

case $1 in
    start)

	if [ -f "$PATH_TO_JAR" ]; then
    java $JAVA_OPT -jar $PATH_TO_JAR $JAVA_CONF > /dev/null 2>&1 &
	  echo "Bot started ..."

	else
	  echo "(ERROR) start fail : $?"
	  exit 4
	fi
    ;;
    stop)
	PID=`ps -ef | grep java | grep TelegramBot | awk '{print $2}'`
	if [ -z $PID ]
	then
		echo "Bot is not running"
	else
		echo "stopping Bot"
		kill $PID
		sleep 1
		PID=`ps -ef | grep java | grep TelegramBot | awk '{print $2}'`
		if [ ! -z $PID ]
		then
			echo "kill -9"
			kill -9 $PID
		fi
		echo "Bot stopped"
	fi
    ;;
  *)
    echo "Unknown Command $1"
    ;;
esac