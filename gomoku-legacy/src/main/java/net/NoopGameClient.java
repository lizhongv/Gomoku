package net;

import java.util.ArrayList;
import game.Game;
import game.Manual;

public class NoopGameClient implements GameClient {
    @Override public void onLoggedIn(String myName) {}
    @Override public void onUserList(String name, String state) {}
    @Override public void onUserStateChange(String name, String state) {}
    @Override public void onUserLeft(String name) {}
    @Override public void onInvite(String fromName, int seconds) {}
    @Override public void onInviteRefused(String fromName) {}
    @Override public void onColor(String color, String opponentName) {}
    @Override public void onOpponentMove(int col, int row) {}
    @Override public void onResult(boolean youWon) {}
    @Override public void onChat(String sender, String text) {}
    @Override public void onGameList(ArrayList<Game> games) {}
    @Override public void onManual(Manual manual) {}
    @Override public void onDisconnected() {}
}
