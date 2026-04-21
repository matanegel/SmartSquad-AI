import { useState, useEffect } from 'react'
import { X, Trash2, Loader } from 'lucide-react'
import { getAllPlayers, deletePlayer } from '../services/api'

function ManagePlayersModal({ onClose }) {
  const [players, setPlayers] = useState([])
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    loadPlayers()
  }, [])

  const loadPlayers = async () => {
    try {
      const data = await getAllPlayers()
      setPlayers(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (name) => {
    setDeleting(name)
    setError('')

    try {
      await deletePlayer(name)
      setPlayers((prev) => prev.filter((p) => p.name !== name))
    } catch (err) {
      setError(err.message)
    } finally {
      setDeleting(null)
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

        <h2 className="text-xl font-bold text-gray-900 mb-1">Manage Players</h2>
        <p className="text-sm text-gray-500 mb-4">
          {players.length} player{players.length !== 1 ? 's' : ''} in database
        </p>

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader size={24} className="animate-spin text-gray-400" />
          </div>
        ) : players.length === 0 ? (
          <p className="text-gray-500 text-center py-12">
            No players yet. Add some first!
          </p>
        ) : (
          <div className="overflow-y-auto flex-1 flex flex-col gap-2 pr-1">
            {players.map((player) => (
              <div
                key={player.id}
                className="flex items-center gap-3 p-3 rounded-lg border border-gray-200"
              >
                <div className="flex-1 min-w-0">
                  <span className="font-medium text-gray-900 block">{player.name}</span>
                  <span className="text-xs text-gray-500">
                    Skill: {player.skillLevel} · Secondary: {player.secondarySkill}
                    {player.hasToBeWith && ` · Partner: ${player.hasToBeWith}`}
                    {player.cannotBeWith && ` · Rival: ${player.cannotBeWith}`}
                  </span>
                </div>

                <button
                  onClick={() => handleDelete(player.name)}
                  disabled={deleting === player.name}
                  className="p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
                >
                  {deleting === player.name ? (
                    <Loader size={16} className="animate-spin" />
                  ) : (
                    <Trash2 size={16} />
                  )}
                </button>
              </div>
            ))}
          </div>
        )}

        {error && <p className="text-red-500 text-sm mt-3">{error}</p>}
      </div>
    </div>
  )
}

export default ManagePlayersModal
