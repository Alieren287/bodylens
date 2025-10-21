package com.example.bodylens.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ProgressEntryWithDetails(
    @Embedded val entry: ProgressEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val photos: List<ProgressPhoto>,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val measurements: List<Measurement>,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val aiInsights: List<AIInsight>
)


