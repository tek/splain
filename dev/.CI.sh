#!/usr/bin/env bash

CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

echo "[COMPILING]" && \
"${CRDIR}"/make-all.sh "${@}" && \
echo "[RUNNING TESTS]" && \
"${CRDIR}"/test.sh "${@}"
