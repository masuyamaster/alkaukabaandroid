package Site.elahady.alkaukaba.arahkiblat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import Site.elahady.alkaukaba.R

/**
 * A simple [Fragment] subclass.
 * Use the [FalakiyahFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FalakiyahFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_falakiyah, container, false)
    }
}