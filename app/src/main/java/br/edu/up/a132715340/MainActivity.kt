package br.edu.up.a132715340

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.up.a132715340.ui.theme.AppA132715340Theme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


// Data class para armazenar os dados
data class Entry(val id: String, val content: String)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val firestore = FirebaseFirestore.getInstance() // Instância do Firestore
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa o Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        logFirebaseEvent()

        setContent {
            AppA132715340Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Gerenciador de Dados", fontSize = 20.sp) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color(0xFF6200EE),
                                titleContentColor = Color.White
                            )
                        )
                    },
                ) { innerPadding ->
                    ScreenContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun logFirebaseEvent() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "main_activity")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Main Activity Started")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScreenContent(modifier: Modifier = Modifier) {
        var userInput by remember { mutableStateOf("") }
        var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
        var editingEntry by remember { mutableStateOf<Entry?>(null) }

        // Carregar dados do Firestore
        LaunchedEffect(Unit) {
            listenerRegistration = firestore.collection("entries")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        Toast.makeText(this@MainActivity, "Erro ao carregar dados: ${exception.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        entries = snapshot.documents.map { document ->
                            Entry(
                                id = document.id,
                                content = document.getString("content") ?: "Sem conteúdo"
                            )
                        }
                    }
                }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (editingEntry != null) "Editando Entrada" else "Nova Entrada",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EE)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de texto
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Digite algo...") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color(0xFFF2F2F2),
                    focusedIndicatorColor = Color(0xFF6200EE),
                    unfocusedIndicatorColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão para salvar ou atualizar
            Button(
                onClick = {
                    if (userInput.isNotEmpty()) {
                        if (editingEntry != null) {
                            updateInFirestore(editingEntry!!, userInput)
                        } else {
                            saveToFirestore(userInput)
                        }
                        userInput = ""
                        editingEntry = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text(
                    text = if (editingEntry != null) "Atualizar" else "Salvar",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de entradas
            if (entries.isNotEmpty()) {
                Text("Entradas Salvas:", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(entries) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.content, fontSize = 16.sp)
                                Row {
                                    // Botão de editar
                                    Button(
                                        onClick = {
                                            userInput = entry.content
                                            editingEntry = entry
                                        },
                                        modifier = Modifier.padding(end = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                                    ) {
                                        Text("Editar", color = Color.White)
                                    }
                                    // Botão de excluir
                                    Button(
                                        onClick = { deleteFromFirestore(entry.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                                    ) {
                                        Text("Excluir", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Nenhuma entrada encontrada.", color = Color.Gray, fontSize = 16.sp)
            }
        }
    }

    private fun saveToFirestore(data: String) {
        val entry = hashMapOf(
            "content" to data,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("entries")
            .add(entry)
            .addOnSuccessListener {
                Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar os dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateInFirestore(entry: Entry, newContent: String) {
        firestore.collection("entries").document(entry.id)
            .update("content", newContent)
            .addOnSuccessListener {
                Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar os dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteFromFirestore(documentId: String) {
        firestore.collection("entries").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Dados excluídos com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScreen() {
    AppA132715340Theme {
        Text("Hello Android!")
    }
}

