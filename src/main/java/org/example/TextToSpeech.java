package org.example;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class TextToSpeech {
    private static TextToSpeech instance = new TextToSpeech();
    private static VoiceManager voiceManager;
    private static Voice voice;

    public TextToSpeech() {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        voiceManager=VoiceManager.getInstance();
        voice = this.voiceManager.getVoice("kevin16");
        voice.allocate();
    }

    public static void speak(String voiceMessage) {
        voice.speak(voiceMessage);
    }
}
