package com.prateekshah.funtrivia

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.funtrivia.R
import com.example.funtrivia.databinding.FragmentCreateJoinRoomBinding
import com.prateekshah.funtrivia.model.QuestionsViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [CreateJoinRoomFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateJoinRoomFragment : Fragment() {

    private var binding:FragmentCreateJoinRoomBinding? = null
    private val viewModel: QuestionsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragmentBinding = FragmentCreateJoinRoomBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            fragment = this@CreateJoinRoomFragment
        }

        binding!!.rulesFab.setOnClickListener {
            viewModel.dialogMode = 2
            val dialogFragment = RulesOrAlertDialogFragment()
            dialogFragment.isCancelable = false
            dialogFragment.show(parentFragmentManager, "My Fragment")
        }
    }

    fun onCreateRoomClick() {
        viewModel.mode = 1
        findNavController().navigate(R.id.action_createJoinRoomFragment_to_chooseCategoryFragment)
    }

    fun onJoinClick() {
        viewModel.mode = 2
        viewModel.dialogMode = 4
        val dialogFragment = RulesOrAlertDialogFragment()
        dialogFragment.isCancelable = false
        dialogFragment.show(parentFragmentManager, "My Fragment")
    }
}