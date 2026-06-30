<?php

namespace App\Domains\Chatbot\Controllers;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreLeadRequest;
use App\Domains\Chatbot\Services\LeadService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class LeadController extends Controller
{
    public function __construct(protected LeadService $leadService) {}

    public function index(): JsonResponse
    {
        return response()->json(['data' => $this->leadService->getAll()]);
    }

    public function store(StoreLeadRequest $request): JsonResponse
    {
        $lead = $this->leadService->create($request->validated());

        return response()->json([
            'message' => 'Lead berhasil disimpan',
            'data'    => $lead,
        ], 201);
    }

    public function update(Request $request, int $id): JsonResponse
    {
        $request->validate([
            'status' => 'required|in:new,contacted,qualified,closed',
        ]);

        $this->leadService->updateStatus($id, $request->status);

        return response()->json(['message' => 'Status lead berhasil diperbarui']);
    }
}
