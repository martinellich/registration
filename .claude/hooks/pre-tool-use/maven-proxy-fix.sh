#!/bin/bash

# Parse proxy from https_proxy or HTTPS_PROXY env var
PROXY="${https_proxy:-$HTTPS_PROXY}"

if [ -n "$PROXY" ]; then
  # Extract host and port from URL like http://host:port
  PROXY_HOST=$(echo "$PROXY" | sed 's|https\?://||' | cut -d: -f1)
  PROXY_PORT=$(echo "$PROXY" | sed 's|https\?://||' | cut -d: -f2 | tr -d '/')

  export JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=${PROXY_HOST} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY_HOST} -Dhttps.proxyPort=${PROXY_PORT} -Dhttp.nonProxyHosts=localhost|127.*|[::1]"
fi