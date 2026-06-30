<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Ingredient;
use App\Models\Tenant;

class IngredientSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $tenant = Tenant::first();

        Ingredient::create(['tenant_id' => $tenant->id, 'name' => 'Biji Kopi Arabica', 'unit' => 'gram', 'stock_qty' => 5000, 'min_threshold' => 1000]);
        Ingredient::create(['tenant_id' => $tenant->id, 'name' => 'Susu Segar', 'unit' => 'ml', 'stock_qty' => 10000, 'min_threshold' => 2000]);
        Ingredient::create(['tenant_id' => $tenant->id, 'name' => 'Gula Aren', 'unit' => 'ml', 'stock_qty' => 5000, 'min_threshold' => 1000]);
        Ingredient::create(['tenant_id' => $tenant->id, 'name' => 'Roti Tawar', 'unit' => 'pcs', 'stock_qty' => 100, 'min_threshold' => 20]);
    }
}
