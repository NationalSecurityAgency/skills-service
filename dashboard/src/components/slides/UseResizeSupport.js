/*
 * Copyright 2025 SkillTree
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

import {ref} from "vue";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

export const useResizeSupport = () => {

    const isResizing = ref(false)
    const announcer = useSkillsAnnouncer()
    const handle = ref(null)
    const latestWidth = ref(0);
    const onResizeCallbackRef = ref(null)
    const onResizeCompleteCallbackRef = ref(null)
    const containerSelectorRef = ref(null)

    const resize = (e) => {
        isResizing.value = true
        const element = document.querySelector(containerSelectorRef.value)
        const clientRect = element.getBoundingClientRect()
        latestWidth.value = e.pageX - clientRect.left
        onResizeCallbackRef.value(latestWidth.value)
    }

    const stopResize = function stopResizeHandler() {
        window.removeEventListener('mousemove', resize)
        window.removeEventListener('mouseup', stopResizeHandler); // Remove self
        isResizing.value = false
        if (latestWidth.value > 0) {
            if (onResizeCompleteCallbackRef.value) {
                onResizeCompleteCallbackRef.value(latestWidth.value)
            }
            announcer.polite(`Resized to ${latestWidth.value} width`)
        }
    }

    const addResizeEvents = (e) => {
        e.preventDefault()
        window.addEventListener('mousemove', resize)
        window.addEventListener('mouseup', stopResize)
    }

    const addMouseDownEvent = (e) => {
        handle.value.addEventListener('mousedown', addResizeEvents)
    }

    const removeResizeSupport = () => {
        handle.value.removeEventListener('mousedown', addResizeEvents)
    }

    const initResizeSupport = (containerSelector, resizeHandleSelector, onResizeCallback, onResizeCompleteCallback = null) => {
        handle.value = document.querySelector(resizeHandleSelector)
        if (!handle.value) {
            throw new Error(`Could not find resize handle: ${resizeHandleSelector}`)
        }
        if (!onResizeCallback) {
            throw new Error('onResizeCallback is required')
        }
        onResizeCallbackRef.value = onResizeCallback
        if (!document.querySelector(containerSelector)) {
            throw new Error(`Could not find container: ${containerSelector}`)
        }
        containerSelectorRef.value = containerSelector
        onResizeCompleteCallbackRef.value = onResizeCompleteCallback

        addMouseDownEvent()
    }

    return {
        initResizeSupport,
        removeResizeSupport,
        isResizing
    }
}