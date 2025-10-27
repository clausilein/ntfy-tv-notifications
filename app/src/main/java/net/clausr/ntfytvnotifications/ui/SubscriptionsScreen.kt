package net.clausr.ntfytvnotifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.*
import kotlinx.coroutines.launch
import net.clausr.ntfytvnotifications.data.db.entities.Subscription
import net.clausr.ntfytvnotifications.data.repository.AddSubscriptionResult
import net.clausr.ntfytvnotifications.data.repository.SubscriptionRepository
import net.clausr.ntfytvnotifications.util.TopicValidator

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    subscriptionRepository: SubscriptionRepository,
    onBackPressed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val subscriptions by subscriptionRepository.getAllSubscriptions()
        .collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Subscription?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Subscriptions",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp
                )
            )

            Button(
                onClick = onBackPressed,
                modifier = Modifier.semantics {
                    contentDescription = "Go back to main screen"
                }
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 18.sp
                    )
                )
            }
        }

        // Add subscription button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .padding(bottom = 16.dp)
                .semantics {
                    contentDescription = "Add new subscription"
                }
        ) {
            Text(
                text = "Add Subscription",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 18.sp
                )
            )
        }

        // Subscriptions list
        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No subscriptions yet. Add one to get started!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subscriptions) { subscription ->
                    SubscriptionItem(
                        subscription = subscription,
                        onToggle = { isActive ->
                            scope.launch {
                                subscriptionRepository.toggleSubscriptionStatus(
                                    subscription.id,
                                    isActive
                                )
                            }
                        },
                        onDelete = { showDeleteDialog = subscription }
                    )
                }
            }
        }
    }

    // Add subscription dialog
    if (showAddDialog) {
        var dialogError by remember { mutableStateOf<String?>(null) }
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { topic ->
                scope.launch {
                    when (val result = subscriptionRepository.addSubscription(topic)) {
                        is AddSubscriptionResult.Success -> {
                            showAddDialog = false
                        }
                        is AddSubscriptionResult.AlreadyExists -> {
                            dialogError = "Subscription for \"$topic\" already exists"
                        }
                        is AddSubscriptionResult.Error -> {
                            dialogError = "Error: ${result.message}"
                        }
                    }
                }
            },
            error = dialogError
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { subscription ->
        DeleteConfirmationDialog(
            subscriptionTopic = subscription.topic,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                scope.launch {
                    try {
                        subscriptionRepository.deleteSubscriptionWithMessages(subscription)
                        showDeleteDialog = null
                    } catch (e: Exception) {
                        // Log error but still dismiss dialog
                        android.util.Log.e("SubscriptionsScreen", "Error deleting subscription", e)
                        showDeleteDialog = null
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SubscriptionItem(
    subscription: Subscription,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .background(
                color = if (isFocused)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "ntfy.sh/${subscription.topic}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = if (subscription.isActive) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp
                    ),
                    color = if (subscription.isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = subscription.isActive,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = if (subscription.isActive)
                            "Subscription is active, toggle to deactivate"
                        else
                            "Subscription is inactive, toggle to activate"
                    }
                )

                Button(
                    onClick = onDelete,
                    modifier = Modifier.semantics {
                        contentDescription = "Delete subscription ${subscription.topic}"
                    }
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    error: String? = null
) {
    var topic by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Update local error when error prop changes
    LaunchedEffect(error) {
        localError = error
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(600.dp)
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Subscription",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                )

                Text(
                    text = "Enter the ntfy.sh topic name:",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                )

                // BasicTextField for TV-friendly input
                BasicTextField(
                    value = topic,
                    onValueChange = {
                        topic = it
                        localError = null  // Clear error on input
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        if (topic.isEmpty()) {
                            Text(
                                text = "Enter topic name...",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()  // Auto-focus text field
                }

                Text(
                    text = "Note: Use your remote or connected keyboard to type",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                localError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    }

                    Button(
                        onClick = {
                            val trimmedTopic = topic.trim()
                            when (val result = TopicValidator.validate(trimmedTopic)) {
                                is TopicValidator.ValidationResult.Valid -> onConfirm(trimmedTopic)
                                is TopicValidator.ValidationResult.Invalid -> localError = result.message
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = topic.isNotBlank()
                    ) {
                        Text("Add", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DeleteConfirmationDialog(
    subscriptionTopic: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(600.dp)
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Delete Subscription?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                )

                Text(
                    text = "Are you sure you want to delete the subscription to \"$subscriptionTopic\"? This will also delete all saved messages for this topic.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.sp
                            )
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
