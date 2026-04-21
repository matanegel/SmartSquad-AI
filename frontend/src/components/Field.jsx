import PlayerBadge from './PlayerBadge'

const TEAM_COLORS = ['#3b82f6', '#ef4444', '#eab308', '#a855f7', '#f97316', '#06b6d4']

function Field({ teams = [] }) {
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

      {/* ===== TEAM OVERLAY ===== */}
      {hasTeams ? (
        <div className="absolute inset-0 flex">
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
              {/* Team header */}
              <span
                className="text-xs font-bold uppercase tracking-widest px-2 py-1 rounded"
                style={{ color: TEAM_COLORS[teamIndex % TEAM_COLORS.length] }}
              >
                Team {teamIndex + 1}
              </span>

              {/* Players */}
              {team.players.map((player) => (
                <PlayerBadge
                  key={player.id}
                  name={player.name}
                  skillLevel={player.skillLevel}
                  color={TEAM_COLORS[teamIndex % TEAM_COLORS.length]}
                />
              ))}

              {/* Total skill */}
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
