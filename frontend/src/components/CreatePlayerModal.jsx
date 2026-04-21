import { useState } from 'react'
import { X } from 'lucide-react'
import { createPlayer } from '../services/api'

function CreatePlayerModal({ onClose, onPlayerCreated }) {
  const [name, setName] = useState('')
  const [skillLevel, setSkillLevel] = useState(3)
  const [secondarySkill, setSecondarySkill] = useState(2)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const result = await createPlayer({
        name,
        skillLevel,
        secondarySkill,
      })
      onPlayerCreated(result)
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-sm p-6 relative">
        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-gray-400 hover:text-gray-700 cursor-pointer"
        >
          <X size={20} />
        </button>

        <h2 className="text-xl font-bold text-gray-900 mb-4">Add New Player</h2>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Player Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              placeholder="e.g. matan"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Skill Level: {skillLevel}
            </label>
            <input
              type="range"
              min="1"
              max="5"
              value={skillLevel}
              onChange={(e) => setSkillLevel(Number(e.target.value))}
              className="w-full"
            />
            <div className="flex justify-between text-xs text-gray-400">
              <span>Beginner</span>
              <span>Pro</span>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Secondary Skill: {secondarySkill}
            </label>
            <input
              type="range"
              min="1"
              max="5"
              value={secondarySkill}
              onChange={(e) => setSecondarySkill(Number(e.target.value))}
              className="w-full"
            />
            <div className="flex justify-between text-xs text-gray-400">
              <span>Beginner</span>
              <span>Pro</span>
            </div>
          </div>

          {error && (
            <p className="text-red-500 text-sm">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2 bg-gray-900 text-white rounded-lg font-medium hover:bg-gray-700 transition-colors disabled:opacity-50 cursor-pointer"
          >
            {loading ? 'Saving...' : 'Save Player'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default CreatePlayerModal
