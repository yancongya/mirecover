@echo off
REM 在线版构建 (依赖未缓存时使用, 需要联网下载)
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=C:\Users\Administrator\AppData\Local\Android\Sdk"
set "PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%"

set "GRADLE=C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.4-bin\1w5dpkrfk8irigvoxmyhowfim\gradle-8.4\bin\gradle.bat"

cd /d %~dp0
echo 在线构建 (会下载缺失依赖)...
call "%GRADLE%" assembleDebug --no-daemon %*
echo BUILD EXIT CODE = %ERRORLEVEL%
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo 成功! APK: %~dp0app\build\outputs\apk\debug\app-debug.apk
) else (
    echo 构建失败, 查看 build_log.txt
)
pause
