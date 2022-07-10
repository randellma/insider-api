package com.mattrandell.insiderapi

import com.mattrandell.insiderapi.model.*
import com.mattrandell.insiderapi.repository.GameRepository
import com.mattrandell.insiderapi.service.GameService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class GameServiceTest {

  private val gameRepository: GameRepository = mockk()
  private val gameService: GameService = GameService(gameRepository)

  private fun getTestGame(): Game {
    return Game("1234").apply {
      players["p1"] = Player("p1", "jim")
      players["p2"] = Player("p2", "bob")
      lastActivity = LocalDateTime.now().minusDays(1)
    }
  }

  @Test
  fun `getGame when game not found gameDto is mapped correctly`() {
    val playerGameMap: MutableMap<String, Game> = mutableMapOf()
    every { gameRepository.playerGameMap } returns playerGameMap

    val gameState = gameService.getGameState("p1")

    assertEquals(GameStatus.NO_GAME, gameState.status)
    assertEquals("", gameState.code)
    assertEquals("p1", gameState.playerId)
    assertNull(gameState.yourRole)
    assertNull(gameState.secretWord)
    assertTrue(gameState.players.isEmpty())
  }

  @Test
  fun `getGame when game is found gameDto is mapped correctly`() {
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    every { gameRepository.playerGameMap } returns playerGameMap

    val gameState = gameService.getGameState("p1")

    verify(exactly = 1) { gameRepository.playerGameMap }
    assertEquals(GameStatus.WAITING, gameState.status)
    assertEquals("1234", gameState.code)
    assertEquals("p1", gameState.playerId)
    assertNull(gameState.yourRole)
    assertNull(gameState.secretWord)
    assertEquals(game.lastActivity, gameState.lastActivity)
    assertTrue(gameState.players.any { it.name == "jim" && !it.active })
    assertTrue(gameState.players.any { it.name == "bob" && !it.active })
    assertTrue(gameState.actions.containsAll(listOf(GameAction.ASSIGN_ROLES, GameAction.READY, GameAction.END)))
  }

  @Test
  fun `getGame, when user is insider, the secret word is mapped`() {
    // assemble
    val game = getTestGame().apply {
      secretWord = "test"
      players["p1"]?.role = PlayerRole.INSIDER
    }
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    every { gameRepository.playerGameMap } returns playerGameMap

    // act
    val gameState = gameService.getGameState("p1")

    // assert
    assertEquals(GameStatus.WAITING, gameState.status)
    assertEquals("1234", gameState.code)
    assertEquals(PlayerRole.INSIDER, gameState.yourRole)
    assertEquals("test", gameState.secretWord)
    assertTrue(gameState.players.any { it.name == "jim" && !it.active })
    assertTrue(gameState.players.any { it.name == "bob" && !it.active })
    assertTrue(gameState.actions.containsAll(listOf(GameAction.ASSIGN_ROLES, GameAction.READY, GameAction.END)))
  }

  @Test
  fun `getGame, when user is leader, the secret word is mapped`() {
    // assemble
    val game = getTestGame().apply {
      secretWord = "test"
      players["p1"]?.role = PlayerRole.LEADER
    }
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    every { gameRepository.playerGameMap } returns playerGameMap

    // act
    val gameState = gameService.getGameState("p1")

    // assert
    assertEquals(GameStatus.WAITING, gameState.status)
    assertEquals("1234", gameState.code)
    assertEquals(PlayerRole.LEADER, gameState.yourRole)
    assertEquals("test", gameState.secretWord)
    assertTrue(gameState.players.any { it.name == "jim" && !it.active })
    assertTrue(gameState.players.any { it.name == "bob" && !it.active })
    assertTrue(gameState.actions.containsAll(listOf(GameAction.ASSIGN_ROLES, GameAction.READY, GameAction.END)))
  }

  @Test
  fun `getGame, when user is common, the secret word is not mapped`() {
    // assemble
    val game = getTestGame().apply {
      secretWord = "test"
      players["p1"]?.role = PlayerRole.COMMON
    }
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    every { gameRepository.playerGameMap } returns playerGameMap

    // act
    val gameState = gameService.getGameState("p1")

    // assert
    assertEquals(GameStatus.WAITING, gameState.status)
    assertEquals("1234", gameState.code)
    assertEquals(PlayerRole.COMMON, gameState.yourRole)
    assertNull(gameState.secretWord)
    assertNull(gameState.gameSummary)
    assertTrue(gameState.players.any { it.name == "jim" && !it.active})
    assertTrue(gameState.players.any { it.name == "bob" && !it.active })
    assertTrue(gameState.actions.containsAll(listOf(GameAction.ASSIGN_ROLES, GameAction.READY, GameAction.END)))
  }

  @Test
  fun `getGame, when status is Summary, the summary is mapped`() {
    // assemble
    val game = getTestGame().apply {
      status = GameStatus.SUMMARY
      secretWord = "test"
      players["p1"]?.role = PlayerRole.COMMON
    }
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    every { gameRepository.playerGameMap } returns playerGameMap

    // act
    val gameState = gameService.getGameState("p1")

    // assert
    assertNotNull(gameState.gameSummary)
  }

  @Test
  fun `getGame, when status is Summary, maps summary correctly`() {
    // assemble
    val game = getTestGame().apply {
      status = GameStatus.SUMMARY
      secretWord = "test"
      players["p1"]?.isActive = true
      players["p1"]?.role = PlayerRole.COMMON
      players["p1"]?.accusedPlayer = players["p2"]
      players["p2"]?.isActive = true
      players["p2"]?.role = PlayerRole.COMMON
      players["p2"]?.accusedPlayer = players["p1"]
    }
    game.players["p3"] = Player("p3", "bobby").apply {
      isActive = true
      role = PlayerRole.COMMON
      accusedPlayer = game.players["p1"]
    }
    game.players["p4"] = Player("p3", "ysbo").apply {
      isActive = true
      role = PlayerRole.INSIDER
    }
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    every { gameRepository.playerGameMap } returns playerGameMap

    // act
    val gameState = gameService.getGameState("p1")

    // assert
    assertNotNull(gameState.gameSummary)
    assertEquals("test", gameState.gameSummary?.secretWord)
    assertEquals("ysbo", gameState.gameSummary?.insider)
    assertEquals("bob", gameState.players.first { it.name == "jim" }.accusedPlayer)
    assertEquals("jim", gameState.players.first { it.name == "bob" }.accusedPlayer)
    assertEquals("jim", gameState.players.first { it.name == "bobby" }.accusedPlayer)
    assertEquals(2, gameState.gameSummary?.votes?.get("jim") ?: -1)
    assertEquals(1, gameState.gameSummary?.votes?.get("bob") ?: -1)
    assertEquals(1, gameState.gameSummary?.votes?.get("no vote") ?: -1)
  }

  @Test
  fun `All game status has an actions list`() {
    GameStatus.values().forEach { status -> assertNotNull(ACTION_LOOKUP[status]) }
  }

  @Test
  fun `createGame removes player from existing game and deletes old`() {
    val game = getTestGame()
    game.players.remove("p2")
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    val gameDto = gameService.createGame("p1", "Jimmy")

    assertNull(gameMap["1234"])
    assertEquals(gameDto.code, gameMap[gameDto.code]?.code)
    assertEquals(gameDto.code, playerGameMap["p1"]?.code)
  }

  @Test
  fun `createGame removes player from existing games but doesn't delete if other players exist`() {
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    val gameDto = gameService.createGame("p1", "Jimmy")

    assertNotNull(gameMap["1234"])
    assertEquals(gameDto.code, gameMap[gameDto.code]?.code)
    assertEquals(gameDto.code, playerGameMap["p1"]?.code)
  }

  @Test
  fun `createGame with blank name throws an exception`() {
    val playerGameMap: MutableMap<String, Game> = mutableMapOf()
    val gameMap: MutableMap<String, Game> = mutableMapOf()
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    gameService.createGame("p1", "Jimmy")

    val thrown = assertThrows<IllegalArgumentException> { gameService.createGame("p1", "") }

    assertEquals("Player name cannot be blank.", thrown.message)
  }

  @Test
  fun `createGame generates a valid id`() {
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf()
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    val gameDto = gameService.createGame("p1", "Jimmy")

    assertTrue(gameDto.code.length == 5)
  }

  @Test
  fun `createGame with default configuration is correctly mapped`() {
    // assemble
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf()
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    val gameDto = gameService.createGame("p1", "Jimmy")

    //assert
    assertEquals(GameStatus.WAITING, gameDto.status)
    assertNotNull(gameDto.code)
    assertNull(gameDto.yourRole)
    assertNull(gameDto.secretWord)
    assertNull(gameDto.gameSummary)
    assertTrue(gameDto.players.any { it.name == "Jimmy" && !it.active })
    assertEquals(1, gameDto.players.size)
    assertEquals(GameSettings(), gameDto.gameSettings)
    assertTrue(gameDto.actions.containsAll(listOf(GameAction.ASSIGN_ROLES, GameAction.READY, GameAction.END)))
  }

  @Test
  fun `createGame with custom configuration is correctly mapped`() {
    // assemble
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf()
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap


    // act
    val gameDto = gameService.createGame("p1", "Jimmy", GameSettings(canClaimLeader = true, canClaimInsider = true, canClaimCommon = true, guessTimeLimit = 10))

    //assert
    assertEquals(GameStatus.WAITING, gameDto.status)
    assertNotNull(gameDto.code)
    assertNull(gameDto.yourRole)
    assertNull(gameDto.secretWord)
    assertNull(gameDto.gameSummary)
    assertTrue(gameDto.players.any { it.name == "Jimmy" && !it.active })
    assertEquals(1, gameDto.players.size)
    assertEquals(GameSettings(canClaimLeader = true, canClaimInsider = true, canClaimCommon = true, guessTimeLimit = 10), gameDto.gameSettings)
    assertTrue(gameDto.actions.containsAll(listOf(GameAction.ASSIGN_ROLES, GameAction.READY, GameAction.END)))
  }

  @Test
  fun `joinGame when it exists adds the player and returns the gameDto`() {
    // assemble
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.joinGame("p3", "Frank", "1234")

    // assert
    assertEquals(game, playerGameMap["p3"])
    assertEquals(3, game.players.size)
    assertEquals("Frank", game.players["p3"]?.name)
  }

  @Test
  fun `joinGame when valid, updates lastUpdated`() {
    // assemble
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.joinGame("p3", "Frank", "1234")

    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `joinGame when player exists in another game, player is removed from existing game`() {
    // assemble
    val game = getTestGame()

    val frank = Player("p3", "Frank")
    val existingGame = Game("4321").apply { players["p3"] = frank }

    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to existingGame)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game, "4321" to existingGame)

    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.joinGame("p3", "Frank", "1234")

    // assert
    assertEquals(game, playerGameMap["p3"])
    assertEquals(3, game.players.size)
    assertEquals("Frank", game.players["p3"]?.name)
    assertFalse(gameMap.containsKey("4321"))
    assertTrue(existingGame.players.isEmpty())
  }

  @Test
  fun `joinGame when player map contains player but game doesn't (somehow) the player still gets added`() {
    // assemble
    val game = getTestGame()

    val existingGame = Game("4321")

    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to existingGame)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game, "4321" to existingGame)

    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.joinGame("p3", "Frank", "1234")

    // assert
    assertEquals(game, playerGameMap["p3"])
    assertEquals(3, game.players.size)
    assertEquals("Frank", game.players["p3"]?.name)
    assertFalse(gameMap.containsKey("4321"))
    assertTrue(existingGame.players.isEmpty())
  }

  @Test
  fun `joinGame when player name is blank, throws exception`() {
    // assemble
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    val thrown = assertThrows<IllegalArgumentException> { gameService.joinGame("p3", "", "1234") }

    // assert
    assertEquals("Player name cannot be blank.", thrown.message)
  }


  @Test
  fun `joinGame when game not found, throws exception`() {
    // assemble
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    val thrown = assertThrows<IllegalArgumentException> { gameService.joinGame("p3", "Frank", "4321") }

    // assert
    assertEquals("No game found with code 4321.", thrown.message)
  }

  @Test
  fun `leaveGame removes player from existing game and deletes old`() {
    val game = getTestGame()
    game.players.remove("p2")
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    gameService.leaveGame("p1")

    assertNull(gameMap["1234"])
    assertTrue(!game.players.containsKey("p1"))
  }

  @Test
  fun `leaveGame removes player from existing game and does not delete game if players remain`() {
    val game = getTestGame()
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    gameService.leaveGame("p1")

    assertNotNull(gameMap["1234"])
    assertTrue(!game.players.containsKey("p1"))
  }

  @Test
  fun `setPlayerReady when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `setPlayerReady when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `setPlayerReady when game not WAITING, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1") }

    assertEquals("The game is not waiting for players to ready-up.", thrown.message)
  }

  @Test
  fun `setPlayerReady when claiming LEADER and game not permitting, throws exception`() {
    val game = getTestGame()
    game.gameSettings.canClaimLeader = false
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1", PlayerRole.LEADER) }

    assertEquals("Not allowed to claim Leader role.", thrown.message)
  }

  @Test
  fun `setPlayerReady when claiming LEADER and game already has a leader, throws exception`() {
    val game = getTestGame()
    game.players["p2"]?.role = PlayerRole.LEADER
    game.gameSettings.canClaimLeader = true
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1", PlayerRole.LEADER) }

    assertEquals("There is already a Leader.", thrown.message)
  }

  @Test
  fun `setPlayerReady when claiming INSIDER and game not permitting, throws exception`() {
    val game = getTestGame()
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1", PlayerRole.INSIDER) }

    assertEquals("Not allowed to claim Insider role.", thrown.message)
  }

  @Test
  fun `setPlayerReady when claiming INSIDER and game already has an insider, throws exception`() {
    val game = getTestGame()
    game.players["p2"]?.role = PlayerRole.INSIDER
    game.gameSettings.canClaimInsider = true
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1", PlayerRole.INSIDER) }

    assertEquals("There is already an Insider.", thrown.message)
  }

  @Test
  fun `setPlayerReady when claiming COMMON and game not permitting, throws exception`() {
    val game = getTestGame()
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerReady("p1", PlayerRole.COMMON) }

    assertEquals("Not allowed to claim Common role.", thrown.message)
  }

  @Test
  fun `setPlayerReady when claiming role, assigns player role and sets player active`() {
    val game = getTestGame()
    game.gameSettings.canClaimInsider = true
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.setPlayerReady("p1", PlayerRole.INSIDER)

    assertEquals(PlayerRole.INSIDER, game.players["p1"]?.role)
    assertTrue(game.players["p1"]?.isActive!!)
  }

  @Test
  fun `setPlayerReady when not claiming role, sets player role to null and sets player active`() {
    val game = getTestGame()
    game.gameSettings.canClaimInsider = true
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.setPlayerReady("p1")

    assertNull(game.players["p1"]?.role)
    assertTrue(game.players["p1"]?.isActive!!)
  }

  @Test
  fun `setPlayerReady when valid, updates lastUpdated`() {
    val game = getTestGame()
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.setPlayerReady("p1")

    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `setPlayerNotReady when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerNotReady("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `setPlayerNotReady when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.setPlayerNotReady("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `setPlayerNotReady when valid, sets player role to null and sets player inactive`() {
    val game = getTestGame()
    game.players["p1"]?.role = PlayerRole.INSIDER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.setPlayerNotReady("p1")

    assertNull(game.players["p1"]?.role)
    assertFalse(game.players["p1"]?.isActive!!)
  }

  @Test
  fun `setPlayerNotReady when valid, updates lastUpdated`() {
    val game = getTestGame()
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.setPlayerNotReady("p1")

    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `resetGameStatus when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.resetGameStatus("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `resetGameStatus when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.resetGameStatus("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `resetGameStatus inactivates all players, nulls their role, nulls their accusations, resets game to waiting, nulls secretWord`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    game.secretWord = "Book"
    game.players["p1"]?.isActive = true
    game.players["p1"]?.accusedPlayer = game.players["p2"]
    game.players["p1"]?.role = PlayerRole.INSIDER
    game.players["p2"]?.isActive = true
    game.players["p2"]?.role = PlayerRole.LEADER
    game.players["p2"]?.accusedPlayer = game.players["p1"]
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.resetGameStatus("p1")

    assertTrue(game.players.values.none { it.role != null || it.isActive })
    assertTrue(game.players.values.none { it.accusedPlayer != null || it.isActive })
    assertNull(game.secretWord)
    assertEquals(GameStatus.WAITING, game.status)
    assertEquals(GameStatus.WAITING, game.status)
  }

  @Test
  fun `resetGameStatus when valid, updates lastUpdated`() {
    val game = getTestGame()
    game.status = GameStatus.PRE_GAME
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.resetGameStatus("p1")

    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `assignPlayerRoles when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.assignPlayerRoles("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `assignPlayerRoles when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.assignPlayerRoles("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `assignPlayerRoles when game invalid status, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.assignPlayerRoles("p1") }

    assertEquals("Roles cannot be assigned in current status.", thrown.message)
  }

  @Test
  fun `assignPlayerRoles without enough unassigned players, throws exception`() {
    val game = getTestGame()
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.assignPlayerRoles("p1") }

    assertEquals("Not enough players to assign roles", thrown.message)
  }

  @Test
  fun `assignPlayerRoles with inactive players, only assigns roles to active players`() {
    // assemble
    val game = getTestGame()
    game.players["p3"] = Player("p3", "Bobby")
    game.players.values.forEach { it.isActive = true }
    game.players["p4"] = Player("p4", "IA1")
    game.players["p5"] = Player("p5", "IA2")
    game.players["p6"] = Player("p6", "IA2")
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.assignPlayerRoles("p1")

    assertEquals(3, game.players.values.filter { !it.isActive }.size)
    assertTrue(game.players.values.filter { !it.isActive }.all { it.role == null })
  }

  @Test
  fun `assignPlayerRoles with leader already assigned, respects assignment`() {
    // assemble
    val game = getTestGame()
    game.players["p3"] = Player("p3", "Bobby")
    game.players.values.forEach { it.isActive = true }
    game.players["p1"]?.role = PlayerRole.LEADER
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.assignPlayerRoles("p1")

    assertEquals(PlayerRole.LEADER, game.players["p1"]?.role)
  }

  @Test
  fun `assignPlayerRoles with insider already assigned, respects assignment`() {
    // assemble
    val game = getTestGame()
    game.players["p3"] = Player("p3", "Bobby")
    game.players.values.forEach { it.isActive = true }
    game.players["p1"]?.role = PlayerRole.INSIDER
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.assignPlayerRoles("p1")

    assertEquals(PlayerRole.INSIDER, game.players["p1"]?.role)
  }

  @Test
  fun `assignPlayerRoles with common already assigned, respects assignment`() {
    // assemble
    val game = getTestGame()
    game.players["p3"] = Player("p3", "Bobby")
    game.players.values.forEach { it.isActive = true }
    game.players["p1"]?.role = PlayerRole.COMMON
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.assignPlayerRoles("p1")

    assertEquals(PlayerRole.COMMON, game.players["p1"]?.role)
  }

  @Test
  fun `assignPlayerRoles with no assignments, distributes roles`() {
    // assemble
    val game = getTestGame()
    game.players["p3"] = Player("p3", "Bobby")
    game.players.values.forEach { it.isActive = true }
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.assignPlayerRoles("p1")

    assertTrue(game.players.values.filter { it.isActive }.all { it.role != null })
  }

  @Test
  fun `assignPlayerRoles with valid players, creates a secret word and sets game to PreGame and updates lastUpdate`() {
    // assemble
    val game = getTestGame()
    game.players["p3"] = Player("p3", "Bobby")
    game.players.values.forEach { it.isActive = true }
    val playerGameMap: MutableMap<String, Game> = mutableMapOf("p1" to game, "p2" to game, "p3" to game)
    val gameMap: MutableMap<String, Game> = mutableMapOf("1234" to game)
    every { gameRepository.playerGameMap } returns playerGameMap
    every { gameRepository.gameMap } returns gameMap

    // act
    gameService.assignPlayerRoles("p1")

    assertEquals(GameStatus.PRE_GAME, game.status)
    assertNotNull(game.secretWord)
    assertTrue(WORD_LIST.contains(game.secretWord))
    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `exchangeWord when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.exchangeWord("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `exchangeWord when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.exchangeWord("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `exchangeWord when game non-leader, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()
    val thrown = assertThrows<IllegalArgumentException> { gameService.exchangeWord("p1") }

    assertEquals("Word can only be exchanged by the leader.", thrown.message)
  }

  @Test
  fun `exchangeWord when game invalid status, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    game.players["p1"]?.role = PlayerRole.LEADER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.exchangeWord("p1") }

    assertEquals("Word cannot be exchanged in current status.", thrown.message)
  }

  @Test
  fun `exchangeWord when valid, the word changes and updates lastUpdated`() {
    val game = getTestGame()
    game.status = GameStatus.PRE_GAME
    game.players["p1"]?.role = PlayerRole.LEADER
    game.secretWord = "My secret word"
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.exchangeWord("p1")

    assertNotEquals("My secret word", game.secretWord)
    assertNotNull(game.secretWord)
    assertTrue(WORD_LIST.contains(game.secretWord))
    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `startGame when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.startGame("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `startGame when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.startGame("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `startGame when game invalid status, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    game.players["p1"]?.role = PlayerRole.LEADER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.startGame("p1") }

    assertEquals("Game cannot be started in current status.", thrown.message)
  }

  @Test
  fun `startGame when valid, the word changes and updates lastUpdated`() {
    val game = getTestGame()
    game.status = GameStatus.PRE_GAME
    game.players["p1"]?.role = PlayerRole.LEADER
    game.secretWord = "My secret word"
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.startGame("p1")

    assertEquals(GameStatus.PLAYING, game.status)
    assertEquals("My secret word", game.secretWord)
    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }


  @Test
  fun `wordGuessed when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.wordGuessed("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `wordGuessed when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.wordGuessed("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `wordGuessed when game invalid status, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.WAITING
    game.players["p1"]?.role = PlayerRole.LEADER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.wordGuessed("p1") }

    assertEquals("Word cannot be guessed in current status.", thrown.message)
  }

  @Test
  fun `wordGuessed when player not leader, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.WAITING
    game.players["p1"]?.role = PlayerRole.COMMON
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.wordGuessed("p1") }

    assertEquals("Only the leader can mark the word as guessed.", thrown.message)
  }

  @Test
  fun `wordGuessed when game valid, sets status find insider and updates last activity`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    game.players["p1"]?.role = PlayerRole.LEADER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.wordGuessed("p1")

    assertEquals(GameStatus.FIND_INSIDER, game.status)
    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }


  @Test
  fun `timeUp when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.timeUp("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `timeUp when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.timeUp("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `timeUp when game invalid status, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.WAITING
    game.players["p1"]?.role = PlayerRole.LEADER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.timeUp("p1") }

    assertEquals("Time cannot be up in current status.", thrown.message)
  }

  @Test
  fun `timeUp when player not leader, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.WAITING
    game.players["p1"]?.role = PlayerRole.COMMON
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.timeUp("p1") }

    assertEquals("Only the leader can claim time is up.", thrown.message)
  }

  @Test
  fun `timeUp when game valid, sets status to summary and updates last activity`() {
    val game = getTestGame()
    game.status = GameStatus.PLAYING
    game.players["p1"]?.role = PlayerRole.LEADER
    game.players["p1"]?.isActive = true
    game.players["p2"]?.role = PlayerRole.INSIDER
    game.players["p2"]?.isActive = true
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val dto = gameService.timeUp("p1")

    assertEquals(GameStatus.LOST, game.status)
    assertNotNull(dto.gameSummary)
    assertNotNull(dto.gameSummary!!.secretWord)
    assertEquals("bob", dto.gameSummary!!.insider)
    assertEquals(1, dto.gameSummary!!.votes.keys.size)
    assertEquals(2, dto.gameSummary!!.votes.get("no vote"))
    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `votePlayer when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.votePlayer("p1", "p2") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `votePlayer when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.votePlayer("p1", "p2") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `votePlayer when game invalid status, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.WAITING
    game.players["p1"]?.role = PlayerRole.LEADER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.votePlayer("p1", "p2") }

    assertEquals("Accusations cannot be cast in current status.", thrown.message)
  }

  @Test
  fun `votePlayer when accused player does not exist, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.FIND_INSIDER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.votePlayer("p1", "no player") }

    assertEquals("The accused player does not exist.", thrown.message)
  }

  @Test
  fun `votePlayer when valid, sets accused player and updates last activity`() {
    val game = getTestGame()
    game.status = GameStatus.FIND_INSIDER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.votePlayer("p1", "p2")

    assertEquals(game.players["p2"], game.players["p1"]?.accusedPlayer)
    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

  @Test
  fun `completeVoting when game not found for players, throws exception`() {
    every { gameRepository.playerGameMap } returns mutableMapOf()
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.completeVoting("p1") }

    assertEquals("No game found for player.", thrown.message)
  }

  @Test
  fun `completeVoting when game record found but player not in player list, fixes state and throws exception`() {
    val game = getTestGame()
    game.players.remove("p1")
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.completeVoting("p1") }

    assertEquals("No game found for player.", thrown.message)
    assertFalse(game.players.containsKey("p1"))
    assertFalse(gameRepository.playerGameMap.containsKey("p1"))
  }

  @Test
  fun `completeVoting when game invalid status, throws exception`() {
    val game = getTestGame()
    game.status = GameStatus.WAITING
    game.players["p1"]?.role = PlayerRole.LEADER
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    val thrown = assertThrows<IllegalArgumentException> { gameService.completeVoting("p1") }

    assertEquals("Voting cannot be completed in current status.", thrown.message)
  }

  @Test
  fun `completeVoting when valid, sets status to summary and updates last active`() {
    val game = getTestGame()
    game.status = GameStatus.FIND_INSIDER
    game.players["p1"]?.role = PlayerRole.LEADER
    game.players["p1"]?.isActive = true
    game.players["p1"]?.accusedPlayer = game.players["p2"]
    every { gameRepository.playerGameMap } returns mutableMapOf("p1" to game)
    every { gameRepository.gameMap } returns mutableMapOf()

    gameService.completeVoting("p1")

    assertEquals(GameStatus.SUMMARY, game.status)
    assertTrue(game.lastActivity.truncatedTo(ChronoUnit.SECONDS).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
  }

//  @Test
//  fun `endGame destroys game from lookup`() {
//    assertTrue(false)
//  }

}