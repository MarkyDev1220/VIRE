package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object TournamentManager {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val localTournaments = mutableListOf<Tournament>()
    private val localMatches = mutableMapOf<String, MutableList<TournamentMatch>>()

    fun createTournament(tournament: Tournament, onResult: (Boolean, String?) -> Unit) {
        try {
            val ref = db.collection("tournaments").document()
            val hostUid = auth.currentUser?.uid ?: "demo_host"
            val finalTournament = tournament.copy(
                id = ref.id,
                hostUid = hostUid,
                participants = if (hostUid.isNotBlank()) listOf(hostUid) else emptyList()
            )

            localTournaments.add(0, finalTournament)

            if (auth.currentUser != null) {
                ref.set(finalTournament)
                    .addOnSuccessListener { onResult(true, finalTournament.id) }
                    .addOnFailureListener { onResult(true, finalTournament.id) }
            } else {
                onResult(true, finalTournament.id)
            }
        } catch (e: Exception) {
            onResult(false, null)
        }
    }

    fun getTournaments(onResult: (List<Tournament>) -> Unit) {
        try {
            db.collection("tournaments")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(Tournament::class.java)
                    localTournaments.clear()
                    localTournaments.addAll(list)
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localTournaments.toList())
                }
        } catch (e: Exception) {
            onResult(localTournaments.toList())
        }
    }

    fun registerPlayer(tournamentId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val index = localTournaments.indexOfFirst { it.id == tournamentId }
            if (index >= 0) {
                val t = localTournaments[index]
                if (!t.participants.contains(userUid)) {
                    val updated = t.copy(participants = t.participants + userUid)
                    localTournaments[index] = updated
                }
            }

            if (auth.currentUser != null && tournamentId.isNotEmpty()) {
                db.collection("tournaments").document(tournamentId)
                    .update("participants", FieldValue.arrayUnion(userUid))
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun unregisterPlayer(tournamentId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val index = localTournaments.indexOfFirst { it.id == tournamentId }
            if (index >= 0) {
                val t = localTournaments[index]
                val updated = t.copy(participants = t.participants.filter { it != userUid })
                localTournaments[index] = updated
            }

            if (auth.currentUser != null && tournamentId.isNotEmpty()) {
                db.collection("tournaments").document(tournamentId)
                    .update("participants", FieldValue.arrayRemove(userUid))
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun registerSpectator(tournamentId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val index = localTournaments.indexOfFirst { it.id == tournamentId }
            if (index >= 0) {
                val t = localTournaments[index]
                if (!t.spectators.contains(userUid)) {
                    val updated = t.copy(spectators = t.spectators + userUid)
                    localTournaments[index] = updated
                }
            }

            if (auth.currentUser != null && tournamentId.isNotEmpty()) {
                db.collection("tournaments").document(tournamentId)
                    .update("spectators", FieldValue.arrayUnion(userUid))
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun unregisterSpectator(tournamentId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val index = localTournaments.indexOfFirst { it.id == tournamentId }
            if (index >= 0) {
                val t = localTournaments[index]
                val updated = t.copy(spectators = t.spectators.filter { it != userUid })
                localTournaments[index] = updated
            }

            if (auth.currentUser != null && tournamentId.isNotEmpty()) {
                db.collection("tournaments").document(tournamentId)
                    .update("spectators", FieldValue.arrayRemove(userUid))
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun generateBrackets(
        tournament: Tournament,
        playerNames: Map<String, String>,
        onResult: (Boolean) -> Unit
    ) {
        try {
            val players = tournament.participants.shuffled()
            val matchesList = mutableListOf<TournamentMatch>()

            var matchNum = 1
            var i = 0
            while (i < players.size) {
                val p1Uid = players[i]
                val p1Name = playerNames[p1Uid] ?: "Player ${i + 1}"

                val p2Uid = if (i + 1 < players.size) players[i + 1] else ""
                val p2Name = if (p2Uid.isNotEmpty()) playerNames[p2Uid] ?: "Player ${i + 2}" else "BYE"

                val matchRef = db.collection("tournamentMatches").document()
                val match = TournamentMatch(
                    id = matchRef.id,
                    tournamentId = tournament.id,
                    roundNumber = 1,
                    matchNumber = matchNum++,
                    player1Uid = p1Uid,
                    player1Name = p1Name,
                    player2Uid = p2Uid,
                    player2Name = p2Name,
                    isCompleted = p2Uid.isEmpty(), // Automatic BYE
                    winnerUid = if (p2Uid.isEmpty()) p1Uid else ""
                )

                matchesList.add(match)
                if (auth.currentUser != null) {
                    matchRef.set(match)
                }
                i += 2
            }

            localMatches[tournament.id] = matchesList

            // Update tournament status to "In Progress"
            val idx = localTournaments.indexOfFirst { it.id == tournament.id }
            if (idx >= 0) {
                localTournaments[idx] = tournament.copy(status = "In Progress")
            }
            if (auth.currentUser != null) {
                db.collection("tournaments").document(tournament.id)
                    .update("status", "In Progress")
            }

            onResult(true)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun getMatches(tournamentId: String, onResult: (List<TournamentMatch>) -> Unit) {
        try {
            db.collection("tournamentMatches")
                .whereEqualTo("tournamentId", tournamentId)
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(TournamentMatch::class.java)
                    localMatches[tournamentId] = list.toMutableList()
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localMatches[tournamentId] ?: emptyList())
                }
        } catch (e: Exception) {
            onResult(localMatches[tournamentId] ?: emptyList())
        }
    }

    fun updateMatchScore(
        match: TournamentMatch,
        p1Score: Int,
        p2Score: Int,
        winnerUid: String,
        onResult: (Boolean) -> Unit
    ) {
        try {
            val updated = match.copy(
                player1Score = p1Score,
                player2Score = p2Score,
                winnerUid = winnerUid,
                isCompleted = true
            )

            val list = localMatches[match.tournamentId]
            if (list != null) {
                val idx = list.indexOfFirst { it.id == match.id }
                if (idx >= 0) list[idx] = updated
            }

            if (auth.currentUser != null && match.id.isNotEmpty()) {
                db.collection("tournamentMatches").document(match.id)
                    .set(updated)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun searchTournaments(query: String, tournaments: List<Tournament>): List<Tournament> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return tournaments
        return tournaments.filter {
            it.name.lowercase().contains(q) ||
                    it.gameSystem.lowercase().contains(q) ||
                    it.location.lowercase().contains(q) ||
                    it.organizer.lowercase().contains(q)
        }
    }
}
