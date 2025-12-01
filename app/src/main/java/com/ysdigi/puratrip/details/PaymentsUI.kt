package com.ysdigi.puratrip.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.R
import com.ysdigi.puratrip.models.Expense
import com.ysdigi.puratrip.models.Settlement
import com.ysdigi.puratrip.ui.theme.NegativeBalance
import com.ysdigi.puratrip.ui.theme.NegativeBalanceDark
import com.ysdigi.puratrip.ui.theme.PositiveBalance
import com.ysdigi.puratrip.ui.theme.PositiveBalanceDark
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    uiState: TripDetailsUiState,
    onDeleteExpense: (String) -> Unit,
    currency: String,
    onSettleUp: (String, String, Double) -> Unit,
    onUpdateExpense: (Expense) -> Unit
) {
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var showSettleUpDialog by remember { mutableStateOf(false) }
    var showEditExpenseDialog by remember { mutableStateOf(false) }
    var showExpenseDetailsDialog by remember { mutableStateOf(false) }

    val sortedExpenses = uiState.expenses.sortedByDescending { it.timestamp }
    val groupedExpenses = sortedExpenses.groupBy {
        val calendar = Calendar.getInstance()
        calendar.time = it.timestamp ?: Date()
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BalanceSummary(uiState.balances, uiState.userNames, currency)
        }

        if (uiState.settlements.isNotEmpty()) {
            item {
                DebtsToSettle(uiState.settlements, uiState.userNames, currency) {
                    showSettleUpDialog = true
                }
            }
        }

        item {
            Text("Expenses", style = MaterialTheme.typography.titleMedium)
        }

        groupedExpenses.forEach { (month, expenses) ->
            item {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(expenses) { expense ->
                ExpenseItem(
                    expense = expense,
                    onDeleteExpense = onDeleteExpense,
                    onExpenseClick = {
                        selectedExpense = it
                        showExpenseDetailsDialog = true
                    },
                    userNames = uiState.userNames,
                    currency = currency
                )
            }
        }
    }

    if (showSettleUpDialog) {
        SettleUpDialog(
            users = uiState.trip?.users ?: emptyList(),
            userNames = uiState.userNames,
            onDismiss = { showSettleUpDialog = false },
            onSettleUp = { from, to, amount ->
                onSettleUp(from, to, amount)
                showSettleUpDialog = false
            }
        )
    }

    if (showEditExpenseDialog) {
        selectedExpense?.let {
            EditExpenseDialog(
                expense = it,
                users = uiState.trip?.users ?: emptyList(),
                userNames = uiState.userNames,
                onDismiss = { showEditExpenseDialog = false },
                onUpdateExpense = { expense ->
                    onUpdateExpense(expense)
                    showEditExpenseDialog = false
                }
            )
        }
    }

    if (showExpenseDetailsDialog) {
        selectedExpense?.let {
            ExpenseDetailsDialog(
                expense = it,
                onDismiss = { showExpenseDetailsDialog = false },
                onEdit = {
                    showExpenseDetailsDialog = false
                    showEditExpenseDialog = true
                },
                userNames = uiState.userNames,
                currency = currency
            )
        }
    }
}

@Composable
fun DebtsToSettle(settlements: List<Settlement>, userNames: Map<String, String>, currency: String, onSettleUpClick: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Debts to Settle", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onSettleUpClick) {
                    Icon(painter = painterResource(id = R.drawable.ic_settle_up), contentDescription = "Settle up")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settle Up")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            settlements.forEach { settlement ->
                ListItem(
                    headlineContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = userNames[settlement.from] ?: settlement.from,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Owes",
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = userNames[settlement.to] ?: settlement.to,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                        }
                    },
                    trailingContent = {
                        Text("${getCurrencySymbol(currency)}%.2f".format(settlement.amount), color = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }
}

@Composable
fun BalanceSummary(balances: Map<String, Double>, userNames: Map<String, String>, currency: String) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Balances", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            balances.forEach { (user, balance) ->
                val color = if (balance >= 0) {
                    if (isSystemInDarkTheme()) PositiveBalanceDark else PositiveBalance
                } else {
                    if (isSystemInDarkTheme()) NegativeBalanceDark else NegativeBalance
                }
                Text("${userNames[user] ?: user}: ${getCurrencySymbol(currency)}%.2f".format(balance), color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onDeleteExpense: (String) -> Unit, onExpenseClick: (Expense) -> Unit, userNames: Map<String, String>, currency: String) {
    val currentUser = Firebase.auth.currentUser
    val color = when {
        expense.paidBy == currentUser?.email -> if (isSystemInDarkTheme()) PositiveBalanceDark.copy(alpha = 0.3f) else PositiveBalance.copy(alpha = 0.3f)
        expense.splitWith.contains(currentUser?.email) -> if (isSystemInDarkTheme()) NegativeBalanceDark.copy(alpha = 0.3f) else NegativeBalance.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseClick(expense) },
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (expense.isSettlement) {
                Icon(painter = painterResource(id = R.drawable.ic_settle_up), contentDescription = "Settlement")
            } else {
                Icon(Icons.Default.Receipt, contentDescription = "Expense")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.Bold)
                Text("Amount: ${getCurrencySymbol(currency)}${expense.amount}")
                Text("Paid by: ${userNames[expense.paidBy] ?: expense.paidBy}")
                Text("Split with: ${expense.splitWith.size} people")
            }
            IconButton(onClick = { onDeleteExpense(expense.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Expense")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpDialog(
    users: List<String>,
    userNames: Map<String, String>,
    onDismiss: () -> Unit,
    onSettleUp: (String, String, Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var fromUser by remember { mutableStateOf(users.firstOrNull() ?: "") }
    var toUser by remember { mutableStateOf(users.firstOrNull() ?: "") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle Up") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = !fromExpanded }
                ) {
                    OutlinedTextField(
                        value = userNames[fromUser] ?: fromUser,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(userNames[user] ?: user) },
                                onClick = {
                                    fromUser = user
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = !toExpanded }
                ) {
                    OutlinedTextField(
                        value = userNames[toUser] ?: toUser,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(userNames[user] ?: user) },
                                onClick = {
                                    toUser = user
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val settlementAmount = amount.toDoubleOrNull()
                    if (settlementAmount != null && settlementAmount > 0) {
                        onSettleUp(fromUser, toUser, settlementAmount)
                    }
                }
            ) {
                Text("Settle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
