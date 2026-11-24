package com.thesasan1.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user project that groups notes and tasks (DATABASE.md §3.8). */
@Entity(
    tableName = "projects",
    indices = [Index("sort_order"), Index("updated_at")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** ARGB colour as a hex string such as `#FF4F46E5`, or null for the default. */
    val color: String? = null,
    /** Icon key from the design system, or null. */
    val icon: String? = null,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Long = 0,
    @Embedded val sync: SyncColumns,
)
