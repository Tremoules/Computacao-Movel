package com.example.travelbadge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelbadge.ui.theme.TravelBadgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelBadgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TravelBadge(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TravelBadge(modifier: Modifier = Modifier) {

    val backgroundTop = Color(0xFFF7F1EB)
    val backgroundBottom = Color(0xFFEDE3DA)

    val cardColor = Color(0xEADFD7D2)
    val darkColor = Color(0xFF1E1A1A)
    val accentColor = Color(0xFFC73E36)
    val secondaryAccent = Color(0xFFB89B7A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundTop,
                        backgroundBottom
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {

            // HERO IMAGE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(26.dp))
            ) {

                Image(
                    painter = painterResource(id = R.drawable.tokyo),
                    contentDescription = "Tokyo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "TOKYO",
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "東京  ·  JAPAN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                Text(
                    text = "TRAVEL BADGE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(accentColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Where tradition meets tomorrow.",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = darkColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Text(
                text = "Temples, neon streets, incredible food and endless energy.",
                fontSize = 12.sp,
                color = darkColor.copy(alpha = 0.68f),
                modifier = Modifier.padding(start = 4.dp, top = 3.dp, end = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ROW 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoCard(
                    title = "BEST TIME",
                    value = "Mar — May",
                    modifier = Modifier.weight(1f),
                    cardColor = cardColor,
                    darkColor = darkColor,
                    accentColor = accentColor
                )

                InfoCard(
                    title = "IDEAL STAY",
                    value = "5 — 7 days",
                    modifier = Modifier.weight(1f),
                    cardColor = cardColor,
                    darkColor = darkColor,
                    accentColor = accentColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ROW 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoCard(
                    title = "MUST TRY",
                    value = "Ramen",
                    modifier = Modifier.weight(1f),
                    cardColor = cardColor,
                    darkColor = darkColor,
                    accentColor = accentColor
                )

                InfoCard(
                    title = "VIBE",
                    value = "Neon & calm",
                    modifier = Modifier.weight(1f),
                    cardColor = cardColor,
                    darkColor = darkColor,
                    accentColor = accentColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MUST SEE
            SectionTitle(
                title = "MUST SEE",
                accentColor = accentColor,
                darkColor = darkColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(darkColor)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(id = R.drawable.shibuya),
                    contentDescription = "Shibuya Crossing",
                    modifier = Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Shibuya Crossing",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "The heartbeat of modern Tokyo.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.76f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "渋谷",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // HIGHLIGHTS
            SectionTitle(
                title = "HIGHLIGHTS",
                accentColor = accentColor,
                darkColor = darkColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HighlightChip(
                    text = "Temples",
                    modifier = Modifier.weight(1f),
                    chipColor = Color.White.copy(alpha = 0.55f),
                    textColor = darkColor
                )

                HighlightChip(
                    text = "Sushi",
                    modifier = Modifier.weight(1f),
                    chipColor = Color.White.copy(alpha = 0.55f),
                    textColor = darkColor
                )

                HighlightChip(
                    text = "Culture",
                    modifier = Modifier.weight(1f),
                    chipColor = Color.White.copy(alpha = 0.55f),
                    textColor = darkColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Discover a city of contrasts.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = darkColor.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    cardColor: Color,
    darkColor: Color,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = darkColor
        )
    }
}

@Composable
fun SectionTitle(
    title: String,
    accentColor: Color,
    darkColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(18.dp)
                .width(4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(accentColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = darkColor
        )
    }
}

@Composable
fun HighlightChip(
    text: String,
    modifier: Modifier = Modifier,
    chipColor: Color,
    textColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(chipColor)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TravelBadgeTheme {
        TravelBadge()
    }
}