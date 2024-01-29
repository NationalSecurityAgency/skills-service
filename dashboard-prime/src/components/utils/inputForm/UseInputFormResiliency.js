import { toRaw, watch, ref } from 'vue'
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
    indexedDb.load(componentName).then((objFromStorage) => {
      if (objFromStorage) {
        for (const [key, value] of Object.entries(objFromStorage)) {
          console.log(`compare [${value}] !== [${modelObj[key]}]`)
          if (value !== modelObj[key]) {
            console.log(`calling setFieldValueFunction(${key}, ${value})`)
            setFieldValueFunction(key, value)
            isRestoredFromStore.value = true
          }
        }
      }

      isInitializing.value = false
    })
  }

  const init = (componentName, modelObj, initialValues, setFieldValueFunction) => {
    operationsContainer.componentName = componentName
    operationsContainer.setFieldValueFunction = setFieldValueFunction
    operationsContainer.originalModal = initialValues
    operationsContainer.currentReactiveModel = modelObj
console.log(`init ${JSON.stringify(operationsContainer, null, 2)}`)

    loadFromStorageAndUpdateAsNeeded(componentName, modelObj, setFieldValueFunction)

    watch(modelObj, (newValue) => {
      const rawObj = toRaw(newValue)
      indexedDb.save(componentName, rawObj)
    })
  }

  const discard = (updateModel = true) => {
    if (updateModel) {
      for (const [key, value] of Object.entries(operationsContainer.originalModal)) {

        console.log(`discard [${value}] !== [${operationsContainer.currentReactiveModel[key]}]`)
        if (value !== operationsContainer.currentReactiveModel[key]) {
          operationsContainer.setFieldValueFunction(key, value)
          isRestoredFromStore.value = true
        }
      }
  }
    indexedDb.clear(operationsContainer.componentName)
  }

  return {
    init,
    discard,
    isRestoredFromStore,
    isInitializing
  }
}