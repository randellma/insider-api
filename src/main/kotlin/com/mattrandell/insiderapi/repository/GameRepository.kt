package com.mattrandell.insiderapi.repository

import com.mattrandell.insiderapi.model.Game
import org.springframework.stereotype.Service

@Service
class GameRepository {
    val gameMap: MutableMap<String, Game> = mutableMapOf()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf()
}