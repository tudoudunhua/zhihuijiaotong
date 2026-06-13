import request from '@/utils/request'

export function generateCapturedViolations(count = 10) {
  return request({
    url: `/simulate/captured-violations?count=${encodeURIComponent(count)}`,
    method: 'post',
  })
}

export function generateDemoWarnings(perLevel = 5) {
  return request({
    url: `/simulate/demo-warnings?perLevel=${encodeURIComponent(perLevel)}`,
    method: 'post',
  })
}

