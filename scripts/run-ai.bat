@echo off
REM Launch human-vs-AI Gomoku
setlocal
pushd "%~dp0\.."
REM Stage 1 redesign: standalone ai.AiGame removed; launch client, click [人机对战] on home
java -cp "target\classes;lib\*" app.Main
popd
