package com.adolfosalado.duckhuntfirestore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RankingActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var tvTitulo: TextView
    private lateinit var tvTusPatos: TextView
    private lateinit var tvPrimero: TextView
    private lateinit var tvSegundo: TextView
    private lateinit var tvTercero: TextView
    private lateinit var tvCuarto: TextView
    private lateinit var tvQuinto: TextView
    private lateinit var btnSalir: Button
    private lateinit var btnReiniciar: Button
    private lateinit var listaTop: ArrayList<String>
    private lateinit var listaPatos: ArrayList<String>
    private var nick: String? = null
    private var patos: Long = 0
    private var listado: MutableList<Usuario> = mutableListOf()
    val db = Firebase.firestore




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        tvTitulo = findViewById(R.id.tvTitulo)
        tvTusPatos = findViewById(R.id.tvTusPuntos)
        tvPrimero = findViewById(R.id.tvPrimero)
        tvSegundo = findViewById(R.id.tvSegundo)
        tvTercero = findViewById(R.id.tvTercero)
        tvCuarto = findViewById(R.id.tvCuarto)
        tvQuinto = findViewById(R.id.tvQuinto)
        btnSalir = findViewById(R.id.btnSalir)
        btnReiniciar = findViewById(R.id.btnReiniciar)
        val typeface = Typeface.createFromAsset(assets, "pixel.ttf")
        tvTitulo.typeface = typeface
        tvTusPatos.typeface = typeface
        tvPrimero.typeface = typeface
        tvSegundo.typeface = typeface
        tvTercero.typeface = typeface
        tvCuarto.typeface = typeface
        tvQuinto.typeface = typeface
        btnSalir.typeface = typeface
        btnReiniciar.typeface = typeface

        nick = intent.getStringExtra("nick")
        patos = intent.getLongExtra("patos", 0)

        btnReiniciar.setOnClickListener(this)
        btnSalir.setOnClickListener(this)

        var comprobante = true
        lifecycleScope.launch {
            while (comprobante) {
                listado = obtenerTotalUsuarios()
                delay(1000)

                println("CORRIENDO CORRUTINA")
                if (listado.size > 0) {
                    comprobante = false
                }
            }

            listado = ordenarUsuarios() as MutableList<Usuario>


            //listaTop = intent.getStringArrayListExtra("listaTop") as ArrayList<String>
            //listaPatos = intent.getStringArrayListExtra("listaPatos") as ArrayList<String>
            mostrarRanking()
        }




    }

    private fun mostrarRanking() {
        val tusPuntos = "$nick ha cazado $patos patos"

        tvTusPatos.text = tusPuntos
        var texto = "1º - " + listado[0].nombre + " - " + listado[0].puntos + " patos"
        tvPrimero.text = texto
        texto =
            if (listado.size > 1) "2º - " + listado[1].nombre + " - " + listado[1].puntos + " patos" else "2º - jugador2 - 0 patos"
        tvSegundo.text = texto
        texto =
            if (listado.size > 2) "3º - " + listado[2].nombre + " - " + listado[2].puntos + " patos" else "3º - jugador3 - 0 patos"
        tvTercero.text = texto
        texto =
            if (listado.size > 2) "4º - " + listado[3].nombre + " - " + listado[3].puntos + " patos" else "4º - jugador4 - 0 patos"
        tvCuarto.text = texto
        texto =
            if (listado.size > 4) "5º - " + listado[4].nombre + " - " + listado[4].puntos + " patos" else "5º - jugador5 - 0 patos"
        tvQuinto.text = texto
    }

    override fun onClick(v: View) {
        val miIntent = Intent()
        when (v.id) {
            R.id.btnSalir -> {
                miIntent.putExtra("accion", "Salir")
                setResult(RESULT_OK, miIntent)
            }
            R.id.btnReiniciar -> {
                miIntent.putExtra("nick", nick)
                miIntent.putExtra("accion", "Reiniciar")
            }
        }
        setResult(RESULT_OK, miIntent)
        finish()
    }

    private fun obtenerTotalUsuarios(): MutableList<Usuario> {
        var listadoUsuariosPuntuaciones: MutableList<Usuario> = mutableListOf()

        db.collection("puntuaciones")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    var usuario = Usuario(document.id, document.data["puntos"].toString().toLong())
                    listadoUsuariosPuntuaciones.add(usuario)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

        return listadoUsuariosPuntuaciones
    }

    private fun insertarDatos(usuario: Usuario) {
        val db = Firebase.firestore
        val datos = hashMapOf("puntos" to usuario.puntos)
        db.collection("puntuaciones").document(usuario.nombre).set(datos).addOnSuccessListener {
            Log.i("Firebase", "Datos insertados correctamente")
        }.addOnFailureListener { error ->
            Log.e("FirebaseError", error.message.toString())
        }
    }

    private fun ordenarUsuarios(): List<Usuario> {
        return listado.sortedByDescending { it.puntos }.map { Usuario(it.nombre, it.puntos) }
    }
}