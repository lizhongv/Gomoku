@echo off
REM Run unit tests under src/test/java (skip E2eTest which needs server)
setlocal
pushd "%~dp0\.."
set CP=target\classes;target\test-classes;lib\*
set EXIT=0
for /r src\test\java %%f in (*Test.java) do (
    call :runone "%%~nf"
)
popd
exit /b %EXIT%

:runone
if "%~1"=="E2eTest" goto :eof
if "%~1"=="E2eTest2" goto :eof
echo === %~1 ===
java -Dfile.encoding=UTF-8 -cp "%CP%" %~1
if errorlevel 1 set EXIT=1
goto :eof
