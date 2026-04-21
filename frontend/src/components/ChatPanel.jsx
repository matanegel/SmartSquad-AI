import { useState, useRef, useEffect } from 'react'
import { Send, Loader, Bot, User } from 'lucide-react'
import { sendChatMessage } from '../services/api'

function ChatPanel({ numTeams, onTeamsReceived }) {
  const [messages, setMessages] = useState([
    { role: 'ai', text: 'Hey! I can help you manage players and balance teams. Try:\n• "Create Messi with skill 5"\n• "Balance all players"\n• "Show all players"' },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const scrollRef = useRef(null)

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages])

  const handleSend = async () => {
    const text = input.trim()
    if (!text || loading) return

    setMessages((prev) => [...prev, { role: 'user', text }])
    setInput('')
    setLoading(true)

    try {
      const response = await sendChatMessage(text, numTeams)
      setMessages((prev) => [
        ...prev,
        { role: 'ai', text: response.reply, intent: response.intent, data: response.data },
      ])

      if (response.intent === 'balance_teams' && response.data && onTeamsReceived) {
        onTeamsReceived(response.data)
      }
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'ai', text: 'Error: ' + err.message },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="flex flex-col h-full border-t border-gray-200">
      <div className="px-3 py-2 bg-gray-50 border-b border-gray-200 flex items-center gap-2">
        <Bot size={16} className="text-purple-600" />
        <span className="text-xs font-semibold text-gray-600 uppercase tracking-wide">AI Assistant</span>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto p-3 flex flex-col gap-2">
        {messages.map((msg, i) => (
          <div
            key={i}
            className={`flex gap-2 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            {msg.role === 'ai' && (
              <div className="w-6 h-6 rounded-full bg-purple-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <Bot size={12} className="text-purple-600" />
              </div>
            )}
            <div
              className={`max-w-[85%] px-3 py-2 rounded-xl text-sm whitespace-pre-wrap ${
                msg.role === 'user'
                  ? 'bg-gray-900 text-white rounded-br-sm'
                  : 'bg-gray-100 text-gray-800 rounded-bl-sm'
              }`}
            >
              {msg.text}
            </div>
            {msg.role === 'user' && (
              <div className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0 mt-0.5">
                <User size={12} className="text-gray-600" />
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="flex gap-2 justify-start">
            <div className="w-6 h-6 rounded-full bg-purple-100 flex items-center justify-center flex-shrink-0">
              <Bot size={12} className="text-purple-600" />
            </div>
            <div className="bg-gray-100 px-3 py-2 rounded-xl rounded-bl-sm">
              <Loader size={14} className="animate-spin text-gray-400" />
            </div>
          </div>
        )}
      </div>

      <div className="p-2 border-t border-gray-200">
        <div className="flex gap-1.5">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask AI anything..."
            disabled={loading}
            className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 disabled:opacity-50"
          />
          <button
            onClick={handleSend}
            disabled={loading || !input.trim()}
            className="p-2 bg-purple-600 text-white rounded-lg hover:bg-purple-500 disabled:opacity-30 cursor-pointer transition-colors"
          >
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  )
}

export default ChatPanel
