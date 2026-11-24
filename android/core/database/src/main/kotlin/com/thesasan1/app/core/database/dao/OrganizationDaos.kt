package com.thesasan1.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.thesasan1.app.core.database.entity.CategoryEntity
import com.thesasan1.app.core.database.entity.CategoryScope
import com.thesasan1.app.core.database.entity.ProjectEntity
import com.thesasan1.app.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/*
 * Deletes are soft: callers upsert the row with `sync.deleted(now)` and enqueue an outbox entry in
 * the same transaction. "Active" queries hide tombstones. Hard deletes only happen in trash purge.
 */

@Dao
interface ProjectDao {
    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query(
        "SELECT * FROM projects WHERE deleted_at IS NULL AND archived_at IS NULL " +
            "ORDER BY sort_order, name COLLATE NOCASE",
    )
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("DELETE FROM projects WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffMillis")
    suspend fun purgeDeletedBefore(cutoffMillis: Long): Int
}

@Dao
interface TagDao {
    /**
     * Fails with a constraint error when another tag already has the same `name_key`. Tags have no
     * upsert on purpose: Room's upsert would turn that conflict into a silent no-op.
     */
    @Insert
    suspend fun insert(tag: TagEntity)

    /** Returns the number of rows changed (0 when the id doesn't exist). */
    @Update
    suspend fun update(tag: TagEntity): Int

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    /** Includes tombstones, so creating a tag whose name was deleted can restore the old row. */
    @Query("SELECT * FROM tags WHERE name_key = :nameKey")
    suspend fun getByNameKey(nameKey: String): TagEntity?

    @Query("SELECT * FROM tags WHERE deleted_at IS NULL ORDER BY name_key")
    fun observeActive(): Flow<List<TagEntity>>

    @Query("DELETE FROM tags WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffMillis")
    suspend fun purgeDeletedBefore(cutoffMillis: Long): Int
}

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE scope = :scope AND deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    fun observeActive(scope: CategoryScope): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffMillis")
    suspend fun purgeDeletedBefore(cutoffMillis: Long): Int
}
