<script setup>

import { computed, onMounted, ref } from 'vue';
import { useDebounceFn } from '@vueuse/core'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import { useUserInfo } from '@/components/utils/UseUserInfo.js';
import { useFocusState } from '@/stores/UseFocusState.js';
import AccessService from '@/components/access/AccessService.js';
import { useField } from 'vee-validate';
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

const emit = defineEmits(['update:modelValue']);
const selectedSuggestOption = ref(null);
const userSuggestOptions = ref([]);

const isFetching = ref(false);
const suggestions = ref([]);

const hasUserSuggestOptions = computed(() => {
  return userSuggestOptions.value && userSuggestOptions.value.length > 0;
});

onMounted(() => {
  if (appConfig.userSuggestOptions) {
    userSuggestOptions.value = appConfig.userSuggestOptions.split(',')
    selectedSuggestOption.value = userSuggestOptions.value[0];
  }
});

const onFilter = (event) => {
  useDebounceFn(suggestUsers, appConfig.formFieldDebounceInMs)(event.value)
}
const onBeforeShow = () => {
  if (!suggestions.value || suggestions.value.length === 0) {
    suggestUsers()
  }
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
        if (query && props.canEnterNewUser) {
          // suggestedUsers.push({ userId: query, label: query });
        }
        suggestions.value = suggestedUsers.filter((suggestedUser) => !props.excludedSuggestions.includes(suggestedUser.userId));
        suggestions.value = suggestions.value.map((suggestedUser) => {
          const label = getUserIdForDisplay(suggestedUser);
          const sug = {
            ...suggestedUser,
            label,
          };
          return sug;
        })
      })
      .finally(() => {
        isFetching.value = false;
      });
}

const createTagIfNecessary = (userId) => {
  console.log(`createTagIfNecessary: ${JSON.stringify(userId)}, type [${typeof userId}]`);
  if (!userId) {
    value.value = null;
  } else if (userId && typeof userId === 'string') {
    console.log(`Before string userId: ${userId}, value[${JSON.stringify(value.value)}]`);
    value.value = {
      userId: userId,
      label: userId,
    };
    console.log(`After string userId: ${userId}, value[${JSON.stringify(value.value)}]`);
  } else {
    console.log(`Before non-string userId: ${userId}, value[${JSON.stringify(value.value)}]`);
    value.value = userId;
    console.log(`After string userId: ${userId}, value[${JSON.stringify(value.value)}]`);
  }
  emit('update:modelValue', value.value);
}

const fallthroughAttributes = useSkillsInputFallthroughAttributes()
const { value, errorMessage } = useField(() => props.name)
//     , undefined, {
//   syncVModel: true,
// });
const useDropdown = false;
const currentSelectedUser = ref('');
</script>

<template>

  <div data-cy="existingUserInput" v-bind="fallthroughAttributes.rootAttrs.value">
    <div class="flex">
<!--    <InputGroup class="">-->
<!--      <InputGroupAddon v-if="hasUserSuggestOptions" class="p-0">-->
        <Dropdown data-cy="userSuggestOptionsDropdown" v-model="selectedSuggestOption" :options="userSuggestOptions"/>
<!--      </InputGroupAddon>-->
      <Dropdown v-if="useDropdown"
                v-model="currentSelectedUser"
                id="existingUserInput"
                data-cy="existingUserInputDropdown"
                class="align-items-center w-full"
                @update:modelValue="createTagIfNecessary"
                :options="suggestions"
                :loading="isFetching"
                :placeholder="placeholder"
                :reset-filter-on-clear="false"
                :reset-filter-on-hide="true"
                :auto-filter-focus="!canEnterNewUser"
                :editable="canEnterNewUser"
                optionLabel="label"
                show-clear
                @filter="onFilter"
                @before-show="onBeforeShow"
                filter>
      </Dropdown>

      <AutoComplete v-if="!useDropdown"
                    v-bind="fallthroughAttributes.inputAttrs.value"
                    v-model="currentSelectedUser"
                    data-cy="existingUserInputDropdown"
                    class="w-full"
                    :dropdown="true"
                    :suggestions="suggestions"
                    optionLabel="label"
                    @item-select="(event) => console.log(`item-select: ${JSON.stringify(event)}`)"
                    @item-unselect="(event) => console.log(`item-unselect: ${JSON.stringify(event)}`)"
                    @update:modelValue="createTagIfNecessary"
                    @complete="suggestUsersFromEvent"
                    @dropdownClick="onBeforeShow">
      </AutoComplete>

<!--    </InputGroup>-->
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