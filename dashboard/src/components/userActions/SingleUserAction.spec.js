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
import { describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import SingleUserAction from './SingleUserAction.vue'
import UserActionsService from './UserActionsService.js'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { projectId: 'proj1' } })
}))
vi.mock('./UserActionsService.js', () => ({
  default: { getDashboardSingleAction: vi.fn() }
}))

describe('SingleUserAction attribution', () => {
  it.each(['Project', 'Skill', 'Subject', 'Quiz'])('shows the requesting system for filtered %s attributes', async (item) => {
    UserActionsService.getDashboardSingleAction.mockResolvedValue({
      name: 'Updated resource',
      requestingSystemUserId: 'trusted-proxy-service',
      unrelatedField: 'Must remain hidden'
    })
    const wrapper = shallowMount(SingleUserAction, {
      props: { actionId: 1, item, action: 'Edit' },
      global: {
        stubs: {
          LoadingContainer: { template: '<div><slot /></div>' },
          Card: true,
          Panel: true
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Requesting System User Id:')
    expect(wrapper.text()).toContain('trusted-proxy-service')
    expect(wrapper.text()).toContain('Updated resource')
    expect(wrapper.text()).not.toContain('Must remain hidden')
    wrapper.unmount()
  })
})
