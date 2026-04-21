import { useState, useEffect } from 'react'
import { X, Check, Loader, Users, Plus, Trash2, Link, Unlink } from 'lucide-react'
import { getAllPlayers, smartBalance } from '../services/api'

function PlayerPickerModal({ numTeams, onClose, onResult }) {
  const [players, setPlayers] = useState([])
  const [selected, setSelected] = useState(new Set())
  const [loading, setLoading] = useState(true)
  const [balancing, setBalancing] = useState(false)
  const [error, setError] = useState('')
  const [constraints, setConstraints] = useState([])
  const [cPlayerA, setCPlayerA] = useState('')
  const [cPlayerB, setCPlayerB] = useState('')
  const [cType, setCType] = useState('must_be_with')

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

  const addConstraint = () => {
    if (!cPlayerA || !cPlayerB || cPlayerA === cPlayerB) return
    const exists = constraints.some(
      (c) =>
        c.type === cType &&
        ((c.playerA === cPlayerA && c.playerB === cPlayerB) ||
          (c.playerA === cPlayerB && c.playerB === cPlayerA))
    )
    if (exists) return
    setConstraints((prev) => [
      ...prev,
      { type: cType, playerA: cPlayerA, playerB: cPlayerB },
    ])
    setCPlayerA('')
    setCPlayerB('')
  }

  const removeConstraint = (index) => {
    setConstraints((prev) => prev.filter((_, i) => i !== index))
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
      const result = await smartBalance(names, numTeams, [], constraints)
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

        {selected.size >= 2 && (
          <div className="border-t pt-3 mt-1">
            <h3 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-1">
              <Link size={14} /> Game-Day Constraints
            </h3>

            <div className="flex gap-1 items-end mb-2">
              <select
                value={cPlayerA}
                onChange={(e) => setCPlayerA(e.target.value)}
                className="flex-1 px-2 py-1.5 border border-gray-300 rounded-md text-sm"
              >
                <option value="">Player A</option>
                {Array.from(selected).map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>

              <select
                value={cType}
                onChange={(e) => setCType(e.target.value)}
                className="px-2 py-1.5 border border-gray-300 rounded-md text-sm"
              >
                <option value="must_be_with">must be with</option>
                <option value="cannot_be_with">cannot be with</option>
              </select>

              <select
                value={cPlayerB}
                onChange={(e) => setCPlayerB(e.target.value)}
                className="flex-1 px-2 py-1.5 border border-gray-300 rounded-md text-sm"
              >
                <option value="">Player B</option>
                {Array.from(selected)
                  .filter((n) => n !== cPlayerA)
                  .map((name) => (
                    <option key={name} value={name}>
                      {name}
                    </option>
                  ))}
              </select>

              <button
                onClick={addConstraint}
                disabled={!cPlayerA || !cPlayerB}
                className="p-1.5 bg-blue-600 text-white rounded-md hover:bg-blue-500 disabled:opacity-30 cursor-pointer flex-shrink-0"
              >
                <Plus size={16} />
              </button>
            </div>

            {constraints.length > 0 && (
              <div className="flex flex-col gap-1 max-h-24 overflow-y-auto">
                {constraints.map((c, i) => (
                  <div
                    key={i}
                    className={`flex items-center justify-between text-xs px-2 py-1.5 rounded-md ${
                      c.type === 'must_be_with'
                        ? 'bg-blue-50 text-blue-700'
                        : 'bg-red-50 text-red-700'
                    }`}
                  >
                    <span className="flex items-center gap-1">
                      {c.type === 'must_be_with' ? (
                        <Link size={12} />
                      ) : (
                        <Unlink size={12} />
                      )}
                      <strong>{c.playerA}</strong>
                      {c.type === 'must_be_with' ? ' with ' : ' not with '}
                      <strong>{c.playerB}</strong>
                    </span>
                    <button
                      onClick={() => removeConstraint(i)}
                      className="text-gray-400 hover:text-red-500 cursor-pointer"
                    >
                      <Trash2 size={12} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
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
