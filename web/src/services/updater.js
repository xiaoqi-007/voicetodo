// 应用版本号，每次发版手动递增
export const APP_VERSION = '1.0.0'

// ========== 以下配置需要修改 ==========
// 你的GitHub用户名和仓库名
const GITHUB_OWNER = 'xiaoqi-007'
const GITHUB_REPO = 'voice-todo'
// ========== 配置结束 ==========

// GitHub Releases API地址
const VERSION_CHECK_URL = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`

export async function checkUpdate() {
  try {
    const res = await fetch(VERSION_CHECK_URL, {
      method: 'GET',
      headers: {
        'Accept': 'application/vnd.github.v3+json',
      },
      cache: 'no-cache',
    })

    if (!res.ok) return null

    const data = await res.json()

    // GitHub tag格式: v1.0.0 或 1.0.0
    const remoteVersion = (data.tag_name || '').replace(/^v/, '')
    const downloadUrl = data.assets?.find(a => a.name.endsWith('.apk'))?.browser_download_url

    if (remoteVersion && isNewerVersion(remoteVersion, APP_VERSION)) {
      return {
        version: remoteVersion,
        url: downloadUrl || data.html_url,
        changelog: data.body || '',
      }
    }

    return null
  } catch {
    // 网络错误时静默失败
    return null
  }
}

function isNewerVersion(remote, local) {
  const r = remote.split('.').map(Number)
  const l = local.split('.').map(Number)

  for (let i = 0; i < 3; i++) {
    if ((r[i] || 0) > (l[i] || 0)) return true
    if ((r[i] || 0) < (l[i] || 0)) return false
  }
  return false
}
