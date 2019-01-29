export default {
  defaultConf(msg, isErr) {
    return { duration: 3000, message: msg, type: isErr ? 'is-danger' : 'is-info', position: 'is-bottom-right' };
  },
};
