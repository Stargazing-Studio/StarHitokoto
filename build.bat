@echo off
echo 清理之前的构建...
call mvn clean

echo 编译并打包...
call mvn package

if %ERRORLEVEL% EQU 0 (
    echo 构建成功！
    echo JAR文件位于: target\StarHitokoto-1.0-SNAPSHOT.jar
) else (
    echo 构建失败。
    exit /b 1
)