import request from '@/utils/request'

export function getViolations() {
  return request({
    url: '/violations',
    method: 'get',
  })
}

// 待处理违章（摄像头自动抓拍未处理）
export function getPendingViolations() {
  return request({
    url: '/violations/pending',
    method: 'get',
  })
}

export function getViolationById(id) {
  return request({
    url: `/violations/${id}`,
    method: 'get',
  })
}

export function addViolation(data) {
  return request({
    url: '/violations',
    method: 'post',
    data,
  })
}

export function updateViolation(id, data) {
  return request({
    url: `/violations/${id}`,
    method: 'put',
    data,
  })
}

export function deleteViolation(id) {
  return request({
    url: `/violations/${id}`,
    method: 'delete',
  })
}

// 标记已处理
export function processViolation(id) {
  return request({
    url: `/violations/${id}/process`,
    method: 'post',
  })
}

// 通知车主（channel: AUTO/SMS/PHONE/MESSAGE, message 可选）
export function notifyViolationOwner(id, data) {
  return request({
    url: `/violations/${id}/notify`,
    method: 'post',
    data: data || {},
  })
}

// 上传 / 更换违章照片
export function uploadViolationImage(id, formData) {
  return request({
    url: `/violations/${id}/image`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// 为违章记录自动生成示例照片（根据违章类型匹配预设图片）
export function autoGenerateViolationImage(id) {
  return request({
    url: `/violations/${id}/auto-image`,
    method: 'post',
  })
}

