package com.grieztech.ytorganizer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.ui.screens.*
import com.grieztech.ytorganizer.ui.theme.GriezTechTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

val Context.dataStore by preferencesDataStore(name = "settings")
val KEY_THEME = stringPreferencesKey("theme")
val KEY_LANG  = stringPreferencesKey("lang")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = runBlocking {
            newBase.dataStore.data.map { it[KEY_LANG] ?: "ar" }.firstOrNull() ?: "ar"
        }
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val savedTheme = runBlocking {
            dataStore.data.map { it[KEY_THEME] ?: "system" }.firstOrNull() ?: "system"
        }
        val savedLang = runBlocking {
            dataStore.data.map { it[KEY_LANG] ?: "ar" }.firstOrNull() ?: "ar"
        }

        setContent {
            var currentTheme by remember { mutableStateOf(savedTheme) }
            var currentLang  by remember { mutableStateOf(savedLang)  }

            val isDark = when (currentTheme) {
                "dark"  -> true
                "light" -> false
                else    -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            GriezTechTheme(darkTheme = isDark) {
                GriezTechNavHost(
                    currentTheme     = currentTheme,
                    currentLang      = currentLang,
                    onThemeChange    = { newTheme ->
                        currentTheme = newTheme
                        kotlinx.coroutines.MainScope().launch {
                            dataStore.edit { it[KEY_THEME] = newTheme }
                        }
                    },
                    onLanguageChange = { newLang ->
                        kotlinx.coroutines.MainScope().launch {
                            dataStore.edit { it[KEY_LANG] = newLang }
                            recreate()
                        }
                    },
                    onLogout = {},
                )
            }
        }
    }
}

object Destinations {
    const val SPLASH        = "splash"
    const val LOGIN         = "login"
    const val HOME          = "home"
    const val FOLDER_DETAIL = "folder/{folderId}/{folderName}/{folderEmoji}"
    const val SETTINGS      = "settings"
    const val ABOUT         = "about"
}

@Composable
fun GriezTechNavHost(
    currentTheme    : String,
    currentLang     : String,
    onThemeChange   : (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onLogout        : () -> Unit,
) {
    val navController = rememberNavController()
    val context       = androidx.compose.ui.platform.LocalContext.current
    val isLoggedIn    = remember { GoogleSignIn.getLastSignedInAccount(context) != null }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }

    // ── Fade & Slide — تلاشٍ ناعم مع حركة خفيفة للأعلى ──
    // مدة 380ms مع EaseInOutCubic لمنحنى طبيعي غير مقطوع
    val fadeDuration   = 380
    val slideOffset    = 40   // بكسل — حركة خفيفة جداً، ليست قفزاً

    val enterFade  = fadeIn( tween(fadeDuration, easing = EaseInOutCubic))
    val exitFade   = fadeOut(tween(fadeDuration, easing = EaseInOutCubic))
    val slideUp    = slideInVertically( tween(fadeDuration, easing = EaseInOutCubic)) { slideOffset }
    val slideDown  = slideInVertically( tween(fadeDuration, easing = EaseInOutCubic)) { -slideOffset }

    NavHost(
        navController    = navController,
        startDestination = Destinations.SPLASH,
        // الدخول: يتلاشى ويصعد قليلاً من الأسفل
        enterTransition    = { enterFade + slideUp   },
        // الخروج: يتلاشى فقط بدون حركة (شاشة المغادرة تختفي بهدوء)
        exitTransition     = { exitFade               },
        // الرجوع: يتلاشى ويهبط قليلاً من الأعلى
        popEnterTransition = { enterFade + slideDown  },
        // الرجوع-خروج: يتلاشى فقط
        popExitTransition  = { exitFade               },
    ) {

        composable(Destinations.SPLASH) {
            SplashScreen(onNavigateToLogin = {
                val dest = if (isLoggedIn) Destinations.HOME else Destinations.LOGIN
                navController.navigate(dest) { popUpTo(Destinations.SPLASH) { inclusive = true } }
            })
        }

        composable(Destinations.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Destinations.HOME) {
                    popUpTo(Destinations.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Destinations.HOME) {
            HomeScreen(
                onFolderClick   = { folder ->
                    selectedFolder = folder
                    navController.navigate("folder/${folder.id}/${folder.name}/${folder.emoji}")
                },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onLogoutSuccess = {
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Destinations.FOLDER_DETAIL,
            arguments = listOf(
                navArgument("folderId")    { type = NavType.LongType   },
                navArgument("folderName")  { type = NavType.StringType },
                navArgument("folderEmoji") { type = NavType.StringType },
            ),
        ) { back ->
            val folderId    = back.arguments?.getLong("folderId")    ?: return@composable
            val folderName  = back.arguments?.getString("folderName")  ?: ""
            val folderEmoji = back.arguments?.getString("folderEmoji") ?: "📁"
            val folder = selectedFolder ?: Folder(id = folderId, name = folderName, emoji = folderEmoji)
            FolderDetailScreen(folder = folder, onBack = { navController.navigateUp() })
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onBack           = { navController.navigateUp() },
                onAboutClick     = { navController.navigate(Destinations.ABOUT) },
                onLogout         = {
                    navController.navigate(Destinations.LOGIN) { popUpTo(0) { inclusive = true } }
                    onLogout()
                },
                onThemeChange    = onThemeChange,
                onLanguageChange = onLanguageChange,
                currentTheme     = currentTheme,
                currentLang      = currentLang,
            )
        }

        composable(Destinations.ABOUT) {
            AboutScreen(onBack = { navController.navigateUp() })
        }
    }
}