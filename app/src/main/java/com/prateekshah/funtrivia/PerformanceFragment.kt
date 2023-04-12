package com.prateekshah.funtrivia

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.funtrivia.databinding.FragmentPerformanceBinding
import com.prateekshah.funtrivia.adapter.LeaderboardAdapter
import com.prateekshah.funtrivia.firestore.Player
import com.prateekshah.funtrivia.model.QuestionsViewModel
import com.google.firebase.firestore.*


/**
 * A simple [Fragment] subclass.
 * Use the [PerformanceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PerformanceFragment : Fragment() {

    private val questionsViewModel: QuestionsViewModel by activityViewModels()
    private var binding: FragmentPerformanceBinding? = null
    private var calcAvgTimePerQuestion: Double? = null
    private var participants: ListenerRegistration? = null
    private var forfeit: ListenerRegistration? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragmentBinding = FragmentPerformanceBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.lifecycleOwner = viewLifecycleOwner

        val name = questionsViewModel.playerName
        val playerScore = questionsViewModel.score.value
        val numCorrectAnswers = questionsViewModel.numCorrectAnswers
        val numIncorrectAnswers = questionsViewModel.numIncorrectAnswers
        val totalQuestions = questionsViewModel.questionAmount
        val playerAccuracy = (numCorrectAnswers.toFloat()/totalQuestions.toFloat())*100.0
        val numUnattemptedQuestions = totalQuestions - numCorrectAnswers - numIncorrectAnswers
        calcAvgTimePerQuestion = questionsViewModel.timeTakenList.average()

        binding!!.apply {
            playerName.text = getString(com.example.funtrivia.R.string.player_name_2, name)
            accuracy.text = getString(com.example.funtrivia.R.string.accuracy, playerAccuracy)
            score.text = getString(com.example.funtrivia.R.string.score_2, playerScore, totalQuestions * 10)
            numCorrect.text = numCorrectAnswers.toString()
            numIncorrect.text = numIncorrectAnswers.toString()
            numUnattempted.text = numUnattemptedQuestions.toString()
            avgTimePerQuestion.text = getString(com.example.funtrivia.R.string.time_2, calcAvgTimePerQuestion)
            waitingText.visibility = View.GONE
            showLeaderboardBtn.visibility = View.GONE
        }

        if (questionsViewModel.mode > 0) {
            questionsViewModel.toShowLeaderboardBtn.observe(this.viewLifecycleOwner) { displayLeaderboardBtn ->
                if (displayLeaderboardBtn == true) {
                    binding!!.waitingText.visibility = View.GONE
                    binding!!.showLeaderboardBtn.visibility = View.VISIBLE
                }
                else {
                    binding!!.waitingText.visibility = View.VISIBLE
                    binding!!.showLeaderboardBtn.visibility = View.GONE
                }
            }

            if (questionsViewModel.isShowLeaderboardBtnClick) {
                participants?.remove()
                forfeit?.remove()
                setVisibility(View.INVISIBLE, View.VISIBLE)
            }
            else
                setVisibility(View.VISIBLE, View.INVISIBLE)

            calcAvgTimePerQuestion?.let { questionsViewModel.updatePlayerDetailsInFS(it) }

            participants = questionsViewModel.db.collection("players")
                .whereEqualTo("RoomId", questionsViewModel.roomId.value.toString())
                .whereEqualTo("CurrentQuestionNum", questionsViewModel.questionAmount+1)
                .addSnapshotListener { value, error ->
                    error?.let {
                        Log.e("Waiting room error", "${it.message}")
                        return@addSnapshotListener
                    }

                    value?.let {
                        questionsViewModel.numParticipantsInPerformFrag++
                        questionsViewModel.setCurrentNumOfParticipants(true)
                        Log.d("To Show Leaderboard", "currentNumParticipants=${questionsViewModel.currentNumOfParticipants}, numParticipantsInPerformFrag=${questionsViewModel.numParticipantsInPerformFrag}, numParticipantsForfeit=${questionsViewModel.numParticipantsForfeit}")
                    }
                }

            forfeit = questionsViewModel.db.collection("players")
                .whereEqualTo("RoomId", questionsViewModel.roomId.value.toString())
                .whereEqualTo("Forfeit", true)
                .addSnapshotListener { value, error ->
                    error?.let {
                        Log.e("Waiting room error", "${it.message}")
                        return@addSnapshotListener
                    }

                    value?.let {
                        questionsViewModel.setCurrentNumOfParticipants(true)
                        Log.d("To Show Leaderboard", "currentNumParticipants=${questionsViewModel.currentNumOfParticipants}, numParticipantsInPerformFrag=${questionsViewModel.numParticipantsInPerformFrag}, numParticipantsForfeit=${questionsViewModel.numParticipantsForfeit}")
                    }
                }
        }
        binding!!.showLeaderboardBtn.setOnClickListener {
            showLeaderboard()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true)
            {
                override fun handleOnBackPressed() {
                    // Leave empty do disable back press or
                    // write your code which you want
                    customBackBehavior()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            callback
        )
    }

    fun customBackBehavior() {
        if (questionsViewModel.mode > 0) {
            participants?.remove()
            forfeit?.remove()
            questionsViewModel.resetToShowLeaderboardBtnAndNumPerformFrag()
            calcAvgTimePerQuestion?.let { questionsViewModel.updatePlayerDetailsInFS(it, true) }
            questionsViewModel.setCurrentNumOfParticipants(fragmentState = 1)
        }
        if (questionsViewModel.mode < 2)
            findNavController().navigate(com.example.funtrivia.R.id.action_performanceFragment_to_settingsFragment)
        else
            findNavController().navigate(com.example.funtrivia.R.id.action_performanceFragment_to_createJoinRoomFragment)
    }

    private fun setVisibility(visibilityMode1: Int, visibilityMode2: Int) {
        binding!!.apply {
            playerNameCard.visibility = visibilityMode1
            scoreCard.visibility = visibilityMode1
            accuracyCard.visibility = visibilityMode1
            performanceStatsTitle.visibility = visibilityMode1
            numCorrectCard.visibility = visibilityMode1
            numIncorrectCard.visibility = visibilityMode1
            numUnattemptedCard.visibility = visibilityMode1
            avgTimePerQuestionCard.visibility = visibilityMode1

            leaderboardRecyclerView.visibility = visibilityMode2
        }
    }

    private fun showLeaderboard() {
        Log.d("Show Leaderboard", "inside showLeaderboard()")
        participants?.remove()
        forfeit?.remove()
        questionsViewModel.isShowLeaderboardBtnClick = true
        binding!!.showLeaderboardBtn.visibility = View.GONE
        setVisibility(View.GONE, View.VISIBLE)
        binding!!.result.text = getString(com.example.funtrivia.R.string.leaderboard)
        val leaderboard = binding!!.leaderboardRecyclerView
        val manager = LinearLayoutManager(requireContext())
        manager.stackFromEnd = true
        leaderboard.layoutManager = manager
        val playerArrayList = arrayListOf<Player>()
        val leaderboardAdapter = LeaderboardAdapter(playerArrayList)
        leaderboard.adapter = leaderboardAdapter

        questionsViewModel.incrementQuestionNumber()
        questionsViewModel.db.collection("players")
            .whereEqualTo("RoomId", questionsViewModel.roomId.value.toString())
            .orderBy("Score", Query.Direction.DESCENDING)
            .orderBy("AvgTimePerQuestion")
            .get(Source.SERVER)
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (data in querySnapshot.documents) {
                        val player: Player? = data.toObject(Player::class.java)
                        if (player != null)
                            playerArrayList.add(player)
                    }
                    calcAvgTimePerQuestion?.let { questionsViewModel.updatePlayerDetailsInFS(it, delete = true) }
                    Log.d("Show Leaderboard", "questionNum incremented and FS updated.")
                    leaderboard.adapter = LeaderboardAdapter(playerArrayList)
                }
            }
    }
}