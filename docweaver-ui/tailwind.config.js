/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#0b1220',
        panel: '#111b2f',
        panelSoft: '#1a2740',
        accent: '#2dd4bf',
        text: '#e2ecff',
        muted: '#8ea4c9',
        danger: '#fb7185',
        success: '#4ade80'
      },
      boxShadow: {
        card: '0 12px 30px -18px rgba(0,0,0,0.7)'
      }
    }
  },
  plugins: []
};
