package com.thesasan1.app.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Which kind of item a category applies to (DATABASE.md §3.8). */
enum class CategoryScope {
    NOTE,
    TASK,
    HABIT,
    GOAL,
    JOURNAL,
}

@Entity(
    tableName = "categories",
    indices = [Index("scope"), Index("updated_at")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val scope: CategoryScope,
    @Embedded val sync: SyncColumns,
)
