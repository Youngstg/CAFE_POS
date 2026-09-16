package com.coffeeos.erp.core.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room = source of truth UI (offline-first). Firestore hanya untuk sync antar HP.
 * Skema dipetakan dari docs/legacy-spec + aturan stok 10%/2%.
 */
@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val name: String,
    val currentStock: Double,
    val maxCapacity: Double?,
    val unit: String,
    val isLow: Boolean = false,
    val isStopped: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val name: String,
    val price: Long,
    val isAvailable: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val key: Long = 0,
    val menuId: String,
    val ingredientId: String,
    val qtyPerPortion: Double,
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val status: String, // QUEUED/COOKING/READY/PAID/CONFLICT_NEED_REVIEW
    val total: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
)

/** Antrian sync -> dikirim WorkManager saat online (NetworkType.CONNECTED). */
@Entity(tableName = "pending_mutations")
data class PendingMutation(
    @PrimaryKey val mutationId: String,
    val kind: String, // ORDER_UPSERT / STOCK_DEDUCT / STOCK_IN / SHIFT_CLOSE / PO_RECEIVE
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
)

@Dao
interface PosDao {
    @Query("SELECT * FROM ingredients WHERE outletId = :outletId")
    fun observeIngredients(outletId: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM menus WHERE outletId = :outletId")
    fun observeMenus(outletId: String): Flow<List<MenuEntity>>

    @Query("SELECT * FROM recipes WHERE menuId = :menuId")
    suspend fun recipesForMenu(menuId: String): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIngredient(entity: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMenu(entity: MenuEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(entity: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(mutation: PendingMutation)

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peekQueue(limit: Int = 50): List<PendingMutation>

    @Query("DELETE FROM pending_mutations WHERE mutationId = :id")
    suspend fun dequeue(id: String)
}

@Database(
    entities = [IngredientEntity::class, MenuEntity::class, RecipeEntity::class, OrderEntity::class, PendingMutation::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao
}
