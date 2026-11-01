@echo off
rem 启动 JavaFX 桌面客户端。
cd /d "%~dp0\.."
call mvnw.cmd -q -pl gomoku-core,gomoku-ai -am install -DskipTests || exit /b 1
call mvnw.cmd -q -pl gomoku-desktop javafx:run
