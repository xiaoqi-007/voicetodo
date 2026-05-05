#!/bin/bash
# 一键发版脚本：升版本 → 编译 → 上传 Release → 推送代码
# 用法: bash release.sh [版本号] [更新日志]
# 示例: bash release.sh 1.2 "新增xxx功能"

set -e
cd "$(dirname "$0")"

TOKEN="${GITHUB_TOKEN:?请设置环境变量 GITHUB_TOKEN}"
REPO="xiaoqi-007/voicetodo"
MIRROR="https://ghfast.top"

# 读取当前版本
OLD_CODE=$(grep 'versionCode' app/build.gradle.kts | grep -oP '\d+' | head -1)
OLD_NAME=$(grep 'versionName' app/build.gradle.kts | grep -oP '"[^"]+"' | tr -d '"')

# 新版本号
NEW_CODE=$((OLD_CODE + 1))
NEW_NAME="${1:-$((OLD_NAME + 0 + 1))}"
[ "$NEW_NAME" = "$OLD_NAME" ] && NEW_NAME="$(echo "$OLD_NAME + 0.1" | bc)"
CHANGELOG="${2:-v$NEW_NAME 更新}"

echo "=== 版本升级: v$OLD_NAME ($OLD_CODE) → v$NEW_NAME ($NEW_CODE) ==="

# 1. 更新 build.gradle.kts
sed -i "s/versionCode = $OLD_CODE/versionCode = $NEW_CODE/" app/build.gradle.kts
sed -i "s/versionName = \"$OLD_NAME\"/versionName = \"$NEW_NAME\"/" app/build.gradle.kts

# 2. 同步 version.json
APK_URL="$MIRROR/https://github.com/$REPO/releases/download/v$NEW_NAME/VoiceTodo.apk"
cat > version.json << EOF
{
    "versionCode": $NEW_CODE,
    "versionName": "$NEW_NAME",
    "apkUrl": "$APK_URL",
    "changelog": "$CHANGELOG"
}
EOF
echo "=== version.json 已同步 ==="

# 3. 编译 APK
echo "=== 开始编译 ==="
./gradlew assembleDebug --no-daemon -q
cp app/build/outputs/apk/debug/app-debug.apk VoiceTodo.apk
echo "=== 编译完成: $(ls -lh VoiceTodo.apk | awk '{print $5}') ==="

# 4. Git 提交推送
git add -A
git commit -m "v$NEW_NAME: $CHANGELOG" 2>/dev/null || true
git push 2>&1
echo "=== 代码已推送 ==="

# 5. 创建 Release
RELEASE_JSON="{\"tag_name\":\"v$NEW_NAME\",\"name\":\"v$NEW_NAME\",\"body\":\"$CHANGELOG\",\"draft\":false,\"prerelease\":false}"
RELEASE_ID=$(curl -s -X POST -H "Authorization: token $TOKEN" -H "Content-Type: application/json" -d "$RELEASE_JSON" "https://api.github.com/repos/$REPO/releases" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "=== Release 创建成功: v$NEW_NAME (ID: $RELEASE_ID) ==="

# 6. 上传 APK
curl -s -X POST -H "Authorization: token $TOKEN" -H "Content-Type: application/vnd.android.package-archive" --data-binary @VoiceTodo.apk "https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=VoiceTodo.apk" > /dev/null
echo "=== APK 已上传到 Release ==="

echo ""
echo "=============================="
echo " 发版完成！v$NEW_NAME"
echo " Release: https://github.com/$REPO/releases/tag/v$NEW_NAME"
echo " App 内将自动检测到新版本"
echo "=============================="
