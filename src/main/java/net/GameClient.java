package net;

import java.util.ArrayList;
import game.Game;
import game.Manual;

public interface GameClient {
    void onLoggedIn(String myName);
    void onUserList(String name, String state);
    void onUserStateChange(String name, String state);
    void onUserLeft(String name);
    void onInvite(String fromName, int seconds);
    void onInviteRefused(String fromName);
    void onColor(String color, String opponentName);
    void onOpponentMove(int col, int row);
    void onResult(boolean youWon);
    void onChat(String sender, String text);
    void onGameList(ArrayList<Game> games);
    void onManual(Manual manual);
    void onDisconnected();
}
