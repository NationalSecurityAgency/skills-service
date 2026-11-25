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
import { flushPromises, shallowMount } from '@vue/test-utils'
import SkillsDisplayPreview from './SkillsDisplayPreview.vue'

const mocks = vi.hoisted(() => ({
  remove: vi.fn(), push: vi.fn(), msgOk: vi.fn(), focus: vi.fn(),
  config: { isReadOnlyProj: false, afterProjConfigStateLoaded: vi.fn() },
  attributes: { loadingConfig: false, loadConfigStateIfNeeded: vi.fn() },
  theme: { theme: {} }
}))
vi.mock('./UsersService.js', () => ({ default: { removeUserFromProject: mocks.remove } }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { projectId: 'proj1', userId: 'learner' } }),
  useRouter: () => ({ push: mocks.push })
}))
vi.mock('@/stores/UseProjConfig.js', () => ({ useProjConfig: () => mocks.config }))
vi.mock('@/skills-display/stores/UseSkillsDisplayAttributesState.js', () => ({ useSkillsDisplayAttributesState: () => mocks.attributes }))
vi.mock('@/skills-display/stores/UseSkillsDisplayThemeState.js', () => ({ useSkillsDisplayThemeState: () => mocks.theme }))
vi.mock('@/components/utils/modal/UseDialogMessages.js', () => ({ useDialogMessages: () => ({ msgOk: mocks.msgOk }) }))
vi.mock('@/stores/UseFocusState.js', () => ({ useFocusState: () => ({ setElementId: mocks.focus }) }))

describe('project user removal', () => {
  let wrapper
  beforeEach(async () => {
    vi.clearAllMocks()
    mocks.config.afterProjConfigStateLoaded.mockResolvedValue({})
    mocks.push.mockResolvedValue(undefined)
    wrapper = shallowMount(SkillsDisplayPreview, {
      global: { stubs: {
        SubPageHeader: { template: '<div><slot /></div>' },
        SkillsButton: { props: ['disabled', 'loading'], template: '<button :disabled="disabled" @click="$emit(\'click\')">Remove User</button>' },
        Message: { template: '<div><slot /></div>' },
        SkillsSpinner: true
      } }
    })
    await flushPromises()
  })
  afterEach(() => wrapper.unmount())

  const submit = async () => {
    await wrapper.get('[data-cy="removeProjectUserBtn"]').trigger('click')
    wrapper.getComponent({ name: 'RemovalValidation' }).vm.$emit('do-remove')
    await flushPromises()
  }

  it('prevents duplicate requests while pending and navigates after success', async () => {
    let resolve
    mocks.remove.mockReturnValue(new Promise((done) => { resolve = done }))
    await submit()
    expect(wrapper.get('[data-cy="removeProjectUserBtn"]').element.disabled).toBe(true)
    wrapper.getComponent({ name: 'RemovalValidation' }).vm.$emit('do-remove')
    await flushPromises()
    expect(mocks.remove).toHaveBeenCalledExactlyOnceWith('proj1', 'learner')
    expect(mocks.push).not.toHaveBeenCalled()
    resolve({ success: true })
    await flushPromises()
    expect(mocks.push).toHaveBeenCalledWith({ name: 'ProjectUsers', params: { projectId: 'proj1' } })
    expect(mocks.msgOk).toHaveBeenCalledWith({ header: 'User Removed', message: 'User learner was removed from this project.' })
    expect(mocks.focus).toHaveBeenCalledWith('mainContent1')
    expect(wrapper.get('[data-cy="removeProjectUserBtn"]').element.disabled).toBe(false)
  })

  it.each([
    ['unsuccessful result', { success: false, explanation: 'Finalization is running' }, false, 'Finalization is running'],
    ['HTTP error', { response: { data: { explanation: 'Access denied' } } }, true, 'Access denied'],
    ['unexpected error', new Error('offline'), true, 'The user could not be removed. Please try again.'],
    ['result without explanation', { success: false }, false, 'The user could not be removed. Please try again.']
  ])('shows %s without navigating and permits retry', async (_name, result, reject, message) => {
    if (reject) mocks.remove.mockRejectedValueOnce(result)
    else mocks.remove.mockResolvedValueOnce(result)
    await submit()
    expect(wrapper.get('[data-cy="removeProjectUserError"]').text()).toBe(message)
    expect(mocks.push).not.toHaveBeenCalled()
    expect(mocks.msgOk).not.toHaveBeenCalled()
    expect(wrapper.get('[data-cy="removeProjectUserBtn"]').element.disabled).toBe(false)
    mocks.remove.mockResolvedValueOnce({ success: true })
    await submit()
    expect(wrapper.find('[data-cy="removeProjectUserError"]').exists()).toBe(false)
    expect(mocks.remove).toHaveBeenCalledTimes(2)
    expect(mocks.push).toHaveBeenCalledTimes(1)
  })
})
