// 内置API配置
const DEFAULT_CONFIG = {
  apiUrl: 'https://api.deepseek.com/v1',
  apiKey: 'sk-b4eec3abeb5d4f229dea9d0aa09c1a40',
  model: 'deepseek-v4-pro',
}

export function getLLMConfig() {
  return { ...DEFAULT_CONFIG }
}

export function saveLLMConfig(config) {
  // 内置配置，无需保存
}

function getLocalISOString() {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  const local = new Date(now.getTime() - offset * 60 * 1000)
  return local.toISOString().replace('Z', '+08:00')
}

function getLocalDateString() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

export async function parseWithLLM(text) {
  const config = getLLMConfig()

  if (!config.apiUrl || !config.apiKey) {
    throw new Error('请先配置大模型API地址和Key')
  }

  const now = new Date()
  const currentTime = getLocalDateString()
  const dayOfWeek = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]

  const systemPrompt = `你是一个待办事项解析助手。用户会用语音输入一段话，你需要从中提取待办事项信息。

当前时间：${currentTime} 星期${dayOfWeek}（北京时间 UTC+8）

请严格按以下JSON格式返回，不要返回其他任何内容：
{
  "todos": [
    {
      "task": "任务内容",
      "remindTime": "提醒时间(北京时间格式：YYYY-MM-DDTHH:mm:ss+08:00，没有则为null)",
      "isAlarm": false
    }
  ]
}

解析规则（所有时间都是北京时间 UTC+8）：
1. 如果用户说了"X分钟后提醒我Y"，remindTime就是当前时间+X分钟
2. 如果用户说了"X小时后提醒我Y"，remindTime就是当前时间+X小时
3. 如果用户说了"下午3点提醒我Y"，remindTime就是今天下午15:00:00+08:00
4. 如果用户说了"明天早上8点Y"，remindTime就是明天08:00:00+08:00
5. 如果用户说了"定个闹钟"或"设个闹钟"，isAlarm设为true
6. 如果没有明确的时间信息，remindTime为null
7. 如果用户说了多个任务，返回多个todo项
8. remindTime必须带+08:00时区后缀，绝对不要用Z结尾

示例（假设当前时间是2024-01-15 10:30）：
- "下午3点提醒我开会" → {"todos":[{"task":"开会","remindTime":"2024-01-15T15:00:00+08:00","isAlarm":false}]}
- "5分钟后提醒我喝水" → {"todos":[{"task":"喝水","remindTime":"2024-01-15T10:35:00+08:00","isAlarm":false}]}
- "1小时后提醒我吃药" → {"todos":[{"task":"吃药","remindTime":"2024-01-15T11:30:00+08:00","isAlarm":false}]}
- "买菜" → {"todos":[{"task":"买菜","remindTime":null,"isAlarm":false}]}
- "定个明天早上8点的闹钟叫我起床" → {"todos":[{"task":"起床","remindTime":"2024-01-16T08:00:00+08:00","isAlarm":true}]}
- "提醒我下午2点开会，还有3点和客户打电话" → {"todos":[{"task":"开会","remindTime":"2024-01-15T14:00:00+08:00","isAlarm":false},{"task":"和客户打电话","remindTime":"2024-01-15T15:00:00+08:00","isAlarm":false}]}`

  const url = config.apiUrl.replace(/\/$/, '')

  const response = await fetch(`${url}/chat/completions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${config.apiKey}`,
    },
    body: JSON.stringify({
      model: config.model || 'gpt-3.5-turbo',
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: text },
      ],
      temperature: 0.1,
    }),
  })

  if (!response.ok) {
    const err = await response.text()
    throw new Error(`API请求失败: ${response.status} ${err}`)
  }

  const data = await response.json()
  const content = data.choices?.[0]?.message?.content || ''

  // 提取JSON部分
  const jsonMatch = content.match(/\{[\s\S]*\}/)
  if (!jsonMatch) {
    throw new Error('大模型返回格式异常')
  }

  const parsed = JSON.parse(jsonMatch[0])

  // 转换remindTime为时间戳
  return (parsed.todos || []).map(todo => ({
    task: todo.task,
    remindTime: parseRemindTime(todo.remindTime),
    isAlarm: todo.isAlarm || false,
  }))
}

function parseRemindTime(timeStr) {
  if (!timeStr) return null

  // 直接解析带时区的ISO字符串
  const timestamp = new Date(timeStr).getTime()

  // 如果解析失败，尝试手动解析 YYYY-MM-DDTHH:mm:ss 格式（无时区）
  if (isNaN(timestamp)) {
    const match = timeStr.match(/(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/)
    if (match) {
      const [_, year, month, day, hour, minute, second] = match
      // 当作北京时间解析
      const date = new Date(`${year}-${month}-${day}T${hour}:${minute}:${second}+08:00`)
      return date.getTime()
    }
    return null
  }

  return timestamp
}
