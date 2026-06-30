<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\WithFaker;
use Tests\TestCase;
use App\Models\Tenant;
use App\Models\Outlet;
use App\Models\User;
use App\Models\Order;

class KitchenFeatureTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        \Illuminate\Database\Eloquent\Model::unguard();
    }

    public function test_kitchen_can_fetch_active_orders()
    {
        $tenant = Tenant::create(['name' => 'Cafe Test', 'is_active' => true]);
        $outlet = Outlet::create(['tenant_id' => $tenant->id, 'name' => 'Pusat Test', 'address' => 'Jl. Test 123']);
        $user = User::factory()->create(['tenant_id' => $tenant->id, 'outlet_id' => $outlet->id, 'role' => 'cashier']);

        // Order pending
        Order::create([
            'tenant_id' => $tenant->id,
            'user_id' => $user->id,
            'outlet_id' => $outlet->id,
            'status' => 'pending',
            'channel' => 'Dine-in',
            'total' => 20000,
            'payment_method' => 'Cash'
        ]);

        // Order completed (seharusnya tidak muncul)
        Order::create([
            'tenant_id' => $tenant->id,
            'user_id' => $user->id,
            'outlet_id' => $outlet->id,
            'status' => 'completed',
            'channel' => 'Takeaway',
            'total' => 30000,
            'payment_method' => 'Cash'
        ]);

        $response = $this->actingAs($user, 'sanctum')->getJson('/api/kitchen/orders');

        $response->assertStatus(200);
        $this->assertCount(1, $response->json());
        $this->assertEquals('pending', $response->json()[0]['status']);
    }

    public function test_kitchen_can_update_order_status()
    {
        $tenant = Tenant::create(['name' => 'Cafe Test', 'is_active' => true]);
        $outlet = Outlet::create(['tenant_id' => $tenant->id, 'name' => 'Pusat Test', 'address' => 'Jl. Test 123']);
        $user = User::factory()->create(['tenant_id' => $tenant->id, 'outlet_id' => $outlet->id, 'role' => 'cashier']);

        $order = Order::create([
            'tenant_id' => $tenant->id,
            'user_id' => $user->id,
            'outlet_id' => $outlet->id,
            'status' => 'pending',
            'channel' => 'Dine-in',
            'total' => 20000,
            'payment_method' => 'Cash'
        ]);

        $response = $this->actingAs($user, 'sanctum')->putJson("/api/kitchen/orders/{$order->id}/status", [
            'status' => 'preparing'
        ]);

        $response->assertStatus(200);
        $this->assertDatabaseHas('orders', [
            'id' => $order->id,
            'status' => 'preparing'
        ]);
    }
}
