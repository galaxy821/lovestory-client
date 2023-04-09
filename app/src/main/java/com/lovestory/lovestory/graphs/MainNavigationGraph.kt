package com.lovestory.lovestory.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lovestory.lovestory.R
import com.lovestory.lovestory.ui.screens.*

@Composable
fun MainNavGraph(navHostController: NavHostController){
    NavHost(navController = navHostController, startDestination =MainScreens.DashBoard.route, route = Graph.MAIN){
        composable(MainScreens.DashBoard.route){
            DashBoardScreen(navHostController = navHostController)
        }
        composable(MainScreens.Gallery.route){
            GalleryScreen(navHostController = navHostController)
        }
        composable(MainScreens.Calendar.route){
            CalendarScreen(navHostController = navHostController)
        }
        composable(MainScreens.Profile.route){
            ProfileScreen(navHostController = navHostController)
        }
        composable(MainScreens.Map.route) {
            MapScreen(navHostController = navHostController)
        }
    }
}

@Composable
fun CalendarNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = CalendarScreens.Calendar.route, route = "CALENDAR_NAV") {
        composable(CalendarScreens.Calendar.route) {
            CalendarScreen(navHostController = navController)
        }
        composable(CalendarScreens.Map.route) {
            MapScreen(navHostController = navController)
        }
    }
}

sealed class CalendarScreens(val route: String, val title: String) {
    object Calendar : CalendarScreens("CALENDAR", "CALENDAR")
    object Map : CalendarScreens("MAP", "MAP")
}

sealed class MainScreens(val route : String, val title : String, val icon : Int){
    object DashBoard : MainScreens(route = "DASHBOARD", title = "DASHBOARD", icon = R.drawable.ic_home)
    object Gallery : MainScreens(route = "GALLERY", title = "GALLERY", icon = R.drawable.ic_gallery)
    object Calendar : MainScreens(route = "CALENDAR", title = "CALENDAR", icon = R.drawable.ic_calendar)
    object Profile : MainScreens(route = "PROFILE", title= "PROFILE", icon = R.drawable.ic_setting)
    object Map : MainScreens(route = "MAP", title = "MAP", icon = R.drawable.ic_option)
}