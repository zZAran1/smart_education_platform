import { describe, expect, it } from 'vitest'

import {
  APPLICATION_STATUS,
  APPLICATION_STATUS_MAP,
  INTERVIEW_STATUS,
  INTERVIEW_STATUS_MAP,
  JOB_STATUS,
  ORDER_STATUS,
  ORDER_STATUS_MAP,
  QUESTION_STATUS,
  ROLE,
  ROLE_MAP,
  SUCCESS_CODE,
  TOKEN_EXPIRED_CODE,
  USER_STATUS,
  formatDuration,
  formatSalary,
  toNumber,
} from './api'

describe('toNumber 数值兜底', () => {
  it('null / undefined 返回 0', () => {
    expect(toNumber(null)).toBe(0)
    expect(toNumber(undefined)).toBe(0)
  })

  it('后端以字符串返回的金额与评分可正确转换', () => {
    expect(toNumber('199.00')).toBe(199)
    expect(toNumber('4.8')).toBe(4.8)
  })

  it('非法字符串返回 0 而不是 NaN', () => {
    expect(toNumber('abc')).toBe(0)
    expect(Number.isNaN(toNumber('abc'))).toBe(false)
  })

  it('number 原样返回（含 0 与负数）', () => {
    expect(toNumber(0)).toBe(0)
    expect(toNumber(-1)).toBe(-1)
  })
})

describe('formatDuration 时长格式化', () => {
  it('非正数统一显示 0:00', () => {
    expect(formatDuration(0)).toBe('0:00')
    expect(formatDuration(-5)).toBe('0:00')
  })

  it('不足一小时使用 分:秒 且秒补零', () => {
    expect(formatDuration(59)).toBe('0:59')
    expect(formatDuration(65)).toBe('1:05')
    expect(formatDuration(600)).toBe('10:00')
  })

  it('达到一小时使用 时:分:秒', () => {
    expect(formatDuration(3600)).toBe('1:00:00')
    expect(formatDuration(3661)).toBe('1:01:01')
  })
})

describe('formatSalary 薪资展示', () => {
  it('两端为空显示面议', () => {
    expect(formatSalary()).toBe('面议')
    expect(formatSalary(null, null)).toBe('面议')
  })

  it('区间、仅下限、仅上限三种形态', () => {
    expect(formatSalary(8, 12)).toBe('8-12K')
    expect(formatSalary(15, null)).toBe('15K起')
    expect(formatSalary(null, 20)).toBe('20K以内')
  })
})

describe('业务状态常量与文案映射', () => {
  it('角色映射覆盖学员/教师/管理员', () => {
    expect(ROLE.STUDENT).toBe(0)
    expect(ROLE_MAP.get(ROLE.STUDENT)).toBe('学员')
    expect(ROLE_MAP.get(ROLE.TEACHER)).toBe('教师')
    expect(ROLE_MAP.get(ROLE.ADMIN)).toBe('管理员')
  })

  it('订单状态映射与后端 0/1/2/3 对齐', () => {
    expect(ORDER_STATUS_MAP.get(ORDER_STATUS.PENDING)).toBe('待支付')
    expect(ORDER_STATUS_MAP.get(ORDER_STATUS.PAID)).toBe('已支付')
    expect(ORDER_STATUS_MAP.get(ORDER_STATUS.REFUNDED)).toBe('已退款')
    expect(ORDER_STATUS_MAP.get(ORDER_STATUS.CANCELED)).toBe('已取消')
  })

  it('职位申请状态映射与后端对齐', () => {
    expect(APPLICATION_STATUS_MAP.get(APPLICATION_STATUS.PENDING)).toBe('待处理')
    expect(APPLICATION_STATUS_MAP.get(APPLICATION_STATUS.PASSED)).toBe('通过')
    expect(APPLICATION_STATUS_MAP.get(APPLICATION_STATUS.REJECTED)).toBe('拒绝')
  })

  it('用户/职位/面试/答疑状态取值与后端一致', () => {
    expect(USER_STATUS.NORMAL).toBe(1)
    expect(USER_STATUS.BANNED).toBe(2)
    expect(JOB_STATUS.OFF).toBe(0)
    expect(JOB_STATUS.ON).toBe(1)
    expect(INTERVIEW_STATUS.DONE).toBe(2)
    expect(QUESTION_STATUS.ANSWERED).toBe(1)
  })

  it('面试状态映射覆盖待进行/进行中/已完成', () => {
    expect(INTERVIEW_STATUS_MAP.get(INTERVIEW_STATUS.TODO)).toBe('待进行')
    expect(INTERVIEW_STATUS_MAP.get(INTERVIEW_STATUS.DOING)).toBe('进行中')
    expect(INTERVIEW_STATUS_MAP.get(INTERVIEW_STATUS.DONE)).toBe('已完成')
  })

  it('响应码常量与后端 Result 约定一致', () => {
    expect(SUCCESS_CODE).toBe(200)
    expect(TOKEN_EXPIRED_CODE).toBe(1002)
  })
})
