<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\WithFaker;
use Tests\TestCase;
use App\Models\Tenant;
use App\Models\User;
use App\Models\Category;

class MenuFeatureTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        \Illuminate\Database\Eloquent\Model::unguard();
    }

    public function test_owner_can_fetch_categories()
    {
        $tenant = Tenant::create(['name' => 'Cafe Test', 'is_active' => true]);
        $user = User::factory()->create(['tenant_id' => $tenant->id, 'role' => 'admin']);

        Category::create([
            'tenant_id' => $tenant->id,
            'name' => 'Kopi Panas'
        ]);

        $response = $this->actingAs($user, 'sanctum')->getJson('/api/categories');

        $response->assertStatus(200);
        $this->assertCount(1, $response->json());
        $this->assertEquals('Kopi Panas', $response->json()[0]['name']);
    }

    public function test_owner_can_create_menu()
    {
        $tenant = Tenant::create(['name' => 'Cafe Test', 'is_active' => true]);
        $user = User::factory()->create(['tenant_id' => $tenant->id, 'role' => 'admin']);
        $category = Category::create(['tenant_id' => $tenant->id, 'name' => 'Teh']);

        $response = $this->actingAs($user, 'sanctum')->postJson('/api/menus', [
            'category_id' => $category->id,
            'name' => 'Es Teh Manis',
            'base_price' => 15000,
            'is_active' => true,
            'description' => 'Teh segar'
        ]);

        $response->assertStatus(200);
        $this->assertDatabaseHas('menus', [
            'tenant_id' => $tenant->id,
            'name' => 'Es Teh Manis',
            'base_price' => 15000
        ]);
    }
}
