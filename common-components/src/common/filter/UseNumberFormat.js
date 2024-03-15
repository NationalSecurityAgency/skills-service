export const useNumberFormat = () => {
  const pretty = (value) => {
    if (!value) {
      return 0
    }
    return new Intl.NumberFormat('en-IN').format(
      value,
    )
  };

  return {
    pretty
  }
}