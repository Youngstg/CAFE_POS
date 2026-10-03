package com.coffeeos.erp.core.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room = source of truth UI (offline-first). Firestore hanya untuk sync antar HP.
 * Skema dipetakan dari docs/legacy-spec + aturan stok 10%/2%.
 * v5: tambah tabel order_items (untuk KDS detail) + kolom orderSeq di orders.
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
    val category: String = "Umum",
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
    val orderSeq: Int = 0,   // Nomor urut per shift (#001, #002, ...)
    val shiftId: String = "", // ID shift saat order dibuat
    val paymentMethod: String = "TUNAI", // TUNAI / QRIS / TRANSFER
    val customerName: String = "", // Nama atau nomor akrilik antrean
    val orderType: String = "DINE_IN", // DINE_IN atau TAKE_AWAY
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

    @Query("SELECT * FROM recipes WHERE ingredientId = :ingredientId")
    suspend fun recipesForIngredient(ingredientId: String): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIngredient(entity: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMenu(entity: MenuEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(entity: RecipeEntity)

    @Query("DELETE FROM recipes WHERE menuId = :menuId AND ingredientId = :ingredientId")
    suspend fun deleteRecipe(menuId: String, ingredientId: String)

    @Query("DELETE FROM recipes WHERE menuId = :menuId")
    suspend fun deleteRecipesForMenu(menuId: String)

    @Query("SELECT COUNT(*) FROM recipes WHERE ingredientId = :ingredientId")
    suspend fun countRecipesUsing(ingredientId: String): Int

    @Query("DELETE FROM menus WHERE id = :menuId")
    suspend fun deleteMenu(menuId: String)

    @Query("DELETE FROM ingredients WHERE id = :ingredientId")
    suspend fun deleteIngredient(ingredientId: String)

    // ---- Promo ----
    @Query("SELECT * FROM promos WHERE outletId = :outletId ORDER BY minOrder ASC")
    fun observePromos(outletId: String): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promos WHERE outletId = :outletId AND active = 1")
    suspend fun listActivePromos(outletId: String): List<PromoEntity>

    @Query("SELECT * FROM promos WHERE id = :id LIMIT 1")
    suspend fun promoById(id: String): PromoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPromo(entity: PromoEntity)

    @Query("DELETE FROM promos WHERE id = :id")
    suspend fun deletePromo(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(entity: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(mutation: PendingMutation)

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peekQueue(limit: Int = 50): List<PendingMutation>

    @Query("DELETE FROM pending_mutations WHERE mutationId = :id")
    suspend fun dequeue(id: String)

    @Query("UPDATE pending_mutations SET retryCount = retryCount + 1 WHERE mutationId = :id")
    suspend fun bumpRetry(id: String)

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

    @Query("SELECT * FROM orders WHERE outletId = :outletId AND status = 'PAID' AND createdAt >= :sinceEpoch ORDER BY createdAt ASC")
    suspend fun listPaidOrdersSince(outletId: String, sinceEpoch: Long): List<OrderEntity>

    @Query("""
        SELECT oi.menuName, SUM(oi.qty) as totalQty, SUM(oi.qty * oi.unitPrice) as totalRevenue
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.outletId = :outletId AND o.status = 'PAID' AND o.createdAt >= :sinceEpoch
        GROUP BY oi.menuName
        ORDER BY totalQty DESC
        LIMIT 5
    """)
    suspend fun topSellingItemsSince(outletId: String, sinceEpoch: Long): List<TopSellingItem>

    /** Nomor urut order tertinggi untuk shift tertentu (untuk generate #001, #002...) */
    @Query("SELECT IFNULL(MAX(orderSeq),0) FROM orders WHERE shiftId = :shiftId")
    suspend fun maxOrderSeq(shiftId: String): Int

    // ---- Order Items ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY key ASC")
    fun observeOrderItems(orderId: String): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY key ASC")
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>

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

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun shiftById(id: String): ShiftEntity?

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun supplierById(id: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPo(entity: PurchaseOrderEntity)

    // ---- Seed helpers ----
    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun ingredientById(id: String): IngredientEntity?

    @Query("SELECT * FROM recipes WHERE menuId IN (:menuIds)")
    suspend fun recipesForMenus(menuIds: List<String>): List<RecipeEntity>

    // ---- Customers & Loyalty ----
    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun customerByPhone(phone: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(entity: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY totalSpent DESC LIMIT :limit")
    suspend fun topCustomers(limit: Int = 20): List<CustomerEntity>
}

/** Data class untuk menu terlaris di Owner Dashboard. */
data class TopSellingItem(
    val menuName: String,
    val totalQty: Int,
    val totalRevenue: Long,
)

/**
 * Migrasi v4 → v5:
 * - Tambah kolom orderSeq, shiftId, paymentMethod ke tabel orders
 * - Tambah tabel order_items untuk detail KDS
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Tambah kolom baru di tabel orders
        database.execSQL("ALTER TABLE orders ADD COLUMN orderSeq INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE orders ADD COLUMN shiftId TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE orders ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'TUNAI'")

        // Buat tabel order_items baru
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS order_items (
                `key` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `orderId` TEXT NOT NULL,
                `menuName` TEXT NOT NULL,
                `variant` TEXT,
                `qty` INTEGER NOT NULL,
                `unitPrice` INTEGER NOT NULL,
                `notes` TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_orderId ON order_items(orderId)")
    }
}

/**
 * Migrasi v5 → v6:
 * - Tambah kolom customerName dan orderType ke tabel orders
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE orders ADD COLUMN customerName TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE orders ADD COLUMN orderType TEXT NOT NULL DEFAULT 'DINE_IN'")
    }
}

/**
 * Migrasi v6 → v7:
 * - Tambah tabel customers untuk loyalty stempel kopi
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS customers (
                phone TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL DEFAULT '',
                stamps INTEGER NOT NULL DEFAULT 0,
                totalOrders INTEGER NOT NULL DEFAULT 0,
                totalSpent INTEGER NOT NULL DEFAULT 0,
                lastVisit INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

@Database(
    entities = [
        IngredientEntity::class,
        MenuEntity::class,
        RecipeEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PendingMutation::class,
        ShiftEntity::class,
        SupplierEntity::class,
        PurchaseOrderEntity::class,
        PromoEntity::class,
        CustomerEntity::class,
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao
}
