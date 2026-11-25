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
import { useAuthState } from './UseAuthState.js'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'

vi.mock('vue-router', () => ({ useRouter: () => ({}) }))
vi.mock('@/common-components/stores/UseAppConfig.js', () => ({ useAppConfig: () => ({}) }))
vi.mock('@/stores/UseAppInfoState.js', () => ({ useAppInfoState: () => ({}) }))
vi.mock('@/stores/UseInceptionStore.js', () => ({ useInceptionStore: () => ({ reset: vi.fn() }) }))
vi.mock('@/router/SkillsClientPath.js', () => ({ default: { isSkillsClientIframePath: () => true } }))

describe('embedded authentication without browser storage', () => {
  let pinia
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.spyOn(axios, 'get').mockImplementation(async (url, config) => ({ data: { userId: 'learner@skills.org' }, config }))
  })
  afterEach(() => {
    disposePinia(pinia)
    vi.restoreAllMocks()
  })

  it.each(['SecurityError', 'QuotaExceededError'])('restores in-memory identity without storage writes when storage throws %s', async (name) => {
    const write = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Storage unavailable', name) })
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => { throw new DOMException('Storage unavailable', name) })
    const auth = useAuthState()
    const parent = useSkillsDisplayParentFrameState()
    parent.setAuthToken('original')
    await auth.fetchUser(false)
    expect(auth.userInfo).toEqual({ userId: 'learner@skills.org' })
    expect(() => auth.clearAuthData()).not.toThrow()
    expect(auth.userInfo).toBeNull()
    expect(parent.authToken).toBe('')
    parent.setAuthToken('replacement')
    await auth.fetchUser(false)
    expect(auth.userInfo).toEqual({ userId: 'learner@skills.org' })
    expect(write).not.toHaveBeenCalled()
  })
})
