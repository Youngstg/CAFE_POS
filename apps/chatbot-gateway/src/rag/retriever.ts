import axios from 'axios';

const BACKEND_URL = process.env.BACKEND_API_URL ?? 'http://localhost:8000/api/v1';

// Chunk size for RAG retrieval (in characters)
const CHUNK_SIZE = 500;
const TOP_K = 5;

export interface RetrievedChunk {
  id: string;
  content: string;
  score: number;
  source: string;
}

/**
 * RAG Retriever
 * Queries the vector store (pgvector via backend API) for relevant knowledge base chunks.
 * Used to augment LLM context with cafe-specific knowledge.
 */
export class Retriever {
  /**
   * Retrieve top-K relevant chunks for a given query.
   */
  async retrieve(query: string, tenantId: string): Promise<RetrievedChunk[]> {
    try {
      const { data } = await axios.post(
        `${BACKEND_URL}/chatbot/rag/retrieve`,
        { query, top_k: TOP_K },
        { headers: { 'X-Tenant-Id': tenantId } }
      );
      return data.chunks ?? [];
    } catch (error) {
      console.error('[Retriever] Error fetching chunks:', error);
      return [];
    }
  }

  /**
   * Build a context string from retrieved chunks for LLM injection.
   */
  buildContext(chunks: RetrievedChunk[]): string {
    if (chunks.length === 0) return '';

    return chunks
      .map((chunk, i) => `[${i + 1}] ${chunk.content}`)
      .join('\n\n');
  }

  /**
   * Full RAG pipeline: retrieve + build context.
   */
  async augment(query: string, tenantId: string): Promise<string> {
    const chunks = await this.retrieve(query, tenantId);
    return this.buildContext(chunks);
  }
}

export const retriever = new Retriever();
