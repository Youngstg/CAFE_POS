package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.PendingMutation
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.local.PurchaseOrderEntity
import com.coffeeos.erp.core.data.local.SupplierEntity
import com.coffeeos.erp.core.domain.supply.PoCalculation
import com.coffeeos.erp.core.sync.SyncTrigger
import java.util.UUID
import javax.inject.Inject

/** Supplier + Purchase Order: DRAFT (gudang) -> APPROVED (owner) -> RECEIVED (gudang). */
class SupplyRepository @Inject constructor(
    private val dao: PosDao,
    private val sync: SyncTrigger,
) {

    fun observeSuppliers(outletId: String) = dao.observeSuppliers(outletId)
    fun observePos(outletId: String) = dao.observePos(outletId)

    suspend fun addSupplier(outletId: String, name: String, phone: String, address: String): String {
        require(name.isNotBlank()) { "Nama supplier wajib diisi" }
        val id = "SUP-" + UUID.randomUUID().toString().take(6).uppercase()
        dao.upsertSupplier(SupplierEntity(id, outletId, name.trim(), phone.trim(), address.trim()))
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "SUPPLIER_UPSERT", """{"supplierId":"$id"}"""))
        sync.request()
        return id
    }

    suspend fun createDraftPo(
        outletId: String,
        supplierId: String,
        items: List<PoCalculation.PoItem>,
        createdBy: String,
    ): String {
        require(items.isNotEmpty()) { "PO kosong" }
        require(items.all { it.qty > 0 }) { "Qty harus > 0" }
        val id = "PO-" + UUID.randomUUID().toString().take(6).uppercase()
        dao.upsertPo(
            PurchaseOrderEntity(
                id, outletId, supplierId, PoJson.of(items),
                PoCalculation.total(items), "DRAFT", createdBy
            )
        )
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "PO_CREATE", """{"poId":"$id"}"""))
        sync.request()
        return id
    }

    suspend fun approvePo(poId: String) {
        val po = dao.poById(poId) ?: throw IllegalArgumentException("PO tidak ada")
        if (po.status != "DRAFT") throw IllegalStateException("Hanya DRAFT yang bisa di-approve")
        dao.upsertPo(po.copy(status = "APPROVED", pendingSync = true))
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "PO_APPROVE", """{"poId":"$poId"}"""))
        sync.request()
    }
}
