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
import { validateSkillsDisplayServiceUrl } from './SkillsDisplayServiceUrl.js'

describe('Skills Display service URL', () => {
  it.each(['/', window.location.origin, `${window.location.origin}/`])('accepts the iframe origin: %s', (url) => {
    expect(validateSkillsDisplayServiceUrl(url)).toBe(window.location.origin)
  })

  it('preserves the context path and normalizes trailing slashes', () => {
    expect(validateSkillsDisplayServiceUrl('/skilltree/')).toBe(`${window.location.origin}/skilltree`)
  })

  it.each([
    undefined, null, {}, '', ' ', 'http://[invalid',
    'https://attacker.example', '//attacker.example', '\\\\attacker.example',
    `${window.location.origin}.attacker.example`,
    `${window.location.origin}:12345`,
    'javascript:alert(1)', 'data:text/plain,test',
    `blob:${window.location.origin}/id`,
    `${window.location.origin.replace('://', '://user:password@')}/`,
    '/skilltree?redirect=https://attacker.example', '/skilltree#fragment',
  ])('rejects invalid or untrusted service URLs: %s', (url) => {
    expect(() => validateSkillsDisplayServiceUrl(url)).toThrow()
  })
})
