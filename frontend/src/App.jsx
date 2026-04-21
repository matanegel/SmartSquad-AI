import { useState } from 'react'
import { UserPlus, Shuffle, Loader } from 'lucide-react'
import Field from './components/Field'
import CreatePlayerModal from './components/CreatePlayerModal'
import { getAllPlayers, balanceTeams } from './services/api'

function App() {
  const [teams, setTeams] = useState([])
  const [showModal, setShowModal] = useState(false)
  const [numTeams, setNumTeams] = useState(3)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleShuffle = async () => {
    if (teams.length > 0) {
      setTeams([])
      setError('')
      return
    }

    setLoading(true)
    setError('')

    try {
      const players = await getAllPlayers()
      if (players.length === 0) {
        setError('No players yet! Add some players first.')
        return
      }
      const names = players.map((p) => p.name)
      const result = await balanceTeams(names, numTeams)
      setTeams(result)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="h-screen flex border-4 border-black bg-black">
      {/* ===== LEFT PANEL (1/4) — Controls ===== */}
      <div className="w-1/4 bg-white flex flex-col items-center justify-center gap-6 p-6">
        <h1 className="text-3xl font-bold text-gray-900 tracking-tight text-center">
          SmartSquad AI
        </h1>
        <p className="text-gray-500 text-sm text-center">
          Build your squad. Balance the teams. Hit the pitch.
        </p>
        <div className="flex flex-col gap-3 w-full max-w-48">
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center justify-center gap-2 px-4 py-3 bg-gray-900 text-white rounded-lg font-medium hover:bg-gray-700 transition-colors cursor-pointer"
          >
            <UserPlus size={20} />
            Create Player
          </button>

          <div className="flex items-center gap-2">
            <select
              value={numTeams}
              onChange={(e) => setNumTeams(Number(e.target.value))}
              disabled={loading}
              className="px-2 py-3 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 cursor-pointer disabled:opacity-50"
            >
              <option value={2}>2</option>
              <option value={3}>3</option>
              <option value={4}>4</option>
            </select>
            <button
              onClick={handleShuffle}
              disabled={loading}
              className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-500 transition-colors cursor-pointer disabled:opacity-50"
            >
              {loading ? (
                <Loader size={20} className="animate-spin" />
              ) : (
                <Shuffle size={20} />
              )}
              {loading ? 'Loading...' : teams.length > 0 ? 'Clear' : 'Squad'}
            </button>
          </div>
        </div>

        {error && (
          <p className="text-red-500 text-xs text-center max-w-48">{error}</p>
        )}
      </div>

      {/* ===== RIGHT PANEL (3/4) — Soccer Field ===== */}
      <div className="w-3/4">
        <Field teams={teams} />
      </div>

      {/* ===== MODAL — Create Player Form ===== */}
      {showModal && (
        <CreatePlayerModal
          onClose={() => setShowModal(false)}
          onPlayerCreated={(result) => {
            console.log('Player created:', result)
          }}
        />
      )}
    </div>
  )
}

export default App
