@echo off
setlocal

set MAVEN_VERSION=3.9.9
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%-bin
set DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
    powershell -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%TEMP%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%TEMP%\maven-extract' -Force"
    powershell -Command "Move-Item '%TEMP%\maven-extract\apache-maven-%MAVEN_VERSION%\*' '%MAVEN_HOME%' -Force"
    rmdir /s /q "%TEMP%\maven-extract"
    del "%TEMP%\maven.zip"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
