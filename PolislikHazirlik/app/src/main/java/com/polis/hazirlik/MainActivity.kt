package com.polis.hazirlik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { AppNav() } }
    }
}

private fun longArg(name: String) = navArgument(name) { type = NavType.LongType }
private fun intArg(name: String) = navArgument(name) { type = NavType.IntType }

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("category/{id}", listOf(longArg("id"))) {
            CategoryScreen(nav, it.arguments!!.getLong("id"))
        }
        composable("test/{id}", listOf(longArg("id"))) {
            TestScreen(nav, it.arguments!!.getLong("id"))
        }
        composable("edit/{testId}/{qid}", listOf(longArg("testId"), longArg("qid"))) {
            QuestionEditScreen(nav, it.arguments!!.getLong("testId"), it.arguments!!.getLong("qid"))
        }
        composable("solve/{id}", listOf(longArg("id"))) {
            SolveScreen(nav, it.arguments!!.getLong("id"))
        }
        composable("result/{id}", listOf(longArg("id"))) {
            ResultScreen(nav, it.arguments!!.getLong("id"))
        }
        composable("review/{id}/{index}", listOf(longArg("id"), intArg("index"))) {
            ReviewScreen(nav, it.arguments!!.getLong("id"), it.arguments!!.getInt("index"))
        }
        composable("favorite/{id}", listOf(longArg("id"))) {
            FavoriteDetailScreen(nav, it.arguments!!.getLong("id"))
        }
    }
}
