#!/usr/bin/env bash

FWDIR="$(cd "`dirname "$0"`"/..; pwd)"

cd "${FWDIR}" || exit

"${FWDIR}"/gradlew clean scalafix -Dorg.gradle.parallel=false
 # consumes too much memory to run in parallel

scalafmt