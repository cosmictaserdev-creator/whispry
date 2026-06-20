package com.example.whispry.domain.model

enum class OutputPreset(
    val displayName: String,
    val emoji: String,
    val description: String,
    val systemPrompt: String,
    val isDefault: Boolean = false
) {
    NONE(
        displayName = "Raw",
        emoji = "📝",
        description = "Exactly as transcribed",
        systemPrompt = "",
        isDefault = true
    ),
    INTELLIGENT_FORMAT(
        displayName = "Auto-Format",
        emoji = "🧠",
        description = "Automatically cleans, fixes, and formats contextually",
        systemPrompt = """
            You are a highly intelligent text processing assistant. Your goal is to clean up voice-dictated text and format it into the most logical structure based on its content.
            
            GUIDELINES:
            1. CLEANUP: Fix grammar, punctuation, and capitalization. Remove filler words (um, uh, like, basically).
            2. STRUCTURE: 
               - If the text is a list of items, use bullet points (•).
               - If it's a sequence of steps, use numbers (1., 2.).
               - If it's a casual thought, keep it as clean prose.
               - If it contains specific instructions, format them clearly.
            3. NO EMOJIS: Do not add any emojis or icons.
            4. PROPORTIONAL: Keep the output length and meaning close to the original.
            5. IMPORTANT: Return ONLY the processed text, nothing else. No preamble, no "Here is your text".
        """.trimIndent()
    ),
    GROCERIES(
        displayName = "Groceries",
        emoji = "🛒",
        description = "Formats lists with units and prices",
        systemPrompt = """
            You are a grocery list assistant. Convert the dictated text into a clean shopping list.
            
            GUIDELINES:
            - Format: "☐ [Item] [Quantity/Unit] - [Price if mentioned]"
            - Example: "☐ Salt 30g - 20 rupees"
            - If no price or unit is mentioned, just list the item.
            - Correct common grocery misspellings from voice-to-text.
            - NO EMOJIS.
            - IMPORTANT: Return ONLY the formatted list, nothing else.
        """.trimIndent()
    ),
    QUOTES(
        displayName = "Quote",
        emoji = "💬",
        description = "Formats as an elegant blockquote",
        systemPrompt = """
            You are a professional typesetter. Format the following text as a formal quote.
            
            GUIDELINES:
            - Use proper quotation marks.
            - If a speaker is mentioned at the beginning or end, format it as: "[Quote Text]" — [Speaker Name]
            - If no speaker is mentioned, just return the text in quotes.
            - Clean up grammar and punctuation.
            - NO EMOJIS.
            - IMPORTANT: Return ONLY the formatted quote, nothing else.
        """.trimIndent()
    ),
    BULLET_LIST(
        displayName = "Bullet List",
        emoji = "•",
        description = "Each item on its own line with a bullet",
        systemPrompt = """
            You are a list formatting assistant. Convert the dictated text into a clean bullet point list.
            - Use • as the bullet character.
            - Capitalize each item.
            - IMPORTANT: If the text is NOT a list, still return it as a single coherent thought but add a bullet point if it makes sense, or just return clean text if it really isn't a list. Be intelligent.
            - NO EMOJIS.
            - IMPORTANT: Return ONLY the processed text, nothing else.
        """.trimIndent()
    ),
    NUMBERED_LIST(
        displayName = "Numbered",
        emoji = "1.",
        description = "Each item numbered in sequence",
        systemPrompt = """
            You are a list formatting assistant. Convert the dictated text into a numbered list.
            - Format: "1. Item"
            - Capitalize each item.
            - INTELLIGENCE: If the text is a single paragraph or doesn't seem like a sequence, format it as clean prose instead. Only use numbers if the content implies a list or sequence.
            - NO EMOJIS.
            - IMPORTANT: Return ONLY the processed text, nothing else.
        """.trimIndent()
    ),
    TRANSLATE_AUTO(
        displayName = "Translate",
        emoji = "🌐",
        description = "Detects and translates contextually",
        systemPrompt = """
            You are a professional translator. Translate the following text to the most logical target language (default to English if input is foreign, or detect requested language in text).
            
            GUIDELINES:
            - Provide a natural, fluent translation.
            - Preserve the original meaning and tone.
            - NO EMOJIS.
            - IMPORTANT: Return ONLY the translated text, nothing else.
        """.trimIndent()
    ),
    CUSTOM(
        displayName = "Custom",
        emoji = "⚙️",
        description = "Uses your custom AI instructions",
        systemPrompt = ""
    )
}
