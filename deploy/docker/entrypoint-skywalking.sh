#!/bin/sh
set -e

AGENT_JAR="/opt/skywalking/agent/skywalking-agent.jar"
AGENT_OPTS=""

if [ "${SW_AGENT_ENABLED:-true}" != "false" ] && [ -f "$AGENT_JAR" ]; then
  AGENT_OPTS="-javaagent:${AGENT_JAR}"
  echo "[skywalking] agent enabled, service=${SW_AGENT_NAME:-unknown}, oap=${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-skywalking-oap:11800}"
else
  echo "[skywalking] agent disabled or jar missing, starting plain java -jar"
fi

exec java ${AGENT_OPTS} -jar /app/app.jar
