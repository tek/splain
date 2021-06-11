#!/usr/bin/env bash

FWDIR="$(
  cd "$(dirname "$0")"/.. || exit
  pwd
)"
DATE=$(date --iso-8601=second)

export VERSIONS="2.12.8 2.12.9 2.12.10 2.12.11 2.12.12 2.12.13"

${FWDIR}/dev/cross-build.sh