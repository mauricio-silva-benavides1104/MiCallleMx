package com.micalle.mx

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.micalle.mx.databinding.FragmentReportFormBinding
import java.io.ByteArrayOutputStream

class ReportFormFragment : Fragment() {

    private var _binding: FragmentReportFormBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Botón para tomar foto o elegir de la galería
        binding.photoView.setOnClickListener {
            showImagePicker()
        }

        // Botón para enviar el reporte
        binding.btnSubmit.setOnClickListener {
            submitReport()
        }
    }

    private fun showImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK) {
            val imageUri = data?.data
            imageUri?.let {
                selectedImageUri = it
                binding.photoView.setImageURI(it)
            }
        }
    }

    private fun submitReport() {
        val problem = binding.problemInput.text.toString()
        val direction = binding.directionInput.text.toString()
        val suggestion = binding.suggestionInput.text.toString()
        val date = binding.dateInput.text.toString()
        val description = binding.descriptionInput.text.toString()

        if (problem.isEmpty() || direction.isEmpty() || suggestion.isEmpty() || date.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(context, "Por favor, selecciona una imagen", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔍 Verificar que el usuario esté autenticado antes de continuar
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Usuario no autenticado. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            Log.e("ReportForm", "Error: auth.currentUser es null al intentar enviar el reporte.")
            // Opcional: Redirigir a la pantalla de login
            // val intent = Intent(context, LoginActivity::class.java) // Ajusta el nombre de tu Activity de login
            // startActivity(intent)
            // requireActivity().finish() // Cierra la actividad actual si es necesario
            return // Detener la ejecución de submitReport
        }

        // ✅ Convertir la imagen a Base64
        val bitmap = getBitmapFromUri(selectedImageUri!!)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 800, 800, true) // Comprimir a 800x800 px
        val byteArray = bitmapToByteArray(scaledBitmap)
        val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

        // Crear un mapa con los datos del reporte
        val report = hashMapOf(
            "userId" to currentUser.uid, // ✅ Se usa el uid del usuario autenticado
            "problem" to problem,
            "direction" to direction,
            "suggestion" to suggestion,
            "date" to date,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pendiente",
            "photoBase64" to base64Image  // ✅ Guardamos la imagen como texto
        )

        Log.d("ReportForm", "Enviando reporte con userId: ${currentUser.uid}") // Log para depuración

        // Guardar en Firestore
        db.collection("reports").add(report)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(context, "¡Reporte enviado correctamente!", Toast.LENGTH_SHORT).show()

                // Limpiar campos
                binding.problemInput.text.clear()
                binding.directionInput.text.clear()
                binding.suggestionInput.text.clear()
                binding.dateInput.text.clear()
                binding.descriptionInput.text.clear()
                binding.photoView.setImageResource(R.drawable.ic_launcher_foreground)
                selectedImageUri = null

                // Navegar de vuelta a ReportsFragment
                findNavController().navigate(R.id.action_reportFormFragment_to_reportsFragment)
            }
            .addOnFailureListener { e ->
                Log.e("ReportForm", "Error al enviar el reporte a Firestore", e)
                Toast.makeText(context, "Error al enviar el reporte: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("ReportForm", "Error al obtener bitmap del URI", e)
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}