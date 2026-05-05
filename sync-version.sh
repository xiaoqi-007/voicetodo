#!/bin/bash
# 从 build.gradle.kts 读取版本号，同步到 version.json
cd "$(dirname "$0")"

VERSION_CODE=$(grep 'versionCode' app/build.gradle.kts | grep -oP '\d+' | head -1)
VERSION_NAME=$(grep 'versionName' app/build.gradle.kts | grep -oP '"[^"]+"' | tr -d '"')

cat > version.json << EOF
{
    "versionCode": $VERSION_CODE,
    "versionName": "$VERSION_NAME",
    "apkUrl": "VoiceTodo.apk",
    "changelog": "$VERSION_NAME: 更新内容"
}
EOF

echo "version.json 已同步: v$VERSION_NAME ($VERSION_CODE)"
