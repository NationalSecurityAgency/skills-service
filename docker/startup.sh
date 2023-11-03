#!/usr/bin/env bash

print_and_mask() {
  PROPS_TO_PRINT=$1
  LABEL=$2
  if [ "$DEBUG_MODE" == true ]
  then
    FORMATTED=$PROPS_TO_PRINT
  else
    PROPS=(${PROPS_TO_PRINT//,/ })

    for PROP in "${!PROPS[@]}"; do
      PROPS[$PROP]=$(sed -e 's/\(.*[pP]ass.*=\)\(.*\)/\1******/' <<< "${PROPS[$PROP]}")
      PROPS[$PROP]=$(sed -e 's/\(.*client-secret.*=\)\(.*\)/\1******/' <<< "${PROPS[$PROP]}")
    done

    FORMATTED=$(printf ",%s" "${PROPS[@]}")
    FORMATTED=${FORMATTED:1}
  fi
  echo -e "${LABEL}=[${FORMATTED}]"
}

echo "Starting Skills Service"
JAVA_OPTS="${JAVA_OPTS} -Dlogging.file=/logs/webapp.log"

print_and_mask "${EXTRA_JAVA_OPTS}", "EXTRA_JAVA_OPTS"
if [ ! -z "${EXTRA_JAVA_OPTS}" ]
then
    JAVA_OPTS="${EXTRA_JAVA_OPTS} ${JAVA_OPTS}"
fi

if [[ -z "${JAVA_OPTS_FILES}" ]]; then
   echo "Optional JAVA_OPTS_FILES is not set"
else
  for sinleOptsFile in ${JAVA_OPTS_FILES//,/ } ; do
     echo "Loading Java environment variables from JAVA_OPTS_FILES=[${sinleOptsFile}]"
     OLDIFS=$IFS; IFS=$'\n';
     for textLine in $(cat $sinleOptsFile) ; do
      JAVA_OPTS="${JAVA_OPTS} -D${textLine}"
     done
     IFS=$OLDIFS
  done
fi
print_and_mask "${JAVA_OPTS}", "JAVA_OPTS"

if [[ -z "${SPRING_PROPS_FILES}" ]]; then
   echo "Optional SPRING_PROPS_FILES is not set"
else
  for sinleOptsFile in ${SPRING_PROPS_FILES//,/ } ; do
     echo "Loading Spring properties from SPRING_PROPS_FILES=[${sinleOptsFile}]"
      OLDIFS=$IFS; IFS=$'\n';
        springProps=()
        for textLine in $(cat $sinleOptsFile) ; do
          springProps+=("${textLine}")
        done
        IFS=","
        if [[ -z "${SPRING_PROPS}" ]]; then
          SPRING_PROPS="${springProps[*]}"
        else
          SPRING_PROPS="${SPRING_PROPS},${springProps[*]}"
        fi
        IFS=$OLDIFS
  done
fi
print_and_mask "${SPRING_PROPS}", "SPRING_PROPS"

# support both \n and , as a prop separator
echo -e $SPRING_PROPS | sed -r 's$([^\])[,]\s?$\1\n$g; s$\\,$,$g' >> application.properties

pid=0
term_handler() {
  echo "SIGTERM handler was called"
  if [ $pid -ne 0 ]; then
    # if provided will wait before issuing the kill command,
    # this wait is useful in the distributed deployment when the new requests are stopped, this will
    # then allow some time for the service to honor existing requests before killing it
    if [ -n "${SIGTERM_HANDLER_SLEEP_BEFORE_KILL}" ]; then
	    echo "SIGTERM handler: Sleep [${SIGTERM_HANDLER_SLEEP_BEFORE_KILL}] seconds before killing"
      sleep ${SIGTERM_HANDLER_SLEEP_BEFORE_KILL}
    fi
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
