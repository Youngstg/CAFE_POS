<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Outlet;
use App\Models\Tenant;

class OutletSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $tenant = Tenant::first();

        Outlet::create([
            'tenant_id' => $tenant->id,
            'name' => 'Cabang Jakarta Pusat',
            'address' => 'Jl. Sudirman No. 1, Jakarta Pusat',
        ]);

        Outlet::create([
            'tenant_id' => $tenant->id,
            'name' => 'Cabang Bandung',
            'address' => 'Jl. Braga No. 10, Bandung',
        ]);
    }
}
