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
import { createPinia, disposePinia, setActivePinia } from 'pinia'
import axios from 'axios'
import { useUserProgressSummaryState } from './UseUserProgressSummaryState.js'
import { useSkillsDisplaySubjectState } from './UseSkillsDisplaySubjectState.js'
import { useSkillsDisplayParentFrameState } from './UseSkillsDisplayParentFrameState.js'

const service = vi.hoisted(() => ({
  loadUserProjectSummary: vi.fn(), loadUserSkillsRanking: vi.fn(),
  loadSubjectSummary: vi.fn(), getSkillSummary: vi.fn(),
  getBadgeSkills: vi.fn(), getSkillsGroupSummary: vi.fn(), getSkillTagSummary: vi.fn(),
}))
vi.mock('@/skills-display/services/UseSkillsDisplayService.js', () => ({ useSkillsDisplayService: () => service }))
vi.mock('vue-router', () => ({ useRoute: () => ({ name: 'SkillsDisplaySubject' }) }))
vi.mock('@/components/badges/global/GlobalBadgeService.js', () => ({ default: {} }))

const loaders = [
  ['project', useUserProgressSummaryState, 'loadUserProgressSummary', 'loadingUserProgressSummary', 'loadUserProjectSummary', 'userProgressSummary'],
  ['rank', useUserProgressSummaryState, 'loadUserSkillsRanking', 'loadingUserSkillsRanking', 'loadUserSkillsRanking', 'userRanking'],
  ['subject', useSkillsDisplaySubjectState, 'loadSubjectSummary', 'loadingSubjectSummary', 'loadSubjectSummary', 'subjectSummary'],
  ['skill', useSkillsDisplaySubjectState, 'loadSkillSummary', 'loadingSkillSummary', 'getSkillSummary', 'skillSummary'],
  ['badge', useSkillsDisplaySubjectState, 'loadBadgeSummary', 'loadingBadgeSummary', 'getBadgeSkills', 'subjectSummary'],
  ['group', useSkillsDisplaySubjectState, 'loadSkillsGroupSummary', 'loadingSkillsGroupSummary', 'getSkillsGroupSummary', 'subjectSummary'],
  ['tag', useSkillsDisplaySubjectState, 'loadSkillTagSummary', 'loadingSkillTagSummary', 'getSkillTagSummary', 'subjectSummary'],
]

describe('Skills Display loading ownership', () => {
  let pinia
  let parent
  beforeEach(() => {
    vi.resetAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    parent = useSkillsDisplayParentFrameState()
    parent.setAuthToken('original')
  })
  afterEach(() => disposePinia(pinia))

  it.each(loaders)('resets cached %s data and restores its loading flag', async (name, createStore, action, loading, method, data) => {
    const store = createStore()
    service[method].mockResolvedValue({ points: 100 })
    await store[action]('id')
    expect(store[data]).toEqual({ points: 100 })
    expect(store[loading]).toBe(false)
    store.reset()
    expect(store[data]).toEqual({})
    expect(store[loading]).toBe(true)
  })

  it.each(loaders)('old %s cleanup cannot hide a pending replacement load', async (name, createStore, action, loading, method, data) => {
    const store = createStore()
    let rejectOld
    let resolveFresh
    service[method]
      .mockReturnValueOnce(new Promise((resolve, reject) => { rejectOld = reject }))
      .mockReturnValueOnce(new Promise((resolve) => { resolveFresh = resolve }))
    // Stale successes are canceled by the response interceptor. Test its rejection
    // boundary here; bypassing it with a successful stale service mock is incorrect.
    const old = store[action]('id').catch((error) => error)
    parent.setAuthToken('replacement')
    store.reset()
    const fresh = store[action]('id')
    rejectOld(new axios.CanceledError('Skills Display request was superseded'))
    expect(axios.isCancel(await old)).toBe(true)
    expect(store[loading]).toBe(true)
    expect(store[data]).toEqual({})
    resolveFresh({ points: 200 })
    await fresh
    expect(store[loading]).toBe(false)
    expect(store[data]).toEqual({ points: 200 })
  })

  it.each(loaders)('current %s failures finish their own loading state', async (name, createStore, action, loading, method) => {
    const store = createStore()
    const failure = new Error('Current request failed')
    service[method].mockRejectedValue(failure)
    await expect(store[action]('id')).rejects.toBe(failure)
    expect(store[loading]).toBe(false)
  })
})
