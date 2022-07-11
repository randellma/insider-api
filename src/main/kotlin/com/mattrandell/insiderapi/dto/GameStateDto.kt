package com.mattrandell.insiderapi.dto

import com.mattrandell.insiderapi.ACTION_LOOKUP
import com.mattrandell.insiderapi.model.GameSettings
import com.mattrandell.insiderapi.model.GameStatus
import com.mattrandell.insiderapi.model.PlayerRole
import java.time.LocalDateTime

data class GameStateDto(val playerId: String, val code: String, val players: Collection<PlayerDto>, val status: GameStatus, val gameSettings: GameSettings, val lastActivity: LocalDateTime) {
  val actions = ACTION_LOOKUP[status] ?: listOf()
  var playStartTime : LocalDateTime? = null
  var secretWord: String? = null
  var yourRole: PlayerRole? = null
  var gameSummary: GameSummary? = null
}
