package com.mattrandell.insiderapi.service

import com.mattrandell.insiderapi.ACTION_LOOKUP
import com.mattrandell.insiderapi.GAME_CODE_CHARS
import com.mattrandell.insiderapi.GAME_CODE_LENGTH
import com.mattrandell.insiderapi.WORD_LIST
import com.mattrandell.insiderapi.dto.GameStateDto
import com.mattrandell.insiderapi.dto.GameSummary
import com.mattrandell.insiderapi.dto.PlayerDto
import com.mattrandell.insiderapi.model.*
import com.mattrandell.insiderapi.repository.GameRepository
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.LocalDateTime

@Service
class GameService(private val gameRepository: GameRepository) {

  fun getGameState(playerId: String): GameStateDto {
    val game = gameRepository.playerGameMap[playerId] ?: Game("").apply { status = GameStatus.NO_GAME }
    val player = game.players[playerId] ?: Player(playerId, "")
    return mapGameStateDto(game, player)
  }

  fun createGame(playerId: String, playerName: String, gameSettings: GameSettings = GameSettings()): GameStateDto {
    if (playerName.isBlank()) {
      throw IllegalArgumentException("Player name cannot be blank.")
    }
    leaveGame(playerId)
    val player = Player(playerId, playerName)
    val game = Game(getGameCode()).apply {
      this.status = GameStatus.WAITING
      this.players[playerId] = player
      this.gameSettings = gameSettings
    }
    gameRepository.gameMap[game.code] = game
    gameRepository.playerGameMap[playerId] = game
    return mapGameStateDto(game, player)
  }

  fun joinGame(playerId: String, playerName: String, gameCode: String): GameStateDto {
    if (playerName.isBlank()) {
      throw IllegalArgumentException("Player name cannot be blank.")
    }
    val game = gameRepository.gameMap[gameCode] ?: throw IllegalArgumentException("No game found with code $gameCode.")
    if (game == gameRepository.playerGameMap[playerId] && game.players.containsKey(playerId)) {
      return mapGameStateDto(game, game.players[playerId]!!)
    }
    leaveGame(playerId)
    val player = Player(playerId, playerName)
    game.players[playerId] = player
    game.lastActivity = LocalDateTime.now()
    gameRepository.playerGameMap[playerId] = game
    return mapGameStateDto(game, player)
  }

  fun leaveGame(playerId: String): GameStateDto {
    val game = gameRepository.playerGameMap[playerId]
    game?.players?.remove(playerId)
    gameRepository.playerGameMap.remove(playerId)
    if (game?.players?.size == 0) {
      gameRepository.gameMap.remove(game.code)
    }
    return getGameState(playerId)
  }

  fun setPlayerReady(playerId: String, playerRole: PlayerRole? = null): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)

    if (game.status != GameStatus.WAITING) {
      throw IllegalArgumentException("The game is not waiting for players to ready-up.")
    }
    when (playerRole) {
      PlayerRole.LEADER -> {
        if (!game.gameSettings.canClaimLeader) throw IllegalArgumentException("Not allowed to claim Leader role.")
        if (game.players.values.any { it.role == PlayerRole.LEADER }) throw IllegalArgumentException("There is already a Leader.")
      }
      PlayerRole.INSIDER -> {
        if (!game.gameSettings.canClaimInsider) throw IllegalArgumentException("Not allowed to claim Insider role.")
        if (game.players.values.any { it.role == PlayerRole.INSIDER }) throw IllegalArgumentException("There is already an Insider.")
      }
      PlayerRole.COMMON -> {
        if (!game.gameSettings.canClaimCommon) throw IllegalArgumentException("Not allowed to claim Common role.")
      }
      null -> {}
    }
    player.role = playerRole
    player.isActive = true
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun setPlayerNotReady(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    player.role = null
    player.isActive = false
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun resetGameStatus(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.RESET) != true) {
      throw IllegalArgumentException("Game cannot be cancelled in current status.")
    }
    game.players.values.forEach {
      it.role = null
      it.isActive = false
      it.accusedPlayer = null
    }
    game.status = GameStatus.WAITING
    game.secretWord = null
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun assignPlayerRoles(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.ASSIGN_ROLES) != true) {
      throw IllegalArgumentException("Roles cannot be assigned in current status.")
    }
    val rolesToFill = mutableMapOf(PlayerRole.LEADER to 1, PlayerRole.INSIDER to 1, PlayerRole.COMMON to 1)
    val unassignedReadyPlayers = mutableListOf<Player>()
    val rolesToAssign = mutableMapOf<Player, PlayerRole>()
    game.players.values.filter { it.isActive }.forEach {
      if (it.role != null) {
        rolesToFill[it.role!!] = (rolesToFill[it.role] ?: 0) - 1
      } else {
        unassignedReadyPlayers.add(it)
      }
    }
    rolesToFill.filter { it.value > 0 }.forEach {
      for (i in 1..it.value) {
        if (unassignedReadyPlayers.size == 0) {
          throw IllegalArgumentException("Not enough players to assign roles")
        }
        val randomPlayer = unassignedReadyPlayers.random()
        rolesToAssign[randomPlayer] = it.key
        unassignedReadyPlayers.remove(randomPlayer)
      }
    }
    rolesToAssign.forEach { it.key.role = it.value }
    unassignedReadyPlayers.forEach { it.role = PlayerRole.COMMON }
    game.status = GameStatus.PRE_GAME
    game.secretWord = WORD_LIST.random()
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun exchangeWord(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (player.role != PlayerRole.LEADER) {
      throw IllegalArgumentException("Word can only be exchanged by the leader.")
    }
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.EXCHANGE_WORD) != true) {
      throw IllegalArgumentException("Word cannot be exchanged in current status.")
    }
    game.secretWord = WORD_LIST.random()
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun startGame(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.START) != true) {
      throw IllegalArgumentException("Game cannot be started in current status.")
    }
    game.status = GameStatus.PLAYING
    game.lastActivity = LocalDateTime.now()
    game.playStartTime = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun wordGuessed(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (player.role != PlayerRole.LEADER) {
      throw IllegalArgumentException("Only the leader can mark the word as guessed.")
    }
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.GUESSED) != true) {
      throw IllegalArgumentException("Word cannot be guessed in current status.")
    }
    game.status = GameStatus.FIND_INSIDER
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun timeUp(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (player.role != PlayerRole.LEADER) {
      throw IllegalArgumentException("Only the leader can claim time is up.")
    }
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.TIME_UP) != true) {
      throw IllegalArgumentException("Time cannot be up in current status.")
    }
    game.status = GameStatus.LOST
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun votePlayer(playerId: String, accusedPlayerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.VOTE_PLAYER) != true) {
      throw IllegalArgumentException("Accusations cannot be cast in current status.")
    }
    val accusedPlayer = game.players[accusedPlayerId]
        ?: throw IllegalArgumentException("The accused player does not exist.")
    player.accusedPlayer = accusedPlayer
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun completeVoting(playerId: String): GameStateDto {
    val (game, player) = getGameAndPlayer(playerId)
    if (ACTION_LOOKUP[game.status]?.contains(GameAction.VOTE_PLAYER) != true) {
      throw IllegalArgumentException("Voting cannot be completed in current status.")
    }
    game.status = GameStatus.SUMMARY
    game.lastActivity = LocalDateTime.now()
    return mapGameStateDto(game, player)
  }

  fun endGame(playerId: String): GameStateDto {
    throw IllegalArgumentException("Ending a game doesn't work yet.")
//    val (game, player) = getGameAndPlayer(playerId)
//    return mapGameStateDto(game, player)
  }

  private fun getGameAndPlayer(playerId: String): Pair<Game, Player> {
    val game = gameRepository.playerGameMap[playerId] ?: throw IllegalArgumentException("No game found for player.")
    // This should not happen, but if it does, fix it and throw an error
    val player = game.players[playerId] ?: run {
      gameRepository.playerGameMap.remove(playerId)
      throw IllegalArgumentException("No game found for player.")
    }
    return Pair(game, player)
  }

  private fun getGameCode(): String {
    var newCode: String
    do {
      newCode = generateGameCode()
    } while (gameRepository.gameMap[newCode] != null)
    return newCode
  }

  private fun generateGameCode(): String {
    return (1..GAME_CODE_LENGTH).map { kotlin.random.Random.nextInt(0, GAME_CODE_CHARS.size) }.map(GAME_CODE_CHARS::get).joinToString("")
  }

  private fun mapGameStateDto(game: Game, player: Player): GameStateDto {
    return GameStateDto(player.id, game.code, game.players.values.stream().map { p -> mapPlayerDto(p) }.toList(), game.status, game.gameSettings, game.lastActivity).apply {
      playStartTime = game.playStartTime
      yourRole = player.role
      secretWord = if (player.role == PlayerRole.INSIDER || player.role == PlayerRole.LEADER) game.secretWord else null
      gameSummary = mapGameSummary(game)
    }
  }

  private fun mapPlayerDto(player: Player): PlayerDto {
    return PlayerDto(player.id, player.name, player.isActive, player.accusedPlayer?.name)
  }

  private fun mapGameSummary(game: Game): GameSummary? {
    if (game.status != GameStatus.SUMMARY && game.status != GameStatus.LOST) {
      return null
    }
    val insiderName = game.players.values.firstOrNull() { it.role == PlayerRole.INSIDER }?.name;
    val votes = game.players.values.filter { it.isActive }.map { it.accusedPlayer?.name ?: "no vote" }.groupingBy { it }.eachCount()
    return GameSummary(
        game.secretWord?:"NO WORD",
        insiderName,
        votes
    )
  }

}