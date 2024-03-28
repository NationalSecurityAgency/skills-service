import { useSlots } from 'vue';


export const useSlotsUtil = () => {
  const slots = useSlots();

  const hasSlot = (name = 'default') => {
    return !!(slots && slots[name]);
  }

  return {
    hasSlot
  }
}