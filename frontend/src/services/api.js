const API_BASE = '/api'

// ─── Player endpoints ────────────────────────────────────

export async function getAllPlayers() {
  const response = await fetch(`${API_BASE}/players`)
  if (!response.ok) throw new Error('Failed to fetch players')
  return response.json()
}

export async function createPlayer(player) {
  const response = await fetch(`${API_BASE}/players`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(player),
  })
  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Failed to create player')
  }
  return response.json()
}

export async function deletePlayer(name) {
  const response = await fetch(`${API_BASE}/players?name=${encodeURIComponent(name)}`, {
    method: 'DELETE',
  })
  if (!response.ok) throw new Error('Failed to delete player')
  return response.text()
}

// ─── Balance endpoint ────────────────────────────────────

export async function balanceTeams(playerNames, numTeams = 3) {
  const response = await fetch(`${API_BASE}/balance?numTeams=${numTeams}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(playerNames),
  })
  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Failed to balance teams')
  }
  return response.json()
}
