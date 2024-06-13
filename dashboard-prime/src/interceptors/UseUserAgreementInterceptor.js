import axios from 'axios'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'

export const useUserAgreementInterceptor = () => {

  const appInfoState = useAppInfoState()

  const handleFunction = (response) => {
    const uaHeader = response?.headers?.['skills-display-ua']
    if (uaHeader) {
      appInfoState.setShowUa(true)
    }
  }

  const register = () => {
    axios.interceptors.response.use(
      (response) => {
        handleFunction(response)
        return response
      }
    )
  }

  return {
    register
  }
}