#!/bin/bash

echo "Starting Skills Service"
JAVA_OPTS="${JAVA_OPTS} -Dlogging.file=/logs/webapp.log"

if [ ! -z "${EXTRA_JAVA_OPTS}" ]
then
    JAVA_OPTS="${EXTRA_JAVA_OPTS} ${JAVA_OPTS}"
    echo "Added EXTRA_JAVA_OPTS to JAVA_OPTS = [$EXTRA_JAVA_OPTS]"
fi

echo "JAVA_OPTS=${JAVA_OPTS}"
echo -e "SPRING_PROPS=${SPRING_PROPS}"

# support both \n and , as a prop separator
echo -e $SPRING_PROPS | sed -r 's$([^\])[,]\s?$\1\n$g; s$\\,$,$g' >> application.properties

pid=0
term_handler() {
  echo "SIGTERM handler was called"
  if [ $pid -ne 0 ]; then
    echo "exec: kill -SIGTERM $pid"
    kill -SIGTERM "$pid"
    echo "exec: wait $pid"
    wait "$pid"
  fi
  exit 143; # 128 + 15 -- SIGTERM
}
trap term_handler SIGTERM

java ${DEBUG_OPTS} ${JAVA_OPTS} -jar skills.jar &
pid="$!"

# wait forever
while true
do
  tail -f /dev/null & wait ${!}
done
