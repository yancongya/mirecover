#!/bin/sh

# Gradle 启动脚本（简化版，仅用于本地 Linux/macOS 构建）
# Windows 用户请使用 gradlew.bat

APP_HOME=$( cd "$(dirname "$0")" && pwd )
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
