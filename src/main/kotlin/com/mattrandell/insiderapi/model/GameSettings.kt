package com.mattrandell.insiderapi.model

data class GameSettings(
        var canClaimLeader: Boolean = true,
        var canClaimInsider: Boolean = false,
        var canClaimCommon: Boolean = false,
        var guessTimeLimit: Int = 5
)