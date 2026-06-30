import { Request, Response } from 'express';
import { orchestrator } from '../llm/orchestrator';
import { resolveRole } from '../identity/resolveRole';

/**
 * WhatsApp Webhook Handler
 * Receives messages from WhatsApp Business API and routes to LLM orchestrator.
 */
export async function handleWhatsAppWebhook(req: Request, res: Response): Promise<void> {
  try {
    const { entry } = req.body;

    for (const e of entry ?? []) {
      for (const change of e.changes ?? []) {
        const message = change.value?.messages?.[0];
        if (!message) continue;

        const from = message.from; // WhatsApp phone number
        const text = message.text?.body ?? '';

        // Resolve user role from identity service
        const identity = await resolveRole('whatsapp', from);

        // Route to LLM orchestrator
        const reply = await orchestrator.process({
          channel: 'whatsapp',
          userId: from,
          role: identity.role,
          tenantId: identity.tenantId,
          message: text,
        });

        // TODO: Send reply back via WhatsApp Cloud API
        console.log(`[WhatsApp] Reply to ${from}:`, reply);
      }
    }

    res.sendStatus(200);
  } catch (error) {
    console.error('[WhatsApp] Error:', error);
    res.sendStatus(500);
  }
}

export function verifyWhatsAppWebhook(req: Request, res: Response): void {
  const VERIFY_TOKEN = process.env.WHATSAPP_VERIFY_TOKEN ?? '';
  const mode = req.query['hub.mode'];
  const token = req.query['hub.verify_token'];
  const challenge = req.query['hub.challenge'];

  if (mode === 'subscribe' && token === VERIFY_TOKEN) {
    res.status(200).send(challenge);
  } else {
    res.sendStatus(403);
  }
}
