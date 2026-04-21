import { useState } from 'react'
import { UserPlus, Shuffle, Settings } from 'lucide-react'
import Field from './components/Field'
import CreatePlayerModal from './components/CreatePlayerModal'
import PlayerPickerModal from './components/PlayerPickerModal'
import ManagePlayersModal from './components/ManagePlayersModal'
import { smartBalance } from './services/api'

function App() {
  const [teams, setTeams] = useState([])
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showPickerModal, setShowPickerModal] = useState(false)
  const [showManageModal, setShowManageModal] = useState(false)
  const [numTeams, setNumTeams] = useState(3)
  const [conflict, setConflict] = useState(null)
  const [isFallback, setIsFallback] = useState(false)
  const [lastPickedNames, setLastPickedNames] = useState([])
  const [lastAdditionalConstraints, setLastAdditionalConstraints] = useState([])
  const [appliedConstraints, setAppliedConstraints] = useState([])
  const [excludedConstraints, setExcludedConstraints] = useState([])
  const [rerunning, setRerunning] = useState(false)

  const handleSquadClick = () => {
    if (teams.length > 0 || conflict) {
      setTeams([])
      setConflict(null)
      setIsFallback(false)
      setExcludedConstraints([])
      setAppliedConstraints([])
      setLastAdditionalConstraints([])
    } else {
      setShowPickerModal(true)
    }
  }

  const handleSmartResult = (result, playerNames, additionalConstraints = []) => {
    setLastPickedNames(playerNames)
    setLastAdditionalConstraints(additionalConstraints)
    setAppliedConstraints(result.appliedConstraints || [])

    if (result.status === 'success') {
      setTeams(result.teams)
      setConflict(null)
      setIsFallback(result.fallback || false)
    } else {
      setTeams([])
      setConflict({
        reason: result.reason,
        message: result.message,
        conflictingPlayers: result.conflictingPlayers || [],
      })
    }
  }

  const handleRerunWithExclusions = async (constraintsToExclude) => {
    setRerunning(true)
    const newExcluded = [...excludedConstraints, ...constraintsToExclude]
    setExcludedConstraints(newExcluded)

    try {
      const result = await smartBalance(
        lastPickedNames,
        numTeams,
        newExcluded,
        lastAdditionalConstraints
      )
      handleSmartResult(result, lastPickedNames, lastAdditionalConstraints)
    } catch (err) {
      setConflict({
        reason: 'network_error',
        message: err.message,
        conflictingPlayers: [],
      })
    } finally {
      setRerunning(false)
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
              {teams.length > 0 || conflict ? 'Clear' : 'Squad'}
            </button>
          </div>
        </div>
      </div>

      {/* ===== RIGHT PANEL (3/4) — Soccer Field ===== */}
      <div className="w-3/4">
        <Field
          teams={teams}
          conflict={conflict}
          isFallback={isFallback}
          appliedConstraints={appliedConstraints}
          onRerunWithExclusions={handleRerunWithExclusions}
          rerunning={rerunning}
        />
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
          onResult={handleSmartResult}
        />
      )}

      {showManageModal && (
        <ManagePlayersModal onClose={() => setShowManageModal(false)} />
      )}
    </div>
  )
}

export default App
