import axios from 'axios';

const BACKEND_URL = process.env.BACKEND_API_URL ?? 'http://localhost:8000/api/v1';

interface DispatchContext {
  tenantId: string;
  role: string;
}

/**
 * LLM Tool Dispatcher
 * Maps LLM function-call names to actual backend API calls.
 * Each tool corresponds to an endpoint in apps/backend (ChatbotToolsController).
 */
export class LlmToolDispatcher {
  private readonly tools: Record<string, (args: Record<string, unknown>, ctx: DispatchContext) => Promise<unknown>> = {
    get_menu: this.getMenu.bind(this),
    create_order: this.createOrder.bind(this),
    check_stock: this.checkStock.bind(this),
    get_promo: this.getPromo.bind(this),
    get_shift_summary: this.getShiftSummary.bind(this),
  };

  async dispatch(
    toolName: string,
    args: Record<string, unknown>,
    ctx: DispatchContext
  ): Promise<unknown> {
    const tool = this.tools[toolName];
    if (!tool) {
      throw new Error(`Unknown tool: ${toolName}`);
    }
    console.log(`[Dispatcher] Calling tool: ${toolName}`, args);
    return tool(args, ctx);
  }

  private async getMenu(_args: Record<string, unknown>, ctx: DispatchContext): Promise<unknown> {
    const { data } = await axios.get(`${BACKEND_URL}/chatbot/menu`, {
      headers: { 'X-Tenant-Id': ctx.tenantId },
    });
    return data;
  }

  private async createOrder(args: Record<string, unknown>, ctx: DispatchContext): Promise<unknown> {
    const { data } = await axios.post(`${BACKEND_URL}/chatbot/orders`, args, {
      headers: { 'X-Tenant-Id': ctx.tenantId },
    });
    return data;
  }

  private async checkStock(args: Record<string, unknown>, ctx: DispatchContext): Promise<unknown> {
    const { data } = await axios.get(`${BACKEND_URL}/chatbot/stock`, {
      params: args,
      headers: { 'X-Tenant-Id': ctx.tenantId },
    });
    return data;
  }

  private async getPromo(_args: Record<string, unknown>, ctx: DispatchContext): Promise<unknown> {
    const { data } = await axios.get(`${BACKEND_URL}/chatbot/promos`, {
      headers: { 'X-Tenant-Id': ctx.tenantId },
    });
    return data;
  }

  private async getShiftSummary(_args: Record<string, unknown>, ctx: DispatchContext): Promise<unknown> {
    const { data } = await axios.get(`${BACKEND_URL}/chatbot/shift-summary`, {
      headers: { 'X-Tenant-Id': ctx.tenantId },
    });
    return data;
  }
}
