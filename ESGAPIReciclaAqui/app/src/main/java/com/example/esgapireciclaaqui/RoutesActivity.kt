package com.example.esgapireciclaaqui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.beust.klaxon.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.net.URL


class RoutesActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routes)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        //-- Atualiza o objeto do Mapa
        mMap = googleMap

        //-- Prepara um objeto com latitude e longitude
        val LatLongB = LatLngBounds.Builder()

        //-- prepara uma coleção de informações das unidades da FIAP
        //-- A informação irá pular uma linha
        val unidades = arrayOf(
            arrayOf(
                "FIAP Campus Vila Olimpia",
                "Rua Olimpíadas,186 São Paulo - SP CEP: 04551-000"
            ),
            arrayOf(
                "FIAP Campus Paulista",
                "Av. Paulista,1106 São Paulo - SP CEP: 01311-000"
            ),
            arrayOf(
                "FIAP Campus Vila Mariana",
                "Av. Lins de Vasconcelos,1264 São Paulo - SP CEP: 01538-001"
            )
        )

        //--Adiciona a latitude e longitude da FIAP Campus Vila Mariana
        val fiap_campus_vila_mariana = LatLng(-23.5746685, -46.6232043)

        //--Adiciona a latitude e longitude da FIAP Campus Vila Olimpia
        val fiap_campus_vila_olimpia = LatLng(-23.5955843, -46.6851937)

        //--Adiciona a latitude e longitude da FIAP Campus Paulista
        val fiap_campus_paulista = LatLng(-23.5643721, -46.652857)

        //-- Selecionando informações
        val pontoA = fiap_campus_paulista;
        val pontoB = fiap_campus_vila_olimpia;

        val unidadePontoA = unidades[1]
        val unidadePontoB = unidades[0]

        //-- Unidade Ponto A
        mMap!!.addMarker(
            MarkerOptions()
                .position(pontoA)
                .title(unidadePontoA[0])
                .snippet(unidadePontoA[1])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

        //-- Unidade Ponto B
        mMap!!.addMarker(
            MarkerOptions()
                .position(pontoB)
                .title(unidadePontoB[0])
                .snippet(unidadePontoB[1])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        )

        //-- Programando a rota entre o Ponto A e Ponto B
        val url = getURL(pontoA, pontoB)

        //-- Processando as informações visual da cor da linha e da largura da linha
        val options = PolylineOptions()
        options.color(Color.BLUE)
        options.width(7f)

        //-- Sincronizando pedido das informações com o Google Mapa via INTERNET
        async {

            val result = URL(url).readText()

            uiThread {

                val parser: Parser = Parser()
                val stringBuilder: StringBuilder = StringBuilder(result)
                val json: JsonObject = parser.parse(stringBuilder) as JsonObject
                val routes = json.array<JsonObject>("routes")
                if ((routes?.size ?: 0) <= 0) {
                    return@uiThread
                }

                val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject>
                val polypts =
                    points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!) }

                //-- Processando as informações do Ponto A
                options.add(pontoA)
                LatLongB.include(pontoA)
                for (point in polypts) {
                    options.add(point)
                    LatLongB.include(point)
                }

                //-- Processando as informações do Ponto B
                options.add(pontoB)
                LatLongB.include(pontoB)
                val bounds = LatLongB.build()

                //-- Adicionando a rota no mapa
                mMap!!.addPolyline(options)

                //-- Centralizando o Mapa
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

                //--Configura a exibição dos títulos e endereços das unidades FIAP
                //--de maneira personalizada
                mMap!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {

                    override fun getInfoWindow(arg0: Marker): View? {
                        return null
                    }

                    override fun getInfoContents(marker: Marker): View {

                        val info = LinearLayout(applicationContext)
                        info.orientation = LinearLayout.VERTICAL

                        //--Título
                        val title = TextView(applicationContext)
                        title.setTextColor(Color.BLACK)
                        title.gravity = Gravity.LEFT
                        title.setTypeface(null, Typeface.BOLD)
                        title.text = marker.title

                        //--Complemento
                        val snippet = TextView(applicationContext)
                        snippet.setTextColor(Color.GRAY)
                        snippet.text = marker.snippet

                        //--Adiciona o titulo e o complemento na marca
                        info.addView(title)
                        info.addView(snippet)

                        return info
                    }
                })

            }
        }
    }

    //-- Coletando os dados do PontoA e PontoB via URL
    private fun getURL(from: LatLng, to: LatLng): String {

        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        var key: String? = "AIzaSyCfweLoBLWJN94rOAwIxQtiuTvpTAKdtQY"
        key?.let {
            val params = "$origin&$dest&$sensor&key=$it"
            return "https://maps.googleapis.com/maps/api/directions/json?$params"
        }
        throw IllegalArgumentException("TROQUE O VALOR DE KEY")
    }

    //-- Decodificando os pontos
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }

        return poly
    }
    //---------------------------------------------------Fim da rotina
}
