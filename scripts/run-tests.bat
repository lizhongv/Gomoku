@echo off
REM Run E2E tests (server must be running)
setlocal
pushd "%~dp0\.."
java -Dfile.encoding=UTF-8 -cp "target\classes;target\test-classes;lib\*" E2eTest
if errorlevel 1 goto err
java -Dfile.encoding=UTF-8 -cp "target\classes;target\test-classes;lib\*" E2eTest2
if errorlevel 1 goto err
echo.
echo All tests passed.
popd
exit /b 0
:err
echo.
echo Tests FAILED.
popd
exit /b 1
