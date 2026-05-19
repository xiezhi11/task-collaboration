import request from '../utils/request'

export function login(username) {
  return request({
    url: '/auth/login',
    method: 'post',
    params: { username }
  })
}

export function getUserList() {
  return request({
    url: '/auth/users',
    method: 'get'
  })
}

export function getCurrentUser() {
  return request({
    url: '/auth/current-user',
    method: 'get'
  })
}
