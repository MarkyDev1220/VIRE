package com.vire.android.android

/**
 * Simple in-memory manager for marketplace listings.
 * Replace with Room/Firestore/REST backend as needed.
 */
object MarketplaceManager {
    private val listings = mutableListOf<MarketplaceItem>()
    private var lastId = 1000L

    init {
        // seed some demo listings using the expanded categories
        addListing(
            MarketplaceItem(
                id = nextId(),
                title = "NM Pokemon Boosters (x3)",
                description = "Near mint boosters, English. Selling as a lot.",
                price = 12.50,
                category = "Decks/Cards",
                owner = "seller1"
            )
        )
        addListing(
            MarketplaceItem(
                id = nextId(),
                title = "Commander Deck - The Drafter",
                description = "Casual commander deck, sleeved. Includes land package.",
                price = 40.00,
                category = "Decks/Cards",
                owner = "seller2"
            )
        )
        addListing(
            MarketplaceItem(
                id = nextId(),
                title = "Sleeves and Toploaders Bundle",
                description = "100 sleeves + 25 toploaders",
                price = 8.00,
                category = "Accessories",
                owner = "seller3"
            )
        )
    }

    fun nextId(): Long = ++lastId

    fun addListing(item: MarketplaceItem) {
        listings.add(0, item) // newest first
    }

    fun getListings(): List<MarketplaceItem> = listings.filter { !it.sold }

    fun getAllListings(): List<MarketplaceItem> = listings.toList()

    fun deleteListing(id: Long) {
        listings.removeAll { it.id == id }
    }

    fun markSold(id: Long) {
        listings.find { it.id == id }?.sold = true
    }

    fun updateListing(updated: MarketplaceItem) {
        val idx = listings.indexOfFirst { it.id == updated.id }
        if (idx >= 0) listings[idx] = updated
    }

    fun searchAndFilter(query: String, category: String): List<MarketplaceItem> {
        val q = query.trim().lowercase()
        return listings.filter { !it.sold }
            .filter { category == "All" || it.category.equals(category, ignoreCase = true) }
            .filter {
                q.isEmpty() ||
                        it.title.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.owner.lowercase().contains(q)
            }
    }
}