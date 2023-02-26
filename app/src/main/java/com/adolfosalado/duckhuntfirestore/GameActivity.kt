package com.adolfosalado.duckhuntfirestore

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class GameActivity : AppCompatActivity() {
    private lateinit var tvCounter: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvNick: TextView
    private var nick: String = ""
    private lateinit var ivPato: ImageView
    private var cazados: Long = 0
    private var anchoPantalla = 0
    private var altoPantalla = 0
    private var gameOver = false
    private var aleatorio: Random = Random()
    private var listaUsuarios = mutableListOf<Usuario>()
    private lateinit var intentLaunch: ActivityResultLauncher<Intent>
    private lateinit var intentRanking: Intent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        nick = intent.extras?.getString("nick").toString()
        intentRanking = Intent(this, RankingActivity::class.java)

        intentLaunch =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    val accion = result.data?.extras?.getString("accion")

                    when (accion) {
                        "Salir" -> finish()
                        "Reiniciar" -> {
                            nick = result.data?.extras?.getString("nick")!!
                            cazados = 0
                            tvCounter.text = "0"
                            gameOver = false
                            initCuentaAtras()
                            moverPato()
                        }
                    }
                }
            }
        leerDatos()
        initPantalla()
        inicializarComponentesVisuales()
        eventos()
        moverPato()
        initCuentaAtras()
    }

    private fun inicializarComponentesVisuales() {
        tvCounter = findViewById(R.id.tvCounter)
        tvTimer = findViewById(R.id.tvTimer)
        tvNick = findViewById(R.id.tvNick)
        ivPato = findViewById(R.id.ivPato)

        val typeface = Typeface.createFromAsset(assets, "pixel.ttf")
        tvCounter.typeface = typeface
        tvTimer.typeface = typeface
        tvNick.typeface = typeface

        tvNick.text = nick
    }

    private fun eventos() {
        ivPato.setOnClickListener {
            if (!gameOver) {
                cazados++
                tvCounter.setText(cazados.toString())
                ivPato.setImageResource(R.drawable.duck_clicked)
                Handler().postDelayed({
                    ivPato.setImageResource(R.drawable.duck)
                    moverPato()
                }, 500)
            }
        }

    }

    private fun initPantalla() {
        val dm1 = resources.displayMetrics
        anchoPantalla = dm1.widthPixels - 10
        altoPantalla = dm1.heightPixels - 10
    }

    private fun moverPato() {
        val maximoX = anchoPantalla - ivPato.width * 2
        val maximoY = altoPantalla - ivPato.height * 2
        //Generamos un número aleatorio para la coordenada X y otro para la Y
        val randomX = aleatorio.nextInt(maximoX + 1)
        val randomY = aleatorio.nextInt(maximoY + 1)
        //Utilizamos los números aleatorios para mover el pato
        ivPato.x = randomX.toFloat()
        ivPato.y = randomY.toFloat()
    }

    private fun initCuentaAtras() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundosRestantes = millisUntilFinished / 1000
                val texto = "${segundosRestantes}s"
                tvTimer.text = texto
            }

            override fun onFinish() {
                tvTimer.text = "0s"
                gameOver = true
                //mostrarDialogoGameOver()
                transferirDatos()
                anadirUsuario(Usuario(nick, cazados))
                listaUsuarios = ordenarUsuarios() as MutableList<Usuario>
                Log.i("PRUEBA", nick + " " + cazados)
                //mostrarDialogoGameOver()
                intentRanking.putExtra("nick", nick)
                intentRanking.putExtra("patos", cazados)
                startActivity(intentRanking)
            }
        }.start()
    }

    private fun ordenarUsuarios(): List<Usuario> {
        return listaUsuarios.sortedByDescending { it.puntos }.map { Usuario(it.nombre, it.puntos) }
    }

    private fun anadirUsuario(nuevo: Usuario) {
        var existe = false
        for (usuario in listaUsuarios) {
            if (usuario.nombre.equals(nuevo.nombre)) {
                existe = true
                if (nuevo.puntos > usuario.puntos)
                    usuario.puntos = nuevo.puntos
            }
        }
        if (!existe) {
            listaUsuarios.add(nuevo)
        }
    }

    private fun mostrarDialogoGameOver() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Has cazado $cazados patos").setTitle("Game Over")
        builder.setCancelable(false)
        builder.setPositiveButton(
            R.string.reiniciar
        ) { dialog, which ->
            cazados = 0
            tvCounter.text = "0"
            gameOver = false
            initCuentaAtras()
            moverPato()
        }
        builder.setNegativeButton(
            R.string.salir
        ) { dialog, which ->
            dialog.dismiss()
            finish()
        }
        val dialogo = builder.create()
        dialogo.show()
        transferirDatos()
    }

    private fun transferirDatos() {
        //Insertamos el usuario con sus puntos o lo actualizamos si existe
        var actualizar = false
        var insertar = true
        if (listaUsuarios.isNotEmpty()) {
            for (usuario in listaUsuarios) {
                if (usuario.nombre.equals(nick)) {
                    //El usuario existe, por lo que no lo vamos a insertar
                    insertar = false
                    if (usuario.puntos < cazados) {
                        //La puntuación es mayor, por lo que actualizamos
                        actualizar = true
                    }
                }
            }
        }
        val jugador = Usuario(nick, cazados)
        //El usuario existe y la puntuación es mayor que la que tenía
        if (actualizar) {
            actualizarDatos(jugador)
        }
        //No existe el usuario y se da de alta
        if (insertar) {
            insertarDatos(jugador)
        }
    }

    private fun leerDatos() {
        var nombre: String
        var puntos: Long
        var usuario: Usuario
        val db = Firebase.firestore
        db.collection("puntuaciones").get().addOnSuccessListener { result ->
            for (document in result) {
                nombre = document.id
                puntos = document.data["puntos"] as Long
                usuario = Usuario(nombre, puntos)
                listaUsuarios.add(usuario)
            }
        }.addOnFailureListener { error ->
            Log.e("FirebaseError", error.message.toString())
        }
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

    private fun actualizarDatos(usuario: Usuario) {
        val db = Firebase.firestore
        db.collection("puntuaciones").document(usuario.nombre).update("puntos", usuario.puntos)
            .addOnSuccessListener {
                Log.i("Firebase", "Datos actualizados correctamente")
            }.addOnFailureListener { error ->
                Log.e("FirebaseError", error.message.toString())
            }
    }

}