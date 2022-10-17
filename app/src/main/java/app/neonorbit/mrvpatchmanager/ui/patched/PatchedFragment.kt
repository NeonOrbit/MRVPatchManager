package app.neonorbit.mrvpatchmanager.ui.patched

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.neonorbit.mrvpatchmanager.databinding.FragmentPatchedBinding

class PatchedFragment : Fragment() {
    private var binding: FragmentPatchedBinding? = null
    private var viewModel: PatchedViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatchedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[PatchedViewModel::class.java]
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel = null
        binding = null
    }
}
