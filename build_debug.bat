@echo off
REM MIUI 缓存图片恢复 - Debug APK 构建脚本
REM 使用 Android Studio 自带 JDK (JBR) 与缓存的 Gradle 8.11.1
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=C:\Users\Administrator\AppData\Local\Android\Sdk"
set "PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%"

set "GRADLE=C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.11.1-all\2qik7nd48slq1ooc2496ixf4i\gradle-8.11.1\bin\gradle.bat"

cd /d %~dp0
echo ============================================
echo  正在构建 debug APK (请稍候, 首次约 1-3 分钟)
echo  日志写入 build_log.txt
echo ============================================
call "%GRADLE%" assembleDebug --no-daemon %*
echo.
echo BUILD EXIT CODE = %ERRORLEVEL%
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ============================================
    echo  成功! APK 位置:
    echo  %~dp0app\build\outputs\apk\debug\app-debug.apk
    echo ============================================
) else (
    echo.
    echo 构建失败, 请查看 build_log.txt
)
pause
