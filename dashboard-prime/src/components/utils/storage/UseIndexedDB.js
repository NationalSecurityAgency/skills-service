import { get, set, del } from 'idb-keyval';
export const useIndexedDB = () => {
  const save = (key, data) => {
    set(key, data);
  }

  const load = (key) => {
    return get(key);
  }

  const clear = (key) => {
    del(key);
  }

  return {
    save,
    load,
    clear
  }
}