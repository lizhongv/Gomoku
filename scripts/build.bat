@echo off
REM Compile main + test sources into target/
setlocal
pushd "%~dp0\.."

if not exist target\classes mkdir target\classes
if not exist target\test-classes mkdir target\test-classes

dir /s /b src\main\java\*.java > target\sources.txt
javac -d target\classes -cp "lib\*" -encoding UTF-8 "@target\sources.txt"
if errorlevel 1 goto err

REM Copy classpath resources (assets etc bundled into target/classes)
if exist src\main\resources xcopy /Y /E /I /Q src\main\resources\* target\classes\ > nul

dir /s /b src\test\java\*.java > target\test-sources.txt
javac -d target\test-classes -cp "target\classes;lib\*" -encoding UTF-8 "@target\test-sources.txt"
if errorlevel 1 goto err

echo Build OK.
popd
exit /b 0

:err
echo Build FAILED.
popd
exit /b 1
