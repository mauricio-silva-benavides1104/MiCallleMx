package com.micalle.mx

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ReportAdapter(
    private val reports: MutableList<Report>,
    private val isMyReports: Boolean,
    private val onReportDeleted: () -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val photoView: ImageView = itemView.findViewById(R.id.photoView)
        val problemText: TextView = itemView.findViewById(R.id.problemText)
        val directionText: TextView = itemView.findViewById(R.id.directionText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.problemText.text = "Problema: ${report.problem}"
        holder.directionText.text = "Dirección: ${report.direction}"
        holder.dateText.text = "Fecha: ${report.date}"
        holder.descriptionText.text = "Descripción: ${report.description}"
        holder.statusText.text = "Estado: ${report.status}"

        // ✅ Mostrar la imagen desde Base64
        val base64Image = report.photoBase64
        if (base64Image.isNotEmpty()) {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            holder.photoView.setImageBitmap(decodedByte)
        } else {
            holder.photoView.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // ✅ Mostrar u ocultar el botón de eliminar según sea "Mis reportes"
        if (isMyReports) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener {
                android.app.AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Eliminar reporte")
                    .setMessage("¿Estás seguro de que quieres eliminar este reporte?")
                    .setPositiveButton("Sí") { _, _ ->
                        deleteReport(report.id, position, holder.itemView.context)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        } else {
            holder.deleteButton.visibility = View.GONE
        }
    }

    private fun deleteReport(reportId: String, position: Int, context: android.content.Context) {
        val db = FirebaseFirestore.getInstance()
        db.collection("reports").document(reportId)
            .delete()
            .addOnSuccessListener {
                reports.removeAt(position)
                notifyItemRemoved(position)
                onReportDeleted()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al eliminar el reporte", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int = reports.size
}