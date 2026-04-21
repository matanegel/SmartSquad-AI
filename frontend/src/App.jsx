import { useState } from 'react'
import { UserPlus, Shuffle, Settings } from 'lucide-react'
import Field from './components/Field'
import CreatePlayerModal from './components/CreatePlayerModal'
import PlayerPickerModal from './components/PlayerPickerModal'
import ManagePlayersModal from './components/ManagePlayersModal'

function App() {
  const [teams, setTeams] = useState([])
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showPickerModal, setShowPickerModal] = useState(false)
  const [showManageModal, setShowManageModal] = useState(false)
  const [numTeams, setNumTeams] = useState(3)

  const handleSquadClick = () => {
    if (teams.length > 0) {
      setTeams([])
    } else {
      setShowPickerModal(true)
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
            onClick={() => setShowCreateModal(true)}
            className="flex items-center justify-center gap-2 px-4 py-3 bg-gray-900 text-white rounded-lg font-medium hover:bg-gray-700 transition-colors cursor-pointer"
          >
            <UserPlus size={20} />
            Create Player
          </button>
          <button
            onClick={() => setShowManageModal(true)}
            className="flex items-center justify-center gap-2 px-4 py-3 border-2 border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-100 transition-colors cursor-pointer"
          >
            <Settings size={20} />
            Manage Players
          </button>

          <div className="flex items-center gap-2">
            <select
              value={numTeams}
              onChange={(e) => setNumTeams(Number(e.target.value))}
              className="px-2 py-3 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 cursor-pointer"
            >
              <option value={2}>2</option>
              <option value={3}>3</option>
              <option value={4}>4</option>
            </select>
            <button
              onClick={handleSquadClick}
              className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-500 transition-colors cursor-pointer"
            >
              <Shuffle size={20} />
              {teams.length > 0 ? 'Clear' : 'Squad'}
            </button>
          </div>
        </div>
      </div>

      {/* ===== RIGHT PANEL (3/4) — Soccer Field ===== */}
      <div className="w-3/4">
        <Field teams={teams} />
      </div>

      {showCreateModal && (
        <CreatePlayerModal
          onClose={() => setShowCreateModal(false)}
          onPlayerCreated={(result) => {
            console.log('Player created:', result)
          }}
        />
      )}

      {showPickerModal && (
        <PlayerPickerModal
          numTeams={numTeams}
          onClose={() => setShowPickerModal(false)}
          onTeamsReady={(result) => setTeams(result)}
        />
      )}

      {showManageModal && (
        <ManagePlayersModal onClose={() => setShowManageModal(false)} />
      )}
    </div>
  )
}

export default App
