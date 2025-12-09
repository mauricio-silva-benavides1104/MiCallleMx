package com.micalle.mx

import android.os.Bundle
import android.util.Log // Asegúrate de tener esta importación
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyReportsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: ReportAdapter
    private val reports = mutableListOf<Report>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // 🔍 Añadimos un TAG para identificar fácilmente los logs de este fragmento
    companion object {
        private const val TAG = "MyReportsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_reports, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.emptyText)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadReports()
    }

    private fun setupRecyclerView() {
        adapter = ReportAdapter(reports, isMyReports = true) { // ✅ Indicamos que sí es "Mis reportes"
            // Función que se llama cuando se elimina un reporte
            loadReports()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadReports() {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "auth.currentUser es null. No se pueden cargar 'Mis reportes'.") // Log de advertencia
            return
        }

        // 🔍 Log para ver el userId con el que se está consultando
        Log.d(TAG, "Consultando reportes para userId: $userId")

        db.collection("reports")
            .whereEqualTo("userId", userId) // ✅ Filtra por el usuario logeado
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                reports.clear()
                Log.d(TAG, "Número de documentos encontrados con userId '$userId': ${result.size()}") // Log para ver cuántos se encontraron
                for (document in result) {
                    // 🔍 Log para ver cada documento encontrado
                    Log.d(TAG, "Documento encontrado - ID: ${document.id}, userId: ${document.get("userId")}, timestamp: ${document.get("timestamp")}")
                    val report = document.toObject(Report::class.java)
                    report.id = document.id // Guardamos el ID del documento
                    reports.add(report)
                }

                if (reports.isEmpty()) {
                    Log.d(TAG, "La lista de reportes está vacía después de la consulta.")
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                } else {
                    Log.d(TAG, "Se encontraron ${reports.size} reportes. Actualizando UI.")
                    recyclerView.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // 🔍 Log para ver el error si la consulta falla
                Log.e(TAG, "Error al cargar reportes desde Firestore", exception)
            }
    }
}