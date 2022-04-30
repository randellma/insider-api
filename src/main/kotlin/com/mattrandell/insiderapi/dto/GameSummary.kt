package com.mattrandell.insiderapi.dto

data class GameSummary(val secretWord: String?, val insider: String?, val votes: Map<String, Int>)
