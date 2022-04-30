package com.mattrandell.insiderapi.model

class Player(val id: String, var name: String) {
    var isActive: Boolean = false
    var role: PlayerRole? = null
    var accusedPlayer: Player? = null
}