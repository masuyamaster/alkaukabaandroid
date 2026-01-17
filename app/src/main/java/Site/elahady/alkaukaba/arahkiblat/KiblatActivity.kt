package Site.elahady.alkaukaba.arahkiblat

import Site.elahady.alkaukaba.databinding.ActivityKiblatBinding
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class KiblatActivity : AppCompatActivity() {
    lateinit var _binding : ActivityKiblatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityKiblatBinding.inflate(layoutInflater)
        setContentView(_binding.root)
    }
}