package com.example.homework2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

data class Calculation(
    val id: Int,
    val total: Double,
    val people: Int,
    val tipAmount: Double,
    val totalWithTip: Double,
    val perPerson: Double
)

class SplitViewModel : ViewModel() {
    var totalText = mutableStateOf("")
    var peopleText = mutableStateOf("")
    private var nextId = 0
    var history = mutableStateListOf<Calculation>()

    fun isInputValid(): Boolean {
        return try {
            val total = totalText.value.toDouble()
            val people = peopleText.value.toInt()
            total > 0 && people > 0
        } catch (e: Exception) {
            false
        }
    }

    fun calculate(): Int {
        val total = totalText.value.toDouble()
        val people = peopleText.value.toInt()
        val tipPercent = 10.0
        val tipAmount = total * tipPercent / 100
        val totalWithTip = total + tipAmount
        val perPerson = totalWithTip / people

        val calc = Calculation(nextId, total, people, tipAmount, totalWithTip, perPerson)
        history.add(0, calc)
        if (history.size > 5) {
            history.removeAt(history.size - 1)
        }

        nextId++
        resetInput()
        return calc.id
    }

    fun getById(id: Int): Calculation? {
        return history.find { it.id == id }
    }

    fun resetInput() {
        totalText.value = ""
        peopleText.value = ""
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SplitMateApp()
            }
        }
    }
}

@Composable
fun SplitMateApp() {
    val navController = rememberNavController()
    val viewModel: SplitViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("input") { InputScreen(navController, viewModel) }
        composable(
            route = "result/{calcId}",
            arguments = listOf(navArgument("calcId") { type = NavType.IntType })
        ) { entry ->
            val calcId = entry.arguments?.getInt("calcId") ?: -1
            ResultScreen(navController, viewModel, calcId)
        }
        composable("history") { HistoryScreen(navController, viewModel) }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SplitMate", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigate("input") }) {
            Text("Начать")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("history") }) {
            Text("История")
        }
    }
}

@Composable
fun InputScreen(navController: NavHostController, viewModel: SplitViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = viewModel.totalText.value,
            onValueChange = { viewModel.totalText.value = it },
            label = { Text("Сумма счёта") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.peopleText.value,
            onValueChange = { viewModel.peopleText.value = it },
            label = { Text("Количество людей") }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val newId = viewModel.calculate()
                navController.navigate("result/$newId")
            },
            enabled = viewModel.isInputValid()
        ) {
            Text("Рассчитать")
        }
    }
}

@Composable
fun ResultScreen(navController: NavHostController, viewModel: SplitViewModel, calcId: Int) {
    val calc = viewModel.getById(calcId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (calc != null) {
            Text("Чаевые: ${String.format("%.2f", calc.tipAmount)}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Итого с чаевыми: ${String.format("%.2f", calc.totalWithTip)}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("На человека: ${String.format("%.2f", calc.perPerson)}")
        } else {
            Text("Расчёт не найден")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigate("input") }) {
            Text("Вернуться к вводу")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            viewModel.resetInput()
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }) {
            Text("Новый расчёт")
        }
    }
}

@Composable
fun HistoryScreen(navController: NavHostController, viewModel: SplitViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Последние 5 расчётов", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.history.isEmpty()) {
            Text("История пуста")
        } else {
            LazyColumn {
                items(viewModel.history) { calc ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Сумма: ${calc.total}, Людей: ${calc.people}")
                        Text("На человека: ${String.format("%.2f", calc.perPerson)}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.navigate("result/${calc.id}") }) {
                            Text("Посмотреть")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}