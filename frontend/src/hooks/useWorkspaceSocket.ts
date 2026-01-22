import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { toast } from "sonner";
import { useAuthStore } from "@/stores/auth";
import { useWorkspaceStore } from "@/stores/workspaces";

let stompClient: Client | null = null;

export function useWorkspaceSocket() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const userId = useAuthStore((state) => state.user?.id);
  const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    stompClient?.deactivate();
    const baseUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
    stompClient = new Client({
      webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      reconnectDelay: 5000,
      onStompError: () => {
        toast.error("Realtime connection failed");
      },
      onWebSocketError: () => {
        toast.error("Realtime connection failed");
      }
    });

    stompClient.activate();

    return () => {
      stompClient?.deactivate();
      stompClient = null;
    };
  }, [accessToken]);

  useEffect(() => {
    if (!stompClient || !workspaceId) {
      return;
    }

    let workspaceSubscription: StompSubscription | null = null;
    let userSubscription: StompSubscription | null = null;
    let interval: number | undefined;

    const handleEvent = (message: { type?: string; payload?: any }) => {
      const type = message.type;
      const payload = message.payload ?? {};

      if (type === "issue_created") {
        const isSelf = Boolean(payload.actorId && payload.actorId === userId);
        if (!isSelf) {
          toast.info(payload.title ? `Issue created: ${payload.title}` : "Issue created", {
            id: payload.issueId ? `issue-created-${payload.issueId}` : undefined
          });
        }
        queryClient.invalidateQueries({ queryKey: ["issues", workspaceId] });
        return;
      }

      if (type === "issue_updated") {
        const isSelf = Boolean(payload.actorId && payload.actorId === userId);
        const isAssignmentOnly =
          payload.fields?.length === 1 && payload.fields[0] === "assigneeId";
        if (!isSelf && !isAssignmentOnly) {
          toast.message("Issue updated", {
            id: payload.issueId ? `issue-updated-${payload.issueId}` : undefined
          });
        }
        queryClient.invalidateQueries({ queryKey: ["issues", workspaceId] });
        if (payload.issueId) {
          queryClient.invalidateQueries({ queryKey: ["issue", payload.issueId] });
        }
        return;
      }

      if (type === "comment_added") {
        const isSelf = Boolean(payload.actorId && payload.actorId === userId);
        if (!isSelf) {
          toast.message("New comment added", {
            id: payload.issueId ? `comment-added-${payload.issueId}` : undefined
          });
        }
        if (payload.issueId) {
          queryClient.invalidateQueries({ queryKey: ["issue-comments", payload.issueId] });
        }
        return;
      }

      if (type === "notification_created") {
        if (payload.message) {
          toast.message(payload.message, {
            id: payload.notificationId ? `notification-${payload.notificationId}` : undefined
          });
        }
        queryClient.invalidateQueries({ queryKey: ["notifications"] });
        queryClient.invalidateQueries({ queryKey: ["notifications", "unread"] });
      }
    };

    const subscribe = () => {
      if (!stompClient?.connected) {
        return false;
      }

      workspaceSubscription = stompClient.subscribe(
        `/topic/workspaces/${workspaceId}/events`,
        (frame) => {
          try {
            const data = JSON.parse(frame.body) as { type?: string; payload?: any };
            handleEvent(data);
          } catch {
            // ignore malformed payloads
          }
        }
      );

      if (userId) {
        userSubscription = stompClient.subscribe(`/topic/users/${userId}/events`, (frame) => {
          try {
            const data = JSON.parse(frame.body) as { type?: string; payload?: any };
            handleEvent(data);
          } catch {
            // ignore malformed payloads
          }
        });
      }
      return true;
    };

    if (!subscribe()) {
      interval = window.setInterval(() => {
        if (subscribe() && interval) {
          window.clearInterval(interval);
          interval = undefined;
        }
      }, 500);
    }

    return () => {
      workspaceSubscription?.unsubscribe();
      userSubscription?.unsubscribe();
      if (interval) {
        window.clearInterval(interval);
      }
    };
  }, [workspaceId, queryClient, userId]);
}