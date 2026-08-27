package com.example.connectany.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.connectany.theme.Dimensions

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimensions.Layout.contentMaxWidth)
                .fillMaxHeight()
                .padding(Dimensions.Padding.large),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Common Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Box(modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 50)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connect Any",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (currentPage == 0) {
                // Page 3 of PDF: "Two permissions, explained."
                Text(
                    text = "Two permissions,\nexplained.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(Dimensions.CornerRadius.large)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("B", color = MaterialTheme.colorScheme.onSurfaceVariant) // Placeholder for BT icon
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Nearby devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Connect Any listens for Bluetooth connection events so it can match a saved profile. Nothing is scanned in the background on a loop.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(Dimensions.CornerRadius.large)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("O", color = MaterialTheme.colorScheme.onSurfaceVariant) // Placeholder for Overlay icon
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Floating overlay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "The drop appears above whatever you are doing. In this browser studio it plays over Connect Any; on a phone it uses the display-over-apps permission.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "You can preview every animation without pairing. Bluetooth is only requested to detect device connections.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { currentPage = 1 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimensions.Sizing.buttonMinHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(Dimensions.CornerRadius.medium)
                ) {
                    Text("Get started", color = MaterialTheme.colorScheme.onPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Page 4 of PDF: Preview
                Spacer(modifier = Modifier.weight(1f))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(0.5f)
                        .background(
                            color = Color.Black,
                            shape = RoundedCornerShape(Dimensions.CornerRadius.xlarge)
                        )
                        .padding(4.dp)
                        .clip(RoundedCornerShape(Dimensions.CornerRadius.popup))
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(0.8f)
                            .height(Dimensions.Sizing.buttonMinHeight)
                            .background(
                                color = Color(0xFFF3F1EC),
                                shape = RoundedCornerShape(Dimensions.CornerRadius.medium)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sony WH-1000XM5", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Connection overlays,\non your terms.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "When a device pairs, a liquid drop rises from the bottom of the screen and opens into a card — name, status, battery. You choose the art and the motion.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimensions.Sizing.buttonMinHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(Dimensions.CornerRadius.medium)
                ) {
                    Text("Continue", color = MaterialTheme.colorScheme.onPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
