package com.prateekshah.funtrivia

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.funtrivia.R
import com.prateekshah.funtrivia.model.QuestionsViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*


class RulesOrAlertDialogFragment: DialogFragment() {

    private val viewModel: QuestionsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        when (viewModel.dialogMode) {
            1 -> {
                val mCoroutineScope = CoroutineScope(Dispatchers.Main)
                builder.setTitle(R.string.warning)
                builder.setMessage(R.string.progress_lost_warning)
                builder.setIcon(R.drawable.warning_icon)
                builder.setPositiveButton(getString(R.string.yes)){ _, _ ->
                    viewModel.timer?.cancel()
                    if (viewModel.mode > 0) {
                        viewModel.updatePlayerDetailsInFS(viewModel.timeTakenList.average(), forfeit = true)
                        //if (count(forfeit==true)+count(CurrentQuestion==questionAmount)==currentNumOfParticipants then delete the room and participants
                        viewModel.setCurrentNumOfParticipants()
                    }
                    if (viewModel.mode < 2) {
                        if (viewModel.fragmentVisible)
                            findNavController().navigate(R.id.action_questionFragment_to_settingsFragment)
                    }
                    else {
                        if (viewModel.fragmentVisible)
                            findNavController().navigate(R.id.action_questionFragment_to_createJoinRoomFragment)
                    }
                }
                builder.setNegativeButton(getString(R.string.no)) { _, _ ->
                    if(mCoroutineScope.isActive) {
                        mCoroutineScope.cancel()
                    }
                    dismiss()
                }
            }
            2 -> {
                val myView = layoutInflater.inflate(R.layout.rules_dialog, null)
                val okBtn = myView.findViewById(R.id.ok_btn) as Button
                builder.setView(myView)
                okBtn.setOnClickListener {
                    dismiss()
                }
            }
            3 -> {
                val mView = layoutInflater.inflate(R.layout.waiting_room, null)
                val myRoomId = mView.findViewById<TextView>(R.id.room_id)
                val startBtn = mView.findViewById<Button>(R.id.start_btn)
                val cancelBtn = mView.findViewById<Button>(R.id.cancel_btn)
                val numOfParticipantsJoined = mView.findViewById<TextView>(R.id.num_of_participants_joined)
                val loadingImg = mView.findViewById<ImageView>(R.id.loading_image)
                val share = mView.findViewById<ImageView>(R.id.share_room_id)
                startBtn.visibility = View.GONE
                myRoomId.text = getString(R.string.room_id, viewModel.roomId.value)
                loadingImg.setImageResource(R.drawable.loading_animation)
                share.setOnClickListener {
                    val intent =  Intent(Intent.ACTION_SEND);
                    intent.type = "text/plain";
                    intent.putExtra(Intent.EXTRA_SUBJECT,"Share Room ID")
                    intent.putExtra(Intent.EXTRA_TEXT, myRoomId.text.toString())
                    startActivity(Intent.createChooser(intent,"Share via"))
                }

                // listen to changes in the query result using addSnapshotListener()
                val participants = viewModel.db.collection("players")
                    .whereEqualTo("RoomId", viewModel.roomId.value.toString())
                    .addSnapshotListener { value, error ->
                        error?.let {
                            Log.e("Waiting room error", "${it.message}")
                            return@addSnapshotListener
                        }

                        value?.let {
                            numOfParticipantsJoined.text = requireActivity().getString(R.string.num_of_participants_joined, value.documents.size, viewModel.participantsCapacity)
                            if (value.documents.size > 1) {
                                startBtn.visibility = View.VISIBLE
                            }
                            else {
                                startBtn.visibility = View.GONE
                            }
                        }
                    }

                builder.setView(mView)
                startBtn.setOnClickListener {
                    participants.remove()
                    dismiss()
                    if (viewModel.mode < 2) {
                        findNavController().navigate(R.id.action_settingsFragment_to_questionFragment)
                    }
                    else {
                        findNavController().navigate(R.id.action_createJoinRoomFragment_to_questionFragment)
                    }
                    viewModel.retain = false
                }
                cancelBtn.setOnClickListener {
                    viewModel.setCurrentNumOfParticipants()
                    participants.remove()
                    dismiss()
                }
            }

            4 -> {
                val lView = layoutInflater.inflate(R.layout.join_room, null)
                val pName = lView.findViewById<TextInputEditText>(R.id.p_name).text
                val roomId = lView.findViewById<TextInputEditText>(R.id.room_id2).text
                val joinBtn = lView.findViewById<Button>(R.id.join_btn)
                val cancelBtn = lView.findViewById<Button>(R.id.cancel_button)
                val mCoroutineScope = CoroutineScope(Dispatchers.Main)

                builder.setView(lView)

                joinBtn.setOnClickListener {
                    if (TextUtils.isEmpty(pName) || TextUtils.isEmpty(roomId)) {
                        Toast.makeText(activity, getString(R.string.enter_all_details_error), Toast.LENGTH_SHORT).show()
                    }
                    else {
                        //check whether a room with the entered roomId exists or not.
                        // if the room exists, then check if a player with the entered name already exists within the room or not.
                        //if not then add a new document to the "players" collection in the firestore
                        mCoroutineScope.launch(Dispatchers.IO) {
                            viewModel.setParticipantsCapacity(roomId.toString()).join()
                            viewModel.db.collection("players")
                                .whereEqualTo("RoomId", roomId.toString())
                                .get()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful && !task.result.isEmpty) {
                                        //Room exists
                                        var chkName = false
                                        val docSnapshots = task.result.documents
                                        for (docSnap in docSnapshots) {
                                            if (pName.toString() == docSnap.get("Name")) {
                                                //Player name already exists in the room
                                                if (docSnap.get("Forfeit") == true)
                                                    Toast.makeText(activity, getString(R.string.already_gave_quiz), Toast.LENGTH_SHORT).show()
                                                else
                                                    Toast.makeText(activity, getString(R.string.player_name_exist), Toast.LENGTH_SHORT).show()
                                                Log.d("Parameters", "plName=${pName.toString()} and docSnap.get('Name')=${docSnap.get("Name")}")
                                                chkName = true
                                            }
                                        }
                                        Log.d("ParticipantsCapacity", "numOfParticipants = ${viewModel.participantsCapacity}")
                                        if (docSnapshots.size >= viewModel.participantsCapacity) {
                                            // Player Capacity Limit reached
                                            Toast.makeText(activity, getString(R.string.participants_capacity_limit), Toast.LENGTH_SHORT).show()
                                        }
                                        else if (!chkName && (docSnapshots.size < viewModel.participantsCapacity)){
                                            Toast.makeText(activity, "Room Joined!!", Toast.LENGTH_SHORT).show()
                                            viewModel.timeLeftInMillis = 20000L
                                            viewModel.playerName = pName.toString()
                                            viewModel.resetValues()
                                            viewModel.setRoomId(roomId.toString())
                                            viewModel.addDocInPlayersCollection() //Add Document to the players collection
                                            viewModel.getResponse(5, "easy") //passed random value as place holder arguments
                                            dismiss()
                                            viewModel.dialogMode = 3
                                            val dialogFragment = RulesOrAlertDialogFragment()
                                            dialogFragment.isCancelable = false
                                            dialogFragment.show(parentFragmentManager, "My Fragment")
                                        }

                                    }
                                    else {
                                        Toast.makeText(activity, getString(R.string.room_not_exist), Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                }

                cancelBtn.setOnClickListener {
                    if(mCoroutineScope.isActive) {
                        mCoroutineScope.cancel()
                    }
                    dismiss()
                }
            }
        }
        return builder.create()
    }
}