import request from '@/utils/request'

export function getOwnerByPlate(plateNumber) {
  return request({
    url: `/owners/${encodeURIComponent(plateNumber)}`,
    method: 'get',
  })
}

export function upsertOwner(data) {
  return request({
    url: '/owners',
    method: 'post',
    data,
  })
}

