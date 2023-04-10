package com.prateekshah.funtrivia

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.funtrivia.R
import com.example.funtrivia.databinding.FragmentSettingsBinding
import com.prateekshah.funtrivia.model.QuestionsViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {

    private val viewModel: QuestionsViewModel by activityViewModels()
    private var binding: FragmentSettingsBinding? = null
    var maxNumQuestions = 50

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragmentBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            settingsFragment = this@SettingsFragment
        }
        if (viewModel.mode == 1) {
            maxNumQuestions = 30
            binding!!.apply {
                participantsIcon.visibility = View.VISIBLE
                noOfParticipantsTextLayout.visibility = View.VISIBLE
                startBtn.text = getString(R.string.create_room)
            }
        }
        else {
            binding!!.apply {
                participantsIcon.visibility = View.GONE
                noOfParticipantsTextLayout.visibility = View.GONE
                startBtn.text = getString(R.string.start_quiz)
            }
        }

        binding!!.rulesFab.setOnClickListener {
            viewModel.dialogMode = 2
            val dialogFragment = RulesOrAlertDialogFragment()
            dialogFragment.isCancelable = false
            dialogFragment.show(parentFragmentManager, "My Fragment")
        }
    }

    private fun checkInputValues(numOfQuestions: String, playerName: String, checkedRadioButtonId: Int, numOfParticipants: Int = 2) {
        if (numOfQuestions.toInt() > maxNumQuestions)
            Toast.makeText(activity, getString(R.string.more_than_limit_error, maxNumQuestions), Toast.LENGTH_SHORT).show()
        if (numOfQuestions.toInt() < 5)
            Toast.makeText(activity, getString(R.string.less_than_limit_error), Toast.LENGTH_SHORT).show()
        if (playerName.length > 15)
            Toast.makeText(activity, getString(R.string.string_length_error), Toast.LENGTH_SHORT).show()
        if (numOfParticipants < 2)
            Toast.makeText(activity, getString(R.string.less_than_min_participants), Toast.LENGTH_SHORT).show()
        if (numOfParticipants > 10)
            Toast.makeText(activity, getString(R.string.more_than_max_participants), Toast.LENGTH_SHORT).show()
        if ((numOfQuestions.toInt() in (5..maxNumQuestions)) && (playerName.length <= 15) && (numOfParticipants in (2..10))) {
            val selectedRadioButton = binding!!.root.findViewById<RadioButton>(checkedRadioButtonId)
            val selectedDifficultyText = selectedRadioButton.text.toString().lowercase()
            val numQuestions = binding!!.noOfQuestions.text.toString()

            viewModel.timeLeftInMillis = 20000L
            viewModel.resetValues()
            viewModel.getResponse(numQuestions.toInt(), selectedDifficultyText)
            viewModel.playerName = playerName
            if (viewModel.mode == 0) {
                findNavController().navigate(R.id.action_settingsFragment_to_questionFragment)
            }
            else {
                viewModel.participantsCapacity = numOfParticipants
                viewModel.setRoomId()

                val dialogFragment = RulesOrAlertDialogFragment()
                dialogFragment.isCancelable = false
                dialogFragment.show(parentFragmentManager, "My Fragment")
            }
        }
    }

    fun goToQuestionFragment(){
        val playerName = binding!!.playerName.text.toString()
        val numOfQuestions = binding!!.noOfQuestions.text.toString()
        val checkedRadioButtonId = binding!!.difficultyOptions.checkedRadioButtonId
        if (viewModel.mode == 0) {
            if (TextUtils.isEmpty(playerName) || TextUtils.isEmpty(numOfQuestions) || checkedRadioButtonId == -1)
                Toast.makeText(activity, getString(R.string.enter_all_details_error), Toast.LENGTH_SHORT).show()
            else
                checkInputValues(numOfQuestions, playerName, checkedRadioButtonId)
        }
        else {
            val numOfParticipants = binding!!.noOfParticipants.text.toString()
            if (TextUtils.isEmpty(playerName) || TextUtils.isEmpty(numOfQuestions) || checkedRadioButtonId == -1 || TextUtils.isEmpty(numOfParticipants))
                Toast.makeText(activity, getString(R.string.enter_all_details_error), Toast.LENGTH_SHORT).show()
            else {
                viewModel.dialogMode = 3
                checkInputValues(numOfQuestions, playerName, checkedRadioButtonId, numOfParticipants.toInt())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.retain = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.retain = false
    }
}