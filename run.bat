@echo off
REM Compila e roda o Cobra Arena sem nenhuma dependencia externa.
setlocal
cd /d "%~dp0"

if not exist bin mkdir bin
set FONTES=%TEMP%\cobra-arena-fontes.txt
dir /s /b src\*.java > "%FONTES%"

echo Compilando...
javac -encoding UTF-8 -d bin @"%FONTES%"
if errorlevel 1 goto erro
del "%FONTES%"

echo Abrindo o jogo...
java -cp bin com.cobrinha.Main
goto fim

:erro
echo.
echo Falha na compilacao. Confira se o JDK esta instalado (javac -version).
pause

:fim
endlocal
