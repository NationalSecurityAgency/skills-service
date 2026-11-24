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
import { effectScope, ref } from 'vue'
import { useIframeInit } from './UseIframeInit.js'
import { useSkillsDisplayParentFrameState } from '../stores/UseSkillsDisplayParentFrameState.js'

const mocks = vi.hoisted(() => ({
  parent: null,
  model: null,
  attributes: {},
  config: {},
  auth: {},
  push: vi.fn(),
  routerPush: vi.fn(),
  emit: vi.fn(),
  pendingHandshake: false,
  resolveHandshake: null,
  resolve: vi.fn(),
  warn: vi.fn(),
}))
vi.mock('postmate', () => ({ default: { Model: class {
  constructor(model) {
    mocks.model = model
    if (mocks.pendingHandshake) return new Promise((resolve) => { mocks.resolveHandshake = resolve })
    return Promise.resolve(mocks.parent)
  }
} } }))
vi.mock('@/skills-display/stores/UseSkillsDisplayAttributesState.js', () => ({ useSkillsDisplayAttributesState: () => mocks.attributes }))
vi.mock('@/common-components/stores/UseAppConfig.js', () => ({ useAppConfig: () => mocks.config }))
vi.mock('@/stores/UseAuthState.js', () => ({ useAuthState: () => mocks.auth }))
vi.mock('@/skills-display/stores/UseUserProgressSummaryState.js', () => ({ useUserProgressSummaryState: () => ({ reset: vi.fn() }) }))
vi.mock('@/skills-display/stores/UseSkillsDisplaySubjectState.js', () => ({ useSkillsDisplaySubjectState: () => ({ reset: vi.fn() }) }))
vi.mock('@/skills-display/stores/UseSkillsDisplayPointHistoryState.js', () => ({ useSkillsDisplayPointHistoryState: () => ({ reset: vi.fn() }) }))
vi.mock('@/skills-display/stores/UseSkillsDisplayThemeState.js', () => ({ useSkillsDisplayThemeState: () => ({}) }))
vi.mock('@/skills-display/theme/ThemeHelper.js', () => ({ default: { build: vi.fn() } }))
vi.mock('@/skills-display/UseSkillsDisplayInfo.js', () => ({ useSkillsDisplayInfo: () => ({ routerPush: mocks.routerPush, isSkillsClientPath: () => true, isSkillsDisplayPath: (route) => route.path.startsWith('/subjects/') }) }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: mocks.push, resolve: mocks.resolve }) }))
vi.mock('@/components/utils/misc/useLog.js', () => ({ useLog: () => ({ debug: vi.fn(), error: vi.fn(), warn: mocks.warn, isTraceEnabled: () => false }) }))

describe('iframe service initialization', () => {
  let pinia
  let scope
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.pendingHandshake = false
    mocks.resolveHandshake = null
    mocks.push.mockResolvedValue(undefined)
    mocks.resolve.mockImplementation((route) => ({ path: typeof route === 'string' ? route : '/dashboard', matched: [] }))
    pinia = createPinia()
    setActivePinia(pinia)
    scope = effectScope()
    mocks.attributes = { loadingConfig: true, invalidateConfig: vi.fn() }
    mocks.config = { isPkiAuthenticated: false }
    mocks.auth = { userInfo: null, clearAuthData: vi.fn(), fetchUser: vi.fn(), setRestoringSession: vi.fn() }
    mocks.parent = {
      parentOrigin: 'https://customer.example',
      model: { serviceUrl: `${window.location.origin}/skilltree/`, projectId: 'project' },
      emit: mocks.emit,
    }
    document.body.innerHTML = '<div id="skills-display-app"></div>'
    vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} })
    vi.stubGlobal('CSS', { supports: (property, value) => property === 'min-height' && /^\d+px$/.test(value) })
  })
  afterEach(() => {
    scope.stop()
    vi.useRealTimers()
    disposePinia(pinia)
    vi.unstubAllGlobals()
    document.body.innerHTML = ''
    document.body.style.overflowY = ''
  })

  it('accepts a cross-origin parent and waits for its token', async () => {
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    expect(mocks.attributes.serviceUrl).toBe(`${window.location.origin}/skilltree`)
    expect(mocks.emit).toHaveBeenCalledWith('needs-authentication')
    expect(init.loadedIframe.value).toBe(false)
    mocks.model.updateAuthenticationToken('token')
    expect(init.loadedIframe.value).toBe(true)
    expect(useSkillsDisplayParentFrameState().authToken).toBe('token')
    expect(init.initializationError.value).toBe('')
  })

  it.each(['https://attacker.example', 'http://[invalid', undefined])('rejects an invalid destination before requesting authentication: %s', async (serviceUrl) => {
    mocks.parent.model.serviceUrl = serviceUrl
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    mocks.model.updateAuthenticationToken('token')
    mocks.model.navigate('/somewhere')
    mocks.model.updateVersion('2')
    expect(init.loadedIframe.value).toBe(false)
    expect(init.initializationError.value).toContain('same origin')
    expect(mocks.emit).not.toHaveBeenCalled()
    expect(mocks.push).not.toHaveBeenCalled()
    expect(mocks.routerPush).not.toHaveBeenCalled()
    expect(mocks.attributes.serviceUrl).toBeUndefined()
    expect(mocks.attributes.loadingConfig).toBe(true)
    expect(useSkillsDisplayParentFrameState().authToken).toBe('')
  })

  it('retains PKI initialization without requiring a bearer token', async () => {
    mocks.config.isPkiAuthenticated = true
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    expect(init.loadedIframe.value).toBe(true)
    expect(useSkillsDisplayParentFrameState().authToken).toBe('')
  })

  it.each([
    { projectId: '' }, { projectId: ' ' }, { projectId: { toString: 'not-a-function' } },
    { version: {} }, { version: false }, { version: [0] }, { version: NaN }, { version: Infinity },
    { userId: {} }, { options: [] }, { minHeight: {} }, { minHeight: 'not-a-height' },
  ])('rejects malformed configuration before requesting authentication: %j', async (overrides) => {
    Object.assign(mocks.parent.model, overrides)
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    expect(init.initializationError.value).toContain('verify the embedding configuration')
    expect(init.loadedIframe.value).toBe(false)
    expect(mocks.emit).not.toHaveBeenCalled()
    expect(useSkillsDisplayParentFrameState().parentFrame).toBeNull()
    expect(mocks.attributes.projectId).toBeUndefined()
  })

  it.each([0, '0', null])('preserves a valid initial version: %j', async (version) => {
    mocks.parent.model.version = version
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    expect(init.initializationError.value).toBe('')
    expect(mocks.attributes.version).toBe(version)
  })

  it.each(['updateAuthenticationToken', 'updateVersion', 'navigate', '__proto__', 'constructor', 'prototype'])('rejects reserved handshake key %s before Postmate merges it', async (key) => {
    mocks.pendingHandshake = true
    const init = scope.run(() => useIframeInit(ref(false)))
    const handshake = init.handleHandshake()
    const model = JSON.parse(`{"${key}":"replacement"}`)
    const event = new MessageEvent('message', {
      source: window.parent, origin: 'https://customer.example',
      data: { postmate: 'handshake', type: 'application/x-postmate-v1+json', model },
    })
    const stopped = vi.spyOn(event, 'stopImmediatePropagation')
    window.dispatchEvent(event)
    expect(stopped).toHaveBeenCalledOnce()
    expect(init.initializationError.value).toContain('verify the embedding configuration')
    mocks.resolveHandshake(mocks.parent)
    await handshake
    expect(mocks.emit).not.toHaveBeenCalled()
    expect(init.loadedIframe.value).toBe(false)
  })

  it('rejects a missing handshake at exactly ten seconds and ignores a late handshake', async () => {
    vi.useFakeTimers()
    mocks.pendingHandshake = true
    const init = scope.run(() => useIframeInit(ref(false)))
    const handshake = init.handleHandshake()
    await vi.advanceTimersByTimeAsync(9999)
    expect(init.initializationError.value).toBe('')
    await vi.advanceTimersByTimeAsync(1)
    expect(init.initializationError.value).toContain('verify the embedding configuration')
    mocks.resolveHandshake(mocks.parent)
    await handshake
    expect(mocks.emit).not.toHaveBeenCalled()
    expect(init.loadedIframe.value).toBe(false)
  })

  it('clears the handshake timeout when disposed', async () => {
    vi.useFakeTimers()
    mocks.pendingHandshake = true
    const init = scope.run(() => useIframeInit(ref(false)))
    const handshake = init.handleHandshake()
    scope.stop()
    await vi.advanceTimersByTimeAsync(10000)
    mocks.resolveHandshake(mocks.parent)
    await handshake
    expect(init.initializationError.value).toBe('')
    expect(mocks.emit).not.toHaveBeenCalled()
  })

  it('ignores duplicate authentication tokens without starting recovery', async () => {
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    mocks.model.updateAuthenticationToken('token')
    const state = useSkillsDisplayParentFrameState()
    const generation = state.authGeneration
    mocks.model.updateAuthenticationToken('token')
    mocks.model.updateAuthenticationToken('token')
    expect(state.authGeneration).toBe(generation)
    expect(mocks.auth.fetchUser).not.toHaveBeenCalled()
    expect(mocks.push).not.toHaveBeenCalled()
    expect(init.authenticationError.value).toBe('')
  })

  it.each([null, '', ' ', {}, ['token'], 'token\r\nInjected: header'])('ignores malformed authentication tokens: %j', async (token) => {
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    mocks.model.updateAuthenticationToken(token)
    expect(init.loadedIframe.value).toBe(false)
    expect(useSkillsDisplayParentFrameState().authToken).toBe('')
  })

  it.each([{}, false, [0], NaN, Infinity])('ignores malformed version updates: %j', async (version) => {
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    mocks.model.updateVersion(version)
    expect(mocks.attributes.version).toBeNull()
    expect(mocks.auth.fetchUser).not.toHaveBeenCalled()
  })

  it.each([0, '0', null])('ignores repeated current versions: %j', async (version) => {
    mocks.parent.model.version = version
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    mocks.model.updateAuthenticationToken('token')
    const state = useSkillsDisplayParentFrameState()
    const generation = state.authGeneration
    mocks.model.updateVersion(version)
    mocks.model.updateVersion(version)
    expect(state.authGeneration).toBe(generation)
    expect(mocks.auth.fetchUser).not.toHaveBeenCalled()
    expect(mocks.push).not.toHaveBeenCalled()
  })

  it.each([null, [], { name: 'NonexistentEmbeddedRoute' }, { name: 'SkillsDisplayInIframe' }])('ignores invalid parent navigation: %j', async (route) => {
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    mocks.model.navigate(route)
    expect(mocks.push).not.toHaveBeenCalled()
    expect(init.initializationError.value).toBe('')
  })

  it('accepts embedded navigation but contains router resolution failures', async () => {
    const init = scope.run(() => useIframeInit(ref(false)))
    await init.handleHandshake()
    mocks.resolve.mockReturnValue({ path: '/subjects/subj1', matched: [{}] })
    mocks.model.navigate('/subjects/subj1')
    expect(mocks.push).toHaveBeenCalledWith('/subjects/subj1')
    mocks.resolve.mockImplementation(() => { throw new Error('Unknown route') })
    expect(() => mocks.model.navigate({ name: 'Unknown' })).not.toThrow()
    expect(mocks.push).toHaveBeenCalledOnce()
  })
})
