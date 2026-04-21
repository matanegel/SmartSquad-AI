import { useState, useEffect } from 'react'
import { X, Check, Loader, Users } from 'lucide-react'
import { getAllPlayers, smartBalance } from '../services/api'

function PlayerPickerModal({ numTeams, onClose, onResult }) {
  const [players, setPlayers] = useState([])
  const [selected, setSelected] = useState(new Set())
  const [loading, setLoading] = useState(true)
  const [balancing, setBalancing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getAllPlayers()
      .then((data) => {
        setPlayers(data)
        setLoading(false)
      })
      .catch((err) => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  const togglePlayer = (name) => {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(name)) {
        next.delete(name)
      } else {
        next.add(name)
      }
      return next
    })
  }

  const selectAll = () => {
    if (selected.size === players.length) {
      setSelected(new Set())
    } else {
      setSelected(new Set(players.map((p) => p.name)))
    }
  }

  const handleBalance = async () => {
    if (selected.size < numTeams * 2) {
      setError(`Need at least ${numTeams * 2} players for ${numTeams} teams`)
      return
    }

    setBalancing(true)
    setError('')

    try {
      const names = Array.from(selected)
      const result = await smartBalance(names, numTeams)
      onResult(result, names)
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setBalancing(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 relative max-h-[80vh] flex flex-col">
        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-gray-400 hover:text-gray-700 cursor-pointer"
        >
          <X size={20} />
        </button>

        <h2 className="text-xl font-bold text-gray-900 mb-1">Pick Players</h2>
        <p className="text-sm text-gray-500 mb-4">
          Select who's coming to the game ({selected.size} selected)
        </p>

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader size={24} className="animate-spin text-gray-400" />
          </div>
        ) : players.length === 0 ? (
          <p className="text-gray-500 text-center py-12">
            No players in the database. Add some first!
          </p>
        ) : (
          <>
            <button
              onClick={selectAll}
              className="text-sm text-blue-600 hover:text-blue-800 font-medium mb-2 self-start cursor-pointer"
            >
              {selected.size === players.length ? 'Deselect all' : 'Select all'}
            </button>

            <div className="overflow-y-auto flex-1 flex flex-col gap-2 mb-4 pr-1">
              {players.map((player) => {
                const isSelected = selected.has(player.name)
                return (
                  <button
                    key={player.id}
                    onClick={() => togglePlayer(player.name)}
                    className={`flex items-center gap-3 p-3 rounded-lg border-2 text-left transition-colors cursor-pointer ${
                      isSelected
                        ? 'border-green-500 bg-green-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div
                      className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 ${
                        isSelected ? 'bg-green-500' : 'bg-gray-200'
                      }`}
                    >
                      {isSelected && <Check size={14} className="text-white" />}
                    </div>

                    <div className="flex-1 min-w-0">
                      <span className="font-medium text-gray-900 block">{player.name}</span>
                      <span className="text-xs text-gray-500">
                        Skill: {player.skillLevel}
                        {player.hasToBeWith && ` · Partner: ${player.hasToBeWith}`}
                        {player.cannotBeWith && ` · Rival: ${player.cannotBeWith}`}
                      </span>
                    </div>

                    <span className="text-lg font-bold text-gray-400 flex-shrink-0">
                      {player.skillLevel}
                    </span>
                  </button>
                )
              })}
            </div>
          </>
        )}

        {error && <p className="text-red-500 text-sm mb-3">{error}</p>}

        <button
          onClick={handleBalance}
          disabled={balancing || selected.size === 0}
          className="w-full py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-500 transition-colors disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
        >
          {balancing ? (
            <Loader size={20} className="animate-spin" />
          ) : (
            <Users size={20} />
          )}
          {balancing ? 'Balancing...' : `Balance ${selected.size} Players into ${numTeams} Teams`}
        </button>
      </div>
    </div>
  )
}

export default PlayerPickerModal
