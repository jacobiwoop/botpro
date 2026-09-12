const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const MODEL = "openai/gpt-oss-20b";

const SYSTEM_PROMPT =
  "Tu es Aiko (@aikobot), une assistante IA conversationnelle sur Telegram (Botpro). " +
  "Tu es chaleureuse, naturelle, vive d'esprit et sympathique. " +
  "Tu réponds toujours en français de manière fluide, humaine et concise, " +
  "comme dans un vrai chat Telegram. Tu peux utiliser des emojis avec modération.";

export default {
  async fetch(request: Request, env: any): Promise<Response> {
    const GROQ_API_KEY = env.GROQ_API_KEY || "";
    const url = new URL(request.url);

    if (request.method === "GET") {
      return Response.json({
        ok: true,
        bot: "Aiko AI (@aikobot)",
        status: "online",
        webhook_endpoint: "POST /webhook",
      });
    }

    if (request.method === "POST" && url.pathname.endsWith("/webhook")) {
      try {
        const update = (await request.json()) as any;
        const msg = update?.message || {};
        const text = msg?.text || "";
        const fromInfo = msg?.from || {};
        const userName = fromInfo?.first_name || "Ami";
        const chatId = msg?.chat?.id;

        if (!text || !chatId) {
          return Response.json({ ok: true, status: "ignored_empty" });
        }

        // Appel à l'IA Groq
        const groqPayload = {
          model: MODEL,
          messages: [
            { role: "system", content: SYSTEM_PROMPT },
            { role: "user", content: text },
          ],
          max_tokens: 350,
          temperature: 0.7,
        };

        const groqRes = await fetch(GROQ_URL, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${GROQ_API_KEY}`,
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
          },
          body: JSON.stringify(groqPayload),
        });

        let aiReply = "Bonjour ! Je suis Aiko, ravie de te parler !";
        if (groqRes.ok) {
          const groqData = (await groqRes.json()) as any;
          aiReply =
            groqData?.choices?.[0]?.message?.content?.trim() || aiReply;
        } else {
          const errText = await groqRes.text();
          console.error("Groq error:", errText);
          aiReply = `Désolée ${userName}, j'ai eu un petit souci de connexion avec Groq (${groqRes.status}).`;
        }

        // Réponse officielle Telegram via webhook direct
        return Response.json({
          method: "sendMessage",
          chat_id: chatId,
          text: aiReply,
        });
      } catch (err: any) {
        return Response.json({ ok: false, error: err.message }, { status: 500 });
      }
    }

    return new Response("Not found", { status: 404 });
  },
};
