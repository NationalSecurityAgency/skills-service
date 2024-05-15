<script setup>

import { computed, onMounted, ref, watch } from 'vue';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import { useFocusState } from '@/stores/UseFocusState.js';
import { useField } from 'vee-validate';
import AccessService from '@/components/access/AccessService.js';
import AutoComplete from 'primevue/autocomplete';
import { useSkillsInputFallthroughAttributes } from '@/components/utils/inputForm/UseSkillsInputFallthroughAttributes.js';

// user type constants
const DASHBOARD = 'DASHBOARD';
const CLIENT = 'CLIENT';
const ROOT = 'ROOT';
const SUPERVISOR = 'SUPERVISOR';

const appConfig = useAppConfig();
const userInfo = useUserInfo();
const focusState = useFocusState();
const fallthroughAttributes = useSkillsInputFallthroughAttributes()

const props = defineProps({
  fieldLabel: {
    default: 'Skills User',
    type: String,
  },
  placeholder: {
    default: 'Enter user id',
    type: String,
  },
  projectId: {
    type: String,
  },
  validate: {
    type: Boolean,
    default: false,
  },
  suggest: {
    type: Boolean,
    default: false,
  },
  userType: {
    type: String,
    default: CLIENT,
    validator: (value) => ([DASHBOARD, CLIENT, ROOT, SUPERVISOR].indexOf(value) >= 0),
  },
  excludedSuggestions: {
    type: Array,
    default: () => ([]),
  },
  canEnterNewUser: {
    type: Boolean,
    default: false,
  },
  modelValue: Object,
  name: {
    type: String,
    default: 'userIdInput',
  }
});

const { value, errorMessage } = useField(() => props.name)

const emit = defineEmits(['update:modelValue']);
const selectedSuggestOption = ref(null);
const userSuggestOptions = ref([]);
const isFetching = ref(false);
const suggestions = ref([]);
const currentSelectedUser = ref('');

const hasUserSuggestOptions = computed(() => {
  return userSuggestOptions.value && userSuggestOptions.value.length > 0;
});

onMounted(() => {
  if (appConfig.userSuggestOptions) {
    userSuggestOptions.value = appConfig.userSuggestOptions.split(',')
    selectedSuggestOption.value = userSuggestOptions.value[0];
  }
});
const onShowDropdown = () => {
  if (!suggestions.value || suggestions.value.length === 0) {
    suggestUsers()
  }
}
const onHideDropdown = () => {
  // if the user clicks off the dropdown without selecting, reset back to the originally selected value
  if (currentSelectedUser.value !== value.value) {
    currentSelectedUser.value = value.value;
  }
}
const onClear = () => {
  value.value = null;
}
const selectCurrentItem = () => {
  // when the user presses enter in the search box (not on an option in the dropdown)
  if (typeof currentSelectedUser.value === 'string') {
    let selectedItem = null
    if (currentSelectedUser.value) {
      selectedItem = suggestions.value.find((suggestion) => suggestion.userId === currentSelectedUser.value);
      if (!selectedItem) {
        // can happen if the user hits enter key before suggestions finish loading
        selectedItem = { userId: currentSelectedUser.value, label: currentSelectedUser.value, isNewUser: true }
      }
    }
    value.value = selectedItem;
    emit('update:modelValue', selectedItem);
  }
}
const itemSelected = (event) => {
  value.value = event.value;
  emit('update:modelValue', value.value);
}
const suggestUrl = computed(() => {
  let suggestUrl;
  if (props.userType === CLIENT) {
    if (appConfig.isPkiAuthenticated.value) {
      suggestUrl = '/app/users/suggestPkiUsers';
    } else if (props.projectId) {
      suggestUrl = `/app/users/projects/${encodeURIComponent(props.projectId)}/suggestClientUsers`;
    } else {
      suggestUrl = '/app/users/suggestClientUsers';
    }
  } else if (props.userType === SUPERVISOR) {
    suggestUrl = '/root/users/without/role/ROLE_SUPERVISOR';
  } else if (props.userType === ROOT) {
    suggestUrl = '/root/users/without/role/ROLE_SUPER_DUPER_USER';
  } else {
    // userType === DASHBOARD
    suggestUrl = '/app/users/suggestDashboardUsers';
    if (appConfig.isPkiAuthenticated.value) {
      suggestUrl = '/app/users/suggestPkiUsers';
    }
  }
  if (selectedSuggestOption.value) {
    suggestUrl += `?userSuggestOption=${selectedSuggestOption.value}`;
  }
  return suggestUrl;
})
watch(() => props.modelValue, (newValue) => {
  currentSelectedUser.value = newValue ? newValue.userId : null;
});
const getUserIdForDisplay = (user) => {
  if (!user.userIdForDisplay) {
    return user.userId;
  }
  if (user.first && user.last) {
    return `${user.first} ${user.last} (${user.userIdForDisplay})`;
  }
  return user.userIdForDisplay;
}
const suggestUsersFromEvent = ({query}) => {
  suggestUsers(query)
}
const suggestUsers = (query) => {
  isFetching.value = true;
  AccessService.suggestUsers(query, suggestUrl.value)
      .then((suggestedUsers) => {
        let queryMatchesExistingUser = false;
        suggestions.value = suggestedUsers.filter((suggestedUser) => !props.excludedSuggestions.includes(suggestedUser.userId));
        suggestions.value = suggestions.value.map((suggestedUser) => {
          if (query === suggestedUser.userId) {
            queryMatchesExistingUser = true;
          }
          const label = getUserIdForDisplay(suggestedUser);
          return {
            ...suggestedUser,
            label,
          };
        })
        if (query && props.canEnterNewUser && !queryMatchesExistingUser) {
          suggestions.value.unshift({ userId: query, label: query, isNewUser: true });
        }
      })
      .finally(() => {
        isFetching.value = false;
      });
}
</script>

<template>
  <div data-cy="existingUserInput" v-bind="fallthroughAttributes.rootAttrs.value">
    <div class="flex">
      <Dropdown v-if="hasUserSuggestOptions" data-cy="userSuggestOptionsDropdown" v-model="selectedSuggestOption" :options="userSuggestOptions"/>
      <AutoComplete v-bind="fallthroughAttributes.inputAttrs.value"
                    v-model="currentSelectedUser"
                    data-cy="existingUserInputDropdown"
                    id="existingUserInput"
                    class="w-full"
                    :dropdown="true"
                    :suggestions="suggestions"
                    optionLabel="label"
                    @item-select="itemSelected"
                    @keydown.enter="selectCurrentItem"
                    @complete="suggestUsersFromEvent"
                    @hide="onHideDropdown"
                    @clear="onClear"
                    @dropdownClick="onShowDropdown">
        <template #option="slotProps">
          <div v-if="slotProps.option.isNewUser" class="flex flex-wrap align-options-center align-items-center">
            <div class="flex-1 existing-user-id">{{ slotProps.option.label }}</div>
            <div class="flex font-light text-sm click-indicator ml-2" style="right: 5px; bottom: 0px;">
              Enter to Select (new user)
            </div>
          </div>
          <div v-else class="flex align-options-center">
            <div>{{ slotProps.option.label }}</div>
          </div>
        </template>
      </AutoComplete>
    </div>
    <small v-if="errorMessage"
           role="alert"
           class="p-error"
           :data-cy="`${name}Error`"
           :id="`${name}Error`">{{ errorMessage || '&nbsp;' }}</small>
  </div>
</template>

<style scoped>

.vs__dropdown-option--highlight .existing-user-id {
  color: #FFFFFF !important;
}
</style>