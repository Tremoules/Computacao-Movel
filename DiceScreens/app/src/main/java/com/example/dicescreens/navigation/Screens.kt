package com.example.dicescreens.navigation

sealed class Screens(val route: String) {
    object Roll : Screens("roll_screen")
    object DiceResult : Screens("result_screen/{result}") {
        fun createRoute(result: Int): String {
            return "result_screen/$result"
        }
    }
}