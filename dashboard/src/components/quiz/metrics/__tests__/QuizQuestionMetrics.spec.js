/*
 * Copyright 2025 SkillTree
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
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import QuizQuestionMetrics from '@/components/quiz/metrics/QuizQuestionMetrics.vue'

const mountQuestion = (numAnsweredCorrect, numAnsweredWrong) => {
  return mount(QuizQuestionMetrics, {
    props: {
      q: {
        questionType: 'MultipleChoice',
        numAnsweredCorrect,
        numAnsweredWrong,
        answers: [],
      },
      isSurvey: false,
      num: 0,
      dateRange: [],
    },
    global: {
      stubs: {
        Chart: true,
        Column: true,
        DataTable: true,
        Tag: true,
        MarkdownText: true,
        CheckSelector: true,
        QuizAnswerHistory: true,
      },
    },
  })
}

describe('QuizQuestionMetrics correct/wrong percentages', () => {
  it('shows the wrong percentage with a single decimal and no floating point artifacts', () => {
    const wrapper = mountQuestion(10, 3)
    expect(wrapper.find('[data-cy="percentCorrect"]').text()).toBe('(76.9%)')
    expect(wrapper.find('[data-cy="percentWrong"]').text()).toBe('(23.1%)')
  })

  it('formats the wrong percentage consistently with the correct percentage', () => {
    const wrapper = mountQuestion(7, 3)
    expect(wrapper.find('[data-cy="percentCorrect"]').text()).toBe('(70.0%)')
    expect(wrapper.find('[data-cy="percentWrong"]').text()).toBe('(30.0%)')
  })
})
