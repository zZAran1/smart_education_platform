/**
 * 测试辅助工具：通过「验证码 → Redis 取答案 → 登录」拿到 Token，
 * 供浏览器端到端测试注入 localStorage 使用（图形验证码无法自动识别，故走此通道）。
 *
 * 用法：
 *   node src/test/e2e/get-token.mjs                # 默认 student01
 *   node src/test/e2e/get-token.mjs admin          # 指定账号
 *   node src/test/e2e/get-token.mjs admin --json   # 输出整行 JSON（含用户信息）
 *
 * 密码可覆盖（改过密码后无需改代码）：
 *   EDU_TEST_PASSWORD    所有账号的默认密码（默认 admin123456）
 *   EDU_ADMIN_PASSWORD   仅 admin 账号的密码（优先于上面那个）
 */
import { execFileSync } from 'node:child_process'

const BASE = process.env.API_BASE || 'http://localhost:8080/api'
const REDIS_CLI = process.env.REDIS_CLI || 'D:/redis/redis-cli.exe'
const username = process.argv[2] || 'student01'
const asJson = process.argv.includes('--json')
const PASSWORD = process.env.EDU_TEST_PASSWORD || 'admin123456'
const password = username === 'admin' ? process.env.EDU_ADMIN_PASSWORD || PASSWORD : PASSWORD

const cap = await fetch(`${BASE}/user/captcha`).then((r) => r.json())
if (cap.code !== 200) {
  console.error('获取验证码失败：', JSON.stringify(cap))
  process.exit(1)
}
const uuid = cap.data.uuid
const code = execFileSync(REDIS_CLI, ['get', `captcha:${uuid}`], { encoding: 'utf8' }).trim()

const res = await fetch(`${BASE}/user/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password: 'admin123456', uuid, captcha: code }),
}).then((r) => r.json())

if (res.code !== 200) {
  console.error('登录失败：', JSON.stringify(res))
  process.exit(1)
}

if (asJson) {
  console.log(JSON.stringify({ token: res.data.token, user: res.data.user_vo }))
} else {
  console.log(res.data.token)
}
