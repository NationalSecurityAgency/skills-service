/*
 * Copyright 2026 SkillTree
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
import { describe, expect, it } from 'vitest'
import { array } from 'yup'
import { buildAnswerSchema } from './BuildAnswerSchema.js'

const validate = (answers, max) => array().of(buildAnswerSchema(max)).validate(answers, { abortEarly: false })
const messages = async (answers, max = 10) => {
  try {
    await validate(answers, max)
    return []
  } catch (e) {
    return e.inner.map((i) => `${i.path}: ${i.message}`)
  }
}

describe('buildAnswerSchema', () => {
  it('accepts an answer at exactly the maximum length', async () => {
    expect(await messages([{ answer: 'a'.repeat(10), isCorrect: true }])).toEqual([])
  })

  it('rejects an answer one character over the maximum length', async () => {
    expect(await messages([{ answer: 'a'.repeat(10), isCorrect: true }, { answer: 'a'.repeat(11), isCorrect: false }]))
      .toEqual(['[1].answer: Answer must be at most 10 characters'])
  })

  it('rejects over-long matching term and value', async () => {
    expect(await messages([{ multiPartAnswer: { term: 't'.repeat(11), value: 'v'.repeat(11) } }]))
      .toEqual([
        '[0].multiPartAnswer.term: Term must be at most 10 characters',
        '[0].multiPartAnswer.value: Answer must be at most 10 characters',
      ])
  })

  it('accepts matching answers within the limit and null/missing text', async () => {
    expect(await messages([{ multiPartAnswer: { term: 'ok', value: 'ok' } }, { answer: null }, {}])).toEqual([])
  })

  it('does not limit length when the config value is not available', async () => {
    expect(await messages([{ answer: 'a'.repeat(5000) }], null)).toEqual([])
  })
})
