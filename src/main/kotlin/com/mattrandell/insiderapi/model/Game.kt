package com.mattrandell.insiderapi.model

import com.mattrandell.insiderapi.model.GameStatus.*
import java.time.LocalDateTime

class Game(val code: String) {
    var lastActivity: LocalDateTime = LocalDateTime.now()
    var players: MutableMap<String, Player> = mutableMapOf()
    var status: GameStatus = WAITING
    var secretWord: String? = null
    var gameSettings: GameSettings = GameSettings()
}