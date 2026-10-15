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
import { boolean, object, string } from 'yup'

const withMax = (schema, maxLength) => (maxLength ? schema.max(maxLength) : schema)

/**
 * Validation schema for a single quiz question answer. The backend rejects any answer text
 * (including matching term/value) longer than skills.config.ui.maxQuizTextAnswerLength,
 * so enforce the same limit here and report it next to the offending field.
 */
export const buildAnswerSchema = (maxLength) => object({
  answer: withMax(string().nullable(), maxLength).label('Answer'),
  isCorrect: boolean().label('Is Correct'),
  multiPartAnswer: object({
    term: withMax(string().nullable(), maxLength).label('Term'),
    value: withMax(string().nullable(), maxLength).label('Answer'),
  }).nullable().default(undefined),
})
