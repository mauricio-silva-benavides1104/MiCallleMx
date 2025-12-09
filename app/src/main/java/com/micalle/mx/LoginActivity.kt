package com.micalle.mx

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore // Importación necesaria
import com.google.firebase.firestore.SetOptions // Importación necesaria

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // Inicializamos Firestore

    // Para manejar el resultado de Google Sign-In
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("LoginActivity", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("LoginActivity", "Google sign in failed", e)
                Toast.makeText(this, "Inicio de sesión fallido: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance() // Inicializamos la instancia de Firestore

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Asegúrate de tener este string en strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        btnGoogleSignIn.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("LoginActivity", "signInWithCredential:success")
                    val user = auth.currentUser
                    // 👇 Llamamos a la función para crear/actualizar el perfil
                    createOrUpdateUserProfile(user)
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Autenticación fallida.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    // 👇 Nueva función para crear o actualizar el perfil del usuario en Firestore
    private fun createOrUpdateUserProfile(user: FirebaseUser?) {
        if (user == null) return

        val userRef = db.collection("users").document(user.uid)

        // Datos iniciales del perfil
        val profileData = hashMapOf(
            "name" to (user.displayName ?: "Usuario Anónimo"), // Usamos paréntesis por seguridad
            "email" to user.email,
            "age" to 0L, // Edad inicial, puedes pedirla al usuario más adelante
            "photoUrl" to user.photoUrl?.toString(), // URL de la foto de Google
            "timestamp" to System.currentTimeMillis()
        )

        // Usamos set con merge para evitar sobrescribir datos existentes
        userRef.set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("LoginActivity", "Perfil de usuario creado/actualizado exitosamente.")
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error al crear/actualizar perfil de usuario", e)
                Toast.makeText(this, "Error al crear perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Usuario autenticado, navegar a la pantalla principal
            val intent = Intent(this, MainActivity::class.java) // Ajusta el nombre según tu actividad principal
            startActivity(intent)
            finish() // Cierra la actividad de login para que no se pueda volver atrás
        } else {
            // Usuario no autenticado, mantener en la pantalla de login
            // Puedes ocultar elementos de UI que requieran autenticación aquí
        }
    }
}