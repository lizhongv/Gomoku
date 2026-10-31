@echo off
setlocal
pushd "%~dp0\.."
java -cp "target\classes;lib\*" server.FiveServer
popd
