import request from '@/utils/request'

// 统计总览
export function getOverview() {
  return request.get('/statistics/overview')
}

// 按违章类型统计
export function getTypeStats() {
  return request.get('/statistics/by-type')
}

// 按月份统计
export function getMonthStats() {
  return request.get('/statistics/by-month')
}

// 违章次数最多的车牌
export function getTopPlates(limit = 10) {
  return request.get('/statistics/top-plates', {
    params: { limit }
  })
}
