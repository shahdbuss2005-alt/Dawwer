package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(EcoBlack, ForestDeep, EcoBlack)
                )
            )
    ) {
        // Aesthetic Top Decorative Leaf Pattern Orbs
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(LeafGreen.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(LimePulse.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Icon Callout
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ForestMid)
                    .border(1.5.dp, LeafGreen, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = "دَوِّر Logo",
                    tint = LimePulse,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Title
            Text(
                text = viewModel.translate("app_title"),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = CreamPaper,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isLoginMode) viewModel.translate("auth_subtitle") else viewModel.translate("auth_subtitle_signup"),
                fontSize = 14.sp,
                color = MistWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ForestMid, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLoginMode) viewModel.translate("auth_login_title") else viewModel.translate("auth_signup_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CreamPaper,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(viewModel.translate("auth_full_name"), color = MistWhite.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = LeafGreen) },
                            textStyle = TextStyle(color = CreamPaper),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LeafGreen,
                                unfocusedBorderColor = ForestMid,
                                focusedContainerColor = EcoBlack.copy(alpha = 0.4f),
                                unfocusedContainerColor = EcoBlack.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(viewModel.translate("auth_email"), color = MistWhite.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LeafGreen) },
                        textStyle = TextStyle(color = CreamPaper),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LeafGreen,
                            unfocusedBorderColor = ForestMid,
                            focusedContainerColor = EcoBlack.copy(alpha = 0.4f),
                            unfocusedContainerColor = EcoBlack.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("email_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(viewModel.translate("auth_password"), color = MistWhite.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = LeafGreen) },
                        textStyle = TextStyle(color = CreamPaper),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LeafGreen,
                            unfocusedBorderColor = ForestMid,
                            focusedContainerColor = EcoBlack.copy(alpha = 0.4f),
                            unfocusedContainerColor = EcoBlack.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                        singleLine = true
                    )

                    AnimatedVisibility(visible = showError.isNotEmpty()) {
                        Text(
                            text = showError,
                            color = DangerRust,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isEmpty() || password.isEmpty() || (!isLoginMode && name.isEmpty())) {
                                showError = viewModel.translate("auth_error_fields")
                            } else if (!email.contains("@")) {
                                showError = viewModel.translate("auth_error_email")
                            } else {
                                showError = ""
                                viewModel.login(email, if (isLoginMode) "أحمد محمد" else name)
                                onAuthSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LeafGreen,
                            contentColor = EcoBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_auth_button")
                    ) {
                        Text(
                            text = if (isLoginMode) viewModel.translate("auth_btn_login") else viewModel.translate("auth_btn_signup"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Social login alternative
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.login("google@dawwer.com", "أحمد البيئي")
                                onAuthSuccess()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CreamPaper),
                            border = BorderStroke(1.dp, ForestMid)
                        ) {
                            Text(viewModel.translate("auth_google"), fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.login("facebook@dawwer.com", "أحمد تدوير")
                                onAuthSuccess()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CreamPaper),
                            border = BorderStroke(1.dp, ForestMid)
                        ) {
                            Text(viewModel.translate("auth_facebook"), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Switch Mode Link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoginMode) viewModel.translate("auth_no_account") else viewModel.translate("auth_has_account"),
                    color = MistWhite.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = if (isLoginMode) viewModel.translate("auth_register_now") else viewModel.translate("auth_login_here"),
                    color = LimePulse,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            isLoginMode = !isLoginMode
                            showError = ""
                        }
                        .testTag("switch_auth_mode")
                )
            }
        }
    }
}
