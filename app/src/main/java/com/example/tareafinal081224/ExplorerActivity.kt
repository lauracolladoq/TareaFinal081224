package com.example.tareafinal081224

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tareafinal081224.adapters.SerieAdapter
import com.example.tareafinal081224.databinding.ActivityExplorerBinding
import com.example.tareafinal081224.models.Genre
import com.example.tareafinal081224.models.ListadoSeries
import com.example.tareafinal081224.models.Serie
import com.example.tareafinal081224.providers.ObjectClientApi.genresClient
import com.example.tareafinal081224.providers.ObjectClientApi.seriesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class ExplorerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExplorerBinding
    val listaSeries = mutableListOf<Serie>()
    val listaGenres = mutableListOf<Genre>()

    // Convertir en función lambda para pasar el objeto Serie
    val adapterSerie = SerieAdapter(listaSeries, { serie -> showDetail(serie) })

    var api = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setRecycler()
    }

    // Inicializa el RecyclerView
    private fun setRecycler() {
        val layoutManager = GridLayoutManager(this, 2)
        binding.rvSeries.layoutManager = layoutManager
        binding.rvSeries.adapter = adapterSerie
        api = getString(R.string.api_themoviedb)

        // Establecer el botón de populares como seleccionado por defecto
        binding.btnPopular.isChecked = true

        setListeners()
        getSeries()
        getGenders()
    }

    private fun setListeners() {
        binding.btnTop.setOnClickListener {
            getSeries()
        }
        binding.btnPopular.setOnClickListener {
            getSeries()
        }
        binding.btnAiring.setOnClickListener {
            getSeries()
        }

        binding.checkBox.setOnClickListener {
            getSeries()
        }
    }

    private fun getGenders() {
        lifecycleScope.launch(Dispatchers.IO) {
            val datos = genresClient.getGenres(api)
            Log.d("API_GENRES", "Response: ${datos.body()}")
            val generos = datos.body()?.listadoGenre ?: emptyList()

            // Limpiar la lista de géneros y añadir los nuevos, no hace falta Adapter ya que no se muestra
            listaGenres.clear()
            listaGenres.addAll(generos)

            // Modificar la pantalla en el main y no en el IO
            withContext(Dispatchers.Main) {
                if (!datos.isSuccessful) {
                    Toast.makeText(
                        this@ExplorerActivity,
                        "Error al cargar los géneros",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Obtener las series según el botón seleccionado en la pantalla (por defecto las populares)
    private fun getSeries() {
        // 3 Dispatchers, Main, Default, IO
        lifecycleScope.launch(Dispatchers.IO) {
            // Devuelve un objeto Response con la lista de series según el botón seleccionado
            var datos: Response<ListadoSeries>

            // Modifica también el titulo de la pantalla en el hilo principal (withContext)
            when {
                binding.btnPopular.isChecked -> {
                    datos = seriesClient.getSeriesPopulares(api)
                    withContext(Dispatchers.Main) {
                        binding.tvTitleExplorer.text = getString(R.string.btn_popular)
                    }
                }

                binding.btnTop.isChecked -> {
                    datos = seriesClient.getSeriesTopRated(api)
                    withContext(Dispatchers.Main) {
                        binding.tvTitleExplorer.text = getString(R.string.btn_top)
                    }
                }

                binding.btnAiring.isChecked -> {
                    datos = seriesClient.getSeriesAiringToday(api)
                    withContext(Dispatchers.Main) {
                        binding.tvTitleExplorer.text = getString(R.string.btn_airing)
                    }
                }

                else -> {
                    datos = seriesClient.getSeriesPopulares(api)
                }
            }

            // QUEDA POR TESTEAR EN LAS TRES CONSULTAS TODOS LOS ADULTS SON FALSE
            // ESPERAR Y BUSCAR UNO TRUE
            // Datos filtrados por adulto
            if (binding.checkBox.isChecked) {
                datos.body()?.listadoSeries?.removeAll { it.adult }
            }

            val listaSeries = datos.body()?.listadoSeries ?: emptyList<Serie>().toMutableList()

            // Modificar la pantalla en el hilo principal
            withContext(Dispatchers.Main) {
                adapterSerie.lista = listaSeries
                adapterSerie.notifyDataSetChanged()

                if (!datos.isSuccessful) {
                    Toast.makeText(
                        this@ExplorerActivity,
                        "Error al cargar las series",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // NAVIGATION MENU -----------------------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.navigation_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_exit -> {
                // Finalizar todas las actividades de la app
                finishAffinity()
            }

            R.id.item_profile -> {
                cargarActivity(MainActivity::class.java)
            }

            R.id.item_explorer -> {
                cargarActivity(ExplorerActivity::class.java)
            }

            R.id.item_favorites -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Paso como parámetro la clase a la que quiero ir
    private fun cargarActivity(java: Class<*>) {
        val intent = Intent(this, java)
        startActivity(intent)
    }

    // DETAILED VIEW -------------------------------------------------------------------------------
    fun showDetail(serie: Serie) {
        // Obtiene los nombres de los géneros de la serie seleccionada por su id
        val genreNames = serie.genres.mapNotNull { id ->
            listaGenres.find { it.id == id }?.name
        }
        val i = Intent(this, DetailActivity::class.java).apply {
            putExtra("serie", serie)
            // Pasar la lista de géneros a la siguiente actividad en forma de ArrayList
            putStringArrayListExtra("genreNames", ArrayList(genreNames))
        }
        startActivity(i)
    }
}