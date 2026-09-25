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
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createPinia, disposePinia, setActivePinia } from 'pinia'
import axios from 'axios'
import { useSkillsDisplayParentFrameState } from './UseSkillsDisplayParentFrameState.js'

describe('skills display bearer token', () => {
  let pinia
  let state
  const request = async (config = {}) => {
    const response = await axios.request({
      url: '/api/projects/project/summary',
      ...config,
      adapter: async (config) => ({ data: null, status: 200, statusText: 'OK', headers: {}, config }),
    })
    return response.config.headers.get('Authorization')
  }

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    state = useSkillsDisplayParentFrameState()
    state.serviceUrl = window.location.origin
    state.setAuthToken('secret-token')
  })

  afterEach(() => disposePinia(pinia))

  it.each(['/api/projects/project/token', `${window.location.origin}/api/projects/project/token`])('adds the token to same-origin requests: %s', async (url) => {
    expect(await request({ url })).toBe('Bearer secret-token')
    expect(axios.defaults.headers.common.Authorization).toBeUndefined()
  })

  it.each([
    { url: 'https://attacker.example/collect' },
    { url: '//attacker.example/collect' },
    { url: '/collect', baseURL: 'https://attacker.example' },
    { url: `${window.location.origin}/api/projects/project/summary`, baseURL: 'https://attacker.example', allowAbsoluteUrls: false },
    { url: 'http://[invalid' },
  ])('does not send the token to an untrusted destination: %j', async (config) => {
    expect(await request(config)).toBeUndefined()
  })

  it('supports a cross-origin parent and a same-origin context path', async () => {
    state.parentFrame = { parentOrigin: 'https://customer.example' }
    state.serviceUrl = `${window.location.origin}/skilltree`
    expect(await request({ url: 'api/projects/project/summary', baseURL: state.serviceUrl })).toBe('Bearer secret-token')
  })

  it.each(['https://attacker.example', 'http://[invalid', ''])('does not trust a parent-supplied service URL: %s', async (serviceUrl) => {
    state.serviceUrl = serviceUrl
    expect(await request()).toBeUndefined()
    expect(await request({ url: 'https://attacker.example/collect' })).toBeUndefined()
  })

  it('uses refreshed tokens and stops sending them when cleared', async () => {
    expect(await request()).toBe('Bearer secret-token')
    state.setAuthToken('refreshed-token')
    expect(await request()).toBe('Bearer refreshed-token')
    state.setAuthToken('')
    expect(await request()).toBeUndefined()
  })

  it('removes the interceptor when the store is disposed', async () => {
    state.$dispose()
    expect(await request()).toBeUndefined()
  })

  it('preserves independently supplied credentials for unrelated requests', async () => {
    expect(await request({ url: 'https://other.example', headers: { Authorization: 'Basic other-credentials' } })).toBe('Basic other-credentials')
  })
})
