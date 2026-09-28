package com.wsnow.app.data

/** Built-in themes for playing offline or before an AI endpoint is set up. */
object FallbackWords {
    val themes: Map<String, List<String>> = mapOf(
        "Under the Sea" to listOf(
            "Dolphin", "Octopus", "Seahorse", "Jellyfish", "Starfish", "Coral", "Shark", "Whale",
            "Lobster", "Anemone", "Plankton", "Stingray", "Squid", "Urchin", "Barracuda", "Clownfish",
            "Manatee", "Walrus", "Narwhal", "Oyster", "Kelp", "Tide", "Reef", "Pearl",
        ),
        "Outer Space" to listOf(
            "Galaxy", "Nebula", "Comet", "Asteroid", "Meteor", "Planet", "Saturn", "Jupiter",
            "Mercury", "Venus", "Orbit", "Rocket", "Astronaut", "Telescope", "Eclipse", "Quasar",
            "Pulsar", "Gravity", "Satellite", "Cosmos", "Crater", "Lunar", "Solar", "Supernova",
        ),
        "In the Kitchen" to listOf(
            "Spatula", "Whisk", "Ladle", "Skillet", "Colander", "Grater", "Blender", "Toaster",
            "Oven", "Kettle", "Tongs", "Peeler", "Rolling Pin", "Saucepan", "Cutting Board", "Knife",
            "Apron", "Mixer", "Timer", "Funnel", "Sieve", "Mortar", "Pestle", "Wok",
        ),
        "Camping Trip" to listOf(
            "Tent", "Lantern", "Campfire", "Backpack", "Compass", "Sleeping Bag", "Marshmallow", "Canoe",
            "Hiking", "Trail", "Firewood", "Flashlight", "Cooler", "Hammock", "Map", "Binoculars",
            "Kindling", "Pinecone", "Creek", "Canteen", "Blanket", "Stars", "Owl", "Moose",
        ),
        "Garden Party" to listOf(
            "Tulip", "Daisy", "Orchid", "Sunflower", "Lavender", "Peony", "Marigold", "Violet",
            "Begonia", "Dahlia", "Magnolia", "Hydrangea", "Petunia", "Lilac", "Poppy", "Iris",
            "Watering Can", "Trowel", "Seedling", "Compost", "Trellis", "Greenhouse", "Hedge", "Bloom",
        ),
        "Musical Instruments" to listOf(
            "Guitar", "Violin", "Piano", "Trumpet", "Drums", "Flute", "Clarinet", "Saxophone",
            "Cello", "Harp", "Banjo", "Ukulele", "Trombone", "Oboe", "Tuba", "Xylophone",
            "Accordion", "Bagpipes", "Harmonica", "Mandolin", "Bassoon", "Sitar", "Cymbal", "Tambourine",
        ),
        "Around the World" to listOf(
            "Canada", "Brazil", "Japan", "Kenya", "Norway", "Egypt", "Peru", "India",
            "Mexico", "France", "Iceland", "Vietnam", "Morocco", "Chile", "Greece", "Portugal",
            "Australia", "Argentina", "Thailand", "Ireland", "Nepal", "Cuba", "Jamaica", "Finland",
        ),
        "Breakfast Table" to listOf(
            "Pancakes", "Waffles", "Bacon", "Omelet", "Cereal", "Toast", "Muffin", "Bagel",
            "Yogurt", "Granola", "Oatmeal", "Croissant", "Sausage", "Hash Browns", "Syrup", "Butter",
            "Coffee", "Orange Juice", "Jam", "Crepe", "Biscuit", "Eggs", "Banana", "Smoothie",
        ),
    )
}
