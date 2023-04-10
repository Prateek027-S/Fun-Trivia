package com.prateekshah.funtrivia

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.funtrivia.R
import com.example.funtrivia.databinding.FragmentQuestionBinding
import com.prateekshah.funtrivia.model.QuestionsViewModel
import com.prateekshah.funtrivia.network.DataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Timer
import kotlin.concurrent.schedule


/**
 * Fragment for displaying questions.
 */
class QuestionFragment : Fragment() {

    private val questionsViewModel: QuestionsViewModel by activityViewModels()
    private var binding: FragmentQuestionBinding? = null
    private lateinit var optionButtonA: Button
    private lateinit var optionButtonB: Button
    private lateinit var optionButtonC: Button
    private lateinit var optionButtonD: Button
    private lateinit var currentResponse: DataResponse
    private lateinit var buttonsList: List<Button>
    var timer: CountDownTimer? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragmentBinding = FragmentQuestionBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            fragment = this@QuestionFragment
            viewModel = questionsViewModel
        }
        optionButtonA = binding!!.optionA
        optionButtonB = binding!!.optionB
        optionButtonC = binding!!.optionC
        optionButtonD = binding!!.optionD

        buttonsList = listOf(optionButtonA, optionButtonB, optionButtonC, optionButtonD)
        questionsViewModel.isShowLeaderboardBtnClick = false

        makeButtonsInvisible()
        questionsViewModel.response.observe(this.viewLifecycleOwner) { newResponse ->
            if (newResponse != null) {
                currentResponse = newResponse
            }
            else{
                makeButtonsInvisible()
            }
            setQuestion(1)
        }
        questionsViewModel.questionNumber.observe(this.viewLifecycleOwner) { newQuestionNumber ->
            setQuestion(newQuestionNumber)
        }
        questionsViewModel.score.observe(this.viewLifecycleOwner) { newScore ->
            updateScoreText(newScore)
        }
        updateTimerText()
    }

    override fun onResume() {
        super.onResume()
        questionsViewModel.fragmentVisible = true
    }

    override fun onStop() {
        super.onStop()
        questionsViewModel.fragmentVisible = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true)
            {
                override fun handleOnBackPressed() {
                    // Leave empty do disable back press or
                    // write your code which you want
                    createWarningDialog()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            callback
        )
    }

    fun createWarningDialog() {
        questionsViewModel.setValues(1, timer)
        val dialogFragment = RulesOrAlertDialogFragment()
        dialogFragment.isCancelable = false
        dialogFragment.show(parentFragmentManager, "My Fragment")
    }

    private fun updateScoreText(newScore: Int) {
        binding!!.score.text = getString(R.string.score, newScore)
    }

    private fun setQuestion(currentQuestion: Int) {
        val optionsList = mutableListOf("", "", "", "")
        val correctAnswerIndex: Int
        if (questionsViewModel.correctAnswerIndex < 0) {
            correctAnswerIndex = (0..3).random()
            questionsViewModel.setCorrectAnswerNumber(correctAnswerIndex)
        }
        else {
            correctAnswerIndex = questionsViewModel.correctAnswerIndex
        }

        try {
            var question = currentResponse.results[currentQuestion - 1].question
            question = questionsViewModel.htmlDecode(question)

            binding!!.question.text = getString(R.string.question, currentQuestion, question)
            optionsList[correctAnswerIndex] = currentResponse.results[currentQuestion - 1].correctAnswer
            var j = 0
            for (i in (0..3).minus(correctAnswerIndex)){
                optionsList[i] = currentResponse.results[currentQuestion - 1].incorrectAnswer[j]
                j++
            }
            for (k in (0..3)) {
                optionsList[k] = questionsViewModel.htmlDecode(optionsList[k])
            }

            for (button in buttonsList){
                button.visibility = View.VISIBLE
                button.setBackgroundResource(R.drawable.rectangle)
            }
            binding!!.skipBtn.visibility = View.VISIBLE
            optionButtonA.text = getString(R.string.option_a, optionsList[0])
            optionButtonB.text = getString(R.string.option_b, optionsList[1])
            optionButtonC.text = getString(R.string.option_c, optionsList[2])
            optionButtonD.text = getString(R.string.option_d, optionsList[3])

            val correctAnswerText = questionsViewModel.htmlDecode(currentResponse.results[currentQuestion - 1].correctAnswer)
            var correctAnswerButton: Button = buttonsList[0]
            for (btn in buttonsList) {
                if (btn.text.subSequence(3, btn.text.length) == correctAnswerText) {
                    correctAnswerButton = btn
                }
            }
            buttonsList[0].setOnClickListener { onOptionButtonClick(buttonsList[0], correctAnswerButton) }
            buttonsList[1].setOnClickListener { onOptionButtonClick(buttonsList[1], correctAnswerButton) }
            buttonsList[2].setOnClickListener { onOptionButtonClick(buttonsList[2], correctAnswerButton) }
            buttonsList[3].setOnClickListener { onOptionButtonClick(buttonsList[3], correctAnswerButton) }

            startTimer()

        } catch (e: Exception){
            Log.d("Exception msg", "$e")
            questionsViewModel.setLoadStatus()
        }
    }

    private fun checkIfFragmentAttached(operation: Context.() -> Unit) {
        if (isAdded && context != null) {
            operation(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(questionsViewModel.timeLeftInMillis, 1000L) {

            // Callback function, fired on regular interval
            override fun onTick(millisUntilFinished: Long) {
                questionsViewModel.timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            // Callback function, fired
            // when the time is up
            override fun onFinish() {
                timer?.cancel()
                questionsViewModel.timeLeftInMillis = 20000L
                questionsViewModel.timeTakenList.add(20F)
                atLastQuestion()
            }
        }
        timer?.start()
    }

    private fun updateTimerText(){
        checkIfFragmentAttached {
            binding!!.time.text = getString(R.string.time, questionsViewModel.timeLeftInMillis/1000)
        }
    }

    private fun atLastQuestion() {
        questionsViewModel.incrementQuestionNumber()
        updateValuesInFS()
        if (questionsViewModel.questionNumber.value == questionsViewModel.questionAmount) {
            activity?.let {
                findNavController().navigate(R.id.action_questionFragment_to_performanceFragment)
            }
        }
        else {
            updateTimerText()
        }
    }

    private fun updateValuesInFS() {
        if (questionsViewModel.mode > 0)
            questionsViewModel.updatePlayerDetailsInFS(questionsViewModel.timeTakenList.average())
    }

    fun onSkipButtonClick() {
        timer?.cancel()
        questionsViewModel.timeTakenList.add(20 - (questionsViewModel.timeLeftInMillis.toFloat()/1000))
        questionsViewModel.timeLeftInMillis = 20000L
        atLastQuestion()
    }

    private fun onOptionButtonClick(button: Button, correctAnswerButton: Button) {
        timer?.cancel()
        questionsViewModel.timeTakenList.add(20 - (questionsViewModel.timeLeftInMillis.toFloat()/1000))
        Log.d("check", "buttonSubString=${button.text} \nCorrectAnswerText = ${correctAnswerButton.text}")
        button.setBackgroundResource(0)
        if (button == correctAnswerButton) {
            questionsViewModel.numCorrectAnswers++
            questionsViewModel.updatePositiveScore()
        }
        else{
            button.setBackgroundResource(R.drawable.incorrect_option_button)
            questionsViewModel.negativeMarking()
            questionsViewModel.numIncorrectAnswers++
        }

        correctAnswerButton.setBackgroundResource(R.drawable.correct_option_button)
        questionsViewModel.timeLeftInMillis = 20000L
        updateTimerText()
        Timer().schedule(1000) {
            questionsViewModel.incrementQuestionNumber()
            updateValuesInFS()
        }
        if (questionsViewModel.questionNumber.value == questionsViewModel.questionAmount) {
            Timer().schedule(1000) {
                runBlocking {
                    withContext(Dispatchers.Main) {
                        activity?.let {
                            findNavController().navigate(R.id.action_questionFragment_to_performanceFragment)
                        }
                    }
                }
            }
        }
    }

    private fun makeButtonsInvisible() {
        binding!!.apply {
            question.text = ""
            optionA.visibility = View.GONE
            optionB.visibility = View.GONE
            optionC.visibility = View.GONE
            optionD.visibility = View.GONE
            skipBtn.visibility = View.GONE
        }
    }
}