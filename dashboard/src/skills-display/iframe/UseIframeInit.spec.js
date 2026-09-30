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
import { useIframeInit } from './UseIframeInit.js'
import { useSkillsDisplayParentFrameState } from '../stores/UseSkillsDisplayParentFrameState.js'

const mocks = vi.hoisted(() => ({
  parent: null,
  model: null,
  attributes: {},
  config: {},
  push: vi.fn(),
  routerPush: vi.fn(),
  emit: vi.fn(),
}))
vi.mock('postmate', () => ({ default: { Model: class {
  constructor(model) {
    mocks.model = model
    return Promise.resolve(mocks.parent)
  }
} } }))
vi.mock('@/skills-display/stores/UseSkillsDisplayAttributesState.js', () => ({ useSkillsDisplayAttributesState: () => mocks.attributes }))
vi.mock('@/common-components/stores/UseAppConfig.js', () => ({ useAppConfig: () => mocks.config }))
vi.mock('@/skills-display/stores/UseSkillsDisplayThemeState.js', () => ({ useSkillsDisplayThemeState: () => ({}) }))
vi.mock('@/skills-display/theme/ThemeHelper.js', () => ({ default: { build: vi.fn() } }))
vi.mock('@/skills-display/UseSkillsDisplayInfo.js', () => ({ useSkillsDisplayInfo: () => ({ routerPush: mocks.routerPush }) }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: mocks.push }) }))
vi.mock('@/components/utils/misc/useLog.js', () => ({ useLog: () => ({ debug: vi.fn(), error: vi.fn(), isTraceEnabled: () => false }) }))

describe('iframe service initialization', () => {
  let pinia
  beforeEach(() => {
    vi.clearAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    mocks.attributes = { loadingConfig: true }
    mocks.config = { isPkiAuthenticated: false }
    mocks.parent = {
      parentOrigin: 'https://customer.example',
      model: { serviceUrl: `${window.location.origin}/skilltree/`, projectId: 'project' },
      emit: mocks.emit,
    }
    document.body.innerHTML = '<div id="skills-display-app"></div>'
    vi.stubGlobal('ResizeObserver', class { observe() {} })
  })
  afterEach(() => {
    disposePinia(pinia)
    vi.unstubAllGlobals()
    document.body.innerHTML = ''
    document.body.style.overflowY = ''
  })

  it('accepts a cross-origin parent and waits for its token', async () => {
    const init = useIframeInit()
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
    const init = useIframeInit()
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
    const init = useIframeInit()
    await init.handleHandshake()
    expect(init.loadedIframe.value).toBe(true)
    expect(useSkillsDisplayParentFrameState().authToken).toBe('')
  })
})
