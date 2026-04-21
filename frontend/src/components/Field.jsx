import { useState } from 'react'
import { AlertTriangle, Loader, Info, Link, Unlink, Trash2 } from 'lucide-react'
import PlayerBadge from './PlayerBadge'

const TEAM_COLORS = ['#3b82f6', '#ef4444', '#eab308', '#a855f7', '#f97316', '#06b6d4']

function ConflictOverlay({ conflict, appliedConstraints, onRerunWithExclusions, rerunning }) {
  const [markedForRemoval, setMarkedForRemoval] = useState(new Set())

  const toggleConstraint = (index) => {
    setMarkedForRemoval((prev) => {
      const next = new Set(prev)
      if (next.has(index)) {
        next.delete(index)
      } else {
        next.add(index)
      }
      return next
    })
  }

  const handleRerun = () => {
    const toExclude = appliedConstraints
      .filter((_, i) => markedForRemoval.has(i))
      .map((c) => ({ playerA: c.playerA, playerB: c.playerB, type: c.type }))
    onRerunWithExclusions(toExclude)
  }

  return (
    <div className="absolute inset-0 flex items-center justify-center p-8">
      <div className="bg-red-900/90 rounded-xl p-6 max-w-lg w-full shadow-2xl">
        <AlertTriangle size={36} className="text-red-400 mx-auto mb-3" />
        <h3 className="text-white font-bold text-lg mb-1 text-center">Constraint Conflict</h3>
        <p className="text-red-200 text-sm mb-4 text-center">{conflict.message}</p>

        {appliedConstraints.length > 0 && (
          <>
            <p className="text-red-300 text-xs font-semibold mb-2 uppercase tracking-wide">
              Select constraints to remove, then re-run:
            </p>
            <div className="flex flex-col gap-1.5 max-h-48 overflow-y-auto mb-4 pr-1">
              {appliedConstraints.map((c, i) => {
                const isMarked = markedForRemoval.has(i)
                const isMust = c.type === 'must_be_with'
                return (
                  <button
                    key={i}
                    onClick={() => toggleConstraint(i)}
                    className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-left transition-all cursor-pointer ${
                      isMarked
                        ? 'bg-red-500/40 line-through opacity-70'
                        : isMust
                          ? 'bg-blue-500/20 text-blue-200'
                          : 'bg-red-500/20 text-red-200'
                    }`}
                  >
                    {isMarked ? (
                      <Trash2 size={14} className="text-red-400 flex-shrink-0" />
                    ) : isMust ? (
                      <Link size={14} className="flex-shrink-0" />
                    ) : (
                      <Unlink size={14} className="flex-shrink-0" />
                    )}
                    <span className="flex-1">
                      <strong>{c.playerA}</strong>
                      {isMust ? ' must be with ' : ' cannot be with '}
                      <strong>{c.playerB}</strong>
                    </span>
                  </button>
                )
              })}
            </div>
          </>
        )}

        <button
          onClick={handleRerun}
          disabled={rerunning || markedForRemoval.size === 0}
          className="w-full px-4 py-2.5 bg-white text-red-900 rounded-lg font-medium hover:bg-red-100 transition-colors disabled:opacity-40 cursor-pointer"
        >
          {rerunning ? (
            <span className="flex items-center gap-2 justify-center">
              <Loader size={16} className="animate-spin" />
              Re-running...
            </span>
          ) : (
            `Remove ${markedForRemoval.size} constraint${markedForRemoval.size !== 1 ? 's' : ''} & Re-run`
          )}
        </button>
      </div>
    </div>
  )
}

function Field({ teams = [], conflict = null, isFallback = false, appliedConstraints = [], onRerunWithExclusions, rerunning = false }) {
  const hasTeams = teams.length > 0

  return (
    <div className="relative w-full h-full bg-green-700 overflow-hidden">
      {/* Field outline */}
      <div className="absolute inset-3 border-2 border-white/60 rounded-sm" />

      {/* Center line */}
      <div className="absolute top-1/2 left-3 right-3 border-t-2 border-white/60" />

      {/* Center circle */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-28 h-28 rounded-full border-2 border-white/60" />

      {/* Center dot */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-2 h-2 rounded-full bg-white/60" />

      {/* Top penalty box */}
      <div className="absolute top-3 left-1/2 -translate-x-1/2 w-44 h-16 border-b-2 border-x-2 border-white/60" />

      {/* Top goal box */}
      <div className="absolute top-3 left-1/2 -translate-x-1/2 w-20 h-8 border-b-2 border-x-2 border-white/60" />

      {/* Bottom penalty box */}
      <div className="absolute bottom-3 left-1/2 -translate-x-1/2 w-44 h-16 border-t-2 border-x-2 border-white/60" />

      {/* Bottom goal box */}
      <div className="absolute bottom-3 left-1/2 -translate-x-1/2 w-20 h-8 border-t-2 border-x-2 border-white/60" />

      {/* Corner arcs */}
      <div className="absolute top-3 left-3 w-5 h-5 border-b-2 border-r-2 border-white/60 rounded-br-full" />
      <div className="absolute top-3 right-3 w-5 h-5 border-b-2 border-l-2 border-white/60 rounded-bl-full" />
      <div className="absolute bottom-3 left-3 w-5 h-5 border-t-2 border-r-2 border-white/60 rounded-tr-full" />
      <div className="absolute bottom-3 right-3 w-5 h-5 border-t-2 border-l-2 border-white/60 rounded-tl-full" />

      {/* ===== CONFLICT ERROR OVERLAY ===== */}
      {conflict ? (
        <ConflictOverlay
          conflict={conflict}
          appliedConstraints={appliedConstraints}
          onRerunWithExclusions={onRerunWithExclusions}
          rerunning={rerunning}
        />
      ) : hasTeams ? (
        <div className="absolute inset-0 flex">
          {/* Fallback notice */}
          {isFallback && (
            <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 bg-yellow-900/80 text-yellow-200 px-3 py-1 rounded-full text-xs flex items-center gap-1">
              <Info size={12} />
              Used fallback algorithm
            </div>
          )}

          {/* Divider lines between team sections */}
          {teams.slice(1).map((_, i) => (
            <div
              key={`divider-${i}`}
              className="absolute top-3 bottom-3 border-l-2 border-dashed border-white/40"
              style={{ left: `${((i + 1) / teams.length) * 100}%` }}
            />
          ))}

          {/* Team sections with players */}
          {teams.map((team, teamIndex) => (
            <div
              key={teamIndex}
              className="h-full flex flex-col items-center justify-evenly py-8"
              style={{ width: `${100 / teams.length}%` }}
            >
              <span
                className="text-xs font-bold uppercase tracking-widest px-2 py-1 rounded"
                style={{ color: TEAM_COLORS[teamIndex % TEAM_COLORS.length] }}
              >
                Team {teamIndex + 1}
              </span>

              {team.players.map((player, playerIndex) => (
                <PlayerBadge
                  key={player.id || `${teamIndex}-${playerIndex}`}
                  name={player.name}
                  skillLevel={player.skillLevel}
                  color={TEAM_COLORS[teamIndex % TEAM_COLORS.length]}
                />
              ))}

              <span className="text-white/50 text-xs font-medium">
                Skill: {team.totalSkill}
              </span>
            </div>
          ))}
        </div>
      ) : (
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-white/30 text-lg font-medium tracking-wider mt-36">
            Waiting for squad...
          </span>
        </div>
      )}
    </div>
  )
}

export default Field
