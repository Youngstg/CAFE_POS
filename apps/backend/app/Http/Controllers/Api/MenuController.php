<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\Menu;

class MenuController extends Controller
{
    // Mengambil semua menu dengan relasi kategori
    public function index(Request $request)
    {
        $tenantId = $request->user()->tenant_id;
        $menus = Menu::with('category')
                     ->where('tenant_id', $tenantId)
                     ->orderBy('category_id')
                     ->get();
                     
        return response()->json($menus);
    }

    // Menambah menu baru
    public function store(Request $request)
    {
        $request->validate([
            'category_id' => 'required|exists:categories,id',
            'name' => 'required|string|max:255',
            'base_price' => 'required|numeric|min:0',
            'is_active' => 'boolean',
        ]);

        $menu = Menu::create([
            'tenant_id' => $request->user()->tenant_id,
            'category_id' => $request->category_id,
            'name' => $request->name,
            'base_price' => $request->base_price,
            'is_active' => $request->is_active ?? true,
        ]);

        return response()->json(['message' => 'Menu berhasil ditambahkan', 'data' => $menu->load('category')]);
    }

    // Memperbarui menu
    public function update(Request $request, $id)
    {
        $tenantId = $request->user()->tenant_id;
        $menu = Menu::where('tenant_id', $tenantId)->findOrFail($id);

        $request->validate([
            'category_id' => 'required|exists:categories,id',
            'name' => 'required|string|max:255',
            'base_price' => 'required|numeric|min:0',
            'is_active' => 'boolean',
        ]);

        $menu->update($request->only(['category_id', 'name', 'base_price', 'is_active']));

        return response()->json(['message' => 'Menu berhasil diubah', 'data' => $menu->load('category')]);
    }

    // Menghapus menu
    public function destroy(Request $request, $id)
    {
        $tenantId = $request->user()->tenant_id;
        $menu = Menu::where('tenant_id', $tenantId)->findOrFail($id);
        
        $menu->delete();

        return response()->json(['message' => 'Menu berhasil dihapus']);
    }
}
