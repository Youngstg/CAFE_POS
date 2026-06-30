<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StoreLeadRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'name'            => 'required|string|max:100',
            'email'           => 'nullable|email|max:255',
            'phone'           => 'nullable|string|max:20',
            'business_type'   => 'nullable|string|max:100',
            'requirement'     => 'nullable|string',
            'budget'          => 'nullable|string|max:100',
            'conversation_id' => 'nullable|integer|exists:conversations,id',
        ];
    }
}
