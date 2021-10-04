#!/usr/bin/env bash

CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

git config --global credential.helper store
git pull --rebase --autostash

"${CRDIR}"/CI-latest.sh "${@}"