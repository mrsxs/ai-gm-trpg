import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import router from '../router'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 70000 // AI 回合较慢，留足 60s+
})

request.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 统一返回体 R{code,message,data}
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401) {
      const store = useUserStore()
      store.clear()
      ElMessage.error(body?.message || '登录已失效，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(body?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
