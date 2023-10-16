package me.kaigermany.ultimateutils.networking.websocket;

public class WebSocketEvent implements IWebSocketEvent {

    @Override
    public void onOpen(WebSocketBasic socket) {}

    @Override
    public void onBinary(byte[] data, WebSocketBasic socket) {}

    @Override
    public void onMessage(String data, WebSocketBasic socket) {}

    @Override
    public void onClose() {}

    @Override
    public void onError(String message) {}

}