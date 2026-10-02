package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object DeckManager {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val localDecks = mutableListOf<Deck>()
    private val localComments = mutableMapOf<String, MutableList<DeckComment>>()

    fun createDeck(deck: Deck, onResult: (Boolean, String?) -> Unit) {
        try {
            val ref = db.collection("userDecks").document()
            val uid = auth.currentUser?.uid ?: "demo_user"
            val finalDeck = deck.copy(
                id = ref.id,
                userId = uid
            )

            localDecks.add(0, finalDeck)

            if (auth.currentUser != null) {
                ref.set(finalDeck)
                    .addOnSuccessListener { onResult(true, finalDeck.id) }
                    .addOnFailureListener { onResult(true, finalDeck.id) }
            } else {
                onResult(true, finalDeck.id)
            }
        } catch (e: Exception) {
            onResult(false, null)
        }
    }

    fun updateDeck(deck: Deck, onResult: (Boolean) -> Unit) {
        try {
            val idx = localDecks.indexOfFirst { it.id == deck.id }
            val updated = deck.copy(cardCount = deck.cards.sumOf { it.quantity })
            if (idx >= 0) localDecks[idx] = updated else localDecks.add(updated)

            if (auth.currentUser != null && deck.id.isNotEmpty()) {
                db.collection("userDecks").document(deck.id)
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

    fun deleteDeck(deckId: String, onResult: (Boolean) -> Unit) {
        try {
            localDecks.removeAll { it.id == deckId }
            if (auth.currentUser != null && deckId.isNotEmpty()) {
                db.collection("userDecks").document(deckId)
                    .delete()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun getUserDecks(targetUserId: String? = null, onResult: (List<Deck>) -> Unit) {
        try {
            val uid = targetUserId ?: auth.currentUser?.uid
            if (uid.isNullOrEmpty() || auth.currentUser == null) {
                onResult(localDecks.toList())
                return
            }

            db.collection("userDecks")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(Deck::class.java)
                    localDecks.clear()
                    localDecks.addAll(list)
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localDecks.toList())
                }
        } catch (e: Exception) {
            onResult(localDecks.toList())
        }
    }

    fun getPublicDecks(gameSystemFilter: String = "All", onResult: (List<Deck>) -> Unit) {
        try {
            var query = db.collection("userDecks").whereEqualTo("isPublic", true)
            if (!gameSystemFilter.equals("All", ignoreCase = true)) {
                query = query.whereEqualTo("gameSystem", gameSystemFilter)
            }

            query.get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(Deck::class.java)
                    onResult(list)
                }
                .addOnFailureListener {
                    val filtered = localDecks.filter {
                        it.isPublic && (gameSystemFilter.equals("All", ignoreCase = true) || it.gameSystem.equals(gameSystemFilter, ignoreCase = true))
                    }
                    onResult(filtered)
                }
        } catch (e: Exception) {
            onResult(localDecks.toList())
        }
    }

    fun toggleLikeDeck(deckId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val idx = localDecks.indexOfFirst { it.id == deckId }
            if (idx >= 0) {
                val d = localDecks[idx]
                val isLiked = d.likedBy.contains(userUid)
                val newLikedBy = if (isLiked) d.likedBy - userUid else d.likedBy + userUid
                localDecks[idx] = d.copy(likedBy = newLikedBy, likesCount = newLikedBy.size)

                if (auth.currentUser != null && deckId.isNotEmpty()) {
                    val ref = db.collection("userDecks").document(deckId)
                    if (isLiked) {
                        ref.update("likedBy", FieldValue.arrayRemove(userUid), "likesCount", FieldValue.increment(-1))
                    } else {
                        ref.update("likedBy", FieldValue.arrayUnion(userUid), "likesCount", FieldValue.increment(1))
                    }
                }
            }
            onResult(true)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun addDeckComment(comment: DeckComment, onResult: (Boolean) -> Unit) {
        try {
            val ref = db.collection("deckComments").document()
            val finalComment = comment.copy(id = ref.id)

            val list = localComments.getOrPut(comment.deckId) { mutableListOf() }
            list.add(0, finalComment)

            if (auth.currentUser != null) {
                ref.set(finalComment)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun getDeckComments(deckId: String, onResult: (List<DeckComment>) -> Unit) {
        try {
            db.collection("deckComments")
                .whereEqualTo("deckId", deckId)
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(DeckComment::class.java)
                    localComments[deckId] = list.toMutableList()
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localComments[deckId] ?: emptyList())
                }
        } catch (e: Exception) {
            onResult(localComments[deckId] ?: emptyList())
        }
    }

    // Starter dataset for the 7 TCG systems
    fun getStarterCardDatabase(gameSystem: String): List<DeckCard> {
        return when (gameSystem) {
            "Magic: The Gathering" -> listOf(
                DeckCard(cardName = "Atraxa, Praetors' Voice", gameSystem = gameSystem, cardType = "Creature", manaCostOrLevel = "4 Mana"),
                DeckCard(cardName = "Sol Ring", gameSystem = gameSystem, cardType = "Artifact", manaCostOrLevel = "1 Mana"),
                DeckCard(cardName = "Arcane Signet", gameSystem = gameSystem, cardType = "Artifact", manaCostOrLevel = "2 Mana"),
                DeckCard(cardName = "Counterspell", gameSystem = gameSystem, cardType = "Instant", manaCostOrLevel = "2 Mana"),
                DeckCard(cardName = "Swords to Plowshares", gameSystem = gameSystem, cardType = "Instant", manaCostOrLevel = "1 Mana"),
                DeckCard(cardName = "Cyclonic Rift", gameSystem = gameSystem, cardType = "Instant", manaCostOrLevel = "2 Mana"),
                DeckCard(cardName = "Rhystic Study", gameSystem = gameSystem, cardType = "Enchantment", manaCostOrLevel = "3 Mana")
            )
            "Pokémon" -> listOf(
                DeckCard(cardName = "Charizard ex", gameSystem = gameSystem, cardType = "Pokémon", manaCostOrLevel = "Stage 2"),
                DeckCard(cardName = "Pikachu ex", gameSystem = gameSystem, cardType = "Pokémon", manaCostOrLevel = "Basic"),
                DeckCard(cardName = "Professor's Research", gameSystem = gameSystem, cardType = "Trainer", manaCostOrLevel = "Supporter"),
                DeckCard(cardName = "Boss's Orders", gameSystem = gameSystem, cardType = "Trainer", manaCostOrLevel = "Supporter"),
                DeckCard(cardName = "Ultra Ball", gameSystem = gameSystem, cardType = "Trainer", manaCostOrLevel = "Item"),
                DeckCard(cardName = "Fire Energy", gameSystem = gameSystem, cardType = "Energy", manaCostOrLevel = "Basic")
            )
            "Yu-Gi-Oh!" -> listOf(
                DeckCard(cardName = "Blue-Eyes White Dragon", gameSystem = gameSystem, cardType = "Monster", manaCostOrLevel = "Level 8"),
                DeckCard(cardName = "Dark Magician", gameSystem = gameSystem, cardType = "Monster", manaCostOrLevel = "Level 7"),
                DeckCard(cardName = "Ash Blossom & Joyous Spring", gameSystem = gameSystem, cardType = "Monster", manaCostOrLevel = "Level 3"),
                DeckCard(cardName = "Infinite Impermanence", gameSystem = gameSystem, cardType = "Trap", manaCostOrLevel = "Normal Trap"),
                DeckCard(cardName = "Pot of Prosperity", gameSystem = gameSystem, cardType = "Spell", manaCostOrLevel = "Normal Spell")
            )
            "Disney Lorcana" -> listOf(
                DeckCard(cardName = "Elsa - Spirit of Winter", gameSystem = gameSystem, cardType = "Character", manaCostOrLevel = "6 Ink"),
                DeckCard(cardName = "Mickey Mouse - Wayward Sorcerer", gameSystem = gameSystem, cardType = "Character", manaCostOrLevel = "4 Ink"),
                DeckCard(cardName = "Be Prepared", gameSystem = gameSystem, cardType = "Action", manaCostOrLevel = "7 Ink"),
                DeckCard(cardName = "A Whole New World", gameSystem = gameSystem, cardType = "Song", manaCostOrLevel = "5 Ink")
            )
            "Cardfight Vanguard (CFV)" -> listOf(
                DeckCard(cardName = "Chronojet Dragon", gameSystem = gameSystem, cardType = "Unit", manaCostOrLevel = "Grade 3"),
                DeckCard(cardName = "Dragonic Overlord", gameSystem = gameSystem, cardType = "Unit", manaCostOrLevel = "Grade 3"),
                DeckCard(cardName = "Blaster Blade", gameSystem = gameSystem, cardType = "Unit", manaCostOrLevel = "Grade 2")
            )
            "Battle Spirits Saga (BSS)" -> listOf(
                DeckCard(cardName = "Siegwurm", gameSystem = gameSystem, cardType = "Spirit", manaCostOrLevel = "6 Core"),
                DeckCard(cardName = "Nova, the Super Star Dragon", gameSystem = gameSystem, cardType = "Spirit", manaCostOrLevel = "8 Core")
            )
            "Force of Will (FOW)" -> listOf(
                DeckCard(cardName = "Ruler - Alice", gameSystem = gameSystem, cardType = "Ruler", manaCostOrLevel = "Ruler"),
                DeckCard(cardName = "Thunder", gameSystem = gameSystem, cardType = "Chant", manaCostOrLevel = "1 Will")
            )
            else -> listOf(
                DeckCard(cardName = "Starter Game Card", gameSystem = gameSystem, cardType = "Card", manaCostOrLevel = "1")
            )
        }
    }
}
