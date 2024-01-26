import { useStorage } from '@vueuse/core'

export const useThemesHelper = () => {

  const themeOptions =[
    { icon: 'fas fa-sun', name: 'Light', value: 'skills-light-green' },
    { icon: 'fas fa-moon', name: 'Dark', value: 'skills-dark-green' },
  ];
  const currentTheme = useStorage('currentTheme', themeOptions[0])

  const configureDefaultThemeFileInHeadTag = () =>{
    let file = document.createElement('link')
    file.id=   'theme-link'
    file.rel = 'stylesheet'
    file.href = `/themes/${currentTheme.value.value}/theme.css`
    document.head.appendChild(file)
  }

  return {
    currentTheme,
    themeOptions,
    configureDefaultThemeFileInHeadTag
  }
}