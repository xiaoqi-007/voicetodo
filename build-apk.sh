#!/bin/bash
# 监听源码变化，自动重新编译 APK
cd "$(dirname "$0")"

echo "开始监听源码变化，自动编译 APK..."
echo "按 Ctrl+C 停止"

LAST_HASH=""

while true; do
    CURRENT_HASH=$(find app/src -type f -name "*.kt" -o -name "*.xml" -o -name "*.kts" | sort | xargs md5sum 2>/dev/null | md5sum | cut -d' ' -f1)

    if [ "$CURRENT_HASH" != "$LAST_HASH" ] && [ -n "$LAST_HASH" ]; then
        echo ""
        echo "=== 检测到代码变化，开始编译 ==="
        ./gradlew assembleDebug --no-daemon -q 2>&1
        if [ $? -eq 0 ]; then
            cp app/build/outputs/apk/debug/app-debug.apk VoiceTodo-debug.apk
            echo "=== APK 已更新: VoiceTodo-debug.apk ==="
        else
            echo "=== 编译失败 ==="
        fi
    fi

    LAST_HASH="$CURRENT_HASH"
    sleep 3
done
