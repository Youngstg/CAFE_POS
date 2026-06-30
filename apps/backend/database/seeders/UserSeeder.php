<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\User;
use App\Models\Tenant;

class UserSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $tenant = Tenant::first(); // Only 1 brand now

        // Admin/Owner Brand (Super Admin of the brand)
        User::create([
            'name' => 'Owner CoffeeOS',
            'email' => 'admin@coffeeos.com',
            'password' => bcrypt('password123'),
            'tenant_id' => $tenant->id,
            'is_active' => true,
        ]);

        // Manajer Cabang A
        User::create([
            'name' => 'Manajer Cabang A',
            'email' => 'cabanga@coffeeos.com',
            'password' => bcrypt('password123'),
            'tenant_id' => $tenant->id,
            'is_active' => true,
        ]);

        // Manajer Cabang B
        User::create([
            'name' => 'Manajer Cabang B',
            'email' => 'cabangb@coffeeos.com',
            'password' => bcrypt('password123'),
            'tenant_id' => $tenant->id,
            'is_active' => true,
        ]);
    }
}
