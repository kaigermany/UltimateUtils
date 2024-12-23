package me.kaigermany.ultimateutils.networking.websocket;

public abstract class WebSocketEvent {

    public void onOpen(WebSocketBasic socket) {}

    public void onBinary(byte[] data, WebSocketBasic socket) {}

    public void onMessage(String data, WebSocketBasic socket) {}

    public void onClose(WebSocketBasic socket) {}

    public void onError(WebSocketBasic socket, String message, Exception exception) {}

}