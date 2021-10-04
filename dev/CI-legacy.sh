#!/usr/bin/env bash

CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

ARGS=("-PscalaVersion=2.12.13" ${@})

exec "${CRDIR}"/.CI.sh ${ARGS}