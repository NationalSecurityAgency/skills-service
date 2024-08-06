/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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