package com.mattrandell.insiderapi.model

enum class GameAction(val route: String) {
    CREATE("create"),
    JOIN("join"),
    READY("ready"),
    RESET("reset"),
    ASSIGN_ROLES("assignRoles"),
    EXCHANGE_WORD("exchangeWord"),
    START("start"),
    GUESSED("guessed"),
    TIME_UP("timeUp"),
    VOTE_PLAYER("votePlayer"),
    COMPLETE_VOTING("complete"),
    END("end")
}