#!/usr/bin/env bash

FWDIR="$(
  cd "$(dirname "$0")"/.. || exit
  pwd
)"

EXIT=false
#for VV in "2.12.13" "2.13.5"
for VV in ${VERSIONS}; do
  cd ${FWDIR} && \
  sbt ++${VV} clean publishM2 && \
  COMPILED="${COMPILED} ${VV}" && \
  sbt ++${VV} test && \
  TESTED="${TESTED} ${VV}" && \
  EXIT=true
done

echo "COMPILED: ${COMPILED}"
echo "TESTED: ${TESTED}"
echo "EXIT: ${EXIT}"

${EXIT}