package com.prateekshah.funtrivia.model

import android.os.Build
import android.os.CountDownTimer
import android.text.Html
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prateekshah.funtrivia.network.DataResponse
import com.prateekshah.funtrivia.network.QuizApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class QuizApiStatus{LOADING, ERROR, DONE}

class QuestionsViewModel: ViewModel() {
    private val _status = MutableLiveData<QuizApiStatus>()
    val status: LiveData<QuizApiStatus> = _status

    private val _response = MutableLiveData<DataResponse?>()
    val response: LiveData<DataResponse?> = _response

    private val _questionNumber = MutableLiveData(1) // current question number
    val questionNumber: LiveData<Int> =  _questionNumber

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _roomId = MutableLiveData<String>()
    val roomId: LiveData<String> = _roomId

    private val _toShowLeaderboardBtn = MutableLiveData(false)
    val toShowLeaderboardBtn: LiveData<Boolean> = _toShowLeaderboardBtn

    val db: FirebaseFirestore = Firebase.firestore

    var questionAmount = 0 // total number of questions
    var correctAnswerIndex = -1
    var timeLeftInMillis = 20000L
    var numCorrectAnswers = 0
    var numIncorrectAnswers = 0
    val timeTakenList = mutableListOf<Float>()
    var category: Int = 0
    var dialogMode: Int = 1
    var timer: CountDownTimer? = null
    var fragmentVisible: Boolean = false
    var mode: Int = 0
    var retain: Boolean = false
    var participantsCapacity = 2 //Participants Capacity of the room
    var currentNumOfParticipants = 0
    var playerName: String = ""
    var isShowLeaderboardBtnClick = false
    var numParticipantsForfeit = 0
    var numParticipantsFinished = 0
    var numParticipantsInPerformFrag = 0


    fun setRoomId() {
        if (!retain) {
            val str = ('0'..'9') + ('a'..'z') + ('A'..'Z')
            _roomId.value = (1..6).map{ str.random() }.joinToString("")
        }
    }

    fun setRoomId(roomId: String) {
        _roomId.value = roomId
    }

    fun setValues(dialogModeValue: Int, timerArg: CountDownTimer?) {
        dialogMode = dialogModeValue
        timer = timerArg
    }

    fun resetToShowLeaderboardBtnAndNumPerformFrag() {
        numParticipantsInPerformFrag = 0
        _toShowLeaderboardBtn.value = false
    }

    fun incrementQuestionNumber() {
        if (_questionNumber.value!! <= (questionAmount+2)) {
            _questionNumber.postValue(_questionNumber.value!!.plus(1))
            Log.d("Increment Question Num", "Incrementation successful")
            correctAnswerIndex = -1
        }
    }

    fun updatePlayerDetailsInFS(avgTimePerQuestion: Double = 0.0, forfeit: Boolean = false, delete: Boolean = false) {
        Log.d("updatePlayerDetailsInFS", "_questionNumber.value=${_questionNumber.value}")
        val playerDetails = hashMapOf(
            "CurrentQuestionNum" to _questionNumber.value,
            "Score" to _score.value,
            "AvgTimePerQuestion" to avgTimePerQuestion,
            "Forfeit" to forfeit
        )
        db.collection("players")
            .whereEqualTo("RoomId", _roomId.value)
            .whereEqualTo("Name", playerName)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isEmpty) {
                    val docSnapshot = task.result.documents[0]
                    val documentId = docSnapshot.id
                    db.collection("players")
                        .document(documentId)
                        .update(playerDetails as Map<String, Any>)
                        .addOnSuccessListener {
                            Log.d("ScoreQuestionNumUpdate", "Score and Question number updated successfully")
                            if (delete)
                                setCurrentNumOfParticipants()
                        }
                        .addOnFailureListener {
                            Log.d("ScoreQuestionNumUpdate", "Score and Question number could not be updated")
                        }
                }
            }
    }

    fun negativeMarking() {
        _score.value = _score.value!!.minus(2)
    }

    fun  updatePositiveScore() {
        if (timeLeftInMillis >= 15000L) {
            _score.postValue(_score.value!!.plus(10))
        }
        else if (timeLeftInMillis >= 10000L) {
            _score.postValue(_score.value!!.plus(8))
        }
        else if (timeLeftInMillis >= 5000L) {
            _score.postValue(_score.value!!.plus(5))
        }
        else if (timeLeftInMillis >= 0L) {
            _score.postValue(_score.value!!.plus(3))
        }
        else
            _score.postValue(_score.value!!.plus(0))

    }

    fun setCorrectAnswerNumber(correctIndex: Int) {
        correctAnswerIndex = correctIndex
    }

    fun setLoadStatus() {
        if (_status.value != QuizApiStatus.ERROR)
            _status.value = QuizApiStatus.LOADING
    }

    fun resetValues() {
        _questionNumber.value = 1
        _score.value = 0
        _response.value = null
        numCorrectAnswers = 0
        numIncorrectAnswers = 0
        timeTakenList.clear()
    }

    fun htmlDecode(text: String): String {
        val decodedText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text , Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            Html.fromHtml(text).toString()
        }
        return decodedText
    }

    fun getResponse(numQuestions: Int, difficulty: String) {
        // mode represents whether the user is playing single player(0) or multiplayer(1)
        // In case of multiplayer, after retrieving the questions from response, they must be uploaded on the firestore
        // create an intermediate variable whose value is the retrofitService response, then according to mode assign its value to _response
        viewModelScope.launch(Dispatchers.IO) {
            _status.postValue(QuizApiStatus.LOADING)
            try {
                if (mode < 2) {
                    val intermediateResponse: MutableList<DataResponse.Question> = if (difficulty == "any difficulty")
                        QuizApi.retrofitService.getQuestions(numQuestions, "multiple", category).results
                    else
                        QuizApi.retrofitService.getQuestions(numQuestions, "multiple", difficulty, category).results

                    if (mode == 0)
                        _response.postValue(DataResponse(0, intermediateResponse))
                    else {
                        //post intermediateResponse value to firestore, then assign the value of _response to the response(record) obtained from firestore
                        postIntermediateResponse(intermediateResponse)
                        getQuestionsFromFirestore()
                    }
                }

                else {
                    getQuestionsFromFirestore()
                }

                _status.postValue(QuizApiStatus.DONE)
                questionAmount = numQuestions
                Log.d("Response", "${_response.value}")
            } catch (e: Exception){
                _status.postValue(QuizApiStatus.ERROR)
                _response.postValue(DataResponse(-1, mutableListOf()))
                Log.d("Response2", "${_response.value}")
            }
        }
    }

    private fun getQuestionsFromFirestore() {
        db.collection("roomQuestions")
            .whereEqualTo("RoomId", roomId.value)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isEmpty) {
                    val docSnapshot = task.result.documents.first()
                    val questions = docSnapshot.get("Questions")
                    Log.d("Retrieved Questions", "$questions")
                    val questionsList : MutableList<DataResponse.Question> = mutableListOf()

                    for(question in questions as List<Map<String,Any>>){
                        questionsList.add(DataResponse.fromJson(question))
                    }
                    questionAmount = questionsList.size
                    Log.d("List of models", "_response.value=${questionsList}")
                    _response.value = DataResponse(0, questionsList)
                    Log.d("Retrieved Questions", "_response.value=${_response.value}")
                    _status.postValue(QuizApiStatus.DONE)
                }
                else {
                    Log.d("GetQuestions", "Could not get the questions.")
                }
            }
    }

    private fun postIntermediateResponse(intermediateRespQues: MutableList<DataResponse.Question>) {

        val doc = hashMapOf(
            "RoomId" to roomId.value,
            "PlayerCapacity" to participantsCapacity,
            "Questions" to intermediateRespQues
        )

        db.collection("roomQuestions")
            .add(doc)
            .addOnSuccessListener {
                addDocInPlayersCollection()
            }
            .addOnFailureListener {
                Log.d("Addition", "Addition of doc in 'roomQuestions' failed!")
            }
    }

    fun addDocInPlayersCollection() {
        val document = hashMapOf(
            "Name" to playerName,
            "CurrentQuestionNum" to questionNumber.value,
            "Score" to score.value,
            "RoomId" to roomId.value,
            "AvgTimePerQuestion" to 0.0,
            "Forfeit" to false
        )

        db.collection("players")
            .add(document)
            .addOnSuccessListener {
                Log.d("Addition", "Addition of doc in 'players' successful")
            }
            .addOnFailureListener {
                Log.d("Addition", "Addition of doc in 'players' failed!")
            }
    }

    fun setParticipantsCapacity(rmId: String): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            db.collection("roomQuestions")
                .whereEqualTo("RoomId", rmId)
                .get()
                .addOnCompleteListener { task2 ->
                    if (task2.isSuccessful && !task2.result.isEmpty) {
                        participantsCapacity = (task2.result.documents[0].get("PlayerCapacity") as Long).toInt()
                        Log.d("Parameters", "numOfParticipants=$participantsCapacity")
                    }
                }
        }
    }

    private fun deleteDoc(collectionName: String, playerName: String? = null) {
        val docRef = if (playerName != null) {
            db.collection(collectionName)
                .whereEqualTo("RoomId", _roomId.value)
                .whereEqualTo("Name", playerName)
        } else {
            db.collection(collectionName)
                .whereEqualTo("RoomId", _roomId.value)
        }
        docRef.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isEmpty) {
                    val docSnapshot = task.result.documents[0]
                    val docId = docSnapshot.id
                    db.collection(collectionName)
                        .document(docId)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("Deletion", "Deletion of doc in $collectionName successful")
                        }
                        .addOnFailureListener {
                            Log.d("Deletion", "Deletion of doc in $collectionName failed!")
                        }
                }
                else {
                    Log.d("Deletion", "Deletion of doc in $collectionName failed!")
                }
            }
    }

    fun setCurrentNumOfParticipants(displayLeaderboardBtn: Boolean = false) {
        db.collection("players")
            .whereEqualTo("RoomId", _roomId.value)
            .get()
            .addOnCompleteListener { task2 ->
                if (task2.isSuccessful && !task2.result.isEmpty) {
                    currentNumOfParticipants = task2.result.documents.size
                    Log.d("participantsValues", "currentNumOfParticipants=$currentNumOfParticipants")
                    if ((dialogMode == 1) || (_questionNumber.value!! > questionAmount)) {
                        setNumParticipantsForfeit(displayLeaderboardBtn)
                    }
                    else if (dialogMode == 3) {
                        deleteDoc("players", playerName)
                        if (currentNumOfParticipants < 2)
                            deleteDoc("roomQuestions")
                    }
                }
                else {
                    Log.d("participantsValues", "currentNumOfParticipants failed: ${task2.exception}")
                    if (task2.result.isEmpty)
                        Log.d("participantsValues", "Empty task2.result of currentNumOfParticipants")
                }
            }
    }

    private fun setNumParticipantsForfeit(displayLeaderboardBtn: Boolean) {
        db.collection("players")
            .whereEqualTo("RoomId", _roomId.value)
            .whereEqualTo("Forfeit", true)
            .get(Source.SERVER)
            .addOnCompleteListener { task2 ->
                if (task2.isSuccessful) {
                    numParticipantsForfeit = task2.result.documents.size
                    Log.d("participantsValues", "numParticipantsForfeit=$numParticipantsForfeit")
                    if (displayLeaderboardBtn) {
                        if ((numParticipantsInPerformFrag + numParticipantsForfeit) ==
                            currentNumOfParticipants) {
                            _toShowLeaderboardBtn.value = true
                        }
                    }
                    else
                        numParticipantsFinished()
                }
                else {
                    Log.d("participantsValues", "numParticipantsForfeit failed: ${task2.result.documents}")
                }
            }
    }

    private fun numParticipantsFinished() {
        db.collection("players")
            .whereEqualTo("RoomId", _roomId.value)
            .whereEqualTo("CurrentQuestionNum", questionAmount+2)
            .get(Source.SERVER)
            .addOnCompleteListener { task2 ->
                if (task2.isSuccessful) {
                    numParticipantsFinished = task2.result.documents.size
                    Log.d("participantsValues", "numParticipantsFinished=$numParticipantsFinished")
                    if ((numParticipantsForfeit + numParticipantsFinished) == currentNumOfParticipants) {
                        deleteDoc("roomQuestions")
                        deleteAllDocsInPlayers()
                    }
                    Log.d("num values", "numParticipantsForfeit=${numParticipantsForfeit}, numParticipantsFinished=${numParticipantsFinished}, currentNumOfParticipants=${currentNumOfParticipants}")
                }
                else {
                    Log.d("participantsValues", "numParticipantsFinished failed: ${task2.result.documents}")
                }
            }
    }

    private fun deleteAllDocsInPlayers() {
        db.collection("players")
            .whereEqualTo("RoomId", _roomId.value)
            .get(Source.SERVER)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isEmpty) {
                    val documents = task.result.documents
                    for (doc in documents) {
                        val docId = doc.id
                        db.collection("players")
                            .document(docId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("Deletion", "Deletion of doc in players successful")
                            }
                            .addOnFailureListener {
                                Log.d("Deletion", "Deletion of doc in players failed!")
                            }
                    }
                }
            }
    }
}