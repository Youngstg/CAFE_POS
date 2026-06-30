<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\Menu;
use App\Models\Order;
use App\Models\OrderItem;
use Illuminate\Support\Facades\DB;

class PublicOrderController extends Controller
{
    // Mengambil katalog menu berdasarkan tenant dan outlet
    public function getMenu($tenantId, $outletId)
    {
        // Dalam kasus nyata, kita pastikan tenant dan outlet ada dan aktif.
        $menus = Menu::with('category')
            ->where('tenant_id', $tenantId)
            ->where('is_active', true)
            ->get();
            
        return response()->json($menus);
    }

    // Pelanggan melakukan pemesanan (Checkout)
    public function checkout(Request $request)
    {
        $request->validate([
            'tenant_id' => 'required|exists:tenants,id',
            'outlet_id' => 'required|exists:outlets,id',
            'table_number' => 'required|string',
            'customer_email' => 'required|email',
            'items' => 'required|array|min:1',
            'items.*.menu_id' => 'required|exists:menus,id',
            'items.*.qty' => 'required|integer|min:1',
            'items.*.subtotal' => 'required|numeric',
        ]);

        try {
            DB::beginTransaction();

            $total = collect($request->items)->sum('subtotal');

            $order = Order::create([
                'tenant_id' => $request->tenant_id,
                'user_id' => null, // Pelanggan anonim (bukan kasir)
                'outlet_id' => $request->outlet_id,
                'status' => 'unpaid', // Harus bayar dulu
                'channel' => 'Dine-in', // Dari meja QR
                'total' => $total,
                'payment_method' => 'QRIS', // Default E-Menu
                'customer_email' => $request->customer_email,
                'table_number' => $request->table_number,
            ]);

            foreach ($request->items as $item) {
                OrderItem::create([
                    'order_id' => $order->id,
                    'menu_id' => $item['menu_id'],
                    'qty' => $item['qty'],
                    'price' => $item['subtotal'] / $item['qty'],
                    'subtotal' => $item['subtotal'],
                ]);
            }

            DB::commit();

            return response()->json([
                'message' => 'Pesanan berhasil dibuat, silakan lakukan pembayaran.',
                'order' => $order
            ], 201);

        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json(['message' => 'Gagal membuat pesanan', 'error' => $e->getMessage()], 500);
        }
    }

    // Menyimulasikan pembayaran sukses
    public function simulatePayment($orderId)
    {
        $order = Order::findOrFail($orderId);

        if ($order->status !== 'unpaid') {
            return response()->json(['message' => 'Pesanan sudah dibayar atau tidak valid'], 400);
        }

        // Ubah status ke pending agar muncul di Layar Dapur KDS
        $order->update([
            'status' => 'pending'
        ]);

        // Catatan: Disini biasanya kita akan mengirim Email struk ke $order->customer_email.
        // Simulasi: "Email dikirim..."

        return response()->json([
            'message' => 'Pembayaran berhasil, pesanan sedang diproses.',
            'order' => $order
        ]);
    }

    // Mendapatkan detail pesanan publik
    public function getOrder($orderId)
    {
        $order = Order::with('items.menu')->findOrFail($orderId);
        return response()->json($order);
    }
}
