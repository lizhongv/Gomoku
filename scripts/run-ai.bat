@echo off
REM Launch human-vs-AI Gomoku
setlocal
pushd "%~dp0\.."
java -cp "target\classes;lib\*" ai.AiGame
popd
