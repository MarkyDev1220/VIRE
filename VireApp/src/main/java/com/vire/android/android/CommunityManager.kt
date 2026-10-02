package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object CommunityManager {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val localCommunities = mutableListOf<Community>()
    private val localPosts = mutableMapOf<String, MutableList<CommunityPost>>()

    private val defaultCommunities = listOf(
        Community(id = "mtg", name = "Magic: The Gathering", category = "TCG", description = "Commander, Modern, Standard, deckbuilding and rulings."),
        Community(id = "pokemon", name = "Pokémon TCG", category = "TCG", description = "Casual & competitive play, card trades, set discussion."),
        Community(id = "yugioh", name = "Yu-Gi-Oh!", category = "TCG", description = "Dueling, decklists, banlist discussion, combos."),
        Community(id = "dnd", name = "Dungeons & Dragons", category = "RPG", description = "DMs, campaign ideas, character builds, homebrew content."),
        Community(id = "warhammer", name = "Warhammer 40K", category = "Miniature Wargame", description = "Army painting, 40K & Age of Sigmar battle reports."),
        Community(id = "lorcana", name = "Disney Lorcana", category = "TCG", description = "Illumineers assembling for decklists, lore, and tournaments."),
        Community(id = "flesh_and_blood", name = "Flesh and Blood", category = "TCG", description = "Hero deckbuilding, tournament meta, card discussion."),
        Community(id = "board_games", name = "Board Games Hub", category = "Board Game", description = "Strategy, eurogames, party games, reviews and game nights.")
    )

    fun getCommunities(onResult: (List<Community>) -> Unit) {
        try {
            db.collection("communities")
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        // Seed default communities into Firestore
                        seedDefaultCommunities()
                        onResult(defaultCommunities)
                    } else {
                        val list = result.toObjects(Community::class.java)
                        localCommunities.clear()
                        localCommunities.addAll(list)
                        onResult(list)
                    }
                }
                .addOnFailureListener {
                    if (localCommunities.isEmpty()) localCommunities.addAll(defaultCommunities)
                    onResult(localCommunities.toList())
                }
        } catch (e: Exception) {
            if (localCommunities.isEmpty()) localCommunities.addAll(defaultCommunities)
            onResult(localCommunities.toList())
        }
    }

    private fun seedDefaultCommunities() {
        for (c in defaultCommunities) {
            db.collection("communities").document(c.id).set(c)
        }
    }

    fun createCommunity(community: Community, onResult: (Boolean, String?) -> Unit) {
        try {
            val ref = db.collection("communities").document()
            val finalCommunity = community.copy(id = ref.id)
            localCommunities.add(0, finalCommunity)

            if (auth.currentUser != null) {
                ref.set(finalCommunity)
                    .addOnSuccessListener { onResult(true, finalCommunity.id) }
                    .addOnFailureListener { onResult(true, finalCommunity.id) }
            } else {
                onResult(true, finalCommunity.id)
            }
        } catch (e: Exception) {
            onResult(false, null)
        }
    }

    fun joinCommunity(communityId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val idx = localCommunities.indexOfFirst { it.id == communityId }
            if (idx >= 0) {
                val c = localCommunities[idx]
                if (!c.members.contains(userUid)) {
                    val newMembers = c.members + userUid
                    localCommunities[idx] = c.copy(members = newMembers, memberCount = newMembers.size)
                }
            }

            if (auth.currentUser != null && communityId.isNotEmpty()) {
                db.collection("communities").document(communityId)
                    .update(
                        mapOf(
                            "members" to FieldValue.arrayUnion(userUid),
                            "memberCount" to FieldValue.increment(1)
                        )
                    )
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun leaveCommunity(communityId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val idx = localCommunities.indexOfFirst { it.id == communityId }
            if (idx >= 0) {
                val c = localCommunities[idx]
                val newMembers = c.members.filter { it != userUid }
                localCommunities[idx] = c.copy(members = newMembers, memberCount = newMembers.size.coerceAtLeast(0))
            }

            if (auth.currentUser != null && communityId.isNotEmpty()) {
                db.collection("communities").document(communityId)
                    .update(
                        mapOf(
                            "members" to FieldValue.arrayRemove(userUid),
                            "memberCount" to FieldValue.increment(-1)
                        )
                    )
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun createPost(post: CommunityPost, onResult: (Boolean, String?) -> Unit) {
        try {
            val ref = db.collection("communityPosts").document()
            val finalPost = post.copy(id = ref.id)

            val list = localPosts.getOrPut(post.communityId) { mutableListOf() }
            list.add(0, finalPost)

            if (auth.currentUser != null) {
                ref.set(finalPost)
                    .addOnSuccessListener { onResult(true, finalPost.id) }
                    .addOnFailureListener { onResult(true, finalPost.id) }
            } else {
                onResult(true, finalPost.id)
            }
        } catch (e: Exception) {
            onResult(false, null)
        }
    }

    fun getPostsForCommunity(communityId: String, onResult: (List<CommunityPost>) -> Unit) {
        try {
            db.collection("communityPosts")
                .whereEqualTo("communityId", communityId)
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(CommunityPost::class.java)
                    localPosts[communityId] = list.toMutableList()
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localPosts[communityId] ?: emptyList())
                }
        } catch (e: Exception) {
            onResult(localPosts[communityId] ?: emptyList())
        }
    }

    fun toggleLikePost(postId: String, communityId: String, userUid: String, onResult: (Boolean) -> Unit) {
        try {
            val list = localPosts[communityId]
            if (list != null) {
                val idx = list.indexOfFirst { it.id == postId }
                if (idx >= 0) {
                    val p = list[idx]
                    val isLiked = p.likedBy.contains(userUid)
                    val newLikedBy = if (isLiked) p.likedBy - userUid else p.likedBy + userUid
                    val newLikesCount = newLikedBy.size
                    list[idx] = p.copy(likedBy = newLikedBy, likesCount = newLikesCount)

                    if (auth.currentUser != null && postId.isNotEmpty()) {
                        val ref = db.collection("communityPosts").document(postId)
                        if (isLiked) {
                            ref.update("likedBy", FieldValue.arrayRemove(userUid), "likesCount", FieldValue.increment(-1))
                        } else {
                            ref.update("likedBy", FieldValue.arrayUnion(userUid), "likesCount", FieldValue.increment(1))
                        }
                    }
                }
            }
            onResult(true)
        } catch (e: Exception) {
            onResult(false)
        }
    }
}
