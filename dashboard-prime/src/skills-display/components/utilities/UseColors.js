export const useColors = () => {
  const colors = ['text-blue-500', 'text-green-500', 'text-cyan-500', 'text-indigo-500', 'text-teal-500', 'text-orange-500',  'text-purple-500']
  const getTextClass = (index) => {
    const colorIndex = index % colors.length;
    const color = colors[colorIndex];
    return color
  }

  return {
    getTextClass
  }
}