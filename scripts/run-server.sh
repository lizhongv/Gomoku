#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
    *) SEP=':' ;;
esac
java -cp "target/classes${SEP}lib/*" server.FiveServer
