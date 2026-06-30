<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Outlet;
use Illuminate\Support\Facades\DB;

class OrderController extends Controller
{
    // Memproses Checkout (Transaksi Keranjang)
    public function checkout(Request $request)
    {
        $request->validate([
            'channel' => 'required|string',
            'payment_method' => 'required|string',
            'items' => 'required|array|min:1',
            'items.*.menu_id' => 'required|exists:menus,id',
            'items.*.qty' => 'required|integer|min:1',
            'items.*.subtotal' => 'required|numeric|min:0',
        ]);

        $tenantId = $request->user()->tenant_id;
        
        // Dapatkan outlet pertama dari tenant ini (karena saat ini 1 tenant bisa punya banyak outlet, kita hardcode ambil outlet pertama untuk versi awal jika tidak dikirim)
        $outlet = Outlet::where('tenant_id', $tenantId)->first();

        if (!$outlet) {
            return response()->json(['message' => 'Outlet tidak ditemukan untuk tenant ini.'], 400);
        }

        try {
            DB::beginTransaction();

            $total = collect($request->items)->sum('subtotal');

            // 1. Buat Order
            $order = Order::create([
                'tenant_id' => $tenantId,
                'user_id' => $request->user()->id,
                'outlet_id' => $outlet->id,
                'status' => 'pending', // Dikirim ke dapur dulu
                'channel' => $request->channel,
                'total' => $total,
                'payment_method' => $request->payment_method,
            ]);

            // 2. Buat Order Items (tanpa potong stok karena stok diinput manual harian)
            foreach ($request->items as $item) {
                OrderItem::create([
                    'tenant_id' => $tenantId,
                    'order_id' => $order->id,
                    'menu_id' => $item['menu_id'],
                    'qty' => $item['qty'],
                    'subtotal' => $item['subtotal'],
                ]);
            }

            DB::commit();

            return response()->json([
                'message' => 'Pesanan berhasil diproses',
                'order_id' => $order->id
            ], 201);

        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json([
                'message' => 'Gagal memproses pesanan',
                'error' => $e->getMessage()
            ], 500);
        }
    }
}
