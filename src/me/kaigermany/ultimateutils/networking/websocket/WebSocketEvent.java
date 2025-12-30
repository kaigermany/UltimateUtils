package me.kaigermany.ultimateutils.networking.websocket;

public abstract class WebSocketEvent {

    public abstract void onOpen(WebSocketBasic socket);

    public abstract void onBinary(byte[] data, WebSocketBasic socket);

    public abstract void onMessage(String data, WebSocketBasic socket);

    public abstract void onClose(WebSocketBasic socket);

    public abstract void onError(WebSocketBasic socket, String message, Exception exception);

}