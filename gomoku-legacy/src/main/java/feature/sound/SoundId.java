package feature.sound;

public enum SoundId {
    PLACE("place.wav"),
    WIN("win.wav"),
    LOSE("lose.wav"),
    INVITE("invite.wav"),
    EMOTE("emote.wav"),
    CLICK("click.wav");

    public final String fileName;

    SoundId(String f) { this.fileName = f; }
}
