export const useColors = () => {
  const colors = ['text-blue-500', 'text-green-500', 'text-cyan-500', 'text-indigo-500', 'text-teal-500', 'text-orange-500', 'text-purple-500']
  const getTextClass = (index) => {
    const colorIndex = index % colors.length
    const color = colors[colorIndex]
    return color
  }


  const goldText = 'text-yellow-600'
  const silverText = 'text-bluegray-600'
  const bronzeText = 'text-orange-800'

  const getRankTextClass = (rank) => {
    if (rank === 1) {
      return goldText;
    }
    if (rank === 2) {
      return silverText;
    }
    if (rank === 3) {
      return bronzeText;
    }
    return null;
  }

  return {
    getTextClass,
    getRankTextClass
  }
}