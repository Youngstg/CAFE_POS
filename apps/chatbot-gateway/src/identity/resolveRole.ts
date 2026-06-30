import axios from 'axios';

const BACKEND_URL = process.env.BACKEND_API_URL ?? 'http://localhost:8000/api/v1';

export type UserRole = 'customer' | 'cashier' | 'owner' | 'kitchen' | 'guest';

export interface UserIdentity {
  role: UserRole;
  tenantId: string;
  userId?: string;
  name?: string;
}

/**
 * Resolve user role and tenant from channel identifier.
 * Calls the backend API (EnsureChannelIdentity logic).
 *
 * @param channel - 'whatsapp' | 'telegram'
 * @param channelUserId - Phone number (WA) or chat ID (Telegram)
 */
export async function resolveRole(
  channel: 'whatsapp' | 'telegram',
  channelUserId: string
): Promise<UserIdentity> {
  try {
    const { data } = await axios.post(`${BACKEND_URL}/identity/resolve`, {
      channel,
      channel_user_id: channelUserId,
    });

    return {
      role: data.role ?? 'guest',
      tenantId: data.tenant_id,
      userId: data.user_id,
      name: data.name,
    };
  } catch (error) {
    console.warn(`[ResolveRole] Could not resolve identity for ${channel}:${channelUserId}, defaulting to guest`);
    return {
      role: 'guest',
      tenantId: process.env.DEFAULT_TENANT_ID ?? '',
    };
  }
}
