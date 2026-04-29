@echo off
setlocal
pushd "%~dp0\.."
java -cp "target\classes;lib\*" tool.CreateDatabase
popd
