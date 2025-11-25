import '@testing-library/jest-dom/vitest'

const createMemoryStorage = () => {
  let store = new Map()
  return {
    get length() {
      return store.size
    },
    key(index) {
      return Array.from(store.keys())[index] ?? null
    },
    getItem(key) {
      const normalized = String(key)
      return store.has(normalized) ? store.get(normalized) : null
    },
    setItem(key, value) {
      store.set(String(key), String(value))
    },
    removeItem(key) {
      store.delete(String(key))
    },
    clear() {
      store.clear()
    },
  }
}

if (typeof window !== 'undefined') {
  const memoryStorage = createMemoryStorage()
  Object.defineProperty(window, 'localStorage', {
    value: memoryStorage,
    configurable: true,
    writable: true,
  })
  globalThis.localStorage = window.localStorage
}
