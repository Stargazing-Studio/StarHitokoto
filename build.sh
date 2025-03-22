#!/bin/bash

# 清理之前的构建
echo "清理之前的构建..."
mvn clean

# 编译并打包
echo "编译并打包..."
mvn package

# 检查构建是否成功
if [ $? -eq 0 ]; then
    echo "构建成功！"
    echo "JAR文件位于: target/StarHitokoto-1.2-SNAPSHOT.jar"
else
    echo "构建失败。"
    exit 1
fi