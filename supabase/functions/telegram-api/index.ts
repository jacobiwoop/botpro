import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-telegram-bot-api-secret-token",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
  const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
  const supabase = createClient(supabaseUrl, supabaseKey);

  const url = new URL(req.url);
  const path = url.pathname;

  // 1. Route spéciale : Envoi d'un message utilisateur et dispatching automatique vers le Webhook du Bot
  if (path.endsWith("/dispatch-user-message") && req.method === "POST") {
    try {
      const body = await req.json();
      const { chat_id, user_id, text } = body;

      if (!chat_id || !user_id || !text) {
        return Response.json(
          { ok: false, error: "chat_id, user_id, and text are required" },
          { status: 400, headers: corsHeaders }
        );
      }

      // Recherche des membres du chat pour identifier le bot et l'humain
      const { data: memberRows } = await supabase
        .from("chat_members")
        .select("user_id")
        .eq("chat_id", chat_id);

      const memberIds = (memberRows || []).map((m: any) => m.user_id);
      const { data: chatUsers } = await supabase
        .from("users")
        .select("id, first_name, username, is_bot")
        .in("id", memberIds);

      const botUser = chatUsers?.find((u: any) => u.is_bot);
      const humanUser = chatUsers?.find((u: any) => !u.is_bot);

      // L'expéditeur du message doit être l'utilisateur humain
      const actualSenderId = humanUser
        ? humanUser.id
        : user_id === botUser?.id
        ? 3
        : user_id;

      const senderInfo = humanUser || {
        id: actualSenderId,
        first_name: "Desmarc",
        username: "desmarc",
      };

      // Insertion du message de l'utilisateur
      const { data: insertedMsg, error: insertErr } = await supabase
        .from("messages")
        .insert({
          chat_id,
          from_user_id: actualSenderId,
          text,
          is_bot: false,
        })
        .select()
        .single();

      if (insertErr || !insertedMsg) {
        return Response.json(
          { ok: false, error: insertErr?.message },
          { status: 500, headers: corsHeaders }
        );
      }

      let webhookDelivered = false;
      let botReply = null;

      if (botUser) {
        const botId = botUser.id;

        // Récupération du webhook configuré pour ce bot
        const { data: webhook } = await supabase
          .from("webhooks")
          .select("*")
          .eq("bot_id", botId)
          .eq("is_active", true)
          .single();

        if (webhook && webhook.url) {
          // Création de l'update
          const updatePayload = {
            update_id: Date.now(),
            message: {
              message_id: insertedMsg.message_id,
              from: {
                id: senderInfo.id,
                is_bot: false,
                first_name: senderInfo.first_name || "Desmarc",
                username: senderInfo.username || "desmarc",
              },
              chat: {
                id: chat_id,
                type: "private",
              },
              date: Math.floor(new Date(insertedMsg.created_at).getTime() / 1000),
              text: text,
            },
          };

          // Sauvegarde de l'update dans la table updates
          const { data: savedUpdate } = await supabase
            .from("updates")
            .insert({
              bot_id: botId,
              message_id: insertedMsg.id,
              payload: updatePayload,
              is_delivered: false,
            })
            .select()
            .single();

          if (savedUpdate) {
            updatePayload.update_id = Number(savedUpdate.update_id);
          }

          // Appel HTTP vers le Webhook du Bot
          try {
            const hookHeaders: Record<string, string> = {
              "Content-Type": "application/json",
              "User-Agent": "BotproTelegramBotAPI/1.0",
            };
            if (webhook.secret_token) {
              hookHeaders["X-Telegram-Bot-Api-Secret-Token"] = webhook.secret_token;
            }

            const hookRes = await fetch(webhook.url, {
              method: "POST",
              headers: hookHeaders,
              body: JSON.stringify(updatePayload),
            });

            webhookDelivered = hookRes.ok;

            // Mise à jour du statut dans updates
            if (savedUpdate) {
              await supabase
                .from("updates")
                .update({ is_delivered: webhookDelivered })
                .eq("update_id", savedUpdate.update_id);
            }

            // Gestion de la réponse directe officielle Telegram via Webhook
            const hookText = await hookRes.text();
            try {
              const hookJson = JSON.parse(hookText);
              if (
                hookJson &&
                hookJson.method === "sendMessage" &&
                hookJson.text
              ) {
                // Le bot a renvoyé directement sa réponse dans le body HTTP !
                const { data: directReply } = await supabase
                  .from("messages")
                  .insert({
                    chat_id: Number(hookJson.chat_id) || chat_id,
                    from_user_id: botId,
                    text: hookJson.text,
                    reply_to_message_id: hookJson.reply_to_message_id || insertedMsg.message_id,
                    is_bot: true,
                  })
                  .select()
                  .single();

                botReply = directReply;
              }
            } catch {
              // Body pas en JSON ou pas de réponse directe
            }
          } catch (hookErr: any) {
            console.error("Erreur appel webhook:", hookErr.message);
            await supabase
              .from("webhooks")
              .update({
                last_error_date: new Date().toISOString(),
                last_error_message: hookErr.message,
              })
              .eq("bot_id", botId);
          }
        }
      }

      return Response.json(
        {
          ok: true,
          message: insertedMsg,
          webhook_delivered: webhookDelivered,
          direct_reply: botReply,
        },
        { headers: corsHeaders }
      );
    } catch (e: any) {
      return Response.json(
        { ok: false, error: e.message },
        { status: 500, headers: corsHeaders }
      );
    }
  }

  // 2. Parser le chemin Telegram officiel : /bot<token>/<method>
  const match = path.match(/bot([^/]+)\/([^/]+)/);
  if (!match) {
    return Response.json(
      {
        ok: true,
        service: "Botpro Telegram Bot API Gateway",
        runtime: "Supabase Edge Functions (Deno)",
        status: "operational",
        pattern: "POST /functions/v1/telegram-api/bot<token>/<method>",
      },
      { headers: corsHeaders }
    );
  }

  const token = match[1];
  const method = match[2];

  // Validation du Bot Token
  const { data: bot, error: botErr } = await supabase
    .from("bots")
    .select("*")
    .eq("token", token)
    .single();

  if (botErr || !bot) {
    return Response.json(
      {
        ok: false,
        error_code: 401,
        description: "Unauthorized",
        debug_error: botErr?.message,
      },
      { status: 401, headers: corsHeaders }
    );
  }

  const { data: botUser } = await supabase
    .from("users")
    .select("*")
    .eq("id", bot.id)
    .single();

  // 3. Implémentation des méthodes Telegram
  switch (method) {
    case "getMe": {
      return Response.json(
        {
          ok: true,
          result: {
            id: bot.id,
            is_bot: true,
            first_name: botUser?.first_name || "Bot",
            username: botUser?.username || "bot",
            can_join_groups: true,
            can_read_all_group_messages: false,
            supports_inline_queries: false,
          },
        },
        { headers: corsHeaders }
      );
    }

    case "sendMessage": {
      if (req.method !== "POST") {
        return Response.json(
          { ok: false, error_code: 405, description: "Method Not Allowed" },
          { status: 405, headers: corsHeaders }
        );
      }

      let body: any = {};
      try {
        body = await req.json();
      } catch {
        return Response.json(
          { ok: false, error_code: 400, description: "Bad Request: JSON expected" },
          { status: 400, headers: corsHeaders }
        );
      }

      const { chat_id, text, reply_to_message_id, reply_markup } = body;
      if (!chat_id || !text) {
        return Response.json(
          { ok: false, error_code: 400, description: "Bad Request: chat_id and text are required" },
          { status: 400, headers: corsHeaders }
        );
      }

      // Insertion du message du bot
      const { data: msg, error: msgErr } = await supabase
        .from("messages")
        .insert({
          chat_id: Number(chat_id),
          from_user_id: bot.id,
          text: String(text),
          reply_to_message_id: reply_to_message_id ? Number(reply_to_message_id) : null,
          reply_markup: reply_markup || null,
          is_bot: true,
        })
        .select()
        .single();

      if (msgErr || !msg) {
        return Response.json(
          { ok: false, error_code: 500, description: msgErr?.message || "Insert failed" },
          { status: 500, headers: corsHeaders }
        );
      }

      return Response.json(
        {
          ok: true,
          result: {
            message_id: msg.message_id,
            from: {
              id: bot.id,
              is_bot: true,
              first_name: botUser?.first_name,
              username: botUser?.username,
            },
            chat: {
              id: Number(chat_id),
              type: "private",
            },
            date: Math.floor(new Date(msg.created_at).getTime() / 1000),
            text: msg.text,
          },
        },
        { headers: corsHeaders }
      );
    }

    case "setWebhook": {
      if (req.method !== "POST") {
        return Response.json(
          { ok: false, error_code: 405, description: "Method Not Allowed" },
          { status: 405, headers: corsHeaders }
        );
      }

      const body = await req.json().catch(() => ({}));
      const { url, secret_token, max_connections, drop_pending_updates } = body;

      if (!url || typeof url !== "string") {
        return Response.json(
          { ok: false, error_code: 400, description: "Bad Request: URL is required" },
          { status: 400, headers: corsHeaders }
        );
      }

      if (drop_pending_updates) {
        await supabase.from("updates").delete().eq("bot_id", bot.id);
      }

      const { error: hookErr } = await supabase.from("webhooks").upsert(
        {
          bot_id: bot.id,
          url,
          secret_token: secret_token || null,
          max_connections: max_connections ? Number(max_connections) : 40,
          is_active: true,
          last_error_date: null,
          last_error_message: null,
        },
        { onConflict: "bot_id" }
      );

      if (hookErr) {
        return Response.json(
          { ok: false, error_code: 500, description: hookErr.message },
          { status: 500, headers: corsHeaders }
        );
      }

      return Response.json(
        {
          ok: true,
          result: true,
          description: "Webhook was set",
        },
        { headers: corsHeaders }
      );
    }

    case "deleteWebhook": {
      const body = await req.json().catch(() => ({}));
      if (body.drop_pending_updates) {
        await supabase.from("updates").delete().eq("bot_id", bot.id);
      }

      await supabase
        .from("webhooks")
        .update({ is_active: false, url: "" })
        .eq("bot_id", bot.id);

      return Response.json(
        {
          ok: true,
          result: true,
          description: "Webhook was deleted",
        },
        { headers: corsHeaders }
      );
    }

    case "getWebhookInfo": {
      const { data: hook } = await supabase
        .from("webhooks")
        .select("*")
        .eq("bot_id", bot.id)
        .single();

      if (!hook || !hook.is_active || !hook.url) {
        return Response.json(
          {
            ok: true,
            result: {
              url: "",
              has_custom_certificate: false,
              pending_update_count: 0,
            },
          },
          { headers: corsHeaders }
        );
      }

      return Response.json(
        {
          ok: true,
          result: {
            url: hook.url,
            has_custom_certificate: hook.has_custom_certificate || false,
            pending_update_count: hook.pending_update_count || 0,
            max_connections: hook.max_connections || 40,
            ip_address: hook.ip_address || undefined,
            last_error_date: hook.last_error_date
              ? Math.floor(new Date(hook.last_error_date).getTime() / 1000)
              : undefined,
            last_error_message: hook.last_error_message || undefined,
          },
        },
        { headers: corsHeaders }
      );
    }

    case "getUpdates": {
      // Règle Telegram officielle : 409 Conflict si un webhook est actif
      const { data: hook } = await supabase
        .from("webhooks")
        .select("is_active, url")
        .eq("bot_id", bot.id)
        .single();

      if (hook && hook.is_active && hook.url) {
        return Response.json(
          {
            ok: false,
            error_code: 409,
            description:
              "Conflict: can't use getUpdates method while webhook is active; use deleteWebhook to delete the webhook first",
          },
          { status: 409, headers: corsHeaders }
        );
      }

      const { data: pendingUpdates } = await supabase
        .from("updates")
        .select("*")
        .eq("bot_id", bot.id)
        .eq("is_delivered", false)
        .order("update_id", { ascending: true })
        .limit(100);

      const formatted = (pendingUpdates || []).map((u: any) => u.payload);

      return Response.json(
        {
          ok: true,
          result: formatted,
        },
        { headers: corsHeaders }
      );
    }

    default:
      return Response.json(
        {
          ok: false,
          error_code: 404,
          description: `Method '${method}' not implemented yet`,
        },
        { status: 404, headers: corsHeaders }
      );
  }
});
