<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\Ingredient;
use Illuminate\Support\Facades\DB;

class StockController extends Controller
{
    // Mengambil semua bahan baku
    public function getIngredients(Request $request)
    {
        $tenantId = $request->user()->tenant_id;
        
        $ingredients = Ingredient::where('tenant_id', $tenantId)
            ->orderBy('name', 'asc')
            ->get();
            
        return response()->json($ingredients);
    }

    // Menerima input stok massal dari karyawan (Stock Opname)
    public function submitDailyStock(Request $request)
    {
        $request->validate([
            'stocks' => 'required|array',
            'stocks.*.id' => 'required|exists:ingredients,id',
            'stocks.*.actual_stock' => 'required|numeric|min:0',
        ]);

        $tenantId = $request->user()->tenant_id;

        DB::beginTransaction();
        try {
            foreach ($request->stocks as $item) {
                // Pastikan ingredient ini milik tenant yang login
                $ingredient = Ingredient::where('tenant_id', $tenantId)->find($item['id']);
                
                if ($ingredient) {
                    // Timpa nilai stock_qty dengan hasil hitungan fisik karyawan
                    $ingredient->update([
                        'stock_qty' => $item['actual_stock']
                    ]);
                }
            }
            DB::commit();
            return response()->json(['message' => 'Laporan stok harian berhasil disimpan.']);
            
        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json(['message' => 'Gagal menyimpan stok.', 'error' => $e->getMessage()], 500);
        }
    }
}
