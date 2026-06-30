import { Request, Response } from 'express';
import { orchestrator } from '../llm/orchestrator';
import { resolveRole } from '../identity/resolveRole';

/**
 * Telegram Webhook Handler
 * Receives updates from Telegram Bot API and routes to LLM orchestrator.
 */
export async function handleTelegramWebhook(req: Request, res: Response): Promise<void> {
  try {
    const update = req.body;
    const message = update.message ?? update.callback_query?.message;

    if (!message) {
      res.sendStatus(200);
      return;
    }

    const chatId = String(message.chat.id);
    const text = message.text ?? '';

    // Resolve user role from identity service
    const identity = await resolveRole('telegram', chatId);

    // Route to LLM orchestrator
    const reply = await orchestrator.process({
      channel: 'telegram',
      userId: chatId,
      role: identity.role,
      tenantId: identity.tenantId,
      message: text,
    });

    // TODO: Send reply via Telegram Bot API
    console.log(`[Telegram] Reply to chatId ${chatId}:`, reply);

    res.sendStatus(200);
  } catch (error) {
    console.error('[Telegram] Error:', error);
    res.sendStatus(500);
  }
}
