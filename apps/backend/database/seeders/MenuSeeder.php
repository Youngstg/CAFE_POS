<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Menu;
use App\Models\Category;
use App\Models\Tenant;

class MenuSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $tenant = Tenant::first();
        $catCoffee = Category::where('tenant_id', $tenant->id)->where('name', 'Coffee')->first();
        
        Menu::create([
            'tenant_id' => $tenant->id,
            'category_id' => $catCoffee->id,
            'name' => 'Kopi Gula Aren',
            'base_price' => 18000,
            'is_active' => true,
        ]);

        Menu::create([
            'tenant_id' => $tenant->id,
            'category_id' => $catCoffee->id,
            'name' => 'Americano',
            'base_price' => 15000,
            'is_active' => true,
        ]);
    }
}
