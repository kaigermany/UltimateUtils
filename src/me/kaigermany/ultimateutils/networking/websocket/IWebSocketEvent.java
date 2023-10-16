package me.kaigermany.ultimateutils.networking.websocket;

public interface IWebSocketEvent {
    void onOpen(WebSocketBasic socket);
    void onBinary(byte[] data, WebSocketBasic socket);

    void onMessage(String data, WebSocketBasic socket);

    void onClose();

    void onError(String message);
}
