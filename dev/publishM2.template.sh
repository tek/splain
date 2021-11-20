#!/usr/bin/env bash

CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

echo "[COMPILING]" && \
  "${CRDIR}"/make-all.sh "${@}" \
  -Psigning.gnupg.secretKey=??? \
  -Psigning.gnupg.passphrase=??? \
  "${@}"
