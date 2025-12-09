package com.micalle.mx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReportsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReportAdapter
    private val reports = mutableListOf<Report>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        val btnUploadReport = view.findViewById<MaterialButton>(R.id.btnUploadReport)
        val searchBar = view.findViewById<EditText>(R.id.searchBar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()

        // Funcionalidad: Botón de subir reporte
        btnUploadReport.setOnClickListener {
            findNavController().navigate(R.id.action_reportsFragment_to_reportFormFragment)
        }

        // Funcionalidad: Barra de búsqueda (por ahora solo un mensaje)
        searchBar.setOnEditorActionListener { _, _, _ ->
            true // Indica que el evento fue manejado
        }

        loadReports()
    }

    private fun setupRecyclerView() {
        adapter = ReportAdapter(reports, isMyReports = false) { // ✅ Indicamos que no es "Mis reportes"
            loadReports()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadReports() {
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                reports.clear()
                for (document in result) {
                    val report = document.toObject(Report::class.java)
                    report.id = document.id // Guardamos el ID del documento
                    reports.add(report)
                }

                if (reports.isEmpty()) {
                    recyclerView.visibility = View.GONE
                } else {
                    recyclerView.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Manejar error
            }
    }
}