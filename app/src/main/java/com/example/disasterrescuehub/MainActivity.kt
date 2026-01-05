package com.example.disasterrescuehub

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.disasterrescuehub.ui.theme.DisasterRescueHubTheme
import kotlinx.coroutines.launch
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DisasterRescueHubTheme {

                val nav = rememberNavController()

                NavHost(navController = nav, startDestination = "home") {

                    composable("home") {
                        HomeScreen(
                            onMedical = { nav.navigate("request_medical") },
                            onRescue = { nav.navigate("request_rescue") },
                            onFood = { nav.navigate("request_food") },
                            onShelter = { nav.navigate("request_shelter") },
                            onVolunteer = { nav.navigate("volunteer_list") }
                        )
                    }

                    composable("request_medical") {
                        AssistanceRequestScreen(
                            "🩺 Medical Assistance",
                            "Provide details — responders will reach faster",
                            "Medical",
                            Color(0xFF5A8CFF),
                            onBack = { nav.popBackStack() },
                            onSubmitted = { id -> nav.navigate("status/$id") }
                        )
                    }

                    composable("request_rescue") {
                        AssistanceRequestScreen(
                            "⚡ Rescue Assistance",
                            "Report danger — emergency responders will assist",
                            "Rescue",
                            Color(0xFFEF4444),
                            onBack = { nav.popBackStack() },
                            onSubmitted = { id -> nav.navigate("status/$id") }
                        )
                    }

                    composable("request_food") {
                        AssistanceRequestScreen(
                            "🍱 Food Assistance",
                            "Request relief food or essential supplies",
                            "Food",
                            Color(0xFF22C55E),
                            onBack = { nav.popBackStack() },
                            onSubmitted = { id -> nav.navigate("status/$id") }
                        )
                    }

                    composable("request_shelter") {
                        AssistanceRequestScreen(
                            "🏠 Shelter Assistance",
                            "Find safe shelter or temporary housing",
                            "Shelter",
                            Color(0xFFF59E0B),
                            onBack = { nav.popBackStack() },
                            onSubmitted = { id -> nav.navigate("status/$id") }
                        )
                    }

                    composable("volunteer_list") {
                        VolunteerRequestListScreen(onBack = { nav.popBackStack() })
                    }

                    composable("status/{id}") { entry ->
                        val id = entry.arguments?.getString("id") ?: ""
                        RequestStatusScreen(id = id, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

//////////////////////////////////////////////////////
// 📡 LIVE HIGH ACCURACY LOCATION
//////////////////////////////////////////////////////

@SuppressLint("MissingPermission")
@Composable
fun rememberLiveLocation(activity: ComponentActivity): State<Pair<Double?, Double?>> {

    val client = remember { LocationServices.getFusedLocationProviderClient(activity) }
    val locState = remember { mutableStateOf<Pair<Double?, Double?>>(null to null) }

    client.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) locState.value = loc.latitude to loc.longitude
    }

    val req = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        2000
    )
        .setMinUpdateDistanceMeters(1f)
        .setWaitForAccurateLocation(true)
        .build()

    DisposableEffect(Unit) {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (loc.accuracy <= 20f) {
                    locState.value = loc.latitude to loc.longitude
                }
            }
        }

        client.requestLocationUpdates(req, callback, activity.mainLooper)

        onDispose {
            client.removeLocationUpdates(callback)
        }
    }

    return locState
}

//////////////////////////////////////////////////////
// 🏠 HOME SCREEN
//////////////////////////////////////////////////////

@Composable
fun HomeScreen(
    onMedical: () -> Unit,
    onRescue: () -> Unit,
    onFood: () -> Unit,
    onShelter: () -> Unit,
    onVolunteer: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF240000), Color(0xFF7A0000))))
            .padding(20.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(Modifier.height(26.dp))

            Text("DISASTER RESCUE HUB", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("Emergency Help • Safety • Rapid Response", color = Color(0xFFE6E6E6), fontSize = 13.sp)

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onMedical,
                modifier = Modifier.size(240.dp).shadow(34.dp, CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(Color(0xFFFF5A5A))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Warning, null, tint = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text("SOS — TAP FOR\nEMERGENCY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Text("Primary emergency button (Auto-location enabled)", color = Color(0xFFEDEDED), fontSize = 12.sp)

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(26.dp))
                    .padding(16.dp)
            ) {
                Text("Choose Your Emergency Type", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    CategoryBox("🩺 Medical", Color(0xFF3B82F6)) { onMedical() }
                    CategoryBox("⚡ Rescue", Color(0xFFEF4444)) { onRescue() }
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    CategoryBox("🍱 Food", Color(0xFF22C55E)) { onFood() }
                    CategoryBox("🏠 Shelter", Color(0xFFF59E0B)) { onShelter() }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .background(Color(0xFF2563EB), RoundedCornerShape(18.dp))
                    .clickable { onVolunteer() },
                contentAlignment = Alignment.Center
            ) {
                Text("🫶 Volunteer Mode — Help Nearby People", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun CategoryBox(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(150.dp).height(70.dp)
            .background(color, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

//////////////////////////////////////////////////////
// 🚑 ASSISTANCE REQUEST  (Description Optional)
//////////////////////////////////////////////////////

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistanceRequestScreen(
    title: String,
    subtitle: String,
    type: String,
    color: Color,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit
) {

    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    val (lat, lng) = rememberLiveLocation(activity).value

    var description by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistance Request", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back", color = Color.White) } },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFF0F3C66),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF0B2640), Color(0xFF0E4A84))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Card(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp), CardDefaults.cardColors(color)) {
                Column(Modifier.padding(18.dp)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color(0xFFE7ECFF), fontSize = 13.sp)
                }
            }

            Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("📌 Describe the situation (Optional)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Example: injured person, trapped, urgent help…") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }

            Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CardDefaults.cardColors(Color(0xFFF4F6FF))) {
                Column(Modifier.padding(18.dp)) {
                    Text("📍 Live Location (Auto-updating)", fontWeight = FontWeight.Bold)

                    if (lat != null && lng != null)
                        Text("📡 $lat , $lng\n(High-accuracy enabled)", color = Color(0xFF2F4B8A))
                    else
                        Text("⏳ Getting accurate GPS… move near open sky", color = Color.Gray)
                }
            }

            statusMessage?.let { Text(it, color = Color.Green) }

            Button(
                onClick = {

                    isSaving = true
                    statusMessage = "✔ Request saved — syncing…"

                    val data = hashMapOf(
                        "type" to type,
                        "description" to description.ifBlank { "(No description provided)" },
                        "lat" to lat,
                        "lng" to lng,
                        "status" to "Pending",
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    description = ""

                    scope.launch {
                        db.collection("help_requests")
                            .add(data)
                            .addOnSuccessListener { doc ->
                                statusMessage = "✔ Synced — opening status screen"
                                isSaving = false
                                onSubmitted(doc.id)
                            }
                            .addOnFailureListener {
                                statusMessage = "⚠ Saved offline — sync later"
                                isSaving = false
                            }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(if (isSaving) Color.Gray else Color(0xFF1D4ED8))
            ) {
                Text(if (isSaving) "Syncing…" else "🚀 Submit Request", color = Color.White)
            }
        }
    }
}

//////////////////////////////////////////////////////
// 👨‍🚒 VOLUNTEER LIST  (Accept → Mark Helped)
//////////////////////////////////////////////////////

data class HelpRequest(
    val id: String = "",
    val type: String = "",
    val description: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val status: String = "Pending"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerRequestListScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val db = FirebaseFirestore.getInstance()

    val (uLat, uLng) = rememberLiveLocation(activity).value
    var requests by remember { mutableStateOf(listOf<HelpRequest>()) }

    LaunchedEffect(Unit) {
        db.collection("help_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                requests = snap?.documents?.map {
                    HelpRequest(
                        it.id,
                        it.getString("type") ?: "",
                        it.getString("description") ?: "",
                        it.getDouble("lat"),
                        it.getDouble("lng"),
                        it.getString("status") ?: "Pending"
                    )
                } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volunteer — Nearby Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back", color = Color.White) } },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFF0F3C66),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Color(0xFFEFF3FF)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            requests.forEach { req ->

                val dist = if (uLat != null && req.lat != null)
                    calcDistance(uLat, uLng, req.lat, req.lng) else null

                Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {

                        Text("🔔 ${req.type}", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(req.description)

                        dist?.let { Text("📍 ${"%.1f".format(it)} km away", color = Color.Gray) }

                        Spacer(Modifier.height(6.dp))

                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {

                            Text(
                                req.status,
                                color = when (req.status) {
                                    "Pending" -> Color.Red
                                    "Accepted" -> Color(0xFFFFA500)
                                    else -> Color(0xFF22C55E)
                                }
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                if (req.status == "Pending") {
                                    Button(onClick = {
                                        db.collection("help_requests")
                                            .document(req.id)
                                            .update("status", "Accepted")
                                    }) { Text("Accept") }
                                }

                                if (req.status == "Accepted") {
                                    Button(onClick = {
                                        db.collection("help_requests")
                                            .document(req.id)
                                            .update("status", "Resolved")
                                    }) { Text("Mark Helped") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

//////////////////////////////////////////////////////
// 🟢 REQUEST STATUS SCREEN (User View)
//////////////////////////////////////////////////////

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestStatusScreen(id: String, onBack: () -> Unit) {

    val db = FirebaseFirestore.getInstance()
    var status by remember { mutableStateOf("Loading…") }

    LaunchedEffect(id) {
        db.collection("help_requests")
            .document(id)
            .addSnapshotListener { snap, _ ->
                status = snap?.getString("status") ?: "Unknown"
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Status", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back", color = Color.White) } },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFF0F3C66),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Color(0xFF0B2640)).padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Your Help Request", color = Color.White, fontSize = 20.sp)

            Spacer(Modifier.height(20.dp))

            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White)) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Text("Current Status", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    Text(
                        when (status) {
                            "Pending" -> "🟡 Waiting for nearby volunteers"
                            "Accepted" -> "🚑 Volunteer is on the way"
                            "Resolved" -> "🟢 Assistance Completed"
                            else -> status
                        },
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

//////////////////////////////////////////////////////
// 🌍 Distance Calculator
//////////////////////////////////////////////////////

fun calcDistance(aLat: Double?, aLng: Double?, bLat: Double?, bLng: Double?): Double {
    if (aLat == null || aLng == null || bLat == null || bLng == null) return 0.0
    val r = 6371
    val dLat = Math.toRadians(bLat - aLat)
    val dLng = Math.toRadians(bLng - aLng)
    val sa = kotlin.math.sin(dLat/2).pow(2.0) +
            kotlin.math.cos(Math.toRadians(aLat)) *
            kotlin.math.cos(Math.toRadians(bLat)) *
            kotlin.math.sin(dLng/2).pow(2.0)
    return 2 * r * kotlin.math.asin(kotlin.math.sqrt(sa))
}