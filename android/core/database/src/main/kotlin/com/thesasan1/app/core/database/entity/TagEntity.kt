package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * A free-form label (DATABASE.md §3.8). Names are unique case-insensitively through
 * [nameKey]; the unique index includes tombstones, so re-creating a deleted tag restores it.
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name_key"], unique = true), Index("updated_at")],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String = nameKeyOf(name),
    val color: String? = null,
    @Embedded val sync: SyncColumns,
) {
    companion object {
        /** Trimmed, lower-cased with the root locale so Persian and Latin names compare predictably. */
        fun nameKeyOf(name: String): String = name.trim().lowercase(Locale.ROOT)
    }
}
