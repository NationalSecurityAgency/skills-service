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
import { effectScope } from 'vue'
import { createPinia, disposePinia, setActivePinia } from 'pinia'
import axios from 'axios'
import { useErrorHandling } from './UseErrorHandling.js'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'

const mocks = vi.hoisted(() => ({ push: vi.fn(), clear: vi.fn(), setError: vi.fn(), reset: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: mocks.push }), useRoute: () => ({ meta: { requiresAuth: true } }) }))
vi.mock('@/stores/UseAuthState.js', () => ({ useAuthState: () => ({ clearAuthData: mocks.clear }) }))
vi.mock('@/common-components/stores/UseAppConfig.js', () => ({ useAppConfig: () => ({}) }))
vi.mock('@/stores/UserErrorState.js', () => ({ userErrorState: () => ({ setErrorParams: mocks.setError, resetToDfeault: mocks.reset }) }))
vi.mock('@/skills-display/UseSkillsDisplayInfo.js', () => ({ useSkillsDisplayInfo: () => ({ isSkillsClientPath: () => true, getContextSpecificRouteName: () => 'SkillsDisplayErrorPage' }) }))
vi.mock('@/components/utils/misc/useLog.js', () => ({ useLog: () => ({ debug: vi.fn() }) }))
vi.mock('@/components/utils/errors/UseUpgradeInProgressErrorChecker.js', () => ({ useUpgradeInProgressErrorChecker: () => ({ isUpgrading: (error) => error.response?.data?.errorCode === 'DbUpgradeInProgress' }) }))

describe('iframe response error handling', () => {
  let pinia
  let scope
  let parent
  beforeEach(() => {
    vi.clearAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    scope = effectScope()
    parent = useSkillsDisplayParentFrameState()
    parent.serviceUrl = window.location.origin
    parent.setAuthToken('current')
    scope.run(() => useErrorHandling().registerErrorHandling())
  })
  afterEach(() => {
    scope.stop()
    disposePinia(pinia)
  })

  const failRequest = (status, data = {}, options = {}) => axios.get('/api/projects/project/summary', {
    ...options,
    adapter: async (config) => {
      const response = status ? { status, data, config } : undefined
      throw new axios.AxiosError('Request failed', status ? 'ERR_BAD_RESPONSE' : 'ERR_NETWORK', config, null, response)
    },
  })

  it.each([
    [403, { errorCode: 'private_project', projectId: 'project', explanation: 'Denied' }, 'User Not Authorized'],
    [404, { explanation: 'Missing' }, 'Resource Not Found'],
    [500, {}, null], [undefined, {}, null],
    [503, { errorCode: 'DbUpgradeInProgress' }, 'Upgrade in Progress'],
  ])('routes current %s errors inside the iframe', async (status, data, title) => {
    await expect(failRequest(status, data)).rejects.toBeInstanceOf(axios.AxiosError)
    expect(mocks.push).toHaveBeenCalledExactlyOnceWith({ name: 'SkillsDisplayErrorPage' })
    expect(mocks.clear).not.toHaveBeenCalled()
    if (title) expect(mocks.setError.mock.calls[0][0]).toBe(title)
    else expect(mocks.reset).toHaveBeenCalledOnce()
    if (status === 403) expect(mocks.setError).toHaveBeenCalledWith(title, 'Denied', 'fas fa-shield-alt')
  })

  it.each([{ handleError: false }, { handleErrorCode: [401] }, { skillsDisplayHandleError: true }])('current 401 still expires authentication with local options %j', async (options) => {
    await expect(failRequest(401, {}, options)).rejects.toBeInstanceOf(axios.AxiosError)
    expect(mocks.clear).toHaveBeenCalledOnce()
    expect(mocks.push).not.toHaveBeenCalled()
  })

  it.each([401, 403, 404, 500, undefined])('ignores stale %s errors after generation changes', async (status) => {
    let rejectOld
    let started
    const ready = new Promise((resolve) => { started = resolve })
    const request = axios.get('/api/projects/project/summary', {
      adapter: (config) => new Promise((resolve, reject) => {
        rejectOld = () => reject(new axios.AxiosError('Old request failed', 'ERR_BAD_RESPONSE', config, null, { status, data: {} }))
        started()
      }),
    }).catch((error) => error)
    await ready
    parent.setAuthToken('replacement')
    rejectOld()
    expect(await request).toBeInstanceOf(axios.AxiosError)
    expect(mocks.clear).not.toHaveBeenCalled()
    expect(mocks.push).not.toHaveBeenCalled()
    expect(mocks.setError).not.toHaveBeenCalled()
    expect(mocks.reset).not.toHaveBeenCalled()
  })
})
