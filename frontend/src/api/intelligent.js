import request from '@/utils/request'

/** 获取违章类型下拉选项 */
export function getTypes() {
  return request({
    url: '/intelligent/types',
    method: 'get',
  })
}

/** 获取行政区下拉选项 */
export function getDistricts() {
  return request({
    url: '/intelligent/districts',
    method: 'get',
  })
}

/** 获取最新智能预警列表 */
export function getLatestIntelligentWarnings() {
  return request({
    url: '/intelligent/latest-warnings',
    method: 'get',
  })
}

/** 智能违章预警预测 */
export function getIntelligentWarning(params) {
  return request({
    url: '/intelligent/predict',
    method: 'get',
    params,
  })
}

/** 手动触发：根据天气规则生成一批智能预警 */
export function triggerWeatherWarnings() {
  return request({
    url: '/intelligent/trigger-weather',
    method: 'post',
  })
}

/** 复犯车辆预警列表（近30天） */
export function getRepeatOffenders(params) {
  return request({
    url: '/intelligent/repeat-offenders',
    method: 'get',
    params,
  })
}
