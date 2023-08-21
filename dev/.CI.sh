#!/usr/bin/env bash

CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

echo "[COMPILING]" && \
"${CRDIR}"/test.sh "${@}"
