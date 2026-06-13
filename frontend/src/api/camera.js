import request from '@/utils/request'

export function getCameras() {
  return request({
    url: '/cameras',
    method: 'get',
  })
}

export function getCameraById(id) {
  return request({
    url: `/cameras/${id}`,
    method: 'get',
  })
}

export function addCamera(data) {
  return request({
    url: '/cameras',
    method: 'post',
    data,
  })
}

export function updateCamera(id, data) {
  return request({
    url: `/cameras/${id}`,
    method: 'put',
    data,
  })
}

export function deleteCamera(id) {
  return request({
    url: `/cameras/${id}`,
    method: 'delete',
  })
}


