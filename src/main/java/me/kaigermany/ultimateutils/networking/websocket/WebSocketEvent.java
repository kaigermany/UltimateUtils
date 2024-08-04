package me.kaigermany.ultimateutils.networking.websocket;

public abstract class WebSocketEvent {

    public void onOpen(WebSocketBasic socket) {}

    public void onBinary(byte[] data, WebSocketBasic socket) {}

    public void onMessage(String data, WebSocketBasic socket) {}

    @Deprecated(forRemoval = true)
    public void onClose() {}
    public void onClose(WebSocketBasic socket) {}

    @Deprecated(forRemoval = true)
    public void onError(String message, Exception exception) {}
    public void onError(WebSocketBasic socket, String message, Exception exception) {}

}