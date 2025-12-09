package com.micalle.mx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    // Coordenadas de Benito Juárez - CORREGIDAS (otra vez)
    private val BENITO_JUAREZ_CENTER = LatLng(19.371212, -99.1614902)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Agregar un marcador en Benito Juárez y mover la cámara
        val marker = MarkerOptions().position(BENITO_JUAREZ_CENTER).title("Benito Juárez")
        mMap.addMarker(marker)
        // Ajusta el zoom si es necesario, 13 es un buen punto de partida
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BENITO_JUAREZ_CENTER, 13f))

        // Opcional: Personalizar el mapa
        // mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE // Cambia el tipo de mapa
    }
}