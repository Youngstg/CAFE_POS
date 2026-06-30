<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Models\KnowledgeBase;

class ChatbotController extends Controller
{
    // Mengambil Pengaturan Chatbot (Token & Prompt)
    public function getSettings(Request $request)
    {
        $tenant = $request->user()->tenant;
        return response()->json([
            'telegram_bot_token' => $tenant->telegram_bot_token,
            'system_prompt' => $tenant->system_prompt,
        ]);
    }

    // Menyimpan Pengaturan Chatbot
    public function updateSettings(Request $request)
    {
        $request->validate([
            'telegram_bot_token' => 'nullable|string',
            'system_prompt' => 'nullable|string',
        ]);

        $tenant = $request->user()->tenant;
        $tenant->update([
            'telegram_bot_token' => $request->telegram_bot_token,
            'system_prompt' => $request->system_prompt,
        ]);

        return response()->json(['message' => 'Pengaturan berhasil disimpan', 'tenant' => $tenant]);
    }

    // Mengambil Daftar Knowledge Base
    public function getKnowledgeBase(Request $request)
    {
        $tenantId = $request->user()->tenant_id;
        $kbs = KnowledgeBase::where('tenant_id', $tenantId)->get();
        return response()->json($kbs);
    }

    // Menambah Knowledge Base Baru
    public function storeKnowledgeBase(Request $request)
    {
        $request->validate([
            'title' => 'required|string',
            'category' => 'required|string',
            'content' => 'required|string',
        ]);

        $kb = KnowledgeBase::create([
            'tenant_id' => $request->user()->tenant_id,
            'title' => $request->title,
            'category' => $request->category,
            'content' => $request->content,
            'is_active' => true,
        ]);

        return response()->json(['message' => 'Knowledge Base berhasil ditambahkan', 'data' => $kb]);
    }
}
