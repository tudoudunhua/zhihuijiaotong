import request from '@/utils/request'

export function getUsers() {
  return request({
    url: '/users',
    method: 'get',
  })
}

export function updateUserPermissions(id, data) {
  return request({
    url: `/users/${id}/permissions`,
    method: 'put',
    data,
  })
}

export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data,
  })
}

export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete',
  })
}
