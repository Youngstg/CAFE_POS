<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\WithFaker;
use Tests\TestCase;
use App\Models\Tenant;
use App\Models\Outlet;
use App\Models\User;
use App\Models\Menu;

class OrderFeatureTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        \Illuminate\Database\Eloquent\Model::unguard();
    }

    public function test_cashier_can_checkout_order()
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

        $category = \App\Models\Category::create([
            'tenant_id' => $tenant->id,
            'name' => 'Kopi'
        ]);

        $menu = Menu::create([
            'tenant_id' => $tenant->id,
            'category_id' => $category->id,
            'name' => 'Kopi Susu Test',
            'base_price' => 25000,
            'is_active' => true
        ]);

        $response = $this->actingAs($user, 'sanctum')->postJson('/api/pos/checkout', [
            'items' => [
                [
                    'menu_id' => $menu->id,
                    'qty' => 2,
                    'subtotal' => 50000
                ]
            ],
            'channel' => 'Dine-in',
            'payment_method' => 'QRIS'
        ]);

        $response->assertStatus(201)
                 ->assertJsonStructure(['message', 'order_id']);

        $this->assertDatabaseHas('orders', [
            'tenant_id' => $tenant->id,
            'total' => 50000,
            'status' => 'pending'
        ]);

        $this->assertDatabaseHas('order_items', [
            'menu_id' => $menu->id,
            'qty' => 2
        ]);
    }
}
