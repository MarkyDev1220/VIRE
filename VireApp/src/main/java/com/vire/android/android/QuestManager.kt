package com.vire.android.android

object QuestManager {
    private val quests = mutableListOf<QuestRequest>()
    private var lastId = 5000L

    init {
        // sample
        addQuest(
            QuestRequest(
                id = nextId(),
                title = "Looking for Standard Matches",
                type = QuestRequest.Type.MATCHES,
                game = "Magic: The Gathering",
                description = "Looking for friendly Standard matches evenings.",
                poster = "bob",
                details = null
            )
        )
    }

    fun nextId(): Long = ++lastId

    fun addQuest(q: QuestRequest) {
        quests.add(0, q)
    }

    fun getQuests(): List<QuestRequest> = quests.toList()

    fun deleteQuest(id: Long) {
        quests.removeAll { it.id == id }
    }

    fun updateQuest(updated: QuestRequest): Boolean {
        val idx = quests.indexOfFirst { it.id == updated.id }
        return if (idx >= 0) {
            quests[idx] = updated
            true
        } else false
    }

    fun search(query: String, type: QuestRequest.Type?): List<QuestRequest> {
        val q = query.trim().lowercase()
        return quests.filter {
            (type == null || it.type == type) &&
                    (q.isEmpty() || it.title.lowercase().contains(q) || it.description.lowercase().contains(q) || (it.details?.lowercase()?.contains(q) ?: false))
        }
    }
}