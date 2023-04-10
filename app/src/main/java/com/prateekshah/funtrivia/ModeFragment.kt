package com.prateekshah.funtrivia

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.funtrivia.R
import com.example.funtrivia.databinding.FragmentModeBinding
import com.prateekshah.funtrivia.model.QuestionsViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [ModeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ModeFragment : Fragment() {

    private val viewModel: QuestionsViewModel by activityViewModels()
    private var binding: FragmentModeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragmentBinding = FragmentModeBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            modeFragment = this@ModeFragment
        }

        binding!!.rulesFab.setOnClickListener {
            viewModel.dialogMode = 2
            val dialogFragment = RulesOrAlertDialogFragment()
            dialogFragment.isCancelable = false
            dialogFragment.show(parentFragmentManager, "My Fragment")
        }
    }

    fun goToChooseCategoryFragmentScreen() {
        viewModel.mode = 0
        findNavController().navigate(R.id.action_modeFragment_to_chooseCategoryFragment)
    }

    fun goToCreateJoinRoomScreen() {
        viewModel.mode = 1
        findNavController().navigate(R.id.action_modeFragment_to_createJoinRoomFragment)
    }
}