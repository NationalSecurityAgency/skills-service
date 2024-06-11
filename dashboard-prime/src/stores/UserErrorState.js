import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'

export const userErrorState = defineStore('errorState', () => {
  const defaultTitle = 'Tiny-bit of an error!'
  const defaultExplanation = 'Oh no, something went wrong! An error occurred and we are unable to process your request, please try again later.'
  const defaultIcon = 'fas fa-heart-broken'
  const title = ref(defaultTitle)
  const explanation = ref(defaultExplanation)
  const icon = ref(defaultIcon)
  const router = useRouter()

  const setErrorParams = (titleParam, explanationParam, iconParam = null) => {
    title.value = titleParam
    explanation.value = explanationParam || defaultExplanation
    icon.value = iconParam || defaultIcon
  }
  const resetToDfeault = () => {
    title.value = defaultTitle
    explanation.value = defaultExplanation
    icon.value = defaultIcon
  }

  const navToErrorPage = (titleParam, explanationParam, iconParam = null) => {
    setErrorParams(titleParam, explanationParam, iconParam)
    router.push({ name: 'ErrorPage' })
  }
  return {
    setErrorParams,
    resetToDfeault,
    title,
    explanation,
    icon,
    navToErrorPage
  }
})