<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\WithFaker;
use Tests\TestCase;
use App\Models\Tenant;
use App\Models\Outlet;
use App\Models\User;
use App\Models\Order;
use Carbon\Carbon;

class DashboardFeatureTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        \Illuminate\Database\Eloquent\Model::unguard();
    }

    public function test_dashboard_returns_analytics_data()
    {
        $tenant = Tenant::create(['name' => 'Cafe Test', 'is_active' => true]);
        $outlet = Outlet::create(['tenant_id' => $tenant->id, 'name' => 'Pusat Test', 'address' => 'Jl. Test 123']);
        $user = User::factory()->create(['tenant_id' => $tenant->id, 'outlet_id' => $outlet->id, 'role' => 'admin']);

        // Order selesai hari ini
        Order::create([
            'tenant_id' => $tenant->id,
            'user_id' => $user->id,
            'outlet_id' => $outlet->id,
            'status' => 'completed',
            'channel' => 'Dine-in',
            'total' => 50000,
            'payment_method' => 'Cash',
            'created_at' => Carbon::now()
        ]);

        // Order selesai kemarin
        Order::create([
            'tenant_id' => $tenant->id,
            'user_id' => $user->id,
            'outlet_id' => $outlet->id,
            'status' => 'completed',
            'channel' => 'Takeaway',
            'total' => 30000,
            'payment_method' => 'QRIS',
            'created_at' => Carbon::now()->subDay()
        ]);

        $response = $this->actingAs($user, 'sanctum')->getJson('/api/dashboard');

        $response->assertStatus(200)
                 ->assertJsonStructure([
                     'todayRevenue',
                     'activeOrders',
                     'chartData'
                 ]);

        $this->assertIsArray($response->json('chartData'));
    }
}
