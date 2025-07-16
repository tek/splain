#!/usr/bin/env bash

CRDIR="$(
  cd "$(dirname "$0")" || exit
  pwd
)"

for i in $(seq 7 16); do
    echo " [PUBLISHING] -PscalaVersion=2.13.$i"
    $CRDIR/publishM2.sh -PscalaVersion=2.13.$i
done