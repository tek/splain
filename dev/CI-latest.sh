#!/usr/bin/env bash

CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

ARGS=${@}

exec "${CRDIR}"/.CI.sh ${ARGS}