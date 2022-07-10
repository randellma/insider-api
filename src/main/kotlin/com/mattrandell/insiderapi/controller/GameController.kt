package com.mattrandell.insiderapi.controller

import com.mattrandell.insiderapi.dto.GameStateDto
import com.mattrandell.insiderapi.model.PlayerRole
import com.mattrandell.insiderapi.service.GameService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class GameController(private val gameService: GameService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/getState")
    fun getState(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Getting state for player: {}", playerId)
        return ResponseEntity(gameService.getGameState(playerId), HttpStatus.OK)
    }

    @PostMapping("/create")
    fun createGame(playerId: String, playerName: String): ResponseEntity<GameStateDto> {
        logger.info("Creating game as player: {}, name: {}", playerId, playerName)
        return ResponseEntity(gameService.createGame(playerId, playerName), HttpStatus.OK)
    }

    @PostMapping("/join")
    fun joinGame(playerId: String, playerName: String, gameCode: String): ResponseEntity<GameStateDto> {
        logger.info("Joining game {} as for player: {}, name: {}", gameCode, playerId, playerName)
        return ResponseEntity(gameService.joinGame(playerId, playerName, gameCode), HttpStatus.OK)
    }

    @PostMapping("/ready")
    fun ready(playerId: String, isReady: Boolean, claimedRole: PlayerRole?): ResponseEntity<GameStateDto> {
        logger.info("Player Ready Change - player: {}, isReady: {}, claimedRole: {}", playerId, isReady, claimedRole)
        return if(isReady) {
            ResponseEntity(gameService.setPlayerReady(playerId, claimedRole), HttpStatus.OK)
        } else {
            ResponseEntity(gameService.setPlayerNotReady(playerId), HttpStatus.OK)
        }
    }

    @PostMapping("/reset")
    fun resetGame(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Game reset - playerId: {}", playerId)
        return ResponseEntity(gameService.resetGameStatus(playerId), HttpStatus.OK)
    }

    @PostMapping("/assignRoles")
    fun assignRoles(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Assign roles - playerId: {}", playerId)
        return ResponseEntity(gameService.assignPlayerRoles(playerId), HttpStatus.OK)
    }

    @PostMapping("/exchangeWord")
    fun exchangeWord(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Exchange word - playerId: {}", playerId)
        return ResponseEntity(gameService.exchangeWord(playerId), HttpStatus.OK)
    }

    @PostMapping("/start")
    fun startGame(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Start game - playerId: {}", playerId)
        return ResponseEntity(gameService.startGame(playerId), HttpStatus.OK)
    }

    @PostMapping("/guessed")
    fun wordGuessed(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Guess confirmed - playerId: {}", playerId)
        return ResponseEntity(gameService.wordGuessed(playerId), HttpStatus.OK)
    }

    @PostMapping("/timeUp")
    fun timeUp(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Time up - playerId: {}", playerId)
        return ResponseEntity(gameService.timeUp(playerId), HttpStatus.OK)
    }

    @PostMapping("/votePlayer")
    fun votePlayer(playerId: String, accusedPlayerId: String): ResponseEntity<GameStateDto> {
        logger.info("Vote player - playerId: {}", playerId)
        return ResponseEntity(gameService.votePlayer(playerId, accusedPlayerId), HttpStatus.OK)
    }

    @PostMapping("/complete")
    fun completeVoting(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Game completed - playerId: {}", playerId)
        return ResponseEntity(gameService.completeVoting(playerId), HttpStatus.OK)
    }

    @PostMapping("/end")
    fun endGame(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Game ended - playerId: {}", playerId)
        return ResponseEntity(gameService.endGame(playerId), HttpStatus.OK)
    }
    @PostMapping("/leave")
    fun leaveGame(playerId: String): ResponseEntity<GameStateDto> {
        logger.info("Player left - playerId: {}", playerId)
        return ResponseEntity(gameService.leaveGame(playerId), HttpStatus.OK)
    }

}