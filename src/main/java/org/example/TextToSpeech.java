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
        speak(voiceMessage, true);
    }

    public static void speak(String voiceMessage, boolean removeUmlauts) {
        voice.speak(removeUmlauts ? removeUmlauts(voiceMessage) : voiceMessage);
    }

    private static String removeUmlauts(String s) {
        return s.toLowerCase().replaceAll("ä","ae")
                .replaceAll("ö", "oe")
                .replaceAll("ü", "ue")
                .replaceAll("ß", "ss");
    }
}
