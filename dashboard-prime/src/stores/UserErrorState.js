import { ref } from 'vue'
import { defineStore } from 'pinia'

export const userErrorState = defineStore('errorState', () => {
  const defaultTitle = 'Tiny-bit of an error!'
  const defaultExplanation = 'Oh no, something went wrong! An error occurred and we are unable to process your request, please try again later.'
  const defaultIcon = 'fas fa-heart-broken'
  const title = ref(defaultTitle)
  const explanation = ref(defaultExplanation)
  const icon = ref(defaultIcon)

  const setErrorParams = (titleParam, explanationParam, iconParam) => {
    title.value = titleParam
    explanation.value = explanationParam
    icon.value = iconParam
  }
  const resetToDfeault = () => {
    title.value = defaultTitle
    explanation.value = defaultExplanation
    icon.value = defaultIcon
  }
  return {
    setErrorParams,
    resetToDfeault,
    title,
    explanation,
    icon
  }
})