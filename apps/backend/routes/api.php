<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\DashboardController;

// Public routes
Route::post('/login', [AuthController::class, 'login']);

// Public QR Menu & Order
Route::get('/public/menu/{tenantId}/{outletId}', [\App\Http\Controllers\Api\PublicOrderController::class, 'getMenu']);
Route::post('/public/checkout', [\App\Http\Controllers\Api\PublicOrderController::class, 'checkout']);
Route::get('/public/order/{orderId}', [\App\Http\Controllers\Api\PublicOrderController::class, 'getOrder']);
Route::post('/public/pay/{orderId}', [\App\Http\Controllers\Api\PublicOrderController::class, 'simulatePayment']);

// Protected routes
Route::middleware('auth:sanctum')->group(function () {
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::get('/user', [AuthController::class, 'user']);
    Route::get('/dashboard', [DashboardController::class, 'index']);

    // Chatbot Config & KB
    Route::get('/chatbot/settings', [\App\Http\Controllers\Api\ChatbotController::class, 'getSettings']);
    Route::put('/chatbot/settings', [\App\Http\Controllers\Api\ChatbotController::class, 'updateSettings']);
    Route::get('/chatbot/kb', [\App\Http\Controllers\Api\ChatbotController::class, 'getKnowledgeBase']);
    Route::post('/chatbot/kb', [\App\Http\Controllers\Api\ChatbotController::class, 'storeKnowledgeBase']);

    // POS Menu Management
    Route::get('/categories', [\App\Http\Controllers\Api\CategoryController::class, 'index']);
    Route::apiResource('menus', \App\Http\Controllers\Api\MenuController::class)->except(['create', 'edit', 'show']);

    // Kasir / Order
    Route::post('/pos/checkout', [\App\Http\Controllers\Api\OrderController::class, 'checkout']);

    // Staff Management
    Route::get('/outlets', [\App\Http\Controllers\Api\StaffController::class, 'getOutlets']);
    Route::apiResource('staff', \App\Http\Controllers\Api\StaffController::class)->except(['create', 'edit', 'show', 'update']);

    // Kitchen Display
    Route::get('/kitchen/orders', [\App\Http\Controllers\Api\KitchenController::class, 'getActiveOrders']);
    Route::put('/kitchen/orders/{id}/status', [\App\Http\Controllers\Api\KitchenController::class, 'updateOrderStatus']);

    // Inventory & Stock
    Route::get('/inventory/ingredients', [\App\Http\Controllers\Api\StockController::class, 'getIngredients']);
    Route::post('/inventory/daily-input', [\App\Http\Controllers\Api\StockController::class, 'submitDailyStock']);
});
