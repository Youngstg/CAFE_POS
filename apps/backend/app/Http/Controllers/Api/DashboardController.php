<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\Order;
use Carbon\Carbon;

class DashboardController extends Controller
{
    public function index(Request $request)
    {
        $tenantId = $request->user()->tenant_id;

        // 1. Hitung Total Pendapatan hari ini
        $todayRevenue = Order::where('tenant_id', $tenantId)
            ->whereDate('created_at', Carbon::today())
            ->where('status', 'completed')
            ->sum('total');

        // 2. Hitung jumlah pesanan aktif hari ini
        $activeOrders = Order::where('tenant_id', $tenantId)
            ->whereDate('created_at', Carbon::today())
            ->where('status', '!=', 'completed')
            ->count();

        // 3. Ambil total pendapatan per hari selama 7 hari terakhir untuk Chart
        $chartData = [];
        for ($i = 6; $i >= 0; $i--) {
            $date = Carbon::today()->subDays($i);
            
            $dailyTotal = Order::where('tenant_id', $tenantId)
                ->whereDate('created_at', $date)
                ->where('status', 'completed')
                ->sum('total');
                
            $chartData[] = [
                'name' => $date->format('D, d M'), // Format nama hari (misal: Mon, 12 Jun)
                'total' => (float) $dailyTotal
            ];
        }

        return response()->json([
            'todayRevenue' => $todayRevenue,
            'activeOrders' => $activeOrders,
            'chartData' => $chartData
        ]);
    }
}
