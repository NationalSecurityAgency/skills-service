export const useNumberFormat = () => {
  const pretty = (value) => {
    return new Intl.NumberFormat('en-IN').format(
      value,
    )
  };

  return {
    pretty
  }
}