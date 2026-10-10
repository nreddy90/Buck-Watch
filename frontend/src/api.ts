export interface HealthStatus {
  status: string
  service: string
}

const API_BASE = '/api'

async function request<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`)

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status} ${response.statusText}`)
  }

  return response.json() as Promise<T>
}

export const api = {
  getHealth: () => request<HealthStatus>('/health'),
}
