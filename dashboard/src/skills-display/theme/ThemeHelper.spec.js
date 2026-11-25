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
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ThemeHelper from './ThemeHelper.js'

describe('parent theme validation', () => {
  beforeEach(() => {
    // Test declaration safety and schema traversal independently of the browser's
    // CSS parser. Cypress retains actual invalid-color and rendered-style coverage.
    vi.stubGlobal('CSS', { supports: () => true })
  })
  afterEach(() => vi.unstubAllGlobals())

  const paths = [
    ['pageTitle', 'textColor'], ['tiles', 'backgroundColor'], ['trophyIconColor'],
    ...['incompleteColor', 'earnedTodayColor', 'beforeTodayColor', 'completeColor'].map((key) => ['progressIndicators', key]),
    ['prerequisites', 'skillColor'], ['charts', 'pointHistory', 'lineColor'],
    ['infoCards', 'iconColors'], ['circleProgressInteriorTextColor'],
  ]
  const themeAt = (path, value) => path.reduceRight((nested, key) => ({ [key]: nested }), value)

  it.each(paths.map((path) => [path.join('.'), path]))('rejects declaration injection and resource references at %s', (name, path) => {
    for (const value of ['red } body { color: blue', 'red; color: blue', 'url(https://attacker.example/css)', 'URL (https://attacker.example/css)', 'r\\65 d']) {
      const leaf = path.at(-1) === 'iconColors' ? ['green', value] : value
      expect(() => ThemeHelper.build(themeAt(path, leaf))).toThrow()
    }
  })

  it.each([
    { progressIndicators: { incompleteColor: { color: 'red' } } },
    { progressIndicators: 'red' }, { progressIndicators: ['red'] }, { progressIndicators: null },
    { infoCards: { iconColors: 'red' } }, { progressIndicators: { unknownColor: 'red' } },
  ])('rejects malformed nested theme shapes: %j', (theme) => {
    expect(() => ThemeHelper.build(theme)).toThrow()
  })

  it('preserves valid declarations and module colors', () => {
    const result = ThemeHelper.build({
      pageTitle: { textColor: '#123456' },
      progressIndicators: { incompleteColor: '#abcdef' },
      infoCards: { iconColors: ['green', '#123456'] },
    })
    expect(result.css).toContain('color: #123456 !important')
    expect(result.themeModule.get('progressIndicators')).toEqual({ incompleteColor: '#abcdef' })
    expect(result.themeModule.get('infoCards')).toEqual({ iconColors: ['green', '#123456'] })
  })
})
