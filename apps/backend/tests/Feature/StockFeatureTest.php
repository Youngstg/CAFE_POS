<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\WithFaker;
use Tests\TestCase;
use App\Models\Tenant;
use App\Models\Outlet;
use App\Models\User;
use App\Models\Ingredient;

class StockFeatureTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        \Illuminate\Database\Eloquent\Model::unguard();
    }

    public function test_staff_can_submit_daily_stock()
    {
        $tenant = Tenant::create([
            'name' => 'Cafe Test',
            'is_active' => true
        ]);

        $outlet = Outlet::create([
            'tenant_id' => $tenant->id,
            'name' => 'Pusat Test',
            'address' => 'Jl. Test 123'
        ]);

        $user = User::factory()->create([
            'tenant_id' => $tenant->id,
            'outlet_id' => $outlet->id,
            'role' => 'cashier'
        ]);

        $ingredient1 = Ingredient::create([
            'tenant_id' => $tenant->id,
            'name' => 'Susu Segar',
            'unit' => 'ml',
            'stock_qty' => 1000,
            'min_threshold' => 500
        ]);

        $ingredient2 = Ingredient::create([
            'tenant_id' => $tenant->id,
            'name' => 'Biji Kopi Arabica',
            'unit' => 'gram',
            'stock_qty' => 500,
            'min_threshold' => 100
        ]);

        $response = $this->actingAs($user, 'sanctum')->postJson('/api/inventory/daily-input', [
            'stocks' => [
                [
                    'id' => $ingredient1->id,
                    'actual_stock' => 900 // Karyawan hitung sisa 900
                ],
                [
                    'id' => $ingredient2->id,
                    'actual_stock' => 450 // Karyawan hitung sisa 450
                ]
            ]
        ]);

        $response->assertStatus(200);

        $this->assertDatabaseHas('ingredients', [
            'id' => $ingredient1->id,
            'stock_qty' => 900
        ]);

        $this->assertDatabaseHas('ingredients', [
            'id' => $ingredient2->id,
            'stock_qty' => 450
        ]);
    }
}
