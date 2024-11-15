/*
 * Copyright 2024 SkillTree
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
import { isProxy, ref, toRaw, watch } from 'vue'
import { useIndexedDB } from '@/components/utils/storage/UseIndexedDB.js'

export const useInputFormResiliency = () => {
  const indexedDb = useIndexedDB()
  const isRestoredFromStore = ref(false)
  const isInitializing = ref(true)
  const operationsContainer = {
    componentName: null,
    setFieldValueFunction: null,
    originalModal: null,
    currentReactiveModel: null
  }
  const loadFromStorageAndUpdateAsNeeded = (componentName, modelObj, setFieldValueFunction) => {
    isInitializing.value = true
    return indexedDb.load(componentName).then((objFromStorage) => {
      if (objFromStorage) {
        for (const [key, value] of Object.entries(objFromStorage)) {
          if (value !== modelObj[key]) {
            setFieldValueFunction(key, value)
            isRestoredFromStore.value = true
          }
        }
      }

      isInitializing.value = false
    })
  }

  const watcherContainer = { unwatch: null }
  const init = (componentName, modelObj, initialValues, setFieldValueFunction) => {
    operationsContainer.componentName = componentName
    operationsContainer.setFieldValueFunction = setFieldValueFunction
    operationsContainer.originalModal = initialValues
    operationsContainer.currentReactiveModel = modelObj

    return loadFromStorageAndUpdateAsNeeded(componentName, modelObj, setFieldValueFunction).then(()=> {
      watcherContainer.unwatch = watch(modelObj, (newValue) => {
        const rawObj = convertToRaw(newValue)
        indexedDb.save(componentName, rawObj)
      })
      return isRestoredFromStore.value
    })
  }

  const convertToRaw = (newValue) => {
    if (isProxy(newValue)) {
      const rawObj = toRaw(newValue)
      for (const [key, value] of Object.entries(rawObj)) {
        if (Array.isArray(value)) {
          rawObj[key] = value.map((item) => convertToRaw(item))
        } else {
          rawObj[key] = convertToRaw(value)
        }
      }
      return rawObj
    }
    return newValue;
  }

  const discard = (updateModel = true) => {
    if (updateModel) {
      if (operationsContainer.originalModal) {
        for (const [key, value] of Object.entries(operationsContainer.originalModal)) {
          if (value !== operationsContainer.currentReactiveModel[key]) {
            operationsContainer.setFieldValueFunction(key, value)
            isRestoredFromStore.value = true
          }
        }
      }
    }
    indexedDb.clear(operationsContainer.componentName)
  }

  const stop = (clearData = true) => {
    if (watcherContainer.unwatch){
      watcherContainer.unwatch()
    }
    if (clearData) {
      indexedDb.clear(operationsContainer.componentName)
    }
  }


  return {
    init,
    discard,
    isRestoredFromStore,
    isInitializing,
    stop
  }
}