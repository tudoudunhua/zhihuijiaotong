import request from '@/utils/request';

/**
 * 获取违章预警列表
 */
export function getWarnings() {
  return request({
    url: '/warnings',
    method: 'get'
  });
}
