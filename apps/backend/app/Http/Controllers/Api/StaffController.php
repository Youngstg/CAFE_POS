<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\User;
use App\Models\Outlet;
use Illuminate\Support\Facades\Hash;

class StaffController extends Controller
{
    public function index(Request $request)
    {
        $tenantId = $request->user()->tenant_id;
        
        $staff = User::with('outlet')
            ->where('tenant_id', $tenantId)
            ->where('role', 'cashier')
            ->get();
            
        return response()->json($staff);
    }

    public function store(Request $request)
    {
        $tenantId = $request->user()->tenant_id;
        
        $request->validate([
            'name' => 'required|string|max:255',
            'email' => 'required|string|email|unique:users,email',
            'password' => 'required|string|min:6',
            'outlet_id' => 'required|exists:outlets,id',
        ]);

        $user = User::create([
            'tenant_id' => $tenantId,
            'outlet_id' => $request->outlet_id,
            'name' => $request->name,
            'email' => $request->email,
            'password' => Hash::make($request->password),
            'role' => 'cashier',
        ]);

        return response()->json(['message' => 'Staff berhasil ditambahkan', 'data' => $user->load('outlet')]);
    }

    public function destroy(Request $request, $id)
    {
        $tenantId = $request->user()->tenant_id;
        
        $user = User::where('tenant_id', $tenantId)
            ->where('role', 'cashier')
            ->findOrFail($id);
            
        $user->delete();
        
        return response()->json(['message' => 'Staff berhasil dihapus']);
    }

    // Mengambil daftar outlet untuk dropdown form staff
    public function getOutlets(Request $request)
    {
        $tenantId = $request->user()->tenant_id;
        $outlets = Outlet::where('tenant_id', $tenantId)->get();
        return response()->json($outlets);
    }
}
