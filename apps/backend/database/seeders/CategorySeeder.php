<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Category;
use App\Models\Tenant;

class CategorySeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $tenant = Tenant::first();

        Category::create(['tenant_id' => $tenant->id, 'name' => 'Coffee']);
        Category::create(['tenant_id' => $tenant->id, 'name' => 'Non-Coffee']);
        Category::create(['tenant_id' => $tenant->id, 'name' => 'Snack']);
        Category::create(['tenant_id' => $tenant->id, 'name' => 'Susu']);
        Category::create(['tenant_id' => $tenant->id, 'name' => 'Toast']);
    }
}
