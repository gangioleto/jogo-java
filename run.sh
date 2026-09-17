#!/usr/bin/env bash
# Compila e roda o Cobra Arena sem nenhuma dependência externa.
set -e
cd "$(dirname "$0")"

mkdir -p bin
echo "Compilando..."
javac -encoding UTF-8 -d bin $(find src -name "*.java")
echo "Abrindo o jogo..."
java -cp bin com.cobrinha.Main
