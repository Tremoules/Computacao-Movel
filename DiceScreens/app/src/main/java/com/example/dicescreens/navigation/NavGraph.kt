package com.example.dicescreens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dicescreens.DiceResult1
import com.example.dicescreens.DiceResult2
import com.example.dicescreens.DiceResult3
import com.example.dicescreens.DiceResult4
import com.example.dicescreens.DiceResult5
import com.example.dicescreens.DiceResult6
import com.example.dicescreens.DiceWithRollerAndImage

@Composable
fun NavGraph (navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screens.Roll.route
    ) {
        composable(route = Screens.Roll.route) {
            DiceWithRollerAndImage(navController = navController)
        }
        composable(
            route = Screens.DiceResult.route,
            arguments = listOf(
                navArgument("result") {
                    type = NavType.IntType
                }
            )
        ) { navBackStack ->
            val result = navBackStack.arguments?.getInt("result") ?: 1

            when (result) {
                1 -> DiceResult1(navController)
                2 -> DiceResult2(navController)
                3 -> DiceResult3(navController)
                4 -> DiceResult4(navController)
                5 -> DiceResult5(navController)
                6 -> DiceResult6(navController)
            }
        }
    }
}
