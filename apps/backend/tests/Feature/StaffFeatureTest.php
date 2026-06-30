<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\WithFaker;
use Tests\TestCase;
use App\Models\Tenant;
use App\Models\Outlet;
use App\Models\User;

class StaffFeatureTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        \Illuminate\Database\Eloquent\Model::unguard();
    }

    public function test_owner_can_create_cashier()
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

        // Owner yang login
        $owner = User::factory()->create([
            'tenant_id' => $tenant->id,
            'outlet_id' => $outlet->id,
            'role' => 'admin' // atau super_admin
        ]);

        $response = $this->actingAs($owner, 'sanctum')->postJson('/api/staff', [
            'name' => 'Budi Kasir Baru',
            'email' => 'budi.baru@cafe.com',
            'password' => 'rahasia123',
            'outlet_id' => $outlet->id
        ]);

        $response->assertStatus(200);

        $this->assertDatabaseHas('users', [
            'tenant_id' => $tenant->id,
            'email' => 'budi.baru@cafe.com',
            'role' => 'cashier',
            'outlet_id' => $outlet->id
        ]);
    }
}
