function PlayerBadge({ name, skillLevel, color }) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div
        className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-lg border-2 border-white/30"
        style={{ backgroundColor: color }}
      >
        {skillLevel}
      </div>
      <span className="text-white text-xs font-medium drop-shadow-md truncate max-w-16 text-center">
        {name}
      </span>
    </div>
  )
}

export default PlayerBadge
