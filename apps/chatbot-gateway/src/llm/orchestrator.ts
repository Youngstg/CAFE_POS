import { LlmToolDispatcher } from './tools/dispatcher';

export interface OrchestratorInput {
  channel: 'whatsapp' | 'telegram';
  userId: string;
  role: 'customer' | 'cashier' | 'owner' | 'kitchen';
  tenantId: string;
  message: string;
}

export interface OrchestratorOutput {
  reply: string;
  toolsUsed?: string[];
}

/**
 * LLM Orchestrator
 * Handles NLU (Natural Language Understanding) + Function Calling pipeline.
 * Routes to appropriate tools based on user intent and role.
 */
class Orchestrator {
  private dispatcher: LlmToolDispatcher;

  constructor() {
    this.dispatcher = new LlmToolDispatcher();
  }

  async process(input: OrchestratorInput): Promise<OrchestratorOutput> {
    const { channel, userId, role, tenantId, message } = input;

    console.log(`[Orchestrator] Processing message from ${channel}:${userId} (role: ${role})`);

    // Step 1: Send to LLM with function-calling tools available
    // TODO: Integrate with Google Gemini / OpenAI function calling
    const llmResponse = await this.callLlm(message, role, tenantId);

    // Step 2: If LLM wants to call a tool/function, dispatch it
    if (llmResponse.toolCall) {
      const toolResult = await this.dispatcher.dispatch(
        llmResponse.toolCall.name,
        llmResponse.toolCall.args,
        { tenantId, role }
      );

      // Step 3: Feed tool result back to LLM for final response
      const finalReply = await this.callLlmWithResult(message, toolResult);
      return { reply: finalReply, toolsUsed: [llmResponse.toolCall.name] };
    }

    return { reply: llmResponse.text ?? 'Maaf, saya tidak mengerti.' };
  }

  private async callLlm(
    message: string,
    role: string,
    tenantId: string
  ): Promise<{ text?: string; toolCall?: { name: string; args: Record<string, unknown> } }> {
    // TODO: implement actual LLM API call (Gemini/OpenAI)
    console.log(`[Orchestrator] Calling LLM for role=${role}, tenant=${tenantId}`);
    return { text: `Echo: ${message}` };
  }

  private async callLlmWithResult(originalMessage: string, toolResult: unknown): Promise<string> {
    // TODO: implement follow-up LLM call with tool result
    return `Hasil: ${JSON.stringify(toolResult)}`;
  }
}

export const orchestrator = new Orchestrator();
