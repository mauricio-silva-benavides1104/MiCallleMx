package com.micalle.mx

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser // Importación necesaria para la función createNewUserDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.io.IOException

class AccountFragment : Fragment() {

    // ✅ Corregido: _binding debe ser de tipo View?
    private var _binding: View? = null
    // private val binding get() = _binding!! // No lo estás usando, déjalo comentado si usas findViewById

    private lateinit var profileImageView: CircleImageView
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var changePhotoButton: Button
    private lateinit var saveChangesButton: Button
    private lateinit var logoutButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    // private lateinit var storage: FirebaseStorage // 🔥 Eliminado

    private var selectedImageUri: Uri? = null

    // Para manejar el resultado de la selección de imagen
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                selectedImageUri = it
                profileImageView.setImageURI(it)
                // Aquí puedes subir la imagen si el usuario lo desea inmediatamente
                // o esperar hasta que presione "Guardar Cambios"
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // ✅ Ahora 'view' es del tipo correcto
        val view = inflater.inflate(R.layout.fragment_account, container, false) // Asegúrate que el nombre del layout sea correcto

        // Inicializar vistas
        profileImageView = view.findViewById(R.id.profileImageView)
        usernameEditText = view.findViewById(R.id.usernameEditText)
        ageEditText = view.findViewById(R.id.ageEditText)
        changePhotoButton = view.findViewById(R.id.changePhotoButton)
        saveChangesButton = view.findViewById(R.id.saveChangesButton)
        logoutButton = view.findViewById(R.id.logoutButton)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        // storage = FirebaseStorage.getInstance() // 🔥 Eliminado

        // Cargar datos del usuario
        loadUserData()

        // Configurar listeners
        changePhotoButton.setOnClickListener { openImagePicker() }
        saveChangesButton.setOnClickListener { saveChanges() }
        logoutButton.setOnClickListener { logout() }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Importante: Limpiar la referencia de binding para evitar fugas de memoria
        _binding = null
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Cargar datos desde Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Nombre no establecido"
                        val age = document.getLong("age")?.toInt() ?: 0
                        // 🔥 Cargar la imagen en formato Base64
                        val photoBase64 = document.getString("photoBase64") // Cambiamos el campo

                        usernameEditText.setText(name)
                        ageEditText.setText(age.toString())

                        // 🔥 Decodificar y mostrar la imagen Base64
                        photoBase64?.let { base64String ->
                            try {
                                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                profileImageView.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                Log.e("AccountFragment", "Error al decodificar la imagen Base64", e)
                                // Mostrar imagen por defecto si falla
                                // profileImageView.setImageResource(R.drawable.ic_default_profile_image)
                            }
                        }
                    } else {
                        Log.d("AccountFragment", "Documento de usuario no encontrado en Firestore. Creando nuevo documento.")
                        // Crear nuevo documento si no existe
                        createNewUserDocument(currentUser)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AccountFragment", "Error al cargar datos del usuario", e)
                    Toast.makeText(context, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Log.w("AccountFragment", "Usuario no autenticado.")
            Toast.makeText(context, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            // Opcional: Redirigir a login
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveChanges() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val newName = usernameEditText.text.toString()
        val newAgeText = ageEditText.text.toString()
        var newAge: Long? = null
        if (newAgeText.isNotEmpty()) {
            try {
                newAge = newAgeText.toLong()
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Edad inválida.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val updates = hashMapOf<String, Any>()
        if (newName.isNotEmpty()) updates["name"] = newName
        if (newAge != null) updates["age"] = newAge

        val batch = db.batch()

        // Actualizar documento del usuario
        val userRef = db.collection("users").document(currentUser.uid)
        // batch.update(userRef, updates) // 🔥 Movemos esta actualización dentro del bloque de imagen

        // 🔥 Guardar imagen como Base64 si se seleccionó una nueva
        if (selectedImageUri != null) {
            try {
                // Convertir URI a Bitmap y luego a Base64
                val bitmap = getBitmapFromUri(selectedImageUri!!) // Reutilizamos la función de ReportFormFragment
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 800, 800, true) // Comprimir
                val byteArray = bitmapToByteArray(scaledBitmap)
                val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

                // Agregar la imagen Base64 a los updates
                updates["photoBase64"] = base64Image // Cambiamos el campo
                batch.update(userRef, updates) // Actualizamos el batch con la imagen y otros campos
            } catch (e: Exception) {
                Log.e("AccountFragment", "Error al convertir imagen a Base64", e)
                Toast.makeText(context, "Error al procesar la imagen: ${e.message}", Toast.LENGTH_LONG).show()
                return // Detener si hay error al procesar la imagen
            }
        } else {
            // Si no hay imagen nueva, solo actualizar otros campos
            batch.update(userRef, updates) // 🔥 Actualizamos solo los campos sin imagen
        }

        // Ejecutar el batch
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Cambios guardados exitosamente!", Toast.LENGTH_SHORT).show()
                selectedImageUri = null // Resetear la URI
            }
            .addOnFailureListener { e ->
                Log.e("AccountFragment", "Error al guardar cambios en Firestore", e)
                Toast.makeText(context, "Error al guardar cambios: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun logout() {
        auth.signOut()
        // Opcional: Navegar a la pantalla de login o a la principal
        // val intent = Intent(context, LoginActivity::class.java) // Ajusta según tu estructura
        // startActivity(intent)
        // requireActivity().finish() // Cierra la actividad principal si es necesario
        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }

    // 🔥 Agregar estas funciones dentro de la clase AccountFragment
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("AccountFragment", "Error al obtener bitmap del URI", e)
            BitmapFactory.decodeResource(resources, R.drawable.ic_default_profile_image) // Imagen por defecto
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream) // Compresión JPEG al 70%
        return stream.toByteArray()
    }

    // 👇 Nueva función para crear un nuevo documento de usuario
    private fun createNewUserDocument(user: FirebaseUser) {
        val userRef = db.collection("users").document(user.uid)

        val profileData = hashMapOf(
            "name" to (user.displayName ?: "Usuario Anónimo"),
            "email" to user.email,
            "age" to 0, // ✅ Corrección: Cambiamos 0L por 0
            "photoBase64" to "", // Inicialmente vacío o puedes usar una imagen Base64 por defecto
            "timestamp" to System.currentTimeMillis()
        )

        userRef.set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("AccountFragment", "Nuevo documento de usuario creado exitosamente.")
                // Recargar los datos después de crear el documento
                loadUserData()
            }
            .addOnFailureListener { e ->
                Log.e("AccountFragment", "Error al crear nuevo documento de usuario", e)
                Toast.makeText(context, "Error al crear perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Opcional: Agregar permiso para leer almacenamiento si es necesario (Android 13 o superior)
    private fun checkAndRequestPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker() // Procede si se otorgó el permiso
            } else {
                Toast.makeText(context, "Permiso denegado para acceder a la galería.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}