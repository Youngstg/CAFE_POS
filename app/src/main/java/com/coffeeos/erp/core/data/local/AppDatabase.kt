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

    @Query("SELECT * FROM ingredients WHERE outletId = :outletId")
    suspend fun listIngredients(outletId: String): List<IngredientEntity>

    @Query("SELECT * FROM menus WHERE outletId = :outletId")
    fun observeMenus(outletId: String): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus WHERE outletId = :outletId")
    suspend fun listMenus(outletId: String): List<MenuEntity>

    @Query("SELECT * FROM recipes WHERE menuId = :menuId")
    suspend fun recipesForMenu(menuId: String): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIngredient(entity: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMenu(entity: MenuEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(entity: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(entity: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(mutation: PendingMutation)

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peekQueue(limit: Int = 50): List<PendingMutation>

    @Query("DELETE FROM pending_mutations WHERE mutationId = :id")
    suspend fun dequeue(id: String)

    @Query("SELECT COUNT(*) FROM pending_mutations")
    suspend fun pendingCount(): Int

    // ---- Shift ----
    @Query("SELECT * FROM shifts WHERE outletId = :outletId AND isClosed = 0 LIMIT 1")
    suspend fun activeShift(outletId: String): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShift(entity: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE outletId = :outletId ORDER BY openedAt DESC LIMIT :limit")
    fun observeShifts(outletId: String, limit: Int = 20): Flow<List<ShiftEntity>>

    // ---- Orders ----
    @Query("SELECT * FROM orders WHERE outletId = :outletId ORDER BY createdAt DESC LIMIT :limit")
    fun observeOrders(outletId: String, limit: Int = 100): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE outletId = :outletId ORDER BY createdAt DESC LIMIT 500")
    suspend fun listOrders(outletId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE outletId = :outletId AND status = :status ORDER BY createdAt ASC")
    fun observeOrdersByStatus(outletId: String, status: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE outletId = :outletId AND status IN ('QUEUED','COOKING','READY') ORDER BY createdAt ASC")
    fun observeKitchenQueue(outletId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun orderById(id: String): OrderEntity?

    @Query("UPDATE orders SET status = :status, pendingSync = 1 WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String)

    @Query("SELECT IFNULL(SUM(total),0) FROM orders WHERE outletId = :outletId AND status = 'PAID'")
    suspend fun paidTotal(outletId: String): Long

    // ---- Supply ----
    @Query("SELECT * FROM suppliers WHERE outletId = :outletId ORDER BY name ASC")
    fun observeSuppliers(outletId: String): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupplier(entity: SupplierEntity)

    @Query("SELECT * FROM purchase_orders WHERE outletId = :outletId ORDER BY createdAt DESC")
    fun observePos(outletId: String): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE outletId = :outletId")
    suspend fun listPos(outletId: String): List<PurchaseOrderEntity>

    @Query("SELECT * FROM purchase_orders WHERE id = :id LIMIT 1")
    suspend fun poById(id: String): PurchaseOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPo(entity: PurchaseOrderEntity)

    // ---- Seed helpers ----
    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun ingredientById(id: String): IngredientEntity?

    @Query("SELECT * FROM recipes WHERE menuId IN (:menuIds)")
    suspend fun recipesForMenus(menuIds: List<String>): List<RecipeEntity>
}

@Database(
    entities = [IngredientEntity::class, MenuEntity::class, RecipeEntity::class, OrderEntity::class, PendingMutation::class, ShiftEntity::class, SupplierEntity::class, PurchaseOrderEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao
}
