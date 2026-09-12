/**
 * 端到端集成测试（真实 HTTP 调用，不参与构建、不影响项目运行）
 *
 * 覆盖三条业务闭环：
 *   1. 课程学习闭环：浏览 → 收藏 → 报名 → 支付 → 评论 → 答疑 → 章节学完
 *   2. 职位求职闭环：浏览 → 收藏 → 投递 → 重复投递拦截 → AI 面试幂等
 *   3. 后台管理闭环：用户/课程/章节/职位/公司/评论/答疑/订单/申请/面试
 * 另含鉴权与越权校验。用例均设计为可重复执行（幂等断言）。
 *
 * 运行：node src/test/e2e/api-flow-test.mjs
 * 前置：后端已在 8080 运行、Redis 可用（脚本通过 redis-cli 读取图形验证码答案）
 *
 * 密码可覆盖（改过密码后无需改代码）：
 *   EDU_TEST_PASSWORD    所有账号的默认密码（默认 admin123456）
 *   EDU_ADMIN_PASSWORD   仅 admin 账号的密码（优先于上面那个）
 */
import { execFileSync } from 'node:child_process'

const BASE = process.env.API_BASE || 'http://localhost:8080/api'
const REDIS_CLI = process.env.REDIS_CLI || 'D:/redis/redis-cli.exe'
const PASSWORD = process.env.EDU_TEST_PASSWORD || 'admin123456'
/** admin 密码可单独覆盖：改过密码后无需改代码，设置 EDU_ADMIN_PASSWORD 即可 */
const ADMIN_PASSWORD = process.env.EDU_ADMIN_PASSWORD || PASSWORD

let pass = 0
let fail = 0
const failures = []

function check(name, cond, detail = '') {
  if (cond) {
    pass += 1
    console.log(`  [PASS] ${name}`)
  } else {
    fail += 1
    failures.push(`${name} -> ${detail}`)
    console.log(`  [FAIL] ${name} -> ${detail}`)
  }
}

function section(title) {
  console.log(`\n=== ${title} ===`)
}

async function api(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  try {
    return { http: res.status, ...JSON.parse(text) }
  } catch {
    return { http: res.status, code: -1, msg: text.slice(0, 200) }
  }
}

const get = (p, o) => api('GET', p, o)
const post = (p, o) => api('POST', p, o)
const put = (p, o) => api('PUT', p, o)
const del = (p, o) => api('DELETE', p, o)

/** 走完整登录链路：取验证码 → redis-cli 读答案 → 登录 */
async function login(username, password = PASSWORD) {
  const cap = await get('/user/captcha')
  if (cap.code !== 200) throw new Error(`获取验证码失败: ${JSON.stringify(cap)}`)
  const uuid = cap.data.uuid
  const code = execFileSync(REDIS_CLI, ['get', `captcha:${uuid}`], { encoding: 'utf8' }).trim()
  const r = await post('/user/login', { body: { username, password, uuid, captcha: code } })
  if (r.code !== 200) throw new Error(`登录失败(${username}): ${JSON.stringify(r)}`)
  return r.data.token
}

async function main() {
  console.log(`端到端集成测试  base=${BASE}\n`)

  /* ============ 0. 鉴权 ============ */
  section('0. 鉴权与越权')
  const anonCourse = await get('/course/page?type=0')
  check('匿名访问课程列表被拒(1002)', anonCourse.code === 1002, JSON.stringify(anonCourse))

  const forged = await get('/course/page?type=0', { token: 'forged.token.value' })
  check('伪造 Token 被拒(1002)', forged.code === 1002, JSON.stringify(forged))

  const studentToken = await login('student01')
  const studentToken2 = await login('student02')
  const adminToken = await login('admin', ADMIN_PASSWORD)
  check('学员登录成功', typeof studentToken === 'string' && studentToken.length > 20)
  check('管理员登录成功', typeof adminToken === 'string' && adminToken.length > 20)

  const studentHitAdmin = await get('/admin/users?page_num=1&page_size=5', { token: studentToken })
  check('学员访问后台被拒(1013)', studentHitAdmin.code === 1013, JSON.stringify(studentHitAdmin))

  /* ============ 1. 课程学习闭环 ============ */
  section('1. 课程学习闭环')

  const cats = await get('/course/categories', { token: studentToken })
  check('课程分类树可获取', cats.code === 200 && Array.isArray(cats.data), JSON.stringify(cats).slice(0, 200))

  const page = await get('/course/page?type=0&page_num=1&page_size=10', { token: studentToken })
  check('课程分页可获取', page.code === 200 && Array.isArray(page.data.records), JSON.stringify(page).slice(0, 200))
  const courses = page.data?.records || []
  check('课程列表有数据', courses.length > 0, `records=${courses.length}`)

  const free = courses.find((c) => Number(c.is_free) === 1) || courses[0]
  const paid = courses.find((c) => Number(c.is_free) === 0)

  if (free) {
    const detail = await get(`/course/${free.id}`, { token: studentToken })
    check('课程详情可获取', detail.code === 200 && detail.data, JSON.stringify(detail).slice(0, 200))

    const chapters = await get(`/course/${free.id}/chapters`, { token: studentToken })
    check('章节目录可获取', chapters.code === 200 && Array.isArray(chapters.data),
      JSON.stringify(chapters).slice(0, 200))

    const c1 = await post(`/course/${free.id}/collect`, { token: studentToken })
    const c2 = await post(`/course/${free.id}/collect`, { token: studentToken })
    check('课程收藏可切换', c1.code === 200 && c2.code === 200 && c1.data !== c2.data,
      `c1=${c1.data} c2=${c2.data}`)

    // 报名（重复执行时返回"请勿重复报名"，两者都是正确行为）
    const enroll = await post(`/course/${free.id}/enroll`, { token: studentToken })
    check('免费课程报名（首次成功 / 重复提示已报名）',
      enroll.code === 200 || enroll.code === 1010, JSON.stringify(enroll))

    // 评论 + 评分联动（重复执行时不再改变统计）
    const before = await get(`/course/${free.id}`, { token: studentToken })
    const beforeCount = Number(before.data?.rating_count || 0)
    const comment = await post(`/course/${free.id}/comments`, {
      token: studentToken,
      body: { score: 5, content: '端到端测试评论' },
    })
    const commentDuplicated = comment.code === 1010
    check('发表评论（首次成功 / 重复提示已评价）',
      comment.code === 200 || commentDuplicated, JSON.stringify(comment))
    if (commentDuplicated) {
      console.log('  [SKIP] 评分人次递增（本轮为重复评价，统计不变）')
    } else {
      const after = await get(`/course/${free.id}`, { token: studentToken })
      check('评分人次递增', Number(after.data?.rating_count || 0) === beforeCount + 1,
        `before=${beforeCount} after=${after.data?.rating_count}`)
    }

    const comments = await get(`/course/${free.id}/comments?page_num=1&page_size=5`, { token: studentToken })
    check('评论分页可获取', comments.code === 200 && Array.isArray(comments.data.records),
      JSON.stringify(comments).slice(0, 200))

    // 提问接口没有唯一约束、也没有删除接口，重复执行会不断累积脏数据；
    // 故先查后写：同课程已存在同内容提问时跳过写入，保证脚本可反复执行
    const QUESTION_TEXT = '端到端测试提问'
    const qsBefore = await get(`/course/${free.id}/questions`, { token: studentToken })
    const questionExisted = (qsBefore.data || []).some((item) => item.question === QUESTION_TEXT)
    if (questionExisted) {
      console.log('  [SKIP] 提交提问（该课程已存在同内容提问，跳过写入以保持可重复执行）')
      const existing = (qsBefore.data || []).find((item) => item.question === QUESTION_TEXT)
      check('答疑列表可获取', qsBefore.code === 200 && Array.isArray(qsBefore.data),
        JSON.stringify(qsBefore).slice(0, 200))
      check('已提问过则复用既有记录（不再重复写入）', !!existing, JSON.stringify(existing).slice(0, 200))
    } else {
      const q = await post(`/course/${free.id}/questions`, {
        token: studentToken,
        body: { question: QUESTION_TEXT },
      })
      check('提交提问成功', q.code === 200, JSON.stringify(q))
      const qs = await get(`/course/${free.id}/questions`, { token: studentToken })
      check('答疑列表可获取', qs.code === 200 && Array.isArray(qs.data), JSON.stringify(qs).slice(0, 200))
    }

    // 章节学完（接口位于 /api/study）
    if (chapters.data?.length) {
      const ch = chapters.data[0]
      const fin = await post(`/study/chapters/${ch.id}/finish`, { token: studentToken })
      check('标记章节学完成功', fin.code === 200, JSON.stringify(fin))
    }
  } else {
    check('存在免费课程用于测试', false, '未找到免费课程')
  }

  // 收费课程 → 下单 → 支付
  if (paid) {
    const enroll = await post(`/course/${paid.id}/enroll`, { token: studentToken })
    const alreadyEnrolled = enroll.code === 1010
    check('收费课程报名（首次生成待支付订单 / 重复提示已报名）',
      (enroll.code === 200 && !!enroll.data?.order_no) || alreadyEnrolled, JSON.stringify(enroll))
    const orderNo = enroll.data?.order_no
    if (orderNo) {
      const pay = await post(`/order/${orderNo}/pay`, { token: studentToken, body: { pay_type: 0 } })
      check('订单支付成功', pay.code === 200, JSON.stringify(pay))
      const again = await post(`/order/${orderNo}/pay`, { token: studentToken, body: { pay_type: 0 } })
      check('重复支付被幂等拦截(1012)', again.code === 1012, JSON.stringify(again))
    } else {
      console.log('  [SKIP] 订单支付流程（本轮已报名，未产生新订单）')
    }
  } else {
    check('存在收费课程用于测试', false, '未找到收费课程')
  }

  /* ============ 2. 职位求职闭环 ============ */
  section('2. 职位求职闭环')

  const jobCats = await get('/job/categories', { token: studentToken })
  check('职位分类树可获取', jobCats.code === 200 && Array.isArray(jobCats.data), JSON.stringify(jobCats).slice(0, 200))

  const jobPage = await get('/job/page?page_num=1&page_size=10', { token: studentToken })
  check('职位分页可获取', jobPage.code === 200 && Array.isArray(jobPage.data.records),
    JSON.stringify(jobPage).slice(0, 200))
  const jobs = jobPage.data?.records || []
  check('职位列表有数据', jobs.length > 0, `records=${jobs.length}`)

  if (jobs.length) {
    const job = jobs[0]
    const jd = await get(`/job/${job.id}`, { token: studentToken })
    check('职位详情可获取', jd.code === 200 && jd.data, JSON.stringify(jd).slice(0, 200))
    check('职位详情含公司信息', !!jd.data?.company_name, `company_name=${jd.data?.company_name}`)

    const jc1 = await post(`/job/${job.id}/collect`, { token: studentToken })
    const jc2 = await post(`/job/${job.id}/collect`, { token: studentToken })
    check('职位收藏可切换', jc1.code === 200 && jc2.code === 200 && jc1.data !== jc2.data,
      `c1=${jc1.data} c2=${jc2.data}`)

    const apply1 = await post(`/job/${job.id}/apply`, { token: studentToken2, body: {} })
    check('投递职位（首次成功 / 重复提示已投递）',
      apply1.code === 200 || apply1.code === 1011, JSON.stringify(apply1))
    const apply2 = await post(`/job/${job.id}/apply`, { token: studentToken2, body: {} })
    check('重复投递被拦截(1011)', apply2.code === 1011, JSON.stringify(apply2))

    // AI 面试必须以「本轮刚投递的账号」发起：列表第一条会随职位上下架/新增而变化，
    // 若换成另一个账号，其历史投递记录未必覆盖当前第一条，会误报「请先投递该职位」
    const ai1 = await post(`/job/${job.id}/ai-interview`, { token: studentToken2 })
    const ai2 = await post(`/job/${job.id}/ai-interview`, { token: studentToken2 })
    check('AI 面试申请成功', ai1.code === 200, JSON.stringify(ai1))
    check('AI 面试重复申请幂等(返回同一记录)',
      ai1.code === 200 && ai2.code === 200 && ai1.data === ai2.data,
      `id1=${ai1.data} id2=${ai2.data}`)
  }

  /* ============ 3. 后台管理闭环 ============ */
  section('3. 后台管理闭环')

  const aUsers = await get('/admin/users?page_num=1&page_size=5', { token: adminToken })
  check('后台用户分页', aUsers.code === 200 && Array.isArray(aUsers.data.records), JSON.stringify(aUsers).slice(0, 200))

  const aCourses = await get('/admin/courses?page_num=1&page_size=5', { token: adminToken })
  check('后台课程分页', aCourses.code === 200 && Array.isArray(aCourses.data.records), JSON.stringify(aCourses).slice(0, 200))

  const aJobs = await get('/admin/jobs?page_num=1&page_size=5', { token: adminToken })
  check('后台职位分页', aJobs.code === 200 && Array.isArray(aJobs.data.records), JSON.stringify(aJobs).slice(0, 200))

  const aOrders = await get('/admin/orders?page_num=1&page_size=5', { token: adminToken })
  check('后台订单分页', aOrders.code === 200 && Array.isArray(aOrders.data.records), JSON.stringify(aOrders).slice(0, 200))

  const aApps = await get('/admin/applications?page_num=1&page_size=5', { token: adminToken })
  check('后台申请分页', aApps.code === 200 && Array.isArray(aApps.data.records), JSON.stringify(aApps).slice(0, 200))

  const aInterviews = await get('/admin/interviews?page_num=1&page_size=5', { token: adminToken })
  check('后台面试分页', aInterviews.code === 200 && Array.isArray(aInterviews.data.records), JSON.stringify(aInterviews).slice(0, 200))

  const aCompanies = await get('/admin/companies?page_num=1&page_size=5', { token: adminToken })
  check('后台公司分页', aCompanies.code === 200 && Array.isArray(aCompanies.data.records), JSON.stringify(aCompanies).slice(0, 200))

  const aComments = await get('/admin/comments?page_num=1&page_size=5', { token: adminToken })
  check('后台评论分页', aComments.code === 200 && Array.isArray(aComments.data.records), JSON.stringify(aComments).slice(0, 200))

  const aQuestions = await get('/admin/questions?page_num=1&page_size=5', { token: adminToken })
  check('后台答疑分页', aQuestions.code === 200 && Array.isArray(aQuestions.data.records), JSON.stringify(aQuestions).slice(0, 200))

  // 公司 CRUD 闭环
  const newCompany = await post('/admin/companies', {
    token: adminToken,
    body: { name: `E2E测试公司_${Date.now()}`, industry: '测试行业', scale: '100-499人', city: '珠海', intro: '端到端测试' },
  })
  check('新增公司成功', newCompany.code === 200 && newCompany.data, JSON.stringify(newCompany))
  const companyId = newCompany.data
  if (companyId) {
    const upd = await put(`/admin/companies/${companyId}`, {
      token: adminToken,
      body: { name: `E2E测试公司改_${Date.now()}`, industry: '测试行业', scale: '100-499人', city: '珠海', intro: 'x' },
    })
    check('修改公司成功', upd.code === 200, JSON.stringify(upd))
    const delRes = await del(`/admin/companies/${companyId}`, { token: adminToken })
    check('删除公司成功', delRes.code === 200, JSON.stringify(delRes))
  }

  // 章节 CRUD 闭环（验证课程资源计数与目录同步）
  const targetCourse = aCourses.data?.records?.[0]
  if (targetCourse) {
    const created = await post(`/admin/courses/${targetCourse.id}/chapters`, {
      token: adminToken,
      body: { title: 'E2E测试章节', resource_type: 1, duration: 600, sort: 999 },
    })
    check('新增章节成功', created.code === 200 && created.data, JSON.stringify(created))
    const chapterId = created.data
    if (chapterId) {
      const after = await get(`/course/${targetCourse.id}`, { token: adminToken })
      const chListAfter = await get(`/admin/courses/${targetCourse.id}/chapters`, { token: adminToken })
      const actualVideo = (chListAfter.data || []).filter((c) => Number(c.resource_type) === 1).length
      check('章节新增后课程视频计数与实际章节同步',
        Number(after.data?.video_count || 0) === actualVideo,
        `video_count=${after.data?.video_count} 实际视频章节=${actualVideo}`)

      const chList = await get(`/admin/courses/${targetCourse.id}/chapters`, { token: adminToken })
      check('后台章节列表可获取', chList.code === 200 && Array.isArray(chList.data), JSON.stringify(chList).slice(0, 200))

      const updCh = await put(`/admin/chapters/${chapterId}`, {
        token: adminToken,
        body: { title: 'E2E测试章节改', resource_type: 1, duration: 700, sort: 999 },
      })
      check('修改章节成功', updCh.code === 200, JSON.stringify(updCh))

      const delCh = await del(`/admin/chapters/${chapterId}`, { token: adminToken })
      check('删除章节成功', delCh.code === 200, JSON.stringify(delCh))
      const final = await get(`/course/${targetCourse.id}`, { token: adminToken })
      const chListFinal = await get(`/admin/courses/${targetCourse.id}/chapters`, { token: adminToken })
      const actualVideoFinal = (chListFinal.data || []).filter((c) => Number(c.resource_type) === 1).length
      check('章节删除后计数与实际章节同步',
        Number(final.data?.video_count || 0) === actualVideoFinal,
        `video_count=${final.data?.video_count} 实际视频章节=${actualVideoFinal}`)
    }
  }

  /* ============ 4. 学员「我的」记录闭环 ============ */
  section('4. 学员「我的」记录（个人中心数据源）')

  const anonMine = [
    ['/course/my', await get('/course/my')],
    ['/order/my', await get('/order/my')],
    ['/job/my-applications', await get('/job/my-applications')],
    ['/job/my-interviews', await get('/job/my-interviews')],
  ]
  for (const [path, res] of anonMine) {
    check(`匿名访问 ${path} 被拒(1002)`, res.code === 1002, JSON.stringify(res))
  }

  const myCourses = await get('/course/my?page_num=1&page_size=10', { token: studentToken })
  check('我的课程分页可获取', myCourses.code === 200 && Array.isArray(myCourses.data?.records),
    JSON.stringify(myCourses).slice(0, 200))
  const myCourseRecords = myCourses.data?.records || []
  check('我的课程有数据', myCourseRecords.length > 0, `records=${myCourseRecords.length}`)
  check('我的课程每条含 course_id 与学习进度',
    myCourseRecords.every((r) => r.course_id != null && r.progress != null),
    JSON.stringify(myCourseRecords).slice(0, 300))
  check('我的课程含课程标题（关联查询生效）',
    myCourseRecords.some((r) => !!r.title), JSON.stringify(myCourseRecords).slice(0, 300))

  const myOrders = await get('/order/my?page_num=1&page_size=10', { token: studentToken })
  check('我的订单分页可获取', myOrders.code === 200 && Array.isArray(myOrders.data?.records),
    JSON.stringify(myOrders).slice(0, 200))
  const myOrderRecords = myOrders.data?.records || []
  check('我的订单有数据', myOrderRecords.length > 0, `records=${myOrderRecords.length}`)
  check('我的订单每条含订单号与状态',
    myOrderRecords.every((r) => !!r.order_no && r.status != null),
    JSON.stringify(myOrderRecords).slice(0, 300))
  check('我的订单含课程标题（关联查询生效）',
    myOrderRecords.some((r) => !!r.course_title), JSON.stringify(myOrderRecords).slice(0, 300))

  const myApps = await get('/job/my-applications?page_num=1&page_size=10', { token: studentToken })
  check('我的投递分页可获取', myApps.code === 200 && Array.isArray(myApps.data?.records),
    JSON.stringify(myApps).slice(0, 200))
  const myAppRecords = myApps.data?.records || []
  check('我的投递有数据', myAppRecords.length > 0, `records=${myAppRecords.length}`)
  check('我的投递每条含 job_id 与状态',
    myAppRecords.every((r) => r.job_id != null && r.status != null),
    JSON.stringify(myAppRecords).slice(0, 300))
  check('我的投递含职位名与公司名（关联查询生效）',
    myAppRecords.some((r) => !!r.job_title && !!r.company_name),
    JSON.stringify(myAppRecords).slice(0, 300))

  const myInterviews = await get('/job/my-interviews?page_num=1&page_size=10', { token: studentToken })
  check('我的面试分页可获取', myInterviews.code === 200 && Array.isArray(myInterviews.data?.records),
    JSON.stringify(myInterviews).slice(0, 200))

  // 数据隔离：不同账号只能看到自己的记录
  const myCourses2 = await get('/course/my?page_num=1&page_size=10', { token: studentToken2 })
  const myOrders2 = await get('/order/my?page_num=1&page_size=10', { token: studentToken2 })
  const myApps2 = await get('/job/my-applications?page_num=1&page_size=10', { token: studentToken2 })
  const ids = (res) => (res.data?.records || []).map((r) => r.id)
  const disjoint = (a, b) => a.every((id) => !b.includes(id))
  check('两个学员的报名记录互不可见',
    disjoint(ids(myCourses), ids(myCourses2)), `s1=${ids(myCourses)} s2=${ids(myCourses2)}`)
  check('两个学员的订单互不可见',
    disjoint(ids(myOrders), ids(myOrders2)), `s1=${ids(myOrders)} s2=${ids(myOrders2)}`)
  check('两个学员的投递记录互不可见',
    disjoint(ids(myApps), ids(myApps2)), `s1=${ids(myApps)} s2=${ids(myApps2)}`)

  // 越权：伪造 user_id 不生效（后端只认 Token 里的身份）
  const forgedMine = await get('/course/my?page_num=1&page_size=10&user_id=2', { token: studentToken2 })
  check('前端伪造 user_id 无法越权读取他人记录',
    forgedMine.code === 200 && disjoint(ids(forgedMine), ids(myCourses)),
    `伪造 user_id=2 得到=${ids(forgedMine)} 他人=${ids(myCourses)}`)

  // 分页兜底
  const mineHuge = await get('/job/my-applications?page_num=1&page_size=500', { token: studentToken })
  check('我的接口 page_size 上限被限制在 100', mineHuge.code === 200 && mineHuge.data?.size <= 100,
    `size=${mineHuge.data?.size}`)
  const mineEmpty = await get('/course/my?page_num=&page_size=', { token: studentToken })
  check('我的接口空分页参数不报错', mineEmpty.code === 200, JSON.stringify(mineEmpty).slice(0, 200))

  /* ============ 5. 职位分类筛选语义 ============ */
  section('5. 职位分类筛选（一级 = 该一级 + 其下所有二级）')

  const jobTree = await get('/job/categories', { token: studentToken })
  const rootCats = Array.isArray(jobTree.data) ? jobTree.data : []
  check('职位分类树可获取', rootCats.length > 0, JSON.stringify(jobTree).slice(0, 200))

  const allJobsRes = await get('/job/page?page_num=1&page_size=100', { token: studentToken })
  const allJobs = allJobsRes.data?.records || []
  check('职位全量列表可获取', allJobsRes.code === 200 && allJobs.length > 0, `records=${allJobs.length}`)

  const rootWithChildren = rootCats.find((r) => (r.children || []).length > 0)
  check('存在带二级分类的一级分类', !!rootWithChildren,
    `一级=${rootCats.map((r) => r.name).join(',')}`)

  if (rootWithChildren) {
    const childIds = rootWithChildren.children.map((c) => c.id)
    // 期望值直接从全量列表按 category_id 归属统计，不写死数字
    const expectedRoot = allJobs.filter(
      (j) => j.category_id === rootWithChildren.id || childIds.includes(j.category_id)).length
    const rootRes = await get(
      `/job/page?page_num=1&page_size=100&category_id=${rootWithChildren.id}`, { token: studentToken })
    check(`一级分类「${rootWithChildren.name}」命中其全部子类职位`,
      rootRes.data?.total === expectedRoot,
      `期望 ${expectedRoot} 实际 ${rootRes.data?.total}`)
    check(`一级分类「${rootWithChildren.name}」结果不超过全量`,
      rootRes.data?.total <= allJobsRes.data?.total,
      `一级=${rootRes.data?.total} 全量=${allJobsRes.data?.total}`)

    const leaf = rootWithChildren.children[0]
    const expectedLeaf = allJobs.filter((j) => j.category_id === leaf.id).length
    const leafRes = await get(
      `/job/page?page_num=1&page_size=100&category_id=${leaf.id}`, { token: studentToken })
    check(`二级分类「${leaf.name}」精确匹配自身`,
      leafRes.data?.total === expectedLeaf,
      `期望 ${expectedLeaf} 实际 ${leafRes.data?.total}`)
    check(`二级「${leaf.name}」结果是一级「${rootWithChildren.name}」的子集`,
      leafRes.data?.total <= rootRes.data?.total,
      `二级=${leafRes.data?.total} 一级=${rootRes.data?.total}`)

    const emptyRoot = rootCats.find((r) => !childIds.includes(r.id)
      && (r.children || []).length === 0 && !allJobs.some((j) => j.category_id === r.id))
    if (emptyRoot) {
      const emptyRes = await get(
        `/job/page?page_num=1&page_size=100&category_id=${emptyRoot.id}`, { token: studentToken })
      check(`无职位的一级分类「${emptyRoot.name}」返回空列表`,
        emptyRes.code === 200 && emptyRes.data?.total === 0, JSON.stringify(emptyRes).slice(0, 200))
    }
  }

  /* ============ 汇总 ============ */
  console.log(`\n================ 结果 ================`)
  console.log(`通过 ${pass} / 失败 ${fail}`)
  if (failures.length) {
    console.log('\n失败明细：')
    failures.forEach((f, i) => console.log(`${i + 1}. ${f}`))
  }
  process.exit(fail > 0 ? 1 : 0)
}

main().catch((e) => {
  console.error('\n测试执行中断:', e.message)
  process.exit(2)
})
