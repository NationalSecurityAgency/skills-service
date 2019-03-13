let idCounter = 0;
export default {
  uniqueId(prefix) {
    idCounter += 1;
    const id = `${idCounter}`;
    return prefix ? prefix + id : id;
  },
};
