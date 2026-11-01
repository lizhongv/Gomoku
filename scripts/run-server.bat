@echo off
rem 启动联机服务端。
cd /d "%~dp0\.."
call mvnw.cmd -q -pl gomoku-server -am package -DskipTests || exit /b 1
java -jar gomoku-server\target\gomoku-server.jar
