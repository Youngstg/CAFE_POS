package com.coffeeos.erp.core.sync

import com.coffeeos.erp.core.data.local.IngredientEntity
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.local.PromoEntity
import com.coffeeos.erp.core.data.local.PurchaseOrderEntity
import com.coffeeos.erp.core.data.local.RecipeEntity
import com.coffeeos.erp.core.data.local.ShiftEntity
import com.coffeeos.erp.core.data.local.SupplierEntity
import com.coffeeos.erp.core.data.repo.PoJson
import com.coffeeos.erp.core.domain.stock.IngredientStock
import com.coffeeos.erp.core.domain.stock.RecipeRequirement
import com.coffeeos.erp.core.domain.stock.isMenuSellable
import com.coffeeos.erp.core.domain.supply.PoCalculation
import com.coffeeos.erp.core.domain.sync.InboundPolicy
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync INBOUND: Firestore -> Room via snapshot listeners (realtime antar HP).
 * Kebalikan dari FirestoreSync (outbound). Aturan timpa lihat InboundPolicy:
 * - Gema tulisan sendiri (hasPendingWrites) diabaikan.
 * - Baris lokal pendingSync=true menang (tidak ditimpa server).
 * - Ingredient last-write-wins via updatedAt; menu terkait dihitung ulang
 *   (isAvailable turunan, tidak pernah ditimpa mentah dari server).
 * - REMOVED dari server diabaikan (arsip lokal dipertahankan).
 *
 * Dipasang saat login (AuthViewModel), dilepas saat logout.
 */
@Singleton
class RealtimeSync @Inject constructor(private val dao: PosDao) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private var scope: CoroutineScope? = null
    private val regs = mutableListOf<ListenerRegistration>()
    private var startedFor: String? = null

    @Synchronized
    fun start(tenantId: String, outletId: String) {
        if (startedFor == "$tenantId/$outletId") return
        stop()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s
        startedFor = "$tenantId/$outletId"
        fun col(name: String) = db.collection("tenants").document(tenantId)
            .collection("outlets").document(outletId).collection(name)

        regs += col("orders").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch {
                snap.documentChanges
                    .filter { it.type != DocumentChange.Type.REMOVED }
                    .forEach { applyOrder(outletId, it) }
            }
        }
        regs += col("ingredients").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch {
                snap.documentChanges
                    .filter { it.type != DocumentChange.Type.REMOVED }
                    .forEach { applyIngredient(outletId, it) }
            }
        }
        regs += col("purchaseOrders").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch {
                snap.documentChanges
                    .filter { it.type != DocumentChange.Type.REMOVED }
                    .forEach { applyPo(outletId, it) }
            }
        }
        regs += col("shifts").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch {
                snap.documentChanges
                    .filter { it.type != DocumentChange.Type.REMOVED }
                    .forEach { applyShift(outletId, it) }
            }
        }
        regs += col("suppliers").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch {
                snap.documentChanges
                    .filter { it.type != DocumentChange.Type.REMOVED }
                    .forEach { applySupplier(outletId, it) }
            }
        }
        // Katalog: menu (nama/harga saja — availability turunan lokal),
        // resep (BOM), promo. REMOVED menu/bahan/promo juga diterapkan di sini
        // karena penghapusan katalog memang disengaja owner (berbeda dengan
        // arsip order/transaksi yang dipertahankan).
        regs += col("menus").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch { snap.documentChanges.forEach { applyMenu(outletId, it) } }
        }
        regs += col("recipes").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch { snap.documentChanges.forEach { applyRecipe(it) } }
        }
        regs += col("promos").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            s.launch { snap.documentChanges.forEach { applyPromo(outletId, it) } }
        }
    }

    @Synchronized
    fun stop() {
        regs.forEach { runCatching { it.remove() } }
        regs.clear()
        scope?.cancel()
        scope = null
        startedFor = null
    }

    // ---------- appliers ----------

    private suspend fun applyOrder(outletId: String, change: DocumentChange) {
        val doc = change.document
        val local = dao.orderById(doc.id)
        if (local != null && !InboundPolicy.shouldApplyDoc(local.pendingSync)) return
        val status = doc.getString("status") ?: local?.status ?: "QUEUED"
        val total = doc.getLong("total") ?: local?.total ?: 0L
        val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
        dao.upsertOrder(
            OrderEntity(
                id = doc.id,
                outletId = outletId,
                status = status,
                total = total,
                orderSeq = doc.getLong("seq")?.toInt() ?: local?.orderSeq ?: 0,
                shiftId = doc.getString("shiftId") ?: local?.shiftId ?: "",
                paymentMethod = doc.getString("payment") ?: local?.paymentMethod ?: "TUNAI",
                customerName = doc.getString("customer") ?: local?.customerName ?: "",
                orderType = doc.getString("type") ?: local?.orderType ?: "DINE_IN",
                createdAt = createdAt ?: local?.createdAt ?: System.currentTimeMillis(),
                pendingSync = false
            )
        )
    }

    private suspend fun applyIngredient(outletId: String, change: DocumentChange) {
        val doc = change.document
        val local = dao.ingredientById(doc.id)
        val serverUpdatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
        if (!InboundPolicy.shouldApplyVersioned(serverUpdatedAt, local?.updatedAt)) return
        val stock = doc.getDouble("currentStock") ?: local?.currentStock ?: return
        val entity = (local ?: IngredientEntity(
            id = doc.id,
            outletId = outletId,
            name = doc.getString("name") ?: doc.id,
            currentStock = stock,
            maxCapacity = doc.getDouble("maxCapacity"),
            unit = doc.getString("unit") ?: "pcs"
        )).copy(
            currentStock = stock,
            maxCapacity = doc.getDouble("maxCapacity") ?: local?.maxCapacity,
            isLow = doc.getBoolean("isLow") ?: local?.isLow ?: false,
            isStopped = doc.getBoolean("isStopped") ?: local?.isStopped ?: false,
            updatedAt = serverUpdatedAt ?: System.currentTimeMillis()
        )
        dao.upsertIngredient(entity)
        refreshMenusForIngredient(outletId, doc.id)
    }

    private suspend fun applyPo(outletId: String, change: DocumentChange) {
        val doc = change.document
        val local = dao.poById(doc.id)
        if (local != null && !InboundPolicy.shouldApplyDoc(local.pendingSync)) return
        @Suppress("UNCHECKED_CAST")
        val items = (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { m ->
            val id = m["ingredientId"] as? String ?: return@mapNotNull null
            val qty = (m["qty"] as? Number)?.toDouble() ?: return@mapNotNull null
            PoCalculation.PoItem(id, qty, 0L)
        } ?: PoJson.parse(local?.itemsJson ?: "").map { (id, qty) -> PoCalculation.PoItem(id, qty, 0L) }
        dao.upsertPo(
            (local ?: PurchaseOrderEntity(
                id = doc.id,
                outletId = outletId,
                supplierId = doc.getString("supplierId") ?: "",
                itemsJson = "[]",
                total = 0L,
                status = "DRAFT",
                createdBy = ""
            )).copy(
                supplierId = doc.getString("supplierId") ?: local?.supplierId ?: "",
                itemsJson = PoJson.of(items),
                total = doc.getLong("total") ?: local?.total ?: 0L,
                status = doc.getString("status") ?: local?.status ?: "DRAFT",
                pendingSync = false
            )
        )
    }

    private suspend fun applyShift(outletId: String, change: DocumentChange) {
        val doc = change.document
        val local = dao.shiftById(doc.id)
        if (local != null && !InboundPolicy.shouldApplyDoc(local.pendingSync)) return
        dao.upsertShift(
            (local ?: ShiftEntity(
                id = doc.id,
                outletId = outletId,
                openedBy = doc.getString("openedBy") ?: "",
                openedByName = "",
                modalAwal = doc.getLong("modalAwal") ?: 0L
            )).copy(
                paidTotal = doc.getLong("paidTotal") ?: local?.paidTotal ?: 0L,
                countedCash = doc.getLong("countedCash") ?: local?.countedCash,
                difference = doc.getLong("difference") ?: local?.difference,
                isClosed = doc.getBoolean("isClosed") ?: local?.isClosed ?: false,
                pendingSync = false
            )
        )
    }

    private suspend fun applySupplier(outletId: String, change: DocumentChange) {
        val doc = change.document
        val local = dao.supplierById(doc.id)
        dao.upsertSupplier(
            (local ?: SupplierEntity(doc.id, outletId, doc.getString("name") ?: doc.id)).copy(
                name = doc.getString("name") ?: local?.name ?: doc.id,
                phone = doc.getString("phone") ?: local?.phone ?: "",
                address = doc.getString("address") ?: local?.address ?: ""
            )
        )
    }

    private suspend fun applyMenu(outletId: String, change: DocumentChange) {
        val doc = change.document
        if (change.type == DocumentChange.Type.REMOVED) {
            dao.deleteRecipesForMenu(doc.id)
            dao.deleteMenu(doc.id)
            return
        }
        val local = dao.listMenus(outletId).firstOrNull { it.id == doc.id }
        // isAvailable TIDAK ditimpa dari server (turunan stok lokal).
        dao.upsertMenu(
            (local ?: MenuEntity(doc.id, outletId, doc.getString("name") ?: doc.id, 0)).copy(
                name = doc.getString("name") ?: local?.name ?: doc.id,
                price = doc.getLong("price") ?: local?.price ?: 0L,
                category = doc.getString("category") ?: local?.category ?: "Umum"
            )
        )
    }

    private suspend fun applyRecipe(change: DocumentChange) {
        val doc = change.document
        val menuId = doc.getString("menuId") ?: return
        val ingredientId = doc.getString("ingredientId") ?: return
        if (change.type == DocumentChange.Type.REMOVED) {
            dao.deleteRecipe(menuId, ingredientId)
            return
        }
        val qty = doc.getDouble("qtyPerPortion") ?: return
        dao.upsertRecipe(RecipeEntity(menuId = menuId, ingredientId = ingredientId, qtyPerPortion = qty))
    }

    private suspend fun applyPromo(outletId: String, change: DocumentChange) {
        val doc = change.document
        if (change.type == DocumentChange.Type.REMOVED) {
            dao.deletePromo(doc.id)
            return
        }
        val local = dao.promoById(doc.id)
        if (local != null && !InboundPolicy.shouldApplyDoc(local.pendingSync)) return
        dao.upsertPromo(
            (local ?: PromoEntity(doc.id, outletId, doc.getString("name") ?: doc.id)).copy(
                name = doc.getString("name") ?: local?.name ?: doc.id,
                percentOff = doc.getLong("percentOff")?.toInt() ?: local?.percentOff ?: 0,
                fixedDiscount = doc.getLong("fixedDiscount") ?: local?.fixedDiscount ?: 0L,
                minOrder = doc.getLong("minOrder") ?: local?.minOrder ?: 0L,
                active = doc.getBoolean("active") ?: local?.active ?: true,
                pendingSync = false
            )
        )
    }

    /** Hitung ulang availability menu yang memakai bahan berubah (turunan, bukan timpa). */
    private suspend fun refreshMenusForIngredient(outletId: String, ingredientId: String) {
        val recipes = dao.recipesForIngredient(ingredientId)
        if (recipes.isEmpty()) return
        val ingredients = dao.listIngredients(outletId)
        val stocks = ingredients.associate { e ->
            e.id to IngredientStock(e.id, e.name, e.currentStock, e.maxCapacity, e.unit)
        }
        val menus: Map<String, MenuEntity> = dao.listMenus(outletId).associateBy { it.id }
        recipes.map { it.menuId }.distinct().forEach { menuId ->
            val menu = menus[menuId] ?: return@forEach
            val reqs = dao.recipesForMenu(menuId)
                .map { RecipeRequirement(it.ingredientId, it.qtyPerPortion) }
            val sellable = if (reqs.isEmpty()) true else isMenuSellable(reqs, stocks)
            if (menu.isAvailable != sellable) dao.upsertMenu(menu.copy(isAvailable = sellable))
        }
    }
}

