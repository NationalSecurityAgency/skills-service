/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['selector', '.my-dark-mode'],
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [require('tailwindcss-primeui')]
}

