package com.mattrandell.insiderapi.controller

import com.mattrandell.insiderapi.dto.GameStateDto
import com.mattrandell.insiderapi.model.PlayerRole
import com.mattrandell.insiderapi.service.GameService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException

@RestController
class GameController(private val gameService: GameService) {

    @PostMapping("/getState")
    fun getState(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.getGameState(playerId), HttpStatus.OK)
    }

    @PostMapping("/create")
    fun createGame(playerId: String, playerName: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.createGame(playerId, playerName), HttpStatus.OK)
    }

    @PostMapping("/join")
    fun joinGame(playerId: String, playerName: String, gameCode: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.joinGame(playerId, playerName, gameCode), HttpStatus.OK)
    }

    @PostMapping("/ready")
    fun ready(playerId: String, isReady: Boolean, claimedRole: PlayerRole?): ResponseEntity<GameStateDto> {
        return if(isReady) {
            ResponseEntity(gameService.setPlayerReady(playerId, claimedRole), HttpStatus.OK)
        } else {
            ResponseEntity(gameService.setPlayerNotReady(playerId), HttpStatus.OK)
        }
    }

    @PostMapping("/reset")
    fun resetGame(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.resetGameStatus(playerId), HttpStatus.OK)
    }

    @PostMapping("/assignRoles")
    fun assignRoles(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.assignPlayerRoles(playerId), HttpStatus.OK)
    }

    @PostMapping("/exchangeWord")
    fun exchangeWord(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.exchangeWord(playerId), HttpStatus.OK)
    }

    @PostMapping("/start")
    fun startGame(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.startGame(playerId), HttpStatus.OK)
    }

    @PostMapping("/guessed")
    fun wordGuessed(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.wordGuessed(playerId), HttpStatus.OK)
    }

    @PostMapping("/timeUp")
    fun timeUp(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.timeUp(playerId), HttpStatus.OK)
    }

    @PostMapping("/votePlayer")
    fun votePlayer(playerId: String, accusedPlayerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.votePlayer(playerId, accusedPlayerId), HttpStatus.OK)
    }

    @PostMapping("/complete")
    fun completeVoting(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.completeVoting(playerId), HttpStatus.OK)
    }

    @PostMapping("/end")
    fun endGame(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.endGame(playerId), HttpStatus.OK)
    }
    @PostMapping("/leave")
    fun leaveGame(playerId: String): ResponseEntity<GameStateDto> {
        return ResponseEntity(gameService.leaveGame(playerId), HttpStatus.OK)
    }

}