<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\Order;

class KitchenController extends Controller
{
    // Ambil pesanan yang belum selesai
    public function getActiveOrders(Request $request)
    {
        $tenantId = $request->user()->tenant_id;
        
        $orders = Order::with('items.menu')
            ->where('tenant_id', $tenantId)
            ->whereIn('status', ['pending', 'preparing'])
            ->orderBy('created_at', 'asc')
            ->get();
            
        return response()->json($orders);
    }

    // Ubah status pesanan
    public function updateOrderStatus(Request $request, $id)
    {
        $request->validate([
            'status' => 'required|in:preparing,ready,completed'
        ]);

        $tenantId = $request->user()->tenant_id;
        
        $order = Order::where('tenant_id', $tenantId)->findOrFail($id);
        $order->update(['status' => $request->status]);

        return response()->json(['message' => 'Status pesanan diperbarui', 'data' => $order]);
    }
}
