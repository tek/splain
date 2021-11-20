#!/usr/bin/env bash

FWDIR="$(
  cd "$(dirname "$0")"/.. || exit
  pwd
)"

${FWDIR}/gradlew publishToSonatype \
  closeSonatypeStagingRepository \
  -PsonatypeApiUser=??? \
  -PsonatypeApiKey=??? \
  -Psigning.gnupg.secretKey=??? \
  -Psigning.gnupg.passphrase=??? \
  "${@}"
