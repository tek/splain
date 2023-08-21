#!/usr/bin/env bash

FWDIR="$(
  cd "$(dirname "$0")"/.. || exit
  pwd
)"
CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

"${CRDIR}"/make-all.sh "${@}" && \
${FWDIR}/gradlew test "${@}"
