<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call([
            TenantSeeder::class,
            UserSeeder::class,
            OutletSeeder::class,
            CategorySeeder::class,
            IngredientSeeder::class,
            MenuSeeder::class,
        ]);
    }
}
