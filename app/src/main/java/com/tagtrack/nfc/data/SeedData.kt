package com.tagtrack.nfc.data

object SeedData {
    fun getSampleItems(): List<Item> {
        val now = System.currentTimeMillis()
        return listOf(
            Item(
                nfcCode = 1001,
                unqCode = 5001,
                itemName = "Classic Tuxedo Suit Set",
                category = "Formal Wear",
                ageGroup = "Adult",
                size = "40R",
                color = "Black",
                condition = "Excellent",
                status = "Available",
                unitQuantity = 1,
                storageLocation = "Rack A-1",
                description = "Two-piece wool blend tuxedo suit with satin lapels.",
                dateAdded = now - 86400000L * 5,
                dateUpdated = null
            ),
            Item(
                nfcCode = 1002,
                unqCode = 5002,
                itemName = "Designer Evening Gown",
                category = "Dresses",
                ageGroup = "Adult",
                size = "S",
                color = "Emerald Green",
                condition = "Good",
                status = "Rented",
                unitQuantity = 1,
                storageLocation = "Rack B-3",
                description = "Floor-length sequin evening dress with slit.",
                dateAdded = now - 86400000L * 4,
                dateUpdated = now - 86400000L * 1
            ),
            Item(
                nfcCode = 1003,
                unqCode = 5003,
                itemName = "Kids Superhero Costume",
                category = "Costumes",
                ageGroup = "Kids",
                size = "M",
                color = "Red/Blue",
                condition = "Fair",
                status = "Maintenance",
                unitQuantity = 2,
                storageLocation = "Bin C-2",
                description = "Polyester cape and jumpsuit with emblem.",
                dateAdded = now - 86400000L * 3,
                dateUpdated = now - 86400000L * 2
            ),
            Item(
                nfcCode = 1004,
                unqCode = 5004,
                itemName = "Leather Biker Jacket",
                category = "Outerwear",
                ageGroup = "Adult",
                size = "L",
                color = "Brown",
                condition = "Excellent",
                status = "Available",
                unitQuantity = 1,
                storageLocation = "Rack A-4",
                description = "Genuine vintage distressed leather jacket.",
                dateAdded = now - 86400000L * 2,
                dateUpdated = null
            ),
            Item(
                nfcCode = 1005,
                unqCode = 5005,
                itemName = "Formal Silk Tie",
                category = "Accessories",
                ageGroup = "Adult",
                size = "One Size",
                color = "Burgundy",
                condition = "Good",
                status = "Available",
                unitQuantity = 5,
                storageLocation = "Drawer D-1",
                description = "100% pure silk jacquard weave necktie.",
                dateAdded = now - 86400000L * 1,
                dateUpdated = null
            )
        )
    }
}
