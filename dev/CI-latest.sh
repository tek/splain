#!/usr/bin/env bash

FWDIR="$(
  cd "$(dirname "$0")"/.. || exit
  pwd
)"
DATE=$(date --iso-8601=second)

export VERSIONS="2.13.0 2.13.1 2.13.2 2.13.3 2.13.4 2.13.5"

${FWDIR}/dev/cross-build.sh