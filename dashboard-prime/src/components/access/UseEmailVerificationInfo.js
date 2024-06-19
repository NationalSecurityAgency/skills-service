import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useEmailVerificationInfo = defineStore('useEmailVerificationInfo', () => {
    const emailVal = ref('')
    const reasonVal = ref('')
    const setEmail = (newEmail) => {
        emailVal.value = newEmail
    }
    const email = computed(() => emailVal.value)

    const setReason = (newReason) => {
        reasonVal.value = newReason
    }
    const reason = computed(() => reasonVal.value)

    return {
        email,
        setEmail,
        reason,
        setReason
    }
})